package com.example.cmpuzz_events;

import static org.junit.Assert.*;
import org.junit.Test;

public class EntrantTest {

    @Test
    public void testEntrantConstructorAndGetters() {
        Double lat = 53.5461;
        Double lon = -113.4938;
        String name = "John Doe";

        Entrant entrant = new Entrant(name, lat, lon);

        assertEquals(name, entrant.getName());
        assertEquals(lat, entrant.getLatitude());
        assertEquals(lon, entrant.getLongitude());
    }
}