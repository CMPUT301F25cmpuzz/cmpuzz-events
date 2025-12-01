package com.example.cmpuzz_events;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.models.event.Invitation;

import java.util.Date;
import java.util.Map;
import java.util.concurrent.TimeUnit;

public class EventEntityTest {

    private EventEntity event;
    private Date testStartDate;
    private Date testEndDate;

    @Before
    public void setUp() {
        testStartDate = new Date(System.currentTimeMillis() + 100000); // In the future
        testEndDate = new Date(System.currentTimeMillis() + 200000); // After start date

        event = new EventEntity(
                "evt123", "Community BBQ", "A fun event", 100,
                testStartDate, testEndDate, "org456", "Organizer Name",
                false, 50 // maxEntrants = 50
        );
    }

    @Test
    public void testConstructorAndGetters() {
        assertEquals("evt123", event.getEventId());
        assertEquals("Community BBQ", event.getTitle());
        assertEquals(100, event.getCapacity());
        assertEquals(50, event.getMaxEntrants());
        assertEquals("org456", event.getOrganizerId());
        assertFalse(event.isGeolocationRequired());
        assertNotNull(event.getWaitlist());
        assertNotNull(event.getAttendees());
        assertNotNull(event.getInvitations());
        assertNotNull(event.getDeclined());
    }

    @Test
    public void testGenerateQRCodeUrl() {
        // The constructor calls generateQRCodeUrl, so we check the getter
        assertEquals("cmpuzzevents://event/evt123", event.getQrCodeUrl());
    }

    @Test
    public void testAddToWaitlist() {
        // Add one user
        assertTrue(event.addToWaitlist("user1"));
        assertEquals(1, event.getWaitlist().size());
        assertTrue(event.getWaitlist().contains("user1"));

        // Add duplicate user
        assertFalse(event.addToWaitlist("user1"));
        assertEquals(1, event.getWaitlist().size());
    }

    @Test
    public void testAddToWaitlistWithLimit() {
        event.setMaxEntrants(2); // Set a small limit

        assertTrue(event.addToWaitlist("user1"));
        assertTrue(event.addToWaitlist("user2"));

        // Waitlist is now full (size 2)
        assertEquals(2, event.getWaitlist().size());

        // Try to add a third user
        assertFalse(event.addToWaitlist("user3"));
        assertEquals(2, event.getWaitlist().size());
    }

    @Test
    public void testAddToWaitlistUnlimited() {
        event.setMaxEntrants(0); // 0 or less means unlimited

        // Add more users than the original capacity to check
        for(int i = 0; i < 60; i++) {
            assertTrue(event.addToWaitlist("user" + i));
        }
        assertEquals(60, event.getWaitlist().size());
    }

    @Test
    public void testRemoveFromWaitlist() {
        event.addToWaitlist("user1");
        event.addToWaitlist("user2");

        // Remove existing user
        assertTrue(event.removeFromWaitlist("user1"));
        assertEquals(1, event.getWaitlist().size());
        assertFalse(event.getWaitlist().contains("user1"));
        assertTrue(event.getWaitlist().contains("user2"));

        // Remove non-existent user
        assertFalse(event.removeFromWaitlist("user3"));
        assertEquals(1, event.getWaitlist().size());
    }

    @Test
    public void testSettersUpdateTimestamp() throws InterruptedException {
        Date originalUpdate = event.getUpdatedAt();

        // Wait for a moment to ensure the timestamp will be different
        TimeUnit.MILLISECONDS.sleep(10);

        event.setTitle("New Event Title");
        Date newUpdate = event.getUpdatedAt();

        assertTrue("New timestamp should be after original", newUpdate.after(originalUpdate));
    }

    @Test
    public void testToMap() {
        event.addToWaitlist("user1");
        event.getAttendees().add("user2");

        Map<String, Object> map = event.toMap();

        assertEquals("evt123", map.get("eventId"));
        assertEquals("Community BBQ", map.get("title"));
        assertEquals(100, map.get("capacity"));
        assertEquals(50, map.get("maxEntrants"));
        assertEquals(testStartDate, map.get("registrationStart"));
        assertEquals("cmpuzzevents://event/evt123", map.get("qrCodeUrl"));

        // Check lists
        assertTrue(((java.util.List<String>)map.get("waitlist")).contains("user1"));
        assertTrue(((java.util.List<String>)map.get("attendees")).contains("user2"));
    }

    @Test
    public void testInvitationManagement() {
        Invitation inv1 = new Invitation("user1", "Alice");
        Invitation inv2 = new Invitation("user2", "Bob");

        // Test Adding
        event.addInvitation(inv1);
        event.addInvitation(inv2);
        assertEquals(2, event.getInvitations().size());

        // Test Getting by ID
        Invitation retrieved = event.getInvitationByUserId("user1");
        assertNotNull(retrieved);
        assertEquals("Alice", retrieved.getUsername());

        assertNull(event.getInvitationByUserId("nonexistent"));

        // Test Removing by ID (User 1)
        assertTrue(event.removeInvitation("user1"));
        assertEquals(1, event.getInvitations().size());
        assertNull(event.getInvitationByUserId("user1"));

        // Test removing nonexistent user
        assertFalse(event.removeInvitation("user1"));
    }

    @Test
    public void testRemoveFromInvitationsList() {
        // This tests the specific list removal method
        Invitation inv = new Invitation("user1", "Alice");
        event.addInvitation(inv);

        assertTrue(event.removeFromInvitationsList("user1"));
        assertTrue(event.getInvitations().isEmpty());
    }

    @Test
    public void testDeclinedListManagement() {
        // Test that the declined list is initialized empty
        assertNotNull(event.getDeclined());
        assertTrue(event.getDeclined().isEmpty());

        // Test setting and retrieving
        java.util.List<String> declinedUsers = new java.util.ArrayList<>();
        declinedUsers.add("user1");
        event.setDeclined(declinedUsers);

        assertEquals(1, event.getDeclined().size());
        assertTrue(event.getDeclined().contains("user1"));
    }

    @Test
    public void testPosterUrlDefaultIsNull() {
        assertNull(event.getPosterUrl());
    }

    @Test
    public void testPosterUrlSetAndGet() {
        String url = "https://example.com/poster123.jpg";

        event.setPosterUrl(url);

        assertEquals(url, event.getPosterUrl());
    }

    @Test
    public void testPosterUrlIncludedInToMap() {
        String url = "https://example.com/poster123.jpg";
        event.setPosterUrl(url);

        Map<String, Object> map = event.toMap();

        assertEquals(url, map.get("posterUrl"));
    }

}
