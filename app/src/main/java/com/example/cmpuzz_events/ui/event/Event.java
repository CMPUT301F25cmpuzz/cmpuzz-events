package com.example.cmpuzz_events.ui.event;

import com.example.cmpuzz_events.Entrant;
import com.example.cmpuzz_events.models.event.Invitation;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Event implements Serializable {
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
    private List<String> waitlist;           // Array of device IDs on waitlist (ALL entrants)
    private List<Invitation> invitations;    // Array of Invitation objects (invited entrants)
    private List<String> attendees;          // Array of device IDs who are confirmed attendees
    private List<String> declined;

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
        this.waitlist = new ArrayList<>();
        this.invitations = new ArrayList<>();
        this.attendees = new ArrayList<>();
        this.declined = new ArrayList<>();
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
    public List<String> getWaitlist() { return waitlist; }
    public void setWaitlist(List<String> waitlist) {
        this.waitlist = waitlist;
    }

    public List<Invitation> getInvitations() { return invitations; }
    public void setInvitations(List<Invitation> invitations) {
        this.invitations = invitations;
    }
    
    public void setRegistrationStart(Date date) { this.registrationStart = date; }
    public void setRegistrationEnd(Date date) { this.registrationEnd = date; }
    public void setGeolocationRequired(boolean required) { this.geolocationRequired = required; }
    public void setMaxEntrants(int n) { this.maxEntrants = n; }

    public void clearEntrants()
    {

    }
}
