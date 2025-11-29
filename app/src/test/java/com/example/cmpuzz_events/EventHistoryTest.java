package com.example.cmpuzz_events;

import com.example.cmpuzz_events.models.event.EventEntity;

import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;

import static org.junit.Assert.assertEquals;

/**
 * Tests the core logic for determining a user's event registration status.
 */
public class EventHistoryTest {

    private final String CURRENT_USER_ID = "user123";

    private EventEntity selectedEvent;
    private EventEntity pendingEvent;
    private EventEntity notSelectedEvent;
    private EventEntity notRegisteredEvent;

    /**
     * direct copy of the status-determining logic from EventHistoryViewHolder.
     */
    private UserEventStatus determineUserStatus(EventEntity event, String userId) {
        boolean isAttendee = event.getAttendees() != null && event.getAttendees().contains(userId);
        boolean isWaitlisted = event.getWaitlist() != null && event.getWaitlist().contains(userId);
        boolean selectionsFinalized = event.isSelectionsFinalized();

        if (isAttendee) {
            return UserEventStatus.SELECTED;
        } else if (isWaitlisted) {
            if (selectionsFinalized) {
                return UserEventStatus.NOT_SELECTED;
            } else {
                return UserEventStatus.PENDING;
            }
        } else {
            return UserEventStatus.NOT_REGISTERED;
        }
    }

    /**
     * An enum to represent the possible outcomes of the status logic.
     */
    private enum UserEventStatus {
        SELECTED,
        NOT_SELECTED,
        PENDING,
        NOT_REGISTERED
    }

    @Before
    public void setUp() {
        // Event 1: User is an attendee
        selectedEvent = new EventEntity();
        selectedEvent.setEventId("event1");
        selectedEvent.setTitle("Selected Event");
        selectedEvent.setAttendees(new ArrayList<>(Arrays.asList(CURRENT_USER_ID, "user456")));
        selectedEvent.setWaitlist(new ArrayList<>());
        selectedEvent.setSelectionsFinalized(true);

        // Event 2: User is waitlisted, selections are not final
        pendingEvent = new EventEntity();
        pendingEvent.setEventId("event2");
        pendingEvent.setTitle("Pending Event");
        pendingEvent.setAttendees(new ArrayList<>(Arrays.asList("user789")));
        pendingEvent.setWaitlist(new ArrayList<>(Arrays.asList(CURRENT_USER_ID)));
        pendingEvent.setSelectionsFinalized(false);

        // Event 3: User was waitlisted, selections are final
        notSelectedEvent = new EventEntity();
        notSelectedEvent.setEventId("event3");
        notSelectedEvent.setTitle("Not Selected Event");
        notSelectedEvent.setAttendees(new ArrayList<>(Arrays.asList("userABC")));
        notSelectedEvent.setWaitlist(new ArrayList<>(Arrays.asList("userXYZ", CURRENT_USER_ID)));
        notSelectedEvent.setSelectionsFinalized(true);

        // Event 4: User is not in any list
        notRegisteredEvent = new EventEntity();
        notRegisteredEvent.setEventId("event4");
        notRegisteredEvent.setTitle("Not Registered Event");
        notRegisteredEvent.setAttendees(new ArrayList<>(Arrays.asList("user456")));
        notRegisteredEvent.setWaitlist(new ArrayList<>(Arrays.asList("user789")));
        notRegisteredEvent.setSelectionsFinalized(true);
    }

    /**
     * Verifies that a user who is in the attendees list is correctly identified as "Selected".
     */
    @Test
    public void testUserStatus_isCorrectlyIdentifiedAsSelected() {
        UserEventStatus status = determineUserStatus(selectedEvent, CURRENT_USER_ID);
        assertEquals(UserEventStatus.SELECTED, status);
    }

    /**
     * Verifies that a user on the waitlist is identified as "Pending"
     * when the event's selections have not yet been finalized.
     */
    @Test
    public void testUserStatus_isCorrectlyIdentifiedAsPending() {
        UserEventStatus status = determineUserStatus(pendingEvent, CURRENT_USER_ID);
        assertEquals(UserEventStatus.PENDING, status);
    }

    /**
     * Verifies that a user on the waitlist is identified as "Not Selected"
     * after the event's selections have been finalized.
     */
    @Test
    public void testUserStatus_isCorrectlyIdentifiedAsNotSelected() {
        UserEventStatus status = determineUserStatus(notSelectedEvent, CURRENT_USER_ID);
        assertEquals(UserEventStatus.NOT_SELECTED, status);
    }

    /**
     * Verifies that a user who is not on the attendee or waitlist is correctly
     * identified as not being registered for the event.
     */
    @Test
    public void testUserStatus_isCorrectlyIdentifiedAsNotRegistered() {
        UserEventStatus status = determineUserStatus(notRegisteredEvent, CURRENT_USER_ID);
        assertEquals(UserEventStatus.NOT_REGISTERED, status);
    }

    /**
     * Verifies that a user who is not the current user but is an attendee
     * does not cause the current user to be marked as selected.
     */
    @Test
    public void testUserStatus_isNotRegistered_WhenAnotherUserIsAnAttendee() {
        UserEventStatus status = determineUserStatus(selectedEvent, "someOtherUser");
        assertEquals(UserEventStatus.NOT_REGISTERED, status);
    }

    /**
     * Verifies that an empty or null attendee/waitlist does not cause a crash
     * and correctly identifies the user as not registered.
     */
    @Test
    public void testUserStatus_handlesNullAndEmptyListsGracefully() {
        EventEntity event = new EventEntity();
        event.setAttendees(null);
        event.setWaitlist(null);
        event.setSelectionsFinalized(true);

        UserEventStatus status = determineUserStatus(event, CURRENT_USER_ID);
        assertEquals(UserEventStatus.NOT_REGISTERED, status);
    }
}
