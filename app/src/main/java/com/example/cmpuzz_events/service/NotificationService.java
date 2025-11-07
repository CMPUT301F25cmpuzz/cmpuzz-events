package com.example.cmpuzz_events.service;

import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;

import com.example.cmpuzz_events.models.notification.Notification;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NotificationService implements INotificationService {
    
    private static final String TAG = "NotificationService";
    private static final String COLLECTION_NOTIFICATIONS = "notifications";
    
    private final FirebaseFirestore db;
    private static NotificationService instance;
    private Context context;
    
    private NotificationService() {
        this.db = FirebaseFirestore.getInstance();
    }
    
    public void setContext(Context context) {
        this.context = context;
    }
    
    public static NotificationService getInstance() {
        if (instance == null) {
            instance = new NotificationService();
        }
        return instance;
    }
    
    @Override
    public void sendNotification(Notification notification, VoidCallback callback) {
        Map<String, Object> data = new HashMap<>();
        data.put("userId", notification.getUserId());
        data.put("eventId", notification.getEventId());
        data.put("eventName", notification.getEventName());
        data.put("type", notification.getTypeString());
        data.put("title", notification.getTitle());
        data.put("message", notification.getMessage());
        data.put("timestamp", notification.getTimestamp());
        data.put("isRead", notification.isRead());
        
        db.collection(COLLECTION_NOTIFICATIONS)
            .add(data)
            .addOnSuccessListener(documentReference -> {
                Log.d(TAG, "Notification sent: " + documentReference.getId());
                if (callback != null) {
                    callback.onSuccess();
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error sending notification", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            });
    }
    
    @Override
    public void sendNotificationsToUsers(List<String> userIds, String eventId, String eventName, 
                                        Notification.NotificationType type, VoidCallback callback) {
        if (userIds == null || userIds.isEmpty()) {
            if (callback != null) {
                callback.onError("No users to notify");
            }
            return;
        }
        
        // Filter users based on notification preferences from Firestore
        filterUsersWithNotificationsEnabledFromFirestore(userIds, enabledUsers -> {
            if (enabledUsers.isEmpty()) {
                Log.d(TAG, "No users have notifications enabled");
                if (callback != null) {
                    callback.onSuccess(); // Consider it success but no notifications sent
                }
                return;
            }
            
            Log.d(TAG, "Sending notifications to " + enabledUsers.size() + "/" + userIds.size() + " users");
            
            String title = generateTitle(type);
            String message = generateMessage(type, eventName);
            
            int[] successCount = {0};
            int[] errorCount = {0};
            int totalUsers = enabledUsers.size();
            
            for (String userId : enabledUsers) {
                Notification notification = new Notification(userId, eventId, eventName, type, title, message);
                
                sendNotification(notification, new VoidCallback() {
                    @Override
                    public void onSuccess() {
                        successCount[0]++;
                        checkCompletion();
                    }
                    
                    @Override
                    public void onError(String error) {
                        errorCount[0]++;
                        checkCompletion();
                    }
                    
                    private void checkCompletion() {
                        if (successCount[0] + errorCount[0] == totalUsers) {
                            if (callback != null) {
                                if (errorCount[0] == 0) {
                                    callback.onSuccess();
                                } else {
                                    callback.onError(errorCount[0] + " notifications failed");
                                }
                            }
                        }
                    }
                });
            }
        });
    }
    
    @Override
    public void notifyOrganizerOfResponse(String organizerId, String userName, String eventId, 
                                         String eventName, boolean accepted, VoidCallback callback) {
        // Check if organizer has notifications enabled in Firestore
        db.collection("users").document(organizerId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                boolean notificationsEnabled = documentSnapshot.getBoolean("notificationsEnabled") != null 
                    ? documentSnapshot.getBoolean("notificationsEnabled") : true;
                
                if (!notificationsEnabled) {
                    Log.d(TAG, "Organizer has notifications disabled, skipping");
                    if (callback != null) {
                        callback.onSuccess();
                    }
                    return;
                }
                
                String title = accepted ? "Invitation Accepted" : "Invitation Declined";
                String message = userName + " has " + (accepted ? "accepted" : "declined") + 
                                " the invitation to \"" + eventName + "\"";
                
                Notification.NotificationType type = accepted ? 
                                                    Notification.NotificationType.ACCEPTED : 
                                                    Notification.NotificationType.DECLINED;
                
                Notification notification = new Notification(organizerId, eventId, eventName, type, title, message);
                sendNotification(notification, callback);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error checking organizer notification preference, sending anyway", e);
                // Send notification on error (fail-safe)
                String title = accepted ? "Invitation Accepted" : "Invitation Declined";
                String message = userName + " has " + (accepted ? "accepted" : "declined") + 
                                " the invitation to \"" + eventName + "\"";
                
                Notification.NotificationType type = accepted ? 
                                                    Notification.NotificationType.ACCEPTED : 
                                                    Notification.NotificationType.DECLINED;
                
                Notification notification = new Notification(organizerId, eventId, eventName, type, title, message);
                sendNotification(notification, callback);
            });
    }
    
    @Override
    public void getNotificationsForUser(String userId, NotificationListCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
            .whereEqualTo("userId", userId)
            .orderBy("timestamp", Query.Direction.DESCENDING)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Notification> notifications = new ArrayList<>();
                queryDocumentSnapshots.forEach(document -> {
                    Notification notification = new Notification();
                    notification.setId(document.getId());
                    notification.setUserId(document.getString("userId"));
                    notification.setEventId(document.getString("eventId"));
                    notification.setEventName(document.getString("eventName"));
                    notification.setTypeString(document.getString("type"));
                    notification.setTitle(document.getString("title"));
                    notification.setMessage(document.getString("message"));
                    
                    Long timestamp = document.getLong("timestamp");
                    notification.setTimestamp(timestamp != null ? timestamp : System.currentTimeMillis());
                    notification.setRead(Boolean.TRUE.equals(document.getBoolean("isRead")));
                    notifications.add(notification);
                });
                
                Log.d(TAG, "Loaded " + notifications.size() + " notifications for user");
                if (callback != null) {
                    callback.onSuccess(notifications);
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error loading notifications", e);
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            });
    }
    
    @Override
    public void markAsRead(String notificationId, VoidCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
            .document(notificationId)
            .update("isRead", true)
            .addOnSuccessListener(aVoid -> {
                if (callback != null) {
                    callback.onSuccess();
                }
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            });
    }
    
    @Override
    public void markAllAsRead(String userId, VoidCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
            .whereEqualTo("userId", userId)
            .whereEqualTo("isRead", false)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                if (queryDocumentSnapshots.isEmpty()) {
                    if (callback != null) {
                        callback.onSuccess();
                    }
                    return;
                }
                
                int[] count = {0};
                int total = queryDocumentSnapshots.size();
                
                queryDocumentSnapshots.forEach(document -> {
                    document.getReference().update("isRead", true)
                        .addOnCompleteListener(task -> {
                            count[0]++;
                            if (count[0] == total && callback != null) {
                                callback.onSuccess();
                            }
                        });
                });
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            });
    }
    
    @Override
    public void deleteNotification(String notificationId, VoidCallback callback) {
        db.collection(COLLECTION_NOTIFICATIONS)
            .document(notificationId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                if (callback != null) {
                    callback.onSuccess();
                }
            })
            .addOnFailureListener(e -> {
                if (callback != null) {
                    callback.onError(e.getMessage());
                }
            });
    }
    
    /**
     * Generate professional title based on notification type
     */
    private String generateTitle(Notification.NotificationType type) {
        switch (type) {
            case INVITED:
                return "You're Invited!";
            case WAITLISTED:
                return "Waitlist Update";
            case CANCELLED:
                return "Event Cancelled";
            case ACCEPTED:
                return "Invitation Accepted";
            case DECLINED:
                return "Invitation Declined";
            case CONFIRMED:
                return "Attendance Confirmed";
            default:
                return "Event Notification";
        }
    }
    
    /**
     * Filter users who have notifications enabled from Firestore
     */
    private void filterUsersWithNotificationsEnabledFromFirestore(List<String> userIds, UserFilterCallback callback) {
        if (userIds.isEmpty()) {
            callback.onFiltered(new ArrayList<>());
            return;
        }
        
        List<String> enabledUsers = Collections.synchronizedList(new ArrayList<>());
        int[] checkedCount = {0};
        
        Log.d(TAG, "Filtering " + userIds.size() + " users for notification preferences");
        
        for (String userId : userIds) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    Boolean notifPref = documentSnapshot.getBoolean("notificationsEnabled");
                    boolean notificationsEnabled = notifPref != null ? notifPref : true;
                    
                    Log.d(TAG, "User " + userId + " notifications: " + notificationsEnabled);
                    
                    synchronized (enabledUsers) {
                        if (notificationsEnabled) {
                            enabledUsers.add(userId);
                        }
                        
                        checkedCount[0]++;
                        if (checkedCount[0] == userIds.size()) {
                            Log.d(TAG, "Filtering complete: " + enabledUsers.size() + "/" + userIds.size() + " users have notifications enabled");
                            callback.onFiltered(new ArrayList<>(enabledUsers));
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error checking notification preference for user " + userId + ", including anyway", e);
                    
                    synchronized (enabledUsers) {
                        // Include user on error (fail-safe)
                        enabledUsers.add(userId);
                        
                        checkedCount[0]++;
                        if (checkedCount[0] == userIds.size()) {
                            Log.d(TAG, "Filtering complete: " + enabledUsers.size() + "/" + userIds.size() + " users have notifications enabled");
                            callback.onFiltered(new ArrayList<>(enabledUsers));
                        }
                    }
                });
        }
    }
    
    /**
     * Callback for filtering users
     */
    private interface UserFilterCallback {
        void onFiltered(List<String> enabledUserIds);
    }
    
    /**
     * Generate professional message based on notification type
     */
    private String generateMessage(Notification.NotificationType type, String eventName) {
        switch (type) {
            case INVITED:
                return "Congratulations! You have been selected for \"" + eventName + 
                       "\". Please accept or decline your invitation.";
            case WAITLISTED:
                return "You have been added to the waitlist for \"" + eventName + 
                       "\". We will notify you if a spot becomes available.";
            case CANCELLED:
                return "We regret to inform you that \"" + eventName + 
                       "\" has been cancelled by the organizer.";
            case ACCEPTED:
                return "You have successfully accepted the invitation to \"" + eventName + "\".";
            case DECLINED:
                return "You have declined the invitation to \"" + eventName + "\".";
            case CONFIRMED:
                return "Congratulations! Your attendance has been confirmed for \"" + eventName + 
                       "\". We look forward to seeing you at the event!";
            default:
                return "Update regarding \"" + eventName + "\".";
        }
    }
}
