package com.example.cmpuzz_events;

import static org.junit.Assert.*;
import com.example.cmpuzz_events.models.notification.Notification;
import org.junit.Test;

public class NotificationTest {

    @Test
    public void testNotificationTypeEnum_fromString() {
        // Test valid cases (case insensitive)
        assertEquals(Notification.NotificationType.INVITED, Notification.NotificationType.fromString("invited"));
        assertEquals(Notification.NotificationType.WAITLISTED, Notification.NotificationType.fromString("WAITLISTED"));
        assertEquals(Notification.NotificationType.CANCELLED, Notification.NotificationType.fromString("Cancelled"));
        assertEquals(Notification.NotificationType.ACCEPTED, Notification.NotificationType.fromString("accepted"));
        assertEquals(Notification.NotificationType.DECLINED, Notification.NotificationType.fromString("declined"));
        assertEquals(Notification.NotificationType.CONFIRMED, Notification.NotificationType.fromString("confirmed"));

        // Test invalid/default case
        assertEquals(Notification.NotificationType.INVITED, Notification.NotificationType.fromString("unknown_type"));
    }

    @Test
    public void testTypeStringHelpers() {
        Notification notification = new Notification();

        // Test setter using string
        notification.setTypeString("cancelled");
        assertEquals(Notification.NotificationType.CANCELLED, notification.getType());

        // Test getter returning string
        assertEquals("cancelled", notification.getTypeString());

        // Test null handling (should default to invited value)
        notification.setType(null);
        assertEquals("invited", notification.getTypeString());
    }

    @Test
    public void testConstructorAndDefaults() {
        Notification notification = new Notification(
                "user1", "event1", "Party",
                Notification.NotificationType.INVITED,
                "You're invited!", "Come join us."
        );

        assertEquals("user1", notification.getUserId());
        assertEquals("event1", notification.getEventId());
        assertEquals("Party", notification.getEventName());
        assertEquals(Notification.NotificationType.INVITED, notification.getType());
        assertFalse("New notifications should be unread", notification.isRead());

        // Verify timestamp is set to recent time
        long currentTime = System.currentTimeMillis();
        assertTrue(notification.getTimestamp() <= currentTime);
        assertTrue(notification.getTimestamp() > currentTime - 5000); // Created within last 5s
    }

    @Test
    public void testReadStatus() {
        Notification notification = new Notification();
        assertFalse(notification.isRead());

        notification.setRead(true);
        assertTrue(notification.isRead());
    }
}