package com.example.cmpuzz_events.service;

import android.util.Log;

import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.models.event.Invitation;
import com.example.cmpuzz_events.ui.event.Event;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Random;

import com.example.cmpuzz_events.models.notification.Notification;

import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import java.util.Date;

/**
 * Implementation of IEventService.
 * Handles Event CRUD operations with Firebase Firestore.
 * Converts between UI Event model and EventEntity backend model.
 */
public class EventService implements IEventService {
    private static final String TAG = "EventService";
    private static final String COLLECTION_EVENTS = "events";
    
    private static EventService instance;
    private final FirebaseFirestore db;

    private EventService() {
        db = FirebaseFirestore.getInstance();
    }

    /**
     * Get singleton instance
     */
    public static synchronized EventService getInstance() {
        if (instance == null) {
            instance = new EventService();
        }
        return instance;
    }

    /**
     * Convert UI Event to EventEntity
     */
    private EventEntity convertToEntity(Event uiEvent) {
        return new EventEntity(
            uiEvent.getEventId(),
            uiEvent.getTitle(),
            uiEvent.getDescription(),
            uiEvent.getCapacity(),
            uiEvent.getRegistrationStart(),
            uiEvent.getRegistrationEnd(),
            uiEvent.getOrganizerId(),
            uiEvent.getOrganizerName(),
            uiEvent.isGeolocationRequired(),
            uiEvent.getMaxEntrants()
        );
    }

    /**
     * Convert EventEntity to UI Event
     */
    private Event convertToUIEvent(EventEntity entity) {
        Event uiEvent = new Event(
            entity.getEventId(),
            entity.getTitle(),
            entity.getDescription(),
            entity.getCapacity(),
            entity.getRegistrationStart(),
            entity.getRegistrationEnd(),
            entity.getOrganizerId(),
            entity.getOrganizerName(),
            entity.isGeolocationRequired()
        );
        uiEvent.setMaxEntrants(entity.getMaxEntrants());
        return uiEvent;
    }

