package com.example.cmpuzz_events;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

import com.example.cmpuzz_events.models.event.EventEntity;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Unit tests for Geolocation data handling.
 * Verifies that coordinates are stored, retrieved, and serialized correctly
 * to ensure the Map feature receives valid data.
 */
public class GeolocationTest {

    private EventEntity event;

    @Before
    public void setUp() {
        // Initialize a standard event before each test
        event = new EventEntity(
                "evt_geo_test", "Map Event", "Testing Maps", 10,
                new Date(), new Date(), "org1", "Organizer",
                true, 10
        );
    }

    @Test
    public void testGeolocationEnabledByDefault() {
        // Verify the toggle state logic
        assertTrue("Geolocation should be required based on constructor", event.isGeolocationRequired());

        event.setGeolocationRequired(false);
        assertFalse("Geolocation should be disabled when set to false", event.isGeolocationRequired());
    }

    @Test
    public void testAddLocation_StoresCoordinatesCorrectly() {
        String userId = "user_123";
        double lat = 53.5461;
        double lon = -113.4938;

        // Act
        event.addLocation(userId, lat, lon);

        // Assert
        Map<String, List<Double>> locations = event.getEntrantLocations();
        assertNotNull("Location map should not be null", locations);
        assertTrue("Map should contain the user ID", locations.containsKey(userId));

        List<Double> coords = locations.get(userId);
        assertNotNull("Coordinates list should not be null", coords);
        assertEquals("Should store exactly 2 values (lat, lon)", 2, coords.size());
        assertEquals("Latitude should match", lat, coords.get(0), 0.0001);
        assertEquals("Longitude should match", lon, coords.get(1), 0.0001);
    }

    @Test
    public void testAddLocation_UpdatesExistingUser() {
        String userId = "user_moving";

        // Initial location (Edmonton)
        event.addLocation(userId, 53.54, -113.49);

        // New location (Calgary)
        double newLat = 51.04;
        double newLon = -114.07;
        event.addLocation(userId, newLat, newLon);

        // Assert
        List<Double> coords = event.getEntrantLocations().get(userId);
        assertEquals("Latitude should be updated", newLat, coords.get(0), 0.0001);
        assertEquals("Longitude should be updated", newLon, coords.get(1), 0.0001);
        assertEquals("Map size should remain 1", 1, event.getEntrantLocations().size());
    }

    @Test
    public void testMultipleUsersLocations() {
        // Add multiple users
        event.addLocation("userA", 10.0, 20.0);
        event.addLocation("userB", 30.0, 40.0);
        event.addLocation("userC", 50.0, 60.0);

        Map<String, List<Double>> locations = event.getEntrantLocations();

        assertEquals("Should have 3 distinct locations", 3, locations.size());
        assertEquals(10.0, locations.get("userA").get(0), 0.001);
        assertEquals(40.0, locations.get("userB").get(1), 0.001);
    }

    @Test
    public void testToMap_IncludesLocationData() {
        // Prepare data
        event.addLocation("user_map_test", 45.0, -90.0);

        // Act
        Map<String, Object> firestoreMap = event.toMap();

        // Assert
        assertTrue("Map must contain entrantLocations key", firestoreMap.containsKey("entrantLocations"));

        @SuppressWarnings("unchecked")
        Map<String, List<Double>> storedLocs = (Map<String, List<Double>>) firestoreMap.get("entrantLocations");

        assertNotNull(storedLocs);
        assertTrue(storedLocs.containsKey("user_map_test"));
        assertEquals(45.0, storedLocs.get("user_map_test").get(0), 0.001);
    }

    /**
     * This test simulates the logic inside EventService.documentToEventEntity
     * to ensure that if Firebase returns a List<Object>, our code logic handles it.
     */
    @Test
    public void testDataParsingSimulation() {
        // Simulate data coming from Firebase (which often treats numbers as generic Objects or Longs)
        Map<String, Object> rawFirestoreMap = new HashMap<>();
        List<Object> coords = Arrays.asList(53.5461, -113.4938); // Firebase might return this

        Map<String, Object> locationsMap = new HashMap<>();
        locationsMap.put("user_123", coords);

        // Manual simulation of the parsing logic you added to EventService
        EventEntity parsedEvent = new EventEntity();

        if (locationsMap != null) {
            for (Map.Entry<String, Object> entry : locationsMap.entrySet()) {
                if (entry.getValue() instanceof List) {
                    List<?> rawList = (List<?>) entry.getValue();
                    if (rawList.size() >= 2) {
                        // Cast to Number first (handles both Integer, Long, and Double from DB)
                        double lat = ((Number) rawList.get(0)).doubleValue();
                        double lon = ((Number) rawList.get(1)).doubleValue();
                        parsedEvent.addLocation(entry.getKey(), lat, lon);
                    }
                }
            }
        }

        // Assert the parsing logic worked
        assertNotNull(parsedEvent.getEntrantLocations());
        assertTrue(parsedEvent.getEntrantLocations().containsKey("user_123"));
        assertEquals(53.5461, parsedEvent.getEntrantLocations().get("user_123").get(0), 0.0001);
    }
}