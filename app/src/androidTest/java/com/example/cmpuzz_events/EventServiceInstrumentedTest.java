package com.example.cmpuzz_events;

import static org.junit.Assert.*;

import androidx.test.ext.junit.runners.AndroidJUnit4;

import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.DocumentSnapshot;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@RunWith(AndroidJUnit4.class)
public class EventServiceInstrumentedTest {

    private FirebaseFirestore db;
    private String testEventId = "testing_lottery_scooby";

    @Before
    public void setUp() {
        // 1. Get the real Firestore instance
        db = FirebaseFirestore.getInstance();
    }

    @After
    public void tearDown() throws InterruptedException {
        // 5. Cleanup: Delete the test event from the database
        CountDownLatch latch = new CountDownLatch(1);
        db.collection("events").document(testEventId).delete()
                .addOnCompleteListener(task -> latch.countDown());
        latch.await(5, TimeUnit.SECONDS);
    }

    @Test
    public void testDrawAttendees_Integration() throws InterruptedException {
        // --- PREPARE DATA ---
        // Create a "raw" event directly in Firestore to simulate existing state
        // (We bypass the service creation to ensure we have exact control over the waitlist)
        Map<String, Object> eventData = new HashMap<>();
        eventData.put("eventId", testEventId);
        eventData.put("title", "Test Lottery Event");
        eventData.put("capacity", 5); // Capacity of 5
        eventData.put("maxEntrants", 50);
        eventData.put("organizerId", "test_organizer");

        // Add 10 dummy users to the waitlist
        List<String> waitlist = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            waitlist.add("user_" + i);
        }
        eventData.put("waitlist", waitlist);
        eventData.put("invitations", new ArrayList<>()); // Start empty
        eventData.put("attendees", new ArrayList<>());   // Start empty

        // Write this setup data to Firestore
        CountDownLatch setupLatch = new CountDownLatch(1);
        db.collection("events").document(testEventId).set(eventData)
                .addOnSuccessListener(aVoid -> setupLatch.countDown());
        assertTrue("Setup timed out", setupLatch.await(5, TimeUnit.SECONDS));

        // --- EXECUTE TEST ---
        // Now call your REAL service method
        CountDownLatch actionLatch = new CountDownLatch(1);
        final boolean[] success = {false};
        final String[] errorMsg = {null};

        // We ask to draw 3 attendees (even though capacity is 5)
        EventService.getInstance().drawAttendees(testEventId, 3, new IEventService.VoidCallback() {
            @Override
            public void onSuccess() {
                success[0] = true;
                actionLatch.countDown();
            }

            @Override
            public void onError(String error) {
                errorMsg[0] = error;
                actionLatch.countDown();
            }
        });

        assertTrue("Action timed out", actionLatch.await(5, TimeUnit.SECONDS));
        assertTrue("Draw attendees failed: " + errorMsg[0], success[0]);

        // --- VERIFY RESULTS ---
        // Fetch the document again to see if it changed correctly
        CountDownLatch verifyLatch = new CountDownLatch(1);
        db.collection("events").document(testEventId).get()
                .addOnSuccessListener(documentSnapshot -> {
                    // Verify Invitations
                    List<Map<String, Object>> invitations = (List<Map<String, Object>>) documentSnapshot.get("invitations");
                    assertNotNull(invitations);
                    assertEquals("Should have invited exactly 3 users", 3, invitations.size());

                    // Verify Waitlist
                    List<String> updatedWaitlist = (List<String>) documentSnapshot.get("waitlist");
                    assertNotNull(updatedWaitlist);
                    assertEquals("Waitlist should have 7 users remaining", 7, updatedWaitlist.size());

                    verifyLatch.countDown();
                });

        assertTrue("Verification timed out", verifyLatch.await(5, TimeUnit.SECONDS));
    }
}