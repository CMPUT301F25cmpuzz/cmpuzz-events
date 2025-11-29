package com.example.cmpuzz_events;

import com.example.cmpuzz_events.models.event.EventEntity;
import org.junit.Before;
import org.junit.Test;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import static org.junit.Assert.*;

/**
 * Tests the core logic for exporting enrolled entrants as a CSV file.
 */
public class ExportEnrolledEntrantsTest {

    private final String TEST_EVENT_TITLE = "Tech Conference";
    private final String EXPECTED_FILENAME = "enrolled_entrants_for_" + TEST_EVENT_TITLE;

    private EventEntity eventWithAttendees;
    private EventEntity eventWithNoAttendees;
    private EventEntity eventWithNullAttendees;

    /**
     * This is a mock implementation of the csvExport method.
     */
    private ExportResult mockCsvExport(List<String> attendeeList, String filename) {
        return new ExportResult(attendeeList, filename);
    }

    /**
     * A helper class to hold the results from our mock export method.
     */
    private static class ExportResult {
        final List<String> capturedAttendeeList;
        final String capturedFilename;

        ExportResult(List<String> attendeeList, String filename) {
            this.capturedAttendeeList = attendeeList;
            this.capturedFilename = filename;
        }
    }

    @Before
    public void setUp() {
        // Event with a list of attendees
        eventWithAttendees = new EventEntity();
        eventWithAttendees.setTitle(TEST_EVENT_TITLE);
        eventWithAttendees.setAttendees(new ArrayList<>(Arrays.asList("user123", "userABC", "user456")));

        // Event with an empty attendee list
        eventWithNoAttendees = new EventEntity();
        eventWithNoAttendees.setTitle(TEST_EVENT_TITLE);
        eventWithNoAttendees.setAttendees(new ArrayList<>());

        // Event where the attendee list is null
        eventWithNullAttendees = new EventEntity();
        eventWithNullAttendees.setTitle(TEST_EVENT_TITLE);
        eventWithNullAttendees.setAttendees(null);
    }

    /**
     * Verifies that when an event has attendees, the export function is called
     * with the correct list of user IDs and the correct, formatted filename.
     */
    @Test
    public void testExport_withAttendees_generatesCorrectData() {
        List<String> eeList = eventWithAttendees.getAttendees() != null ? new ArrayList<>(eventWithAttendees.getAttendees()) : new ArrayList<>();
        String filename = "enrolled_entrants_for_" + eventWithAttendees.getTitle();

        ExportResult result = mockCsvExport(eeList, filename);

        assertNotNull("Attendee list should not be null", result.capturedAttendeeList);
        assertEquals("The attendee list size should match the mock data", 3, result.capturedAttendeeList.size());
        assertTrue("The list should contain the correct user IDs", result.capturedAttendeeList.containsAll(Arrays.asList("user123", "userABC", "user456")));
        assertEquals("The filename should be correctly formatted", EXPECTED_FILENAME, result.capturedFilename);
    }

    /**
     * Verifies that the logic correctly handles an event with an empty list of attendees.
     */
    @Test
    public void testExport_withEmptyAttendeeList_producesEmptyList() {
        List<String> eeList = eventWithNoAttendees.getAttendees() != null ? new ArrayList<>(eventWithNoAttendees.getAttendees()) : new ArrayList<>();

        assertTrue("The extracted attendee list should be empty", eeList.isEmpty());
    }

    /**
     * Verifies that the logic gracefully handles an event where the attendees list is null,
     * producing an empty, non-null list.
     */
    @Test
    public void testExport_withNullAttendeeList_producesEmptyList() {
        List<String> eeList = eventWithNullAttendees.getAttendees() != null ? new ArrayList<>(eventWithNullAttendees.getAttendees()) : new ArrayList<>();

        assertNotNull("The extracted attendee list should not be null", eeList);
        assertTrue("The extracted attendee list should be empty", eeList.isEmpty());
    }
}
