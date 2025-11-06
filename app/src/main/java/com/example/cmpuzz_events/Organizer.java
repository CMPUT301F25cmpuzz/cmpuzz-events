package com.example.cmpuzz_events;

import android.util.Log;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.UUID;

/**
 * Represents an event organizer within the Event Lottery System application.
 * <p>
 * The Organizer class is responsible for creating and managing events,
 * drawing entrants from waiting lists, and communicating with attendees.
 * </p>
 *
 * <p>Implements several user stories including:
 * <ul>
 *     <li>US 02.01.01 – Create a new event</li>
 *     <li>US 02.01.04 – Set registration period</li>
 *     <li>US 02.02.01 – View entrants for created events</li>
 *     <li>US 02.02.02 – View entrants on map</li>
 *     <li>US 02.02.03 – Enable/disable geolocation</li>
 *     <li>US 02.03.01 – Limit max entrants</li>
 *     <li>US 02.06.01 – Manage enrolled or cancelled entrants</li>
 * </ul>
 * </p>
 */
public class Organizer {

    /** Unique identifier of the organizer (Firebase UID or generated ID). */
    private String organizerId;

    /** Full name of the organizer. */
    private String name;

    /** Organizer’s contact email address. */
    private String email;

    /** Optional contact phone number. */
    private String phone;

    /** List of events created by this organizer. */
    private List<Event> createdEvents;

    /** Default constructor required for Firebase deserialization. */
    public Organizer() {
        this.createdEvents = new ArrayList<>();
    }

    /** Constructs an Organizer object with identity info. */
    public Organizer(String organizerId, String name, String email) {
        this.organizerId = organizerId;
        this.name = name;
        this.email = email;
        this.createdEvents = new ArrayList<>();
    }

    // ------------------------------------------------------------
    // USER STORIES:
    // US 02.01.01 - Create a new event
    // US 02.01.04 - Set registration period
    // ------------------------------------------------------------

    /**
     * Creates a new event with the given details and associates it with this organizer.
     * @param title             Event title
     * @param description       Event description
     * @param capacity          Maximum number of attendees
     * @param registrationStart Registration open date
     * @param registrationEnd   Registration close date
     * @param geolocationReq    Whether geolocation is required
     * @return The newly created Event object
     */
    public Event createEvent(String title, String description, int capacity,
                             Date registrationStart, Date registrationEnd,
                             boolean geolocationReq) {

        String eventId = UUID.randomUUID().toString();
        Event event = new Event(eventId, title, description, capacity,
                registrationStart, registrationEnd,
                this.organizerId, geolocationReq);

        Log.d("Organizer", "New event created: " + event.getTitle());

        if (createdEvents == null) createdEvents = new ArrayList<>();
        createdEvents.add(event);

        // TODO: integrate with Firestore
        return event;
    }

    /**
     * Updates the registration period for a specific event.
     */
    public boolean setRegistrationPeriod(String eventId, Date start, Date end) {
        if (createdEvents == null) return false;

        for (Event e : createdEvents) {
            if (e.getEventId().equals(eventId)) {
                e.setRegistrationStart(start);
                e.setRegistrationEnd(end);
                // TODO: update in Firestore
                return true;
            }
        }
        return false;
    }

    // ------------------------------------------------------------
    // USER STORIES:
    // US 02.02.01 - View entrants in waiting list
    // US 02.02.02 - View entrants on map
    // US 02.02.03 - Enable/disable geolocation
    // US 02.03.01 - Limit max entrants
    // ------------------------------------------------------------

    public List<Entrant> getEntrantsForEvent(String eventId) {
        if (createdEvents == null) return new ArrayList<>();

        for (Event e : createdEvents) {
            if (e.getEventId().equals(eventId)) {
                return e.getWaitingList();
            }
        }
        return new ArrayList<>();
    }

    public boolean setGeolocationRequirement(String eventId, boolean isEnabled) {
        if (createdEvents == null) return false;

        for (Event e : createdEvents) {
            if (e.getEventId().equals(eventId)) {
                e.setGeolocationRequired(isEnabled);
                // TODO: update in Firestore
                return true;
            }
        }
        return false;
    }

    public boolean setMaxEntrants(String eventId, int maxEntrants) {
        if (createdEvents == null) return false;

        for (Event e : createdEvents) {
            if (e.getEventId().equals(eventId)) {
                e.setMaxEntrants(maxEntrants);
                // TODO: update in Firestore
                return true;
            }
        }
        return false;
    }

    public List<double[]> getEntrantLocations(String eventId) {
        List<double[]> locations = new ArrayList<>();
        if (createdEvents == null) return locations;

        for (Event e : createdEvents) {
            if (e.getEventId().equals(eventId)) {
                for (Entrant entrant : e.getWaitingList()) {
                    if (entrant.getLatitude() != null && entrant.getLongitude() != null) {
                        locations.add(new double[]{entrant.getLatitude(), entrant.getLongitude()});
                    }
                }
            }
        }
        return locations;
    }

    // ------------------------------------------------------------
    // Getters and Setters
    // ------------------------------------------------------------

    public String getOrganizerId() { return organizerId; }
    public void setOrganizerId(String organizerId) { this.organizerId = organizerId; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public List<Event> getCreatedEvents() { return createdEvents; }
    public void setCreatedEvents(List<Event> createdEvents) { this.createdEvents = createdEvents; }
}
