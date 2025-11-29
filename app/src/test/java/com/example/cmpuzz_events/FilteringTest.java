package com.example.cmpuzz_events;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.ui.event.Event;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Tests the filtering logic from the HomeFragment in complete isolation.
 * verifies that text based queries and availability selections (e.g., "Full", "Not Full") work correctly.
 */
public class FilteringTest {

    private static final int RADIO_ID_ANY = 1;
    private static final int RADIO_ID_NOT_FULL = 2;
    private static final int RADIO_ID_FULL = 3;

    private List<Event> allEvents;
    private List<EventEntity> allEventEntities;

    /**
     * direct copy of the filtering logic from the HomeFragment's applyFilters method.
     * filters a list of events based on a text query and an availability status ID.
     */
    private List<Event> applyFilters(String query, int selectedAvailabilityId, List<Event> allEvents, List<EventEntity> detailedEvents) {
        List<Event> filteredEvents = new ArrayList<>();
        String lowerCaseQuery = (query == null) ? "" : query.toLowerCase();

        for (Event event : allEvents) {
            EventEntity correspondingEntity = null;
            if (detailedEvents != null) {
                for (EventEntity entity : detailedEvents) {
                    if (entity.getEventId().equals(event.getEventId())) {
                        correspondingEntity = entity;
                        break;
                    }
                }
            }

            if (correspondingEntity == null) {
                continue;
            }

            boolean availabilityMatch = false;
            int currentEntrantCount = (correspondingEntity.getEntrants() != null) ? correspondingEntity.getEntrants().size() : 0;
            int capacity = correspondingEntity.getCapacity();

            if (selectedAvailabilityId == RADIO_ID_NOT_FULL) {
                if (capacity == 0 || currentEntrantCount < capacity) {
                    availabilityMatch = true;
                }
            } else if (selectedAvailabilityId == RADIO_ID_FULL) {
                if (capacity > 0 && currentEntrantCount >= capacity) {
                    availabilityMatch = true;
                }
            } else {
                availabilityMatch = true;
            }

            if (availabilityMatch) {
                if (lowerCaseQuery.isEmpty() ||
                        event.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                        event.getDescription().toLowerCase().contains(lowerCaseQuery)) {
                    filteredEvents.add(event);
                }
            }
        }
        return filteredEvents;
    }

    @Before
    public void setUp() {
        Event event1 = new Event("1", "Tech Conference", "tech talk", 0, null, null, "org1", "Org1", false, null);
        EventEntity entity1 = new EventEntity();
        entity1.setEventId("1");
        entity1.setCapacity(100);
        entity1.setEntrants(new ArrayList<>(Arrays.asList("user1", "user2")));

        Event event2 = new Event("2", "Music Festival", "Live bands.", 0, null, null, "org2", "Org2", false, null);
        EventEntity entity2 = new EventEntity();
        entity2.setEventId("2");
        entity2.setCapacity(50);
        entity2.setEntrants(new ArrayList<>());
        for (int i = 0; i < 50; i++) {
            entity2.getEntrants().add("user" + i);
        }

        Event event3 = new Event("3", "Art Workshop", "A workshop on painting.", 0, null, null, "org3", "Org3", false, null);
        EventEntity entity3 = new EventEntity();
        entity3.setEventId("3");
        entity3.setCapacity(0);
        entity3.setEntrants(new ArrayList<>(Arrays.asList("userA", "userB")));

        Event event4 = new Event("4", "Book Club", "Discussing classic literature.", 0, null, null, "org4", "Org4", false, null);

        allEvents = Arrays.asList(event1, event2, event3, event4);
        allEventEntities = Arrays.asList(entity1, entity2, entity3);
    }

    /**
     * Verifies that searching by a keyword in the event title returns the correct event
     * when the availability filter is set to "Any".
     */
    @Test
    public void testFilterByKeyword_withAnyAvailability_returnsCorrectEvent() {
        List<Event> result = applyFilters("Music", RADIO_ID_ANY, allEvents, allEventEntities);
        assertEquals(1, result.size());
        assertEquals("2", result.get(0).getEventId());
    }

    /**
     * Verifies that searching by a keyword in the event description returns the correct event
     * when the availability filter is set to "Any".
     */
    @Test
    public void testFilterByDescription_withAnyAvailability_returnsCorrectEvent() {
        List<Event> result = applyFilters("tech talk", RADIO_ID_ANY, allEvents, allEventEntities);
        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getEventId());
    }

    /**
     * Verifies that an empty query returns all valid events (those with a corresponding entity)
     * when the availability filter is set to "Any".
     */
    @Test
    public void testFilterWithEmptyQuery_withAnyAvailability_returnsAllValidEvents() {
        List<Event> result = applyFilters("", RADIO_ID_ANY, allEvents, allEventEntities);
        assertEquals(3, result.size());
    }

    /**
     * Verifies that a query with no possible matches returns an empty list.
     */
    @Test
    public void testFilterWithNoMatches_withAnyAvailability_returnsEmptyList() {
        List<Event> result = applyFilters("Zebra", RADIO_ID_ANY, allEvents, allEventEntities);
        assertTrue(result.isEmpty());
    }

    /**
     * Verifies that the text-based search is case-insensitive.
     */
    @Test
    public void testFilterIsCaseInsensitive_withAnyAvailability() {
        List<Event> result = applyFilters("tech conference", RADIO_ID_ANY, allEvents, allEventEntities);
        assertEquals(1, result.size());
        assertEquals("1", result.get(0).getEventId());
    }

    /**
     * Verifies that the "Not Full" filter correctly returns events with available space
     * and events with unlimited capacity (capacity set to 0).
     */
    @Test
    public void testFilterForNotFullEvents_returnsCorrectEvents() {
        List<Event> result = applyFilters("", RADIO_ID_NOT_FULL, allEvents, allEventEntities);
        assertEquals("Expected two 'not full' events: one with space and one with unlimited capacity.", 2, result.size());

        boolean foundEvent1 = false;
        boolean foundEvent3 = false;
        for (Event event : result) {
            if (event.getEventId().equals("1")) {
                foundEvent1 = true;
            }
            if (event.getEventId().equals("3")) {
                foundEvent3 = true;
            }
        }
        assertTrue("Event with ID 1 should have been in the results", foundEvent1);
        assertTrue("Event with ID 3 should have been in the results", foundEvent3);
    }

    /**
     * Verifies that the "Full" filter correctly returns only events where the number of
     * entrants is equal to or greater than the capacity, and capacity is greater than 0.
     */
    @Test
    public void testFilterForFullEvents_returnsCorrectEvent() {
        List<Event> result = applyFilters("", RADIO_ID_FULL, allEvents, allEventEntities);
        assertEquals("Expected only one 'full' event.", 1, result.size());
        assertEquals("2", result.get(0).getEventId());
    }

    /**
     * Verifies that a text query combined with the "Not Full" filter returns the correct event
     * if it matches both criteria.
     */
    @Test
    public void testFilterWithKeywordAndNotFull_returnsCorrectEvent() {
        List<Event> result = applyFilters("workshop", RADIO_ID_NOT_FULL, allEvents, allEventEntities);
        assertEquals(1, result.size());
        assertEquals("3", result.get(0).getEventId());
    }

    /**
     * Verifies that a text query for an event that is full returns an empty list
     * when the "Not Full" filter is active.
     */
    @Test
    public void testFilterWithKeywordAndNotFull_returnsEmptyForFullEvent() {
        List<Event> result = applyFilters("Music", RADIO_ID_NOT_FULL, allEvents, allEventEntities);
        assertTrue(result.isEmpty());
    }

    /**
     * Verifies that a text query combined with the "Full" filter returns the correct event
     * if it matches both criteria.
     */
    @Test
    public void testFilterWithKeywordAndFull_returnsCorrectEvent() {
        List<Event> result = applyFilters("Music", RADIO_ID_FULL, allEvents, allEventEntities);
        assertEquals(1, result.size());
        assertEquals("2", result.get(0).getEventId());
    }

    /**
     * Verifies that a text query for an event that is not full returns an empty list
     * when the "Full" filter is active.
     */
    @Test
    public void testFilterWithKeywordAndFull_returnsEmptyForNotFullEvent() {
        List<Event> result = applyFilters("Tech", RADIO_ID_FULL, allEvents, allEventEntities);
        assertTrue(result.isEmpty());
    }
}
