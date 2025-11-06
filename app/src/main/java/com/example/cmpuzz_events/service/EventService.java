package com.example.cmpuzz_events.service;

import android.util.Log;

import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.models.event.Invitation;
import com.example.cmpuzz_events.ui.event.Event;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Map;

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

    @Override
    public void getUIEvent(String eventId, UIEventCallback callback) {
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

    @Override
    public void addToWaitlist(String eventId, String deviceId, VoidCallback callback) {
        getEvent(eventId, new EventCallback() {
            @Override
            public void onSuccess(EventEntity event) {
                if (event.addToWaitlist(deviceId)) {
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

    @Override
    public void joinEvent(String eventId, String deviceId, VoidCallback callback) {
        // joinEvent is just an alias for addToWaitlist with better logging
        Log.d(TAG, "User joining event: " + eventId);
        addToWaitlist(eventId, deviceId, callback);
    }

    @Override
    public void removeFromWaitlist(String eventId, String deviceId, VoidCallback callback) {
        getEvent(eventId, new EventCallback() {
            @Override
            public void onSuccess(EventEntity event) {
                if (event.removeFromWaitlist(deviceId)) {
                    updateEvent(event, callback);
                } else {
                    callback.onError("Device not found in waitlist");
                }
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    @Override
    public void sendInvitations(String eventId, List<Invitation> invitations, VoidCallback callback) {
        getEvent(eventId, new EventCallback() {
            @Override
            public void onSuccess(EventEntity event) {
                for (Invitation invitation : invitations) {
                    event.addInvitation(invitation);
                    // Also remove from waitlist when invited
                    event.removeFromWaitlist(invitation.getDeviceId());
                }
                updateEvent(event, callback);
            }

            @Override
            public void onError(String error) {
                callback.onError(error);
            }
        });
    }

    @Override
    public void respondToInvitation(String eventId, String deviceId, boolean accept, VoidCallback callback) {
        getEvent(eventId, new EventCallback() {
            @Override
            public void onSuccess(EventEntity event) {
                Invitation invitation = event.getInvitationByDeviceId(deviceId);
                if (invitation != null) {
                    if (accept) {
                        invitation.accept();
                    } else {
                        invitation.decline();
                    }
                    updateEvent(event, callback);
                } else {
                    callback.onError("Invitation not found for device");
                }
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
                inv.setDeviceId((String) invMap.get("deviceId"));
                inv.setUsername((String) invMap.get("username"));
                
                String statusStr = (String) invMap.get("status");
                if (statusStr != null) {
                    inv.setStatus(Invitation.InvitationStatus.fromString(statusStr));
                }
                
                Date invitedAt = (Date) invMap.get("invitedAt");
                if (invitedAt != null) inv.setInvitedAt(invitedAt);
                
                Date respondedAt = (Date) invMap.get("respondedAt");
                if (respondedAt != null) inv.setRespondedAt(respondedAt);
                
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
