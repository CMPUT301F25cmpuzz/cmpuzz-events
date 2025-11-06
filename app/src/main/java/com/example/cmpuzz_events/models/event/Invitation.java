package com.example.cmpuzz_events.models.event;

import java.util.Date;
import java.util.HashMap;
import java.util.Map;

/**
 * Represents an invitation sent to a user for an event.
 * Tracks invitation status (pending, accepted, declined).
 */
public class Invitation {
    private String deviceId;
    private String username;           // Optional: for display purposes
    private InvitationStatus status;
    private Date invitedAt;
    private Date respondedAt;

    public enum InvitationStatus {
        PENDING,
        ACCEPTED,
        DECLINED,
        CANCELLED;  // Organizer cancelled the invitation

        public static InvitationStatus fromString(String status) {
            try {
                return InvitationStatus.valueOf(status.toUpperCase());
            } catch (Exception e) {
                return PENDING;
            }
        }
    }

    /**
     * Default constructor for Firebase
     */
    public Invitation() {
        this.status = InvitationStatus.PENDING;
        this.invitedAt = new Date();
    }

    /**
     * Constructor for creating a new invitation
     */
    public Invitation(String deviceId, String username) {
        this.deviceId = deviceId;
        this.username = username;
        this.status = InvitationStatus.PENDING;
        this.invitedAt = new Date();
    }

    /**
     * Accept the invitation
     */
    public void accept() {
        this.status = InvitationStatus.ACCEPTED;
        this.respondedAt = new Date();
    }

    /**
     * Decline the invitation
     */
    public void decline() {
        this.status = InvitationStatus.DECLINED;
        this.respondedAt = new Date();
    }

    /**
     * Cancel the invitation (by organizer)
     */
    public void cancel() {
        this.status = InvitationStatus.CANCELLED;
        this.respondedAt = new Date();
    }

    /**
     * Convert to Map for Firebase
     */
    public Map<String, Object> toMap() {
        Map<String, Object> map = new HashMap<>();
        map.put("deviceId", deviceId);
        map.put("username", username);
        map.put("status", status.name());
        map.put("invitedAt", invitedAt);
        map.put("respondedAt", respondedAt);
        return map;
    }

    // Getters and Setters

    public String getDeviceId() {
        return deviceId;
    }

    public void setDeviceId(String deviceId) {
        this.deviceId = deviceId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public InvitationStatus getStatus() {
        return status;
    }

    public void setStatus(InvitationStatus status) {
        this.status = status;
    }

    public Date getInvitedAt() {
        return invitedAt;
    }

    public void setInvitedAt(Date invitedAt) {
        this.invitedAt = invitedAt;
    }

    public Date getRespondedAt() {
        return respondedAt;
    }

    public void setRespondedAt(Date respondedAt) {
        this.respondedAt = respondedAt;
    }

    public boolean isPending() {
        return status == InvitationStatus.PENDING;
    }

    public boolean isAccepted() {
        return status == InvitationStatus.ACCEPTED;
    }

    public boolean isDeclined() {
        return status == InvitationStatus.DECLINED;
    }
}