    /**
     * Creates a new event in Firestore.
     *
     * @param uiEvent  The UI event to create
     * @param callback Callback for success or error
     */
    @Override
    public void createEvent(Event uiEvent, EventCallback callback) {
        Log.d(TAG, "createEvent called for: " + uiEvent.getTitle());
        
        EventEntity entity = convertToEntity(uiEvent);
        Log.d(TAG, "Converted to entity, ID: " + entity.getEventId() + ", OrganizerID: " + entity.getOrganizerId());
        
        db.collection(COLLECTION_EVENTS)
            .document(entity.getEventId())
            .set(entity.toMap())
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Event created successfully: " + entity.getEventId());
                callback.onSuccess(entity);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error creating event: " + e.getMessage(), e);
                callback.onError(e.getMessage());
            });
    }

    /**
     * Retrieves a Firestore event by its ID.
     *
     * @param eventId  The ID of the event
     * @param callback Callback returning the EventEntity or an error
     */
    @Override
    public void getEvent(String eventId, EventCallback callback) {
        db.collection(COLLECTION_EVENTS)
            .document(eventId)
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    EventEntity entity = documentToEventEntity(documentSnapshot);
                    callback.onSuccess(entity);
                } else {
                    callback.onError("Event not found");
                }
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting event", e);
                callback.onError(e.getMessage());
            });
    }

    /**
     * Retrieves a UI Event by ID, converting from Firestore entity.
     *
     * @param eventId The event ID
     * @param callback Callback with UI Event or error
     */
    @Override
    public void getUIEventById(String eventId, UIEventCallback callback) {
        getEvent(eventId, new EventCallback() {
            @Override
            public void onSuccess(EventEntity entity) {
                Event uiEvent = convertToUIEvent(entity);
                callback.onSuccess(uiEvent);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Retrieves all events for a specific organizer.
     *
     * @param organizerId Organizer's user ID
     * @param callback Callback with list of EventEntity or error
     */
    @Override
    public void getEventsForOrganizer(String organizerId, EventListCallback callback) {
        db.collection(COLLECTION_EVENTS)
            .whereEqualTo("organizerId", organizerId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<EventEntity> events = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    events.add(documentToEventEntity(doc));
                }
                callback.onSuccess(events);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting events for organizer", e);
                callback.onError(e.getMessage());
            });
    }

    /**
     * Retrieves all available events.
     *
     * @param callback Callback returning list of Event or error
     */
    @Override
    public void getAllEvents(UIEventListCallback callback) {
        db.collection(COLLECTION_EVENTS)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Event> uiEvents = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    EventEntity entity = documentToEventEntity(doc);
                    Event uiEvent = convertToUIEvent(entity);
                    uiEvents.add(uiEvent);
                }
                Log.d(TAG, "Retrieved " + uiEvents.size() + " events");
                callback.onSuccess(uiEvents);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting all events", e);
                callback.onError(e.getMessage());
            });
    }

    /**
     * Retrieves all events in which the user is enrolled (on waitlist).
     *
     * @param userId   User ID
     * @param callback Callback returning list of Event or error
     */
    @Override
    public void getEventsUserEnrolledIn(String userId, UIEventListCallback callback) {
        db.collection(COLLECTION_EVENTS)
            .whereArrayContains("waitlist", userId)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Event> uiEvents = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    EventEntity entity = documentToEventEntity(doc);
                    Event uiEvent = convertToUIEvent(entity);
                    uiEvents.add(uiEvent);
                }
                Log.d(TAG, "User enrolled in " + uiEvents.size() + " events");
                callback.onSuccess(uiEvents);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting enrolled events", e);
                callback.onError(e.getMessage());
            });
    }

    /**
     * Retrieves all events in which a user is involved (attending, invited, declined, or waitlisted).
     *
     * @param userId   User ID
     * @param callback Callback with list of Event
     */
    @Override
    public void getEventsForUser(String userId, UIEventListCallback callback) {
        // Get all events and filter for user involvement
        db.collection(COLLECTION_EVENTS)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<Event> uiEvents = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    EventEntity entity = documentToEventEntity(doc);
                    
                    // Check if user is in waitlist
                    boolean inWaitlist = entity.getWaitlist() != null && 
                                        entity.getWaitlist().contains(userId);
                    
                    // Check if user has declined
                    boolean hasDeclined = entity.getDeclined() != null &&
                                         entity.getDeclined().contains(userId);
                    
                    // Check if user is an attendee
                    boolean isAttendee = entity.getAttendees() != null &&
                                        entity.getAttendees().contains(userId);
                    
                    // Check if user has an invitation
                    boolean hasInvitation = false;
                    if (entity.getInvitations() != null) {
                        for (Invitation inv : entity.getInvitations()) {
                            if (inv.getUserId() != null && inv.getUserId().equals(userId)) {
                                hasInvitation = true;
                                break;
                            }
                        }
                    }
                    
                    // Include event if user is involved in any way
                    if (inWaitlist || hasInvitation || hasDeclined || isAttendee) {
                        Event uiEvent = convertToUIEvent(entity);
                        uiEvents.add(uiEvent);
                    }
                }
                Log.d(TAG, "User involved in " + uiEvents.size() + " events total");
                callback.onSuccess(uiEvents);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting user events", e);
                callback.onError(e.getMessage());
            });
    }

    /**
     * Retrieves all event entities for a user’s involvement (attendee, waitlist, etc.).
     *
     * @param userId   User ID
     * @param callback Callback returning list of EventEntity
     */
    @Override
    public void getEventsForUserWithEntities(String userId, EventListCallback callback) {
        // Get all events and filter for user involvement - return EventEntity
        db.collection(COLLECTION_EVENTS)
            .get()
            .addOnSuccessListener(queryDocumentSnapshots -> {
                List<EventEntity> entities = new ArrayList<>();
                for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                    EventEntity entity = documentToEventEntity(doc);
                    
                    // Check if user is in waitlist
                    boolean inWaitlist = entity.getWaitlist() != null && 
                                        entity.getWaitlist().contains(userId);
                    
                    // Check if user has declined
                    boolean hasDeclined = entity.getDeclined() != null &&
                                         entity.getDeclined().contains(userId);
                    
                    // Check if user is an attendee
                    boolean isAttendee = entity.getAttendees() != null &&
                                        entity.getAttendees().contains(userId);
                    
                    // Check if user has an invitation
                    boolean hasInvitation = false;
                    if (entity.getInvitations() != null) {
                        for (Invitation inv : entity.getInvitations()) {
                            if (inv.getUserId() != null && inv.getUserId().equals(userId)) {
                                hasInvitation = true;
                                break;
                            }
                        }
                    }
                    
                    // Include event if user is involved in any way
                    if (inWaitlist || hasInvitation || hasDeclined || isAttendee) {
                        entities.add(entity);
                    }
                }
                Log.d(TAG, "User involved in " + entities.size() + " events (entities)");
                callback.onSuccess(entities);
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error getting user event entities", e);
                callback.onError(e.getMessage());
            });
    }

    /**
     * Updates an existing event document in Firestore.
     *
     * @param event Event entity to update
     * @param callback Callback for success or error
     */
    @Override
    public void updateEvent(EventEntity event, VoidCallback callback) {
        event.setUpdatedAt(new Date());
        
        db.collection(COLLECTION_EVENTS)
            .document(event.getEventId())
            .set(event.toMap())
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Event updated successfully: " + event.getEventId());
                callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error updating event", e);
                callback.onError(e.getMessage());
            });
    }

    /**
     * Deletes an event by ID.
     *
     * @param eventId  ID of the event to delete
     * @param callback Callback for completion
     */
    @Override
    public void deleteEvent(String eventId, VoidCallback callback) {
        db.collection(COLLECTION_EVENTS)
            .document(eventId)
            .delete()
            .addOnSuccessListener(aVoid -> {
                Log.d(TAG, "Event deleted successfully: " + eventId);
                callback.onSuccess();
            })
            .addOnFailureListener(e -> {
                Log.e(TAG, "Error deleting event", e);
                callback.onError(e.getMessage());
            });
    }

    /**
     * Adds a user to an event waitlist.
     *
     * @param eventId Event ID
     * @param userId User ID
     * @param callback Callback for success or error
     */
    @Override
    public void addToWaitlist(String eventId, String userId, VoidCallback callback) {
        getEvent(eventId, new EventCallback() {
            @Override
            public void onSuccess(EventEntity event) {
                if (event.getMaxEntrants() == 0) {
                    callback.onError("This event is not accepting new entrants.");
                    return;
                }
                if (event.addToWaitlist(userId)) {
                    updateEvent(event, callback);
                } else {
                    callback.onError("Failed to add to waitlist - already exists or waitlist full");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Adds a user to a waitlist.
     * @param eventId The event ID
     * @param userId The user ID to add
     * @param callback Callback on success or error
     */
    @Override
    public void joinEvent(String eventId, String userId, VoidCallback callback) {
        // joinEvent is just an alias for addToWaitlist with better logging
        Log.d(TAG, "User joining event: " + eventId);
        addToWaitlist(eventId, userId, callback);
    }

    /**
     * Removes a user from event waitlist.
     * @param eventId The event ID
     * @param userId The user ID to remove
     * @param callback Callback on success or error
     */
    @Override
    public void removeFromWaitlist(String eventId, String userId, VoidCallback callback) {
        getEvent(eventId, new EventCallback() {
            @Override
            public void onSuccess(EventEntity event) {
                if (event.removeFromWaitlist(userId)) {
                    updateEvent(event, callback);
                } else {
                    callback.onError("User not found in waitlist");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Removes user from invitations list.
     * @param eventId The event ID
     * @param userId The user ID to remove
     * @param callback Callback on success or error
     */
    @Override
    public void removeFromInvitiationsList(String eventId, String userId, VoidCallback callback) {
        getEvent(eventId, new EventCallback() {
            @Override
            public void onSuccess(EventEntity event) {
                if (event.removeFromInvitationsList(userId)) {
                    updateEvent(event, callback);
                } else {
                    callback.onError("User not found in waitlist");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Sends invitations to specified users.
     * @param eventId The event ID
     * @param invitations List of invitations to send
     * @param callback Callback on success or error
     */
    @Override
    public void sendInvitations(String eventId, List<Invitation> invitations, VoidCallback callback) {
        getEvent(eventId, new EventCallback() {
            @Override
            public void onSuccess(EventEntity event) {
                for (Invitation invitation : invitations) {
                    event.addInvitation(invitation);
                    // Also remove from waitlist when invited
                    event.removeFromWaitlist(invitation.getUserId());
                }
                updateEvent(event, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Handles user invitation responses.
     * @param eventId The event ID
     * @param userId The user ID
     * @param accept True to accept, false to decline
     * @param callback Callback on success or error
     */
    @Override
    public void respondToInvitation(String eventId, String userId, boolean accept, VoidCallback callback) {
        getEvent(eventId, new EventCallback() {
            @Override
            public void onSuccess(EventEntity event) {
                Invitation invitation = event.getInvitationByUserId(userId);
                if (invitation != null) {
                    if (accept) {
                        // Add user to attendees list
                        if (event.getAttendees() != null && !event.getAttendees().contains(userId)) {
                            event.getAttendees().add(userId);
                        }
                        // Remove from invitations after adding to attendees
                        event.removeInvitation(userId);
                    } else {
                        // Add user to declined list
                        if (event.getDeclined() != null && !event.getDeclined().contains(userId)) {
                            event.getDeclined().add(userId);
                        }
                        // Remove from invitations after adding to declined
                        event.removeInvitation(userId);
                    }
                    
                    // Remove from invitations list after they've responded
                    // This prevents them from receiving duplicate invitation notifications
                    event.removeFromInvitationsList(userId);
                    
                    updateEvent(event, callback);
                } else {
                    callback.onError("Invitation not found for user");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Randomly draws a specified number of attendees from an event's waitlist.
     * Generates invitations for selected users.
     *
     * @param eventId    Event ID
     * @param sampleSize Number of users to invite
     * @param callback   Completion callback
     */
    @Override
    public void drawAttendees(String eventId, Integer sampleSize, VoidCallback callback) {
        getEvent(eventId, new EventCallback() {
            @Override
            public void onSuccess(EventEntity event) {
                List<String> waitlist = event.getWaitlist();
                
                // Validate waitlist is not empty
                if (waitlist == null || waitlist.isEmpty()) {
                    callback.onError("Waitlist is empty - no attendees to draw");
                    return;
                }
                
                if (event.getCapacity() <= 0) {
                    callback.onError("Unable to draw attendees: Attendees for event is set to zero.");
                    return;
                }

                if (event.getMaxEntrants() == 0) {
                    callback.onError("Event max entrants is zero; cannot draw attendees.");
                    return;
                }

                // Count already invited and attending users
                int alreadyInvitedOrAttending = 0;
                if (event.getInvitations() != null) {
                    alreadyInvitedOrAttending += event.getInvitations().size();
                }
                if (event.getAttendees() != null) {
                    alreadyInvitedOrAttending += event.getAttendees().size();
                }
                
                // Determine how many to sample
                int numToSample;
                if (sampleSize != null && sampleSize > 0) {
                    // Use provided sample size, minus already invited/attending
                    numToSample = sampleSize - alreadyInvitedOrAttending;
                } else if (event.getCapacity() > 0) {
                    // Use capacity, minus already invited/attending
                    numToSample = event.getCapacity() - alreadyInvitedOrAttending;
                } else {
                    // Random between 1 and maxEntrants (or waitlist size if smaller)
                    int maxEntrants = event.getMaxEntrants();
                    if (maxEntrants <= 0) {
                        maxEntrants = waitlist.size();
                    }
                    int maxPossible = Math.min(maxEntrants, waitlist.size());
                    Random random = new Random();
                    numToSample = random.nextInt(maxPossible) + 1;
                }
                
                // Ensure we don't draw negative or zero (if capacity already met)
                if (numToSample <= 0) {
                    callback.onError("Capacity already met. " + alreadyInvitedOrAttending + " already invited/attending.");
                    return;
                }
                
                // Cap at waitlist size
                numToSample = Math.min(numToSample, waitlist.size());
                final int finalNumToSample = numToSample; // Make effectively final for inner class
                
                Log.d(TAG, "Drawing " + finalNumToSample + " attendees from waitlist of " + waitlist.size());
                
                // Randomly sample from waitlist
                List<String> shuffledWaitlist = new ArrayList<>(waitlist);
                Collections.shuffle(shuffledWaitlist);
                List<String> selectedUserIds = shuffledWaitlist.subList(0, finalNumToSample);

                // Users who remain on the waitlist = "lost" this draw
                final List<String> loserUserIds = new ArrayList<>(waitlist);
                loserUserIds.removeAll(selectedUserIds);

                // Create invitations for selected attendees
                List<Invitation> invitations = new ArrayList<>();
                for (String userId : selectedUserIds) {
                    Invitation invitation = new Invitation(userId, null); // username can be null
                    invitations.add(invitation);
                }
                
                // Add invitations to event and remove from waitlist
                for (Invitation invitation : invitations) {
                    event.addInvitation(invitation);
                    event.removeFromWaitlist(invitation.getUserId());
                }
                
                // Save the updated event
                updateEvent(event, new VoidCallback() {
                    @Override
                    public void onSuccess() {
                        Log.d(TAG, "Successfully drew " + finalNumToSample + " attendees and sent invitations");
                        // Send "lost the lottery" notifications (best-effort, don’t block UI)
                        if (loserUserIds != null && !loserUserIds.isEmpty()) {
                            NotificationService.getInstance().sendNotificationsToUsers(
                                    loserUserIds,
                                    event.getEventId(),
                                    event.getTitle(),
                                    Notification.NotificationType.WAITLISTED,  // use the “not selected” message
                                    null   // fire-and-forget; we ignore success/failure here
                            );
                        }

                        callback.onSuccess();
                    }
                    
                    @Override
                    public void onError(String error) {
                        callback.onError("Failed to save drawn attendees: " + error);
                    }
                });
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    /**
     * Convert Firestore document to EventEntity
     */
    private EventEntity documentToEventEntity(DocumentSnapshot doc) {
        EventEntity entity = new EventEntity();
        
        entity.setEventId(doc.getString("eventId"));
        entity.setTitle(doc.getString("title"));
        entity.setDescription(doc.getString("description"));
        
        Long capacity = doc.getLong("capacity");
        if (capacity != null) entity.setCapacity(capacity.intValue());
        
        entity.setRegistrationStart(doc.getDate("registrationStart"));
        entity.setRegistrationEnd(doc.getDate("registrationEnd"));
        entity.setOrganizerId(doc.getString("organizerId"));
        entity.setOrganizerName(doc.getString("organizerName"));
        
        Boolean geoRequired = doc.getBoolean("geolocationRequired");
        if (geoRequired != null) entity.setGeolocationRequired(geoRequired);
        
        Long maxEntrants = doc.getLong("maxEntrants");
        if (maxEntrants != null) entity.setMaxEntrants(maxEntrants.intValue());
        
        // Waitlist
        List<String> waitlist = (List<String>) doc.get("waitlist");
        if (waitlist != null) entity.setWaitlist(waitlist);
        
        // Attendees
        List<String> attendees = (List<String>) doc.get("attendees");
        if (attendees != null) entity.setAttendees(attendees);
        
        // Declined
        List<String> declined = (List<String>) doc.get("declined");
        if (declined != null) entity.setDeclined(declined);
        
        // Invitations
        List<Map<String, Object>> invitationMaps = (List<Map<String, Object>>) doc.get("invitations");
        if (invitationMaps != null) {
            List<Invitation> invitations = new ArrayList<>();
            for (Map<String, Object> invMap : invitationMaps) {
                Invitation inv = new Invitation();
                inv.setUserId((String) invMap.get("userId"));
                inv.setUsername((String) invMap.get("username"));
                
                String statusStr = (String) invMap.get("status");
                if (statusStr != null) {
                    inv.setStatus(Invitation.InvitationStatus.fromString(statusStr));
                }
                
                // Handle both Timestamp and Date types
                Object invitedAtObj = invMap.get("invitedAt");
                if (invitedAtObj instanceof Timestamp) {
                    inv.setInvitedAt(((Timestamp) invitedAtObj).toDate());
                } else if (invitedAtObj instanceof Date) {
                    inv.setInvitedAt((Date) invitedAtObj);
                }
                
                Object respondedAtObj = invMap.get("respondedAt");
                if (respondedAtObj instanceof Timestamp) {
                    inv.setRespondedAt(((Timestamp) respondedAtObj).toDate());
                } else if (respondedAtObj instanceof Date) {
                    inv.setRespondedAt((Date) respondedAtObj);
                }
                
                invitations.add(inv);
            }
        entity.setInvitations(invitations);
        }
        
        // QR Code URL
        String qrCodeUrl = doc.getString("qrCodeUrl");
        if (qrCodeUrl != null) entity.setQrCodeUrl(qrCodeUrl);
        
        entity.setCreatedAt(doc.getDate("createdAt"));
        entity.setUpdatedAt(doc.getDate("updatedAt"));
        
        return entity;
    }
}
