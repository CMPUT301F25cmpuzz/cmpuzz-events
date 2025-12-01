package com.example.cmpuzz_events.service;

import android.util.Log;

import androidx.annotation.NonNull;

import com.example.cmpuzz_events.models.user.User;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminService implements IAdminService {
    private static final String TAG = "UserService";
    private static final String COLLECTION_USERS = "users";
    private static AdminService instance;
    private final FirebaseFirestore db;
    private final FirebaseAuth auth = FirebaseAuth.getInstance();

    public AdminService() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized AdminService getInstance()
    {
        if (instance == null)
        {
            instance = new AdminService();
        }
        return instance;
    }

    public void getAllAccountsByRole(User.UserRole role, UIAccountListCallback callback)
    {
        db.collection(COLLECTION_USERS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> accountList = new ArrayList<>();
                    for(QueryDocumentSnapshot doc : queryDocumentSnapshots)
                    {
                        User user = documentToUser(doc);
                        if(user.getRole() == role)
                        {
                            accountList.add(user);
                        }
                    }
                    Log.d(TAG, "Retrieved " + accountList.size() + " users!");
                    callback.onSuccess(accountList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error retreiving users", e);
                    callback.onError(e.getMessage());
                });
    }

    private User documentToUser(DocumentSnapshot doc)
    {
        User user = new User();
        user.setUid(doc.getString("uid"));
        user.setEmail(doc.getString("email"));
        user.setUsername(doc.getString("username"));
        user.setRole(User.UserRole.fromString(doc.getString("role")));
        user.setDisplayName(doc.getString("username"));
        user.setCreatedAt(doc.getLong("createdAt"));
        
        String profileImageUrl = doc.getString("profileImageUrl");
        if (profileImageUrl != null) {
            user.setProfileImageUrl(profileImageUrl);
        }

        return user;
    }

//    private User getUserByUid(String uid)
//    {
//
//    }

    /**
     * Deletes the user account completely:
     * 1. Deletes all events created by the user (if they are an organizer)
     * 2. Removes user from all event lists (waitlist, declined, attendees, invitations)
     * 3. Deletes user document from Firestore
     * 4. Deletes the Firebase Auth user (including email)
     */
    public Task<Void> deleteAccountByUid(@NonNull String uid) {
        FirebaseUser fu = auth.getCurrentUser();
        if (fu == null) {
            return Tasks.forException(new IllegalStateException("Not signed in"));
        }

        // First, delete all events created by this organizer
        return deleteEventsCreatedByOrganizer(uid)
            .onSuccessTask(v -> {
                // Then remove user from all event lists
                return removeUserFromAllEvents(uid);
            })
            .onSuccessTask(v -> {
                // Finally delete user document from Firestore
                return db.collection("users").document(uid).delete();
            });
    }

    /**
     * Deletes all events created by the specified organizer.
     * This is called when an organizer account is being deleted.
     */
    private Task<Void> deleteEventsCreatedByOrganizer(String uid) {
        return db.collection("events")
                .whereEqualTo("organizerId", uid)
                .get()
                .onSuccessTask(querySnapshot -> {
                    List<Task<Void>> deleteTasks = new ArrayList<>();
                    
                    for (DocumentSnapshot doc : querySnapshot.getDocuments()) {
                        Log.d(TAG, "Deleting event created by organizer: " + doc.getId());
                        Task<Void> deleteTask = doc.getReference().delete();
                        deleteTasks.add(deleteTask);
                    }
                    
                    if (deleteTasks.isEmpty()) {
                        Log.d(TAG, "No events found for organizer: " + uid);
                        return Tasks.forResult(null);
                    }
                    
                    Log.d(TAG, "Deleting " + deleteTasks.size() + " events created by organizer");
                    return Tasks.whenAll(deleteTasks);
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
