package com.example.cmpuzz_events.ui.event;

import com.example.cmpuzz_events.Entrant;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Event {
    private String eventId;
    private String title;
    private String description;
    private int capacity;                    // Max attendees (attendance limit)
    private Date registrationStart;
    private Date registrationEnd;
    private String organizerId;
    private String organizerName;            // Display name of organizer
    private boolean geolocationRequired;
    private int maxEntrants;                 // Max people who can enroll
    private List<Entrant> waitingList;

    public Event(String eventId, String title, String description, int capacity,
                 Date registrationStart, Date registrationEnd,
                 String organizerId, String organizerName, boolean geolocationRequired) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.capacity = capacity;
        this.registrationStart = registrationStart;
        this.registrationEnd = registrationEnd;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.geolocationRequired = geolocationRequired;
        this.waitingList = new ArrayList<>();
    }

    public String getEventId() { return eventId; }
    public String getTitle() { return title; }
    public String getDescription() { return description; }
    public int getCapacity() { return capacity; }
    public Date getRegistrationStart() { return registrationStart; }
    public Date getRegistrationEnd() { return registrationEnd; }
    public String getOrganizerId() { return organizerId; }
    public String getOrganizerName() { return organizerName; }
    public boolean isGeolocationRequired() { return geolocationRequired; }
    public int getMaxEntrants() { return maxEntrants; }
    public List<Entrant> getWaitingList() { return waitingList; }
    
    public void setRegistrationStart(Date date) { this.registrationStart = date; }
    public void setRegistrationEnd(Date date) { this.registrationEnd = date; }
    public void setGeolocationRequired(boolean required) { this.geolocationRequired = required; }
    public void setMaxEntrants(int n) { this.maxEntrants = n; }
}
