package com.example.cmpuzz_events.service;

import com.example.cmpuzz_events.models.notification.Notification;

import java.util.List;

public interface INotificationService {
    
    /**
     * Send a notification to a single user
     */
    void sendNotification(Notification notification, VoidCallback callback);
    
    /**
     * Send notifications to multiple users (batch send)
     * Messages are auto-generated based on notification type
     */
    void sendNotificationsToUsers(List<String> userIds, String eventId, String eventName, 
                                  Notification.NotificationType type, VoidCallback callback);
    
    /**
     * Notify organizer when a user accepts/declines invitation
     */
    void notifyOrganizerOfResponse(String organizerId, String userName, String eventId, 
                                   String eventName, boolean accepted, VoidCallback callback);
    
    /**
     * Get all notifications for a specific user
     */
    void getNotificationsForUser(String userId, NotificationListCallback callback);
    
    /**
     * Mark a notification as read
     */
    void markAsRead(String notificationId, VoidCallback callback);
    
    /**
     * Mark all notifications as read for a user
     */
    void markAllAsRead(String userId, VoidCallback callback);
    
    /**
     * Delete a notification
     */
    void deleteNotification(String notificationId, VoidCallback callback);
    
    /**
     * Get notification preference for a user
     */
    void getNotificationPreference(String userId, NotificationPreferenceCallback callback);
    
    /**
     * Update notification preference for a user
     */
    void updateNotificationPreference(String userId, boolean enabled, VoidCallback callback);
    
    /**
     * Get all notifications (admin only - for log review)
     */
    void getAllNotifications(NotificationListCallback callback);
    
    /**
     * Update the important status of a notification
     */
    void updateImportantStatus(String notificationId, boolean isImportant, VoidCallback callback);
    
    // Callback interfaces
    interface VoidCallback {
        void onSuccess();
        void onError(String error);
    }
    
    interface NotificationListCallback {
        void onSuccess(List<Notification> notifications);
        void onError(String error);
    }
    
    interface NotificationPreferenceCallback {
        void onSuccess(boolean enabled);
        void onError(String error);
    }
}
