package com.example.cmpuzz_events;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.example.cmpuzz_events.service.ProfileService;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.SetOptions;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Map;

@RunWith(MockitoJUnitRunner.class)
public class ProfileServiceTest {

    @Mock private FirebaseFirestore mockDb;
    @Mock private FirebaseAuth mockAuth;
    @Mock private FirebaseUser mockUser;
    @Mock private CollectionReference mockCollectionRef;
    @Mock private DocumentReference mockDocRef;
    @Mock private Task<Void> mockVoidTask;

    private MockedStatic<FirebaseFirestore> firestoreStaticMock;
    private MockedStatic<FirebaseAuth> authStaticMock;

    private ProfileService profileService;

    @Before
    public void setUp() {
        // Mock static instances BEFORE instantiating the service
        firestoreStaticMock = Mockito.mockStatic(FirebaseFirestore.class);
        firestoreStaticMock.when(FirebaseFirestore::getInstance).thenReturn(mockDb);

        authStaticMock = Mockito.mockStatic(FirebaseAuth.class);
        authStaticMock.when(FirebaseAuth::getInstance).thenReturn(mockAuth);

        // Setup default mock behavior
        when(mockAuth.getCurrentUser()).thenReturn(mockUser);
        when(mockDb.collection("users")).thenReturn(mockCollectionRef);
        when(mockCollectionRef.document(anyString())).thenReturn(mockDocRef);
        when(mockDocRef.set(any(), any(SetOptions.class))).thenReturn(mockVoidTask);

        profileService = new ProfileService();
    }

    @After
    public void tearDown() {
        firestoreStaticMock.close();
        authStaticMock.close();
    }


    @Test
    public void updateProfile_noEmailChange_updatesFirestoreOnly() {
        String uid = "testUid";
        String name = "New Name";
        String username = "newuser";
        String oldEmail = "old@test.com"; // Same as passed, or ignoring email update logic

        // Mock current user email to match strictly or ensure logic path
        when(mockUser.getEmail()).thenReturn(oldEmail);

        // Call update with same email (should skip Auth update)
        profileService.updateProfile(uid, name, username, oldEmail);

        // Verify Firestore update was called
        verify(mockDocRef).set(any(Map.class), any(SetOptions.class));

        // Verify Auth updateEmail was NOT called
        verify(mockUser, Mockito.never()).updateEmail(anyString());
    }

    @Test
    public void updateAuthEmailOnly_callsFirebaseUserUpdate() {
        String newEmail = "new@test.com";
        when(mockUser.getEmail()).thenReturn("old@test.com");
        when(mockUser.updateEmail(newEmail)).thenReturn(mockVoidTask);

        profileService.updateAuthEmailOnly(newEmail);

        verify(mockUser).updateEmail(newEmail);
    }
}