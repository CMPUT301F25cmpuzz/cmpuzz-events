package com.example.cmpuzz_events;

import static org.junit.Assert.*;

import com.example.cmpuzz_events.models.user.User;

import org.junit.Test;

public class UserTest {

    @Test
    public void userRoleEnum_fromString() {
        assertEquals(User.UserRole.USER, User.UserRole.fromString("user"));
        assertEquals(User.UserRole.ORGANIZER, User.UserRole.fromString("organizer"));
        assertEquals(User.UserRole.ADMIN, User.UserRole.fromString("admin"));
        assertEquals(User.UserRole.USER, User.UserRole.fromString("unknown")); // Default case
    }

    @Test
    public void userRoleEnum_getRoleName() {
        assertEquals("user", User.UserRole.USER.getRoleName());
        assertEquals("organizer", User.UserRole.ORGANIZER.getRoleName());
        assertEquals("admin", User.UserRole.ADMIN.getRoleName());
    }

    @Test
    public void canManageEvents_isCorrectForRole() {
        User user = new User();
        user.setRole(User.UserRole.USER);
        assertFalse(user.canManageEvents());

        User organizer = new User();
        organizer.setRole(User.UserRole.ORGANIZER);
        assertTrue(organizer.canManageEvents());

        User admin = new User();
        admin.setRole(User.UserRole.ADMIN);
        assertTrue(admin.canManageEvents());
    }

    @Test
    public void toMap_containsAllFields() {
        User user = new User("uid123", "test@example.com", "Test User", "testuser", User.UserRole.ORGANIZER);
        long creationTime = user.getCreatedAt();

        java.util.Map<String, Object> map = user.toMap();

        assertEquals("uid123", map.get("uid"));
        assertEquals("test@example.com", map.get("email"));
        assertEquals("Test User", map.get("displayName"));
        assertEquals("testuser", map.get("username"));
        assertEquals("organizer", map.get("role"));
        assertEquals(creationTime, map.get("createdAt"));
    }

    @Test
    public void testRoleBooleanHelpers() {
        User user = new User();

        // Test Admin
        user.setRole(User.UserRole.ADMIN);
        assertTrue(user.isAdmin());
        assertFalse(user.isOrganizer());
        assertFalse(user.isUser());

        // Test Organizer
        user.setRole(User.UserRole.ORGANIZER);
        assertFalse(user.isAdmin());
        assertTrue(user.isOrganizer());
        assertFalse(user.isUser());

        // Test User
        user.setRole(User.UserRole.USER);
        assertFalse(user.isAdmin());
        assertFalse(user.isOrganizer());
        assertTrue(user.isUser());
    }

    @Test
    public void testNotificationSettings() {
        User user = new User();

        // Default should be enabled
        assertTrue("Notifications should be enabled by default", user.isNotificationsEnabled());

        user.setNotificationsEnabled(false);
        assertFalse(user.isNotificationsEnabled());

        user.setNotificationsEnabled(true);
        assertTrue(user.isNotificationsEnabled());
    }

    @Test
    public void testConstructors() {
        // Test empty constructor defaults
        User defaultUser = new User();
        assertEquals(User.UserRole.USER, defaultUser.getRole());
        assertTrue(defaultUser.getCreatedAt() > 0);

        // Test full constructor
        User fullUser = new User("uid1", "a@a.com", "Name", "uname", User.UserRole.ADMIN);
        assertEquals("uid1", fullUser.getUid());
        assertEquals("admin", fullUser.getRole().getRoleName());
    }
}