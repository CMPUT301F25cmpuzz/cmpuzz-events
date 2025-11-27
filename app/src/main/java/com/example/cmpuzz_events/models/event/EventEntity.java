package com.example.cmpuzz_events.models.event;

import android.content.Context;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
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
    private int capacity = 0;                    // Max attendees (attendance limit from UI)
    private Date registrationStart;
    private Date registrationEnd;
    private String organizerId;
    private String organizerName;            // Display name of organizer
    private boolean geolocationRequired;
    private int maxEntrants = 0;                 // Max people who can ENROLL into the event

    // Backend-specific fields
    private List<String> waitlist;           // Array of device IDs on waitlist (ALL entrants)
    private List<Invitation> invitations;    // Array of Invitation objects (invited entrants)
    private List<String> attendees;          // Array of device IDs who are confirmed attendees
    private List<String> declined;           // Array of device IDs who declined invitations
    private String qrCodeUrl;                // Unique URL for QR code
    private Date createdAt;
    private Date updatedAt;
    private List<String> entrants;

    /**
     * Default constructor required for Firebase deserialization
     */
    public EventEntity() {
        this.waitlist = new ArrayList<>();
        this.invitations = new ArrayList<>();
        this.attendees = new ArrayList<>();
        this.declined = new ArrayList<>();
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    /**
     * Constructor for creating a new event
     */
    public EventEntity(String eventId, String title, String description, int capacity,
                       Date registrationStart, Date registrationEnd,
                       String organizerId, String organizerName, boolean geolocationRequired, int maxEntrants) {
        this.eventId = eventId;
        this.title = title;
        this.description = description;
        this.capacity = capacity;
        this.registrationStart = registrationStart;
        this.registrationEnd = registrationEnd;
        this.organizerId = organizerId;
        this.organizerName = organizerName;
        this.geolocationRequired = geolocationRequired;
        this.maxEntrants = maxEntrants;
        this.waitlist = new ArrayList<>();
        this.invitations = new ArrayList<>();
        this.attendees = new ArrayList<>();
        this.declined = new ArrayList<>();
        this.qrCodeUrl = generateQRCodeUrl(eventId);
        this.createdAt = new Date();
        this.updatedAt = new Date();
    }

    /**
     * Generate unique URL for QR code
     */
    private static String generateQRCodeUrl(String eventId) {
        // Deep link format that your app can handle
        return "cmpuzzevents://event/" + eventId;
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
        map.put("organizerName", organizerName);
        map.put("geolocationRequired", geolocationRequired);
        map.put("maxEntrants", maxEntrants);
        map.put("waitlist", waitlist);
        map.put("attendees", attendees);
        map.put("declined", declined);

        // Convert invitations to list of maps
        List<Map<String, Object>> invitationMaps = new ArrayList<>();
        for (Invitation inv : invitations) {
            invitationMaps.add(inv.toMap());
        }
        map.put("invitations", invitationMaps);

        map.put("qrCodeUrl", qrCodeUrl);
        map.put("createdAt", createdAt);
        map.put("updatedAt", updatedAt);

        return map;
    }

    /**
     * Add a user to the waitlist
     */
    public boolean addToWaitlist(String userId) {
        if (waitlist.contains(userId)) {
            return false; // Already on waitlist
        }
        if (maxEntrants == 0) {
            return false; // Event not accepting entrants
        }
        if (maxEntrants > 0 && waitlist.size() >= maxEntrants) {
            return false; // Waitlist is full
        }
        waitlist.add(userId);
        this.updatedAt = new Date();
        return true;
    }

    /**
     * Remove a user from the waitlist
     */
    public boolean removeFromWaitlist(String userId) {
        boolean removed = waitlist.remove(userId);
        if (removed) {
            this.updatedAt = new Date();
        }
        return removed;
    }

    /**
     * Remove a user from the invitation list
     */
    public boolean removeFromInvitationsList(String userId) {
        boolean removed = invitations.removeIf(inv -> inv.getUserId().equals(userId));
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
     * Get invitation by user ID
     */
    public Invitation getInvitationByUserId(String userId) {
        for (Invitation inv : invitations) {
            if (inv.getUserId().equals(userId)) {
                return inv;
            }
        }
        return null;
    }

    /**
     * Remove an invitation by user ID
     */
    public boolean removeInvitation(String userId) {
        Invitation toRemove = getInvitationByUserId(userId);
        if (toRemove != null) {
            boolean removed = invitations.remove(toRemove);
            if (removed) {
                this.updatedAt = new Date();
            }
            return removed;
        }
        return false;
    }

    // Getters and Setters

    /**
     * Gets event ID
     *
     * @return stored event ID
     */
    public String getEventId() {
        return eventId;
    }

    /**
     * Sets event ID
     *
     * @param eventId to set value
     */
    public void setEventId(String eventId) {
        this.eventId = eventId;
    }

    /**
     * Gets event title
     *
     * @return stored event title
     */
    public String getTitle() {
        return title;
    }

    /**
     * Sets event title
     *
     * @param title to set value
     */
    public void setTitle(String title) {
        this.title = title;
        this.updatedAt = new Date();
    }

    /**
     * Gets event description
     *
     * @return stored event description
     */
    public String getDescription() {
        return description;
    }

    /**
     * Sets event description
     *
     * @param description to set description
     */
    public void setDescription(String description) {
        this.description = description;
        this.updatedAt = new Date();
    }

    /**
     * Gets event capacity
     *
     * @return stored event capacity
     */
    public int getCapacity() {
        return capacity;
    }

    /**
     * Sets event capacity and stores update date object
     *
     * @param capacity to set capacity
     */
    public void setCapacity(int capacity) {
        this.capacity = capacity;
        this.updatedAt = new Date();
    }

    /**
     * Gets registration start date
     *
     * @return stored registration start date
     */
    public Date getRegistrationStart() {
        return registrationStart;
    }

    /**
     * Sets registration start date
     *
     * @param registrationStart to set registration start date
     */
    public void setRegistrationStart(Date registrationStart) {
        this.registrationStart = registrationStart;
        this.updatedAt = new Date();
    }

    /**
     * Gets registration end date
     *
     * @return stored registration end date
     */
    public Date getRegistrationEnd() {
        return registrationEnd;
    }

    /**
     * Sets registration end date
     *
     * @param registrationEnd to set registration end datew
     */
    public void setRegistrationEnd(Date registrationEnd) {
        this.registrationEnd = registrationEnd;
        this.updatedAt = new Date();
    }

    /**
     * Gets organizer ID
     *
     * @return stored organizer ID
     */
    public String getOrganizerId() {
        return organizerId;
    }

    /**
     * Sets organizer ID
     *
     * @param organizerId to set organizer ID
     */
    public void setOrganizerId(String organizerId) {
        this.organizerId = organizerId;
    }

    /**
     * Gets organizer name
     *
     * @return stored organizer name
     */
    public String getOrganizerName() {
        return organizerName;
    }

    /**
     * Sets organizer name
     *
     * @param organizerName to set organizer name
     */
    public void setOrganizerName(String organizerName) {
        this.organizerName = organizerName;
    }

    /**
     * Gets geolocation requirement
     *
     * @return stored geolocation requirement
     */
    public boolean isGeolocationRequired() {
        return geolocationRequired;
    }

    /**
     * Sets geolocation requirement
     *
     * @param geolocationRequired to set geolocation requirement
     */
    public void setGeolocationRequired(boolean geolocationRequired) {
        this.geolocationRequired = geolocationRequired;
        this.updatedAt = new Date();
    }

    /**
     * Gets max entrants
     *
     * @return stored max entrants
     */
    public int getMaxEntrants() {
        return maxEntrants;
    }

    /**
     * Sets max amount of entrants
     *
     * @param maxEntrants to set max amount of entrants
     */
    public void setMaxEntrants(int maxEntrants) {
        this.maxEntrants = maxEntrants;
        this.updatedAt = new Date();
    }

    /**
     * Gets waitlist
     *
     * @return stored waitlist
     */
    public List<String> getWaitlist() {
        return waitlist;
    }

    /**
     * Sets waitlist
     *
     * @param waitlist to set waitlist
     */
    public void setWaitlist(List<String> waitlist) {
        this.waitlist = waitlist;
        this.updatedAt = new Date();
    }

    /**
     * Gets list of invitations using a list of the Invitation class
     *
     * @return invitation list
     */
    public List<Invitation> getInvitations() {
        return invitations;
    }

    /**
     * Sets invitation list
     *
     * @param invitations to set invitation list
     */
    public void setInvitations(List<Invitation> invitations) {
        this.invitations = invitations;
        this.updatedAt = new Date();
    }

    /**
     * Gets attendee list
     *
     * @return store attendee list
     */
    public List<String> getAttendees() {
        return attendees;
    }

    /**
     * Sets attendee list
     *
     * @param attendees to set attendee list
     */
    public void setAttendees(List<String> attendees) {
        this.attendees = attendees;
        this.updatedAt = new Date();
    }

    /**
     * Get list of all entrants who have declined
     *
     * @return list of declined entrants
     */
    public List<String> getDeclined() {
        return declined;
    }

    /**
     * Sets list of declined entrants
     *
     * @param declined to set declined entrant list
     */
    public void setDeclined(List<String> declined) {
        this.declined = declined;
        this.updatedAt = new Date();
    }

    /**
     * Gets Date object event was created at
     *
     * @return stored Date object from creation
     */
    public Date getCreatedAt() {
        return createdAt;
    }

    /**
     * Sets original created date
     *
     * @param createdAt to set created date
     */
    public void setCreatedAt(Date createdAt) {
        this.createdAt = createdAt;
    }

    /**
     * Gets last updated Date
     *
     * @return stored last updated Date
     */
    public Date getUpdatedAt() {
        return updatedAt;
    }

    /**
     * Sets last updated date
     *
     * @param updatedAt to set last updated date
     */
    public void setUpdatedAt(Date updatedAt) {
        this.updatedAt = updatedAt;
    }

    /**
     * Gets URL of QR code
     *
     * @return stored URL of QR code
     */
    public String getQrCodeUrl() {
        return qrCodeUrl;
    }

    /**
     * Sets URL of QR code
     *
     * @param qrCodeUrl to set URL of QR code
     */
    public void setQrCodeUrl(String qrCodeUrl) {
        this.qrCodeUrl = qrCodeUrl;
    }

    public List<String> getEntrants() {
        return this.entrants;
    }

    public void setEntrants(List<String> entrants) {
        this.entrants = entrants;
    }

}