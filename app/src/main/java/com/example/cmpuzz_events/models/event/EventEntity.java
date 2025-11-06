package com.example.cmpuzz_events.models.event;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Backend Firebase model for Events.
 * This is the entity that gets saved to Firestore.
 * Combines UI Event fields with backend-specific fields like waitlist and invitations.
 */
public class EventEntity {
    private String eventId;
    private String title;
    private String description;
    private int capacity;                    // Max attendees (attendance limit from UI)
    private Date registrationStart;
    private Date registrationEnd;
    private String organizerId;
    private boolean geolocationRequired;
    private int maxEntrants;                 // Max people who can ENROLL into the event
    
    // Backend-specific fields
    private List<String> waitlist;           // Array of device IDs on waitlist
    private List<Invitation> invitations;    // Array of Invitation objects
    private Date createdAt;
    private Date updatedAt;

    /**
     * Default constructor required for Firebase deserialization
     */
    public EventEntity() {
        this.waitlist = new ArrayList<>();
        this.invitations = new ArrayList<>();
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    /**
     * Constructor for creating a new event
     */
    public EventEntity(String eventId, String title, String description, int capacity,
                       Date registrationStart, Date registrationEnd,
                       String organizerId, boolean geolocationRequired, int maxEntrants) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.capacity = capacity;
        this.registrationStart = registrationStart;
        this.registrationEnd = registrationEnd;
        this.organizerId = organizerId;
        this.geolocationRequired = geolocationRequired;
        this.maxEntrants = maxEntrants;
        this.waitlist = new ArrayList<>();
        this.invitations = new ArrayList<>();
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    /**
     * Convert EventEntity to Map for Firebase
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("eventId", eventId);
        map.put("title", title);
        map.put("description", description);
        map.put("capacity", capacity);
        map.put("registrationStart", registrationStart);
        map.put("registrationEnd", registrationEnd);
        map.put("organizerId", organizerId);
        map.put("geolocationRequired", geolocationRequired);
        map.put("maxEntrants", maxEntrants);
        map.put("waitlist", waitlist);
        
        // Convert invitations to list of maps
        List<Map<String, Object>> invitationMaps = new ArrayList<>();
        for (Invitation inv : invitations) {
            invitationMaps.add(inv.toMap());
        }
        map.put("invitations", invitationMaps);
        
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);
        
        return map;
    }

    /**
     * Add a device to the waitlist
     */
    public boolean addToWaitlist(String deviceId) {
        if (waitlist.contains(deviceId)) {
            return false; // Already on waitlist
        }
        if (maxEntrants > 0 && waitlist.size() >= maxEntrants) {
            return false; // Waitlist is full
        }
        waitlist.add(deviceId);
        this.updatedAt = new Date();
        return true;
    }

    /**
     * Remove a device from the waitlist
     */
    public boolean removeFromWaitlist(String deviceId) {
        boolean removed = waitlist.remove(deviceId);
        if (removed) {
            this.updatedAt = new Date();
        }
        return removed;
    }

    /**
     * Add an invitation
     */
    public void addInvitation(Invitation invitation) {
        invitations.add(invitation);
        this.updatedAt = new Date();
    }

    /**
     * Get invitation by device ID
     */
    public Invitation getInvitationByDeviceId(String deviceId) {
        for (Invitation inv : invitations) {
            if (inv.getDeviceId().equals(deviceId)) {
                return inv;
            }
        }
        return null;
    }

    // Getters and Setters

    public String getEventId() {
        return eventId;
    }

    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = new Date();
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = new Date();
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
        this.updatedAt = new Date();
    }

    public Date getRegistrationStart() {
        return registrationStart;
    }

    public void setRegistrationStart(Date registrationStart) {
        this.registrationStart = registrationStart;
        this.updatedAt = new Date();
    }

    public Date getRegistrationEnd() {
        return registrationEnd;
    }

    public void setRegistrationEnd(Date registrationEnd) {
        this.registrationEnd = registrationEnd;
        this.updatedAt = new Date();
    }

    public String getOrganizerId() {
        return organizerId;
    }

    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    public boolean isGeolocationRequired() {
        return geolocationRequired;
    }

    public void setGeolocationRequired(boolean geolocationRequired) {
        this.geolocationRequired = geolocationRequired;
        this.updatedAt = new Date();
    }

    public int getMaxEntrants() {
        return maxEntrants;
    }

    public void setMaxEntrants(int maxEntrants) {
        this.maxEntrants = maxEntrants;
        this.updatedAt = new Date();
    }

    public List<String> getWaitlist() {
        return waitlist;
    }

    public void setWaitlist(List<String> waitlist) {
        this.waitlist = waitlist;
        this.updatedAt = new Date();
    }

    public List<Invitation> getInvitations() {
        return invitations;
    }

    public void setInvitations(List<Invitation> invitations) {
        this.invitations = invitations;
        this.updatedAt = new Date();
    }

    public Date getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    public Date getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }
}
