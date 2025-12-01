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

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class EventServiceTest {

    @Mock private FirebaseFirestore mockDb;
    @Mock private CollectionReference mockCollectionRef;
    @Mock private DocumentReference mockDocRef;
    @Mock private Task<Void> mockDeleteTask;
    @Mock private IEventService.VoidCallback mockCallback;

    private MockedStatic<FirebaseFirestore> firestoreStaticMock;
    private MockedStatic<Log> logStaticMock;

    private EventService eventService;

    // setting up of mock databases to represnt the connection to a firestore database for testing
    @Before
    public void setUp() {
        firestoreStaticMock = Mockito.mockStatic(FirebaseFirestore.class);
        firestoreStaticMock.when(FirebaseFirestore::getInstance).thenReturn(mockDb);
        logStaticMock = Mockito.mockStatic(Log.class);

        eventService = EventService.getInstance();

        when(mockDb.collection("events")).thenReturn(mockCollectionRef);
        when(mockCollectionRef.document("test_event_id")).thenReturn(mockDocRef);
        when(mockDocRef.delete()).thenReturn(mockDeleteTask);
    }

    @After
    public void tearDown() throws Exception {
        Field instance = EventService.class.getDeclaredField("instance");
        instance.setAccessible(true);
        instance.set(null, null);

        firestoreStaticMock.close();
        logStaticMock.close();
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
}
