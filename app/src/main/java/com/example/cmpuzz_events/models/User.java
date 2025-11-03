package com.example.cmpuzz_events.models;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class User implements Serializable {
    private String uid;
    private String email;
    private String displayName;
    private UserRole role;
    private long createdAt;

    public enum UserRole {
        USER("user"),
        ADMIN("admin"),
        ORGANIZER("organizer");

        private final String value;

        UserRole(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static UserRole fromString(String value) {
            for (UserRole role : UserRole.values()) {
                if (role.value.equalsIgnoreCase(value)) {
                    return role;
                }
            }
            return USER; // Default role
        }
    }

    // Empty constructor for Firebase
    public User() {
        this.role = UserRole.USER;
        this.createdAt = System.currentTimeMillis();
    }

    public User(String uid, String email, String displayName, UserRole role) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.role = role;
        this.createdAt = System.currentTimeMillis();
    }

    // Convert to Map for Firebase
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("email", email);
        map.put("displayName", displayName);
        map.put("role", role.getValue());
        map.put("createdAt", createdAt);
        return map;
    }

    // Getters and Setters
    public String getUid() {
        return uid;
    }

    public void setUid(String uid) {
        this.uid = uid;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getDisplayName() {
        return displayName;
    }

    public void setDisplayName(String displayName) {
        this.displayName = displayName;
    }

    public UserRole getRole() {
        return role;
    }

    public void setRole(UserRole role) {
        this.role = role;
    }

    public long getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(long createdAt) {
        this.createdAt = createdAt;
    }

    // Role checking helpers
    public boolean isAdmin() {
        return role == UserRole.ADMIN;
    }

    public boolean isOrganizer() {
        return role == UserRole.ORGANIZER;
    }

    public boolean isUser() {
        return role == UserRole.USER;
    }

    public boolean canManageEvents() {
        return role == UserRole.ADMIN || role == UserRole.ORGANIZER;
    }
}
