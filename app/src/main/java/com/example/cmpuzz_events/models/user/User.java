package com.example.cmpuzz_events.models.user;

import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

public class User implements Serializable {
    private String uid;
    private String email;
    private String displayName;
    private String username;
    private UserRole role;
    private long createdAt;
    private boolean notificationsEnabled;
    private String profileImageUrl;

    public enum UserRole {
        USER("user"),
        ADMIN("admin"),
        ORGANIZER("organizer");

        private final String roleName;

        UserRole(String value) {
            this.roleName = value;
        }

        public String getRoleName() {
            return roleName;
        }

        public static UserRole fromString(String value) {
            for (UserRole role : UserRole.values()) {
                if (role.roleName.equalsIgnoreCase(value)) {
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
        this.notificationsEnabled = true; // Default to enabled
    }

    /**
     * Constructor for User with known data to parse
     * @param uid
     * @param email
     * @param displayName
     * @param username
     * @param role
     */
    public User(String uid, String email, String displayName, String username, UserRole role) {
        this.uid = uid;
        this.email = email;
        this.displayName = displayName;
        this.username = username;
        this.role = role;
        this.createdAt = System.currentTimeMillis();
        this.notificationsEnabled = true; // Default to enabled
    }

    // Convert to Map for Firebase

    /**
     * Convert User object to a Map to be added to firebase
     * @return map
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("uid", uid);
        map.put("email", email);
        map.put("displayName", displayName);
        map.put("username", username);
        map.put("role", role.getRoleName());
        map.put("createdAt", createdAt);
        map.put("notificationsEnabled", notificationsEnabled);
        map.put("profileImageUrl", profileImageUrl);
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

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
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

    public boolean isNotificationsEnabled() {
        return notificationsEnabled;
    }

    public void setNotificationsEnabled(boolean notificationsEnabled) {
        this.notificationsEnabled = notificationsEnabled;
    }

    public String getProfileImageUrl() {
        return profileImageUrl;
    }

    public void setProfileImageUrl(String profileImageUrl) {
        this.profileImageUrl = profileImageUrl;
    }
}
