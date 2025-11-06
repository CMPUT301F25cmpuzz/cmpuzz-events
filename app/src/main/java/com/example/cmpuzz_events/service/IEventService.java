package com.example.cmpuzz_events.service;

import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.models.event.Invitation;
import com.example.cmpuzz_events.ui.event.Event;

import java.util.List;

/**
 * Interface for Event service operations.
 * Handles conversion between UI Event and EventEntity, and Firebase operations.
 */
public interface IEventService {

    /**
     * Callback interface for async operations
     */
    interface EventCallback {
        void onSuccess(EventEntity event);
        void onError(String error);
    }

    interface EventListCallback {
        void onSuccess(List<EventEntity> events);
        void onError(String error);
    }

    interface UIEventCallback {
        void onSuccess(Event event);
        void onError(String error);
    }

    interface UIEventListCallback {
        void onSuccess(List<Event> events);
        void onError(String error);
    }

    interface VoidCallback {
        void onSuccess();
        void onError(String error);
    }

    /**
     * Create a new event from UI Event model.
     * Converts UI Event to EventEntity and saves to Firebase.
     *
     * @param uiEvent The UI Event from the form
     * @param callback Callback with the created EventEntity or error
     */
    void createEvent(Event uiEvent, EventCallback callback);

    /**
     * Get an event by ID from Firebase
     *
     * @param eventId The event ID
     * @param callback Callback with the EventEntity or error
     */
    void getEvent(String eventId, EventCallback callback);

    /**
     * Get an event as UI Event by ID from Firebase
     *
     * @param eventId The event ID
     * @param callback Callback with the UI Event or error
     */
    void getUIEvent(String eventId, UIEventCallback callback);

    /**
     * Get all events for an organizer
     *
     * @param organizerId The organizer's ID
     * @param callback Callback with list of events or error
     */
    void getEventsForOrganizer(String organizerId, EventListCallback callback);

    /**
     * Get all events as UI Events (for browsing/joining)
     *
     * @param callback Callback with list of UI Events or error
     */
    void getAllEvents(UIEventListCallback callback);

    /**
     * Update an existing event
     *
     * @param event The EventEntity to update
     * @param callback Callback on success or error
     */
    void updateEvent(EventEntity event, VoidCallback callback);

    /**
     * Delete an event
     *
     * @param eventId The event ID to delete
     * @param callback Callback on success or error
     */
    void deleteEvent(String eventId, VoidCallback callback);

    /**
     * Add a device to event waitlist
     *
     * @param eventId The event ID
     * @param deviceId The device ID to add
     * @param callback Callback on success or error
     */
    void addToWaitlist(String eventId, String deviceId, VoidCallback callback);

    /**
     * Join an event (add to waitlist)
     * Convenience method for users to join events
     *
     * @param eventId The event ID
     * @param deviceId The device ID to add
     * @param callback Callback on success or error
     */
    void joinEvent(String eventId, String deviceId, VoidCallback callback);

    /**
     * Remove a device from event waitlist
     *
     * @param eventId The event ID
     * @param deviceId The device ID to remove
     * @param callback Callback on success or error
     */
    void removeFromWaitlist(String eventId, String deviceId, VoidCallback callback);

    /**
     * Send invitations to selected users from waitlist
     * (Random selection logic will be implemented later)
     *
     * @param eventId The event ID
     * @param invitations List of invitations to send
     * @param callback Callback on success or error
     */
    void sendInvitations(String eventId, List<Invitation> invitations, VoidCallback callback);

    /**
     * Update invitation status (accept/decline)
     *
     * @param eventId The event ID
     * @param deviceId The device ID
     * @param accept True to accept, false to decline
     * @param callback Callback on success or error
     */
    void respondToInvitation(String eventId, String deviceId, boolean accept, VoidCallback callback);
}
