package com.example.cmpuzz_events.service;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

/**
 * ProfileService
 *
 * Writes/reads user profile under: users/{uid}
 * - Firestore = source of truth for profile fields in UI.
 * - FirebaseAuth displayName is kept in sync when full name changes.
 * - FirebaseAuth email update is attempted (may require recent login).
 */
public class ProfileService {

    private final FirebaseFirestore db = FirebaseFirestore.getInstance();
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    /**
     * Update any subset of profile fields. Null/blank values are ignored.
     * Writes are merged into users/{uid}. Also attempts to sync FirebaseAuth:
     *  - displayName ← fullName
     *  - email ← email (can fail with REQUIRES_RECENT_LOGIN)
     *
     * @param uid       target user id
     * @param fullName  new full name (display name)
     * @param username  new username
     * @param email     new email
     */
    public Task<Void> updateProfile(String uid, String fullName, String username, String email) {
        Map<String, Object> updates = new HashMap<>();
        if (!isBlank(fullName)) updates.put("displayName", fullName.trim());
        if (!isBlank(username)) updates.put("username", username.trim());
        if (!isBlank(email))    updates.put("email", email.trim());
        updates.put("updatedAt", FieldValue.serverTimestamp());

        // 1) Firestore merge
        Task<Void> firestoreWrite = db.collection("users")
                .document(uid)
                .set(updates, SetOptions.merge());

        // 2) Sync Auth displayName (safe)
        Task<Void> authDisplay = Tasks.forResult(null);
        if (auth.getCurrentUser() != null
                && uid.equals(auth.getCurrentUser().getUid())
                && !isBlank(fullName)) {
            UserProfileChangeRequest req = new UserProfileChangeRequest.Builder()
                    .setDisplayName(fullName.trim())
                    .build();
            authDisplay = auth.getCurrentUser().updateProfile(req);
        }

        // 3) Try Auth email update (may require recent login)
        Task<Void> authEmail = Tasks.forResult(null);
        if (auth.getCurrentUser() != null
                && uid.equals(auth.getCurrentUser().getUid())
                && !isBlank(email)) {
            String newEmail = email.trim();
            String currentEmail = auth.getCurrentUser().getEmail();
            if (currentEmail == null || !currentEmail.equalsIgnoreCase(newEmail)) {
                authEmail = auth.getCurrentUser().updateEmail(newEmail);
                // If this throws FirebaseAuthRecentLoginRequiredException, handle in UI with re-auth then retry.
            }
        }

        return Tasks.whenAll(firestoreWrite, authDisplay, authEmail);
    }

    /**
     * Retry only the Auth email update (useful after a successful re-auth).
     * Also useful if you choose to separate Auth email change from Firestore merge.
     */
    public Task<Void> updateAuthEmailOnly(String newEmail) {
        if (auth.getCurrentUser() == null) return Tasks.forResult(null);
        if (isBlank(newEmail)) return Tasks.forResult(null);
        String current = auth.getCurrentUser().getEmail();
        if (current != null && current.equalsIgnoreCase(newEmail.trim())) return Tasks.forResult(null);
        return auth.getCurrentUser().updateEmail(newEmail.trim());
    }

    /**
     * Convenience: detect if an exception is the "recent login required" case.
     */
    public static boolean isRecentLoginRequired(Exception e) {
        if (e == null) return false;
        // Walk causes to see the specific Firebase exception
        Throwable t = e;
        while (t != null) {
            if (t instanceof com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException) {
                return true;
            }
            t = t.getCause();
        }
        // Fallback on message text if class was wrapped
        String msg = String.valueOf(e.getMessage()).toLowerCase();
        return msg.contains("recent") && msg.contains("login");
    }

    /**
     * One-shot profile fetch, useful for prefilling the edit dialog.
     */
    public Task<Map<String, Object>> getProfile(String uid) {
        return db.collection("users").document(uid).get()
                .continueWith(task -> {
                    if (!task.isSuccessful() || task.getResult() == null || !task.getResult().exists()) {
                        return new HashMap<String, Object>();
                    }
                    return task.getResult().getData();
                });
    }

    /**
     * Optional helper: email/password re-auth (call from UI before retrying email update).
     */
    public Task<Void> reauthenticateWithPassword(String email, String password) {
        if (auth.getCurrentUser() == null) return Tasks.forResult(null);
        return auth.getCurrentUser()
                .reauthenticate(EmailAuthProvider.getCredential(email, password))
                .continueWithTask(t -> Tasks.forResult(null));
    }

    private static boolean isBlank(String s) {
        return s == null || s.trim().isEmpty();
    }
}
