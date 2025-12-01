package com.example.cmpuzz_events;

import android.util.Log;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import java.lang.reflect.Field;

import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.models.event.Invitation;
import com.example.cmpuzz_events.models.notification.Notification;
import com.example.cmpuzz_events.service.NotificationService;
import org.mockito.ArgumentCaptor;
import org.mockito.MockedStatic;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import static org.junit.Assert.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EventServiceTest {

    @Mock private FirebaseFirestore mockDb;
    @Mock private CollectionReference mockCollectionRef;
    @Mock private DocumentReference mockDocRef;
    @Mock private Task<Void> mockDeleteTask;
    @Mock private IEventService.VoidCallback mockCallback;

    private MockedStatic<FirebaseFirestore> firestoreStaticMock;
    private MockedStatic<Log> logStaticMock;
    private MockedStatic<NotificationService> notificationServiceStaticMock;

    private EventService eventService;
    private NotificationService mockNotificationService;

    // setting up of mock databases to represnt the connection to a firestore database for testing
    @Before
    public void setUp() {
        firestoreStaticMock = Mockito.mockStatic(FirebaseFirestore.class);
        firestoreStaticMock.when(FirebaseFirestore::getInstance).thenReturn(mockDb);
        logStaticMock = Mockito.mockStatic(Log.class);

        // Mock NotificationService
        notificationServiceStaticMock = Mockito.mockStatic(NotificationService.class);
        mockNotificationService = mock(NotificationService.class);
        notificationServiceStaticMock.when(NotificationService::getInstance).thenReturn(mockNotificationService);

        eventService = EventService.getInstance();

        when(mockDb.collection("events")).thenReturn(mockCollectionRef);
        when(mockCollectionRef.document(anyString())).thenReturn(mockDocRef);
        when(mockDocRef.delete()).thenReturn(mockDeleteTask);
    }

    @After
    public void tearDown() throws Exception {
        Field instance = EventService.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);

        firestoreStaticMock.close();
        logStaticMock.close();
        if (notificationServiceStaticMock != null) {
            notificationServiceStaticMock.close();
        }
    }

    @Test
    public void deleteEvent_onSuccess_triggersCallback() {
        simulateSuccess(mockDeleteTask);
        eventService.deleteEvent("test_event_id", mockCallback);
        verify(mockDocRef).delete();
        verify(mockCallback).onSuccess();
    }

    @Test
    public void deleteEvent_onFailure_triggersCallback() {
        Exception fakeException = new Exception("Permission denied");
        simulateFailure(mockDeleteTask, fakeException);
        eventService.deleteEvent("test_event_id", mockCallback);
        verify(mockDocRef).delete();
        verify(mockCallback).onError("Permission denied");
    }

    private void simulateSuccess(Task<Void> task) {
        when(task.addOnSuccessListener(any())).thenAnswer(invocation -> {
            OnSuccessListener<Void> listener = invocation.getArgument(0);
            listener.onSuccess(null);
            return task;
        });
        when(task.addOnFailureListener(any())).thenReturn(task);
    }

    private void simulateFailure(Task<Void> task, Exception exception) {
        when(task.addOnFailureListener(any())).thenAnswer(invocation -> {
            OnFailureListener listener = invocation.getArgument(0);
            listener.onFailure(exception);
            return task;
        });
        when(task.addOnSuccessListener(any())).thenReturn(task);
    }

    /**
     * US 01.04.02: Test that notifications are sent to entrants who are not chosen
     * when the organizer draws attendees.
     */
    @Test
    public void drawAttendees_sendsNotificationToNotChosenUsers() {
        String eventId = "test_event_01";
        EventEntity event = createTestEvent(eventId, "Test Event");
        List<String> waitlist = new ArrayList<>();
        waitlist.add("user1");
        waitlist.add("user2");
        waitlist.add("user3");
        waitlist.add("user4");
        waitlist.add("user5");
        event.setWaitlist(waitlist);
        event.setCapacity(2); // Only 2 will be selected

        EventService spyEventService = spy(eventService);
        doAnswer(invocation -> {
            IEventService.EventCallback callback = invocation.getArgument(1);
            callback.onSuccess(event);
            return null;
        }).when(spyEventService).getEvent(eq(eventId), any(IEventService.EventCallback.class));

        doAnswer(invocation -> {
            IEventService.VoidCallback callback = invocation.getArgument(1);
            callback.onSuccess();
            return null;
        }).when(spyEventService).updateEvent(any(EventEntity.class), any(IEventService.VoidCallback.class));

        spyEventService.drawAttendees(eventId, 2, mockCallback);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ArgumentCaptor<List<String>> userIdsCaptor = ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<String> eventIdCaptor = ArgumentCaptor.forClass(String.class);
        ArgumentCaptor<Notification.NotificationType> typeCaptor = ArgumentCaptor.forClass(Notification.NotificationType.class);

        verify(mockNotificationService, timeout(1000)).sendNotificationsToUsers(
                userIdsCaptor.capture(),
                eventIdCaptor.capture(),
                anyString(),
                typeCaptor.capture(),
                any()
        );

        List<String> notifiedUserIds = userIdsCaptor.getValue();
        assertEquals("Should notify 3 users who weren't selected", 3, notifiedUserIds.size());
        assertEquals(Notification.NotificationType.WAITLISTED, typeCaptor.getValue());
        assertEquals(eventId, eventIdCaptor.getValue());
        verify(mockCallback, timeout(1000)).onSuccess();
    }

    /**
     * US 01.04.02: Test that no notification is sent if all waitlist users are selected
     */
    @Test
    public void drawAttendees_noNotificationWhenAllSelected() {
        String eventId = "test_event_02";
        EventEntity event = createTestEvent(eventId, "Test Event 2");
        List<String> waitlist = new ArrayList<>();
        waitlist.add("user1");
        waitlist.add("user2");
        event.setWaitlist(waitlist);
        event.setCapacity(2);

        EventService spyEventService = spy(eventService);
        doAnswer(invocation -> {
            IEventService.EventCallback callback = invocation.getArgument(1);
            callback.onSuccess(event);
            return null;
        }).when(spyEventService).getEvent(eq(eventId), any(IEventService.EventCallback.class));

        doAnswer(invocation -> {
            IEventService.VoidCallback callback = invocation.getArgument(1);
            callback.onSuccess();
            return null;
        }).when(spyEventService).updateEvent(any(EventEntity.class), any(IEventService.VoidCallback.class));

        spyEventService.drawAttendees(eventId, 2, mockCallback);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(mockNotificationService, never()).sendNotificationsToUsers(
                anyList(), anyString(), anyString(), any(), any()
        );
        verify(mockCallback, timeout(1000)).onSuccess();
    }

    /**
     * US 01.05.01: Test that a replacement attendee can be drawn from waitlist
     * when someone declines their invitation.
     */
    @Test
    public void drawReplacementAttendee_successfullyDrawsFromWaitlist() {
        String eventId = "test_event_03";
        EventEntity event = createTestEvent(eventId, "Test Event 3");
        
        List<String> waitlist = new ArrayList<>();
        waitlist.add("waitlist_user1");
        waitlist.add("waitlist_user2");
        event.setWaitlist(waitlist);

        List<String> declined = new ArrayList<>();
        declined.add("declined_user1");
        event.setDeclined(declined);

        event.setCapacity(5);
        event.setAttendees(new ArrayList<>());
        event.setInvitations(new ArrayList<>());

        EventService spyEventService = spy(eventService);
        doAnswer(invocation -> {
            IEventService.EventCallback callback = invocation.getArgument(1);
            callback.onSuccess(event);
            return null;
        }).when(spyEventService).getEvent(eq(eventId), any(IEventService.EventCallback.class));

        doAnswer(invocation -> {
            IEventService.VoidCallback callback = invocation.getArgument(1);
            callback.onSuccess();
            return null;
        }).when(spyEventService).updateEvent(any(EventEntity.class), any(IEventService.VoidCallback.class));

        spyEventService.drawReplacementAttendee(eventId, mockCallback);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(mockCallback, timeout(1000)).onSuccess();
        verify(spyEventService).getEvent(eq(eventId), any(IEventService.EventCallback.class));
        verify(spyEventService).updateEvent(any(EventEntity.class), any(IEventService.VoidCallback.class));
    }

    /**
     * US 01.05.01: Test that replacement fails when waitlist is empty
     */
    @Test
    public void drawReplacementAttendee_failsWhenWaitlistEmpty() {
        String eventId = "test_event_04";
        EventEntity event = createTestEvent(eventId, "Test Event 4");
        event.setWaitlist(new ArrayList<>());
        event.setDeclined(new ArrayList<>());
        event.getDeclined().add("declined_user1");

        EventService spyEventService = spy(eventService);
        doAnswer(invocation -> {
            IEventService.EventCallback callback = invocation.getArgument(1);
            callback.onSuccess(event);
            return null;
        }).when(spyEventService).getEvent(eq(eventId), any(IEventService.EventCallback.class));

        spyEventService.drawReplacementAttendee(eventId, mockCallback);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockCallback, timeout(1000)).onError(errorCaptor.capture());
        assertTrue("Error should mention empty waitlist", 
                errorCaptor.getValue().contains("Waitlist is empty"));
    }

    /**
     * US 01.05.01: Test that replacement fails when no one has declined
     */
    @Test
    public void drawReplacementAttendee_failsWhenNoDeclined() {
        String eventId = "test_event_05";
        EventEntity event = createTestEvent(eventId, "Test Event 5");
        List<String> waitlist = new ArrayList<>();
        waitlist.add("waitlist_user1");
        event.setWaitlist(waitlist);
        event.setDeclined(new ArrayList<>());

        EventService spyEventService = spy(eventService);
        doAnswer(invocation -> {
            IEventService.EventCallback callback = invocation.getArgument(1);
            callback.onSuccess(event);
            return null;
        }).when(spyEventService).getEvent(eq(eventId), any(IEventService.EventCallback.class));

        spyEventService.drawReplacementAttendee(eventId, mockCallback);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockCallback, timeout(1000)).onError(errorCaptor.capture());
        assertTrue("Error should mention no declined entrants", 
                errorCaptor.getValue().contains("No declined entrants"));
    }

    /**
     * US 01.05.01: Test that replacement fails when capacity is full
     */
    @Test
    public void drawReplacementAttendee_failsWhenCapacityFull() {
        String eventId = "test_event_06";
        EventEntity event = createTestEvent(eventId, "Test Event 6");
        List<String> waitlist = new ArrayList<>();
        waitlist.add("waitlist_user1");
        event.setWaitlist(waitlist);

        List<String> declined = new ArrayList<>();
        declined.add("declined_user1");
        event.setDeclined(declined);

        event.setCapacity(2);
        List<String> attendees = new ArrayList<>();
        attendees.add("attendee1");
        event.setAttendees(attendees);
        List<Invitation> invitations = new ArrayList<>();
        Invitation inv = new Invitation("invited_user1", null);
        invitations.add(inv);
        event.setInvitations(invitations);

        EventService spyEventService = spy(eventService);
        doAnswer(invocation -> {
            IEventService.EventCallback callback = invocation.getArgument(1);
            callback.onSuccess(event);
            return null;
        }).when(spyEventService).getEvent(eq(eventId), any(IEventService.EventCallback.class));

        spyEventService.drawReplacementAttendee(eventId, mockCallback);

        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        ArgumentCaptor<String> errorCaptor = ArgumentCaptor.forClass(String.class);
        verify(mockCallback, timeout(1000)).onError(errorCaptor.capture());
        assertTrue("Error should mention capacity full", 
                errorCaptor.getValue().contains("All attendee slots are filled"));
    }

    /**
     * US 02.05.03: Test that multiple replacement attendees can be drawn
     * when organizer clicks the button multiple times (one per declined invitation).
     */
    @Test
    public void drawReplacementAttendee_canDrawMultipleReplacements() {
        String eventId = "test_event_07";
        
        // Create initial event state
        EventEntity event1 = createTestEvent(eventId, "Test Event 7");
        List<String> waitlist1 = new ArrayList<>();
        waitlist1.add("waitlist_user1");
        waitlist1.add("waitlist_user2");
        waitlist1.add("waitlist_user3");
        waitlist1.add("waitlist_user4");
        event1.setWaitlist(waitlist1);

        List<String> declined = new ArrayList<>();
        declined.add("declined_user1");
        declined.add("declined_user2");
        declined.add("declined_user3");
        event1.setDeclined(declined);

        event1.setCapacity(10);
        event1.setAttendees(new ArrayList<>());
        event1.setInvitations(new ArrayList<>());

        // Create event state after first draw
        EventEntity event2 = createTestEvent(eventId, "Test Event 7");
        List<String> waitlist2 = new ArrayList<>();
        waitlist2.add("waitlist_user2");
        waitlist2.add("waitlist_user3");
        waitlist2.add("waitlist_user4");
        event2.setWaitlist(waitlist2);
        event2.setDeclined(declined);
        event2.setCapacity(10);
        event2.setAttendees(new ArrayList<>());
        List<Invitation> invitations2 = new ArrayList<>();
        invitations2.add(new Invitation("waitlist_user1", null));
        event2.setInvitations(invitations2);

        EventService spyEventService = spy(eventService);
        
        // First call returns initial event, second call returns updated event
        doAnswer(invocation -> {
            IEventService.EventCallback callback = invocation.getArgument(1);
            callback.onSuccess(event1);
            return null;
        }).doAnswer(invocation -> {
            IEventService.EventCallback callback = invocation.getArgument(1);
            callback.onSuccess(event2);
            return null;
        }).when(spyEventService).getEvent(eq(eventId), any(IEventService.EventCallback.class));

        doAnswer(invocation -> {
            IEventService.VoidCallback callback = invocation.getArgument(1);
            callback.onSuccess();
            return null;
        }).when(spyEventService).updateEvent(any(EventEntity.class), any(IEventService.VoidCallback.class));

        // Draw first replacement
        spyEventService.drawReplacementAttendee(eventId, mockCallback);
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(mockCallback, timeout(1000)).onSuccess();
        verify(spyEventService, times(1)).getEvent(eq(eventId), any(IEventService.EventCallback.class));
        
        // Reset callback for second draw
        reset(mockCallback);
        
        // Draw second replacement (organizer clicks button again)
        spyEventService.drawReplacementAttendee(eventId, mockCallback);
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(mockCallback, timeout(1000)).onSuccess();
        verify(spyEventService, times(2)).getEvent(eq(eventId), any(IEventService.EventCallback.class));
        verify(spyEventService, times(2)).updateEvent(any(EventEntity.class), any(IEventService.VoidCallback.class));
    }

    /**
     * US 02.05.03: Test that each replacement draw randomly selects from waitlist
     */
    @Test
    public void drawReplacementAttendee_randomlySelectsFromWaitlist() {
        String eventId = "test_event_08";
        EventEntity event = createTestEvent(eventId, "Test Event 8");
        
        List<String> waitlist = new ArrayList<>();
        waitlist.add("waitlist_user1");
        waitlist.add("waitlist_user2");
        waitlist.add("waitlist_user3");
        event.setWaitlist(waitlist);

        List<String> declined = new ArrayList<>();
        declined.add("declined_user1");
        event.setDeclined(declined);

        event.setCapacity(10);
        event.setAttendees(new ArrayList<>());
        event.setInvitations(new ArrayList<>());

        EventService spyEventService = spy(eventService);
        doAnswer(invocation -> {
            IEventService.EventCallback callback = invocation.getArgument(1);
            callback.onSuccess(event);
            return null;
        }).when(spyEventService).getEvent(eq(eventId), any(IEventService.EventCallback.class));

        doAnswer(invocation -> {
            IEventService.VoidCallback callback = invocation.getArgument(1);
            callback.onSuccess();
            return null;
        }).when(spyEventService).updateEvent(any(EventEntity.class), any(IEventService.VoidCallback.class));

        // Draw replacement - should randomly select one from waitlist
        spyEventService.drawReplacementAttendee(eventId, mockCallback);
        
        try {
            Thread.sleep(100);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }

        verify(mockCallback, timeout(1000)).onSuccess();
        
        // Verify that exactly one person was selected and moved
        assertEquals("Should have 2 users left in waitlist", 2, event.getWaitlist().size());
        assertEquals("Should have 1 invitation", 1, event.getInvitations().size());
        
        // Verify the selected user is in invitations and not in waitlist
        String selectedUserId = event.getInvitations().get(0).getUserId();
        assertFalse("Selected user should not be in waitlist", event.getWaitlist().contains(selectedUserId));
        assertTrue("Selected user should be in invitations", 
                event.getInvitations().stream().anyMatch(inv -> inv.getUserId().equals(selectedUserId)));
    }

    private EventEntity createTestEvent(String eventId, String title) {
        Date now = new Date();
        Date future = new Date(now.getTime() + 86400000);
        return new EventEntity(eventId, title, "Test Description", 10, 
                now, future, "organizer123", "Test Organizer", false, 100);
    }
}
