package com.example.cmpuzz_events.service;

import androidx.annotation.NonNull;

import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.HashMap;
import java.util.Map;

public class ProfileService {

    private final FirebaseAuth auth = FirebaseAuth.getInstance();
    private final FirebaseFirestore db = FirebaseFirestore.getInstance();

    /**
     * Updates profile:
     *  - Firestore: displayName, username, updatedAt
     *  - Auth: displayName (optional; if you keep one there)
     *  - Auth: email (only if changed)
     *  - Firestore: email (only if Auth email update succeeded)
     */
    public Task<Void> updateProfile(@NonNull String uid,
                                    String fullName,
                                    String username,
                                    String newEmail) {

        FirebaseUser fu = auth.getCurrentUser();
        if (fu == null) {
            return Tasks.forException(new IllegalStateException("Not signed in"));
        }

        // 1) Base Firestore update (name/username + updatedAt)
        Map<String, Object> base = new HashMap<>();
        if (fullName != null && !fullName.isBlank()) {
            base.put("displayName", fullName);
        }
        if (username != null && !username.isBlank()) {
            base.put("username", username);
        }
        base.put("updatedAt", FieldValue.serverTimestamp());

        Task<Void> baseWrite = db.collection("users").document(uid)
                .set(base, SetOptions.merge());

        // 2) Email update only if different
        boolean emailChanged = false;
        if (newEmail != null && !newEmail.isBlank()) {
            String cur = fu.getEmail();
            emailChanged = (cur == null) || !cur.equalsIgnoreCase(newEmail);
        }

        if (!emailChanged) {
            // No email change requested: just return the base write
            return baseWrite;
        }

        // 3) Try updating Auth email, then sync Firestore.email if it worked
        return baseWrite
                .onSuccessTask(v -> fu.updateEmail(newEmail))
                .onSuccessTask(v -> {
                    Map<String, Object> up = new HashMap<>();
                    up.put("email", newEmail);
                    up.put("updatedAt", FieldValue.serverTimestamp());
                    return db.collection("users").document(uid).set(up, SetOptions.merge());
                })
                .addOnFailureListener(e -> {
                    // Let caller decide whether to re-auth or show message
                });
    }

    /** Utility: detect Firebase's "requires recent login" */
    public static boolean isRecentLoginRequired(Exception e) {
        if (e == null) return false;
        if (e instanceof FirebaseAuthRecentLoginRequiredException) return true;
        String msg = e.getMessage();
        return msg != null && msg.toLowerCase().contains("recent")
                && msg.toLowerCase().contains("login");
    }

    /** For re-auth flow: only update Auth email (no Firestore). */
    public Task<Void> updateAuthEmailOnly(@NonNull String newEmail) {
        FirebaseUser fu = auth.getCurrentUser();
        if (fu == null) return Tasks.forException(new IllegalStateException("Not signed in"));
        String cur = fu.getEmail();
        if (cur != null && cur.equalsIgnoreCase(newEmail)) {
            return Tasks.forResult(null);
        }
        return fu.updateEmail(newEmail);
    }

    /**
     * Deletes the user account completely:
     * 1. Deletes user document from Firestore
     * 2. Deletes the Firebase Auth user (including email)
     */
    public Task<Void> deleteAccount(@NonNull String uid) {
        FirebaseUser fu = auth.getCurrentUser();
        if (fu == null) {
            return Tasks.forException(new IllegalStateException("Not signed in"));
        }
        
        // First delete from Firestore
        return db.collection("users").document(uid)
                .delete()
                .onSuccessTask(v -> {
                    // Then delete from Firebase Auth (this removes email and everything)
                    return fu.delete();
                });
    }
}
