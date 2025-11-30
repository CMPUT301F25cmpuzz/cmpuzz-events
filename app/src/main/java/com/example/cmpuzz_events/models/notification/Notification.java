package com.example.cmpuzz_events.models.notification;

import java.io.Serializable;

/**
 * Represents a notification sent to a user
 */
public class Notification implements Serializable {
    private String id;
    private String userId;
    private String eventId;
    private String eventName;
    private NotificationType type;
    private String title;
    private String message;
    private long timestamp;
    private boolean isRead;
    private boolean isImportant;

    public enum NotificationType {
        INVITED("invited"),
        WAITLISTED("waitlisted"),
        CANCELLED("cancelled"),
        ACCEPTED("accepted"),
        DECLINED("declined"),
        CONFIRMED("confirmed");

        private final String value;

        NotificationType(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static NotificationType fromString(String value) {
            for (NotificationType type : NotificationType.values()) {
                if (type.value.equalsIgnoreCase(value)) {
                    return type;
                }
            }
            return INVITED; // default
        }
    }

    // Default constructor for Firestore
    public Notification() {
    }

    /**
     * Constructor for notification with known data
     * @param userId
     * @param eventId
     * @param eventName
     * @param type
     * @param title
     * @param message
     */
    public Notification(String userId, String eventId, String eventName, NotificationType type, String title, String message) {
        this.userId = userId;
        this.eventId = eventId;
        this.eventName = eventName;
        this.type = type;
        this.title = title;
        this.message = message;
        this.timestamp = System.currentTimeMillis();
        this.isRead = false;
        this.isImportant = false;
    }

    // Getters and Setters
    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getEventName() {
        return eventName;
    }

    public void setEventName(String eventName) {
        this.eventName = eventName;
    }

    public NotificationType getType() {
        return type;
    }

    public void setType(NotificationType type) {
        this.type = type;
    }

    // Firestore stores enums as strings, so we need these methods
    public String getTypeString() {
        return type != null ? type.getValue() : NotificationType.INVITED.getValue();
    }

    public void setTypeString(String typeString) {
        this.type = NotificationType.fromString(typeString);
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public long getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(long timestamp) {
        this.timestamp = timestamp;
    }

    public boolean isRead() {
        return isRead;
    }

    public void setRead(boolean read) {
        isRead = read;
    }

    public boolean isImportant() {
        return isImportant;
    }

    public void setImportant(boolean important) {
        isImportant = important;
    }
}
