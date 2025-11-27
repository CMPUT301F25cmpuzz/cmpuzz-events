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

    interface RegistrationHistoryCallback {
        void onSuccess(List<EventEntity> pastEvents);
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
    void getUIEventById(String eventId, UIEventCallback callback);

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
     * Get events where user is in the waitlist
     *
     * @param userId The user's ID
     * @param callback Callback with list of UI Events or error
     */
    void getEventsUserEnrolledIn(String userId, UIEventListCallback callback);

    /**
     * Get all events where user is involved in any capacity:
     * - In waitlist
     * - Has an invitation (pending, accepted, or declined)
     *
     * @param userId The user's ID
     * @param callback Callback with list of UI Events or error
     */
    void getEventsForUser(String userId, UIEventListCallback callback);

    /**
     * Get all EventEntity objects where user is involved (for status checking)
     *
     * @param userId The user's ID
     * @param callback Callback with list of EventEntity or error
     */
    void getEventsForUserWithEntities(String userId, EventListCallback callback);

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
     * Add a user to event waitlist
     *
     * @param eventId The event ID
     * @param userId The user ID to add
     * @param callback Callback on success or error
     */
    void addToWaitlist(String eventId, String userId, VoidCallback callback);

    /**
     * Join an event (add to waitlist)
     * Convenience method for users to join events
     *
     * @param eventId The event ID
     * @param userId The user ID to add
     * @param callback Callback on success or error
     */
    void joinEvent(String eventId, String userId, VoidCallback callback);

    /**
     * Joins an event with geolocation data.
     *
     * @param eventId   The ID of the event to join.
     * @param userId    The ID of the user joining.
     * @param latitude  The user's latitude.
     * @param longitude The user's longitude.
     * @param callback  Callback for success or error.
     */
    void joinEventWithLocation(String eventId, String userId, double latitude, double longitude, VoidCallback callback);

    /**
     * Remove a user from event waitlist
     *
     * @param eventId The event ID
     * @param userId The user ID to remove
     * @param callback Callback on success or error
     */
    void removeFromWaitlist(String eventId, String userId, VoidCallback callback);

    /**
     * Remove a user from event invitation list
     *
     * @param eventId The event ID
     * @param userId The user ID to remove
     * @param callback Callback on success or error
     */
    void removeFromInvitiationsList(String eventId, String userId, VoidCallback callback);


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
     * @param userId The user ID
     * @param accept True to accept, false to decline
     * @param callback Callback on success or error
     */
    void respondToInvitation(String eventId, String userId, boolean accept, VoidCallback callback);

    /**
     * Draw random attendees from waitlist and send them invitations.
     * If capacity is set (> 0), samples that many attendees.
     * Otherwise, samples a random number between 1 and maxEntrants.
     *
     * @param eventId The event ID
     * @param sampleSize Number of attendees to draw (null to use capacity or random 1-maxEntrants)
     * @param callback Callback on success or error
     */
    void drawAttendees(String eventId, Integer sampleSize, VoidCallback callback);


    /**
     * Retrieves the registration history for a specified user.
     *
     * @param userId  The ID of the user whose registration history is requested.
     * @param callback Callback invoked with the registration history on success,
     *                 or an error on failure.
     */
    void getRegistrationHistory(String userId, RegistrationHistoryCallback callback);
    /**
     * Draw a single replacement attendee from the waitlist.
     * Used when a previously selected entrant cancels or declines.
     *
     * @param eventId The event ID
     * @param callback Callback on success or error
     */
    void drawReplacementAttendee(String eventId, VoidCallback callback);
}
