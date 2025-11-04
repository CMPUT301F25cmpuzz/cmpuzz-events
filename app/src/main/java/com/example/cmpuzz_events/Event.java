package com.example.cmpuzz_events;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class Event {
    private String eventId;
    private String title;
    private String description;
    private int capacity;
    private Date registrationStart;
    private Date registrationEnd;
    private String organizerId;
    private boolean geolocationRequired;
    private int maxEntrants;
    private List<Entrant> waitingList;

    public Event(String eventId, String title, String description, int capacity,
                 Date registrationStart, Date registrationEnd,
                 String organizerId, boolean geolocationRequired) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.capacity = capacity;
        this.registrationStart = registrationStart;
        this.registrationEnd = registrationEnd;
        this.organizerId = organizerId;
        this.geolocationRequired = geolocationRequired;
        this.waitingList = new ArrayList<>();
    }

    public String getEventId() { return eventId; }
    public void setRegistrationStart(Date date) { this.registrationStart = date; }
    public void setRegistrationEnd(Date date) { this.registrationEnd = date; }
    public void setGeolocationRequired(boolean required) { this.geolocationRequired = required; }
    public void setMaxEntrants(int n) { this.maxEntrants = n; }
    public List<Entrant> getWaitingList() { return waitingList; }

    public String getTitle() {
        return title;
    }

}
