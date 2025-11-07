package com.example.cmpuzz_events.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.cmpuzz_events.models.event.Invitation;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FieldValue;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class ProfileService {

    private static final String TAG = "ProfileService";
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
     * 1. Removes user from all event lists (waitlist, declined, attendees, invitations)
     * 2. Deletes user document from Firestore
     * 3. Deletes the Firebase Auth user (including email)
     */
    public Task<Void> deleteAccount(@NonNull String uid) {
        FirebaseUser fu = auth.getCurrentUser();
        if (fu == null) {
            return Tasks.forException(new IllegalStateException("Not signed in"));
        }
        
        // First, remove user from all event lists
        return removeUserFromAllEvents(uid)
                .onSuccessTask(v -> {
                    // Then delete user document from Firestore
                    return db.collection("users").document(uid).delete();
                })
                .onSuccessTask(v -> {
                    // Finally delete from Firebase Auth (this removes email and everything)
                    return fu.delete();
                });
    }
    
    /**
     * Removes the user from all event lists they're in:
     * - waitlist
     * - declined
     * - attendees
     * - invitations
     */
    private Task<Void> removeUserFromAllEvents(String uid) {
        return db.collection("events")
                .get()
                .onSuccessTask(querySnapshot -> {
                    List<Task<Void>> updateTasks = new ArrayList<>();
                    
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        boolean needsUpdate = false;
                        Map<String, Object> updates = new HashMap<>();
                        
                        // Check and remove from waitlist
                        List<String> waitlist = (List<String>) doc.get("waitlist");
                        if (waitlist != null && waitlist.contains(uid)) {
                            waitlist = new ArrayList<>(waitlist);
                            waitlist.remove(uid);
                            updates.put("waitlist", waitlist);
                            needsUpdate = true;
                            Log.d(TAG, "Removing user from waitlist of event: " + doc.getId());
                        }
                        
                        // Check and remove from declined
                        List<String> declined = (List<String>) doc.get("declined");
                        if (declined != null && declined.contains(uid)) {
                            declined = new ArrayList<>(declined);
                            declined.remove(uid);
                            updates.put("declined", declined);
                            needsUpdate = true;
                            Log.d(TAG, "Removing user from declined of event: " + doc.getId());
                        }
                        
                        // Check and remove from attendees
                        List<String> attendees = (List<String>) doc.get("attendees");
                        if (attendees != null && attendees.contains(uid)) {
                            attendees = new ArrayList<>(attendees);
                            attendees.remove(uid);
                            updates.put("attendees", attendees);
                            needsUpdate = true;
                            Log.d(TAG, "Removing user from attendees of event: " + doc.getId());
                        }
                        
                        // Check and remove from invitations
                        List<Map<String, Object>> invitations = (List<Map<String, Object>>) doc.get("invitations");
                        if (invitations != null) {
                            List<Map<String, Object>> updatedInvitations = new ArrayList<>();
                            boolean foundInInvitations = false;
                            
                            for (Map<String, Object> invMap : invitations) {
                                String invUserId = (String) invMap.get("userId");
                                if (!uid.equals(invUserId)) {
                                    updatedInvitations.add(invMap);
                                } else {
                                    foundInInvitations = true;
                                }
                            }
                            
                            if (foundInInvitations) {
                                updates.put("invitations", updatedInvitations);
                                needsUpdate = true;
                                Log.d(TAG, "Removing user from invitations of event: " + doc.getId());
                            }
                        }
                        
                        // If any list contained the user, update the document
                        if (needsUpdate) {
                            Task<Void> updateTask = doc.getReference().update(updates);
                            updateTasks.add(updateTask);
                        }
                    }
                    
                    // Wait for all updates to complete
                    if (updateTasks.isEmpty()) {
                        Log.d(TAG, "User not found in any event lists");
                        return Tasks.forResult(null);
                    }
                    
                    Log.d(TAG, "Updating " + updateTasks.size() + " events to remove user");
                    return Tasks.whenAll(updateTasks);
                });
    }
}
