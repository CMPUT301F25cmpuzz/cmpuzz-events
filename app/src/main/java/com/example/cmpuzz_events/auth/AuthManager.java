package com.example.cmpuzz_events.auth;

import android.util.Log;

import com.example.cmpuzz_events.models.user.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class to manage authentication state across the app.
 * Manages all firebase functionality.
 */
public class AuthManager {
    private static final String TAG = "AuthManager";
    private static AuthManager instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private User currentUser;
    private final List<AuthStateListener> listeners;

    /**
     * Interface definition for a callback to be invoked when authentication state changes.
     */
    public interface AuthStateListener {
        void onAuthStateChanged(User user);
    }

    /**
     * Private constructor to enforce singleton pattern.
     * Initializes FirebaseAuth and Firestore instances and listens for auth state changes.
     */
    private AuthManager() {
        auth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();
        listeners = new ArrayList<>();
        
        // Listen to Firebase auth state changes
        auth.addAuthStateListener(firebaseAuth -> {
            FirebaseUser firebaseUser = firebaseAuth.getCurrentUser();
            if (firebaseUser != null) {
                loadUserData(firebaseUser.getUid());
            } else {
                currentUser = null;
                notifyListeners(null);
            }
        });
    }

    /**
     * Returns the singleton instance of {@link AuthManager}.
     *
     * @return The shared {@link AuthManager} instance.
     */
    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    /**
     * Registers a listener for authentication state changes.
     *
     * @param listener The listener to add.
     */
    public void addAuthStateListener(AuthStateListener listener) {
        listeners.add(listener);
        // Immediately notify with current state
        listener.onAuthStateChanged(currentUser);
    }

    /**
     * Removes a previously registered authentication state listener.
     *
     * @param listener The listener to remove.
     */
    public void removeAuthStateListener(AuthStateListener listener) {
        listeners.remove(listener);
    }

    /**
     * Notifies all registered listeners of an authentication state change.
     *
     * @param user The current user, or {@code null} if signed out.
     */
    private void notifyListeners(User user) {
        for (AuthStateListener listener : listeners) {
            listener.onAuthStateChanged(user);
        }
    }

    /**
     * Creates a new user account using Firebase Authentication and stores
     * additional user details in Firestore.
     *
     * @param email       The user's email.
     * @param password    The user's password.
     * @param displayName The user's display name.
     * @param username    The user's chosen username.
     * @param callback    Callback to handle success or error.
     */
    public void signUp(String email, String password, String displayName, String username, AuthCallback callback) {
        auth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            // Create user document in Firestore
                            User newUser = new User(
                                    firebaseUser.getUid(),
                                    email,
                                    displayName,
                                    username,
                                    User.UserRole.USER
                            );
                            
                            db.collection("users")
                                    .document(firebaseUser.getUid())
                                    .set(newUser.toMap())
                                    .addOnSuccessListener(aVoid -> {
                                        currentUser = newUser;
                                        notifyListeners(newUser);
                                        callback.onSuccess(newUser);
                                    })
                                    .addOnFailureListener(e -> {
                                        Log.e(TAG, "Error creating user document", e);
                                        callback.onError(e.getMessage());
                                    });
                        }
                    } else {
                        String error = task.getException() != null ? 
                                task.getException().getMessage() : "Sign up failed";
                        callback.onError(error);
                    }
                });
    }

    /**
     * Signs in an existing user with email and password.
     *
     * @param email    The user's email.
     * @param password The user's password.
     * @param callback Callback to handle success or error.
     */
    public void signIn(String email, String password, AuthCallback callback) {
        auth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser firebaseUser = auth.getCurrentUser();
                        if (firebaseUser != null) {
                            loadUserData(firebaseUser.getUid(), callback);
                        }
                    } else {
                        String error = task.getException() != null ? 
                                task.getException().getMessage() : "Sign in failed";
                        callback.onError(error);
                    }
                });
    }

    /**
     * Loads user data from Firestore using the provided UID.
     *
     * @param uid The Firebase UID of the user.
     */
    private void loadUserData(String uid) {
        loadUserData(uid, null);
    }

    /**
     * Loads user data from Firestore and optionally invokes a callback.
     *
     * @param uid      The Firebase UID of the user.
     * @param callback Optional callback for success or error.
     */
    private void loadUserData(String uid, AuthCallback callback) {
        db.collection("users").document(uid)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    User user = documentSnapshotToUser(documentSnapshot);
                    currentUser = user;
                    notifyListeners(user);
                    if (callback != null) {
                        callback.onSuccess(user);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user data", e);
                    if (callback != null) {
                        callback.onError(e.getMessage());
                    }
                });
    }

    /**
     * Converts a Firestore document snapshot into a {@link User} object.
     *
     * @param doc The Firestore document snapshot.
     * @return The corresponding {@link User} object.
     */
    private User documentSnapshotToUser(DocumentSnapshot doc) {
        User user = new User();
        user.setUid(doc.getString("uid"));
        user.setEmail(doc.getString("email"));
        user.setDisplayName(doc.getString("displayName"));
        user.setUsername(doc.getString("username"));
        
        String roleString = doc.getString("role");
        if (roleString != null) {
            user.setRole(User.UserRole.fromString(roleString));
        }
        
        Long createdAt = doc.getLong("createdAt");
        if (createdAt != null) {
            user.setCreatedAt(createdAt);
        }
        
        return user;
    }

    /**
     * Signs out the currently authenticated user and clears session data.
     */
    public void signOut() {
        auth.signOut();
        currentUser = null;
        notifyListeners(null);
    }

    /**
     * Returns the currently signed-in user.
     *
     * @return The current {@link User}, or {@code null} if not signed in.
     */
    public User getCurrentUser() {
        return currentUser;
    }

    /**
     * Checks whether a user is currently signed in.
     * Firebase Auth automatically persists sessions, so we check if Firebase has a current user.
     * The currentUser object will be loaded asynchronously via the auth state listener.
     *
     * @return {@code true} if a user is signed in (Firebase has a persisted session), otherwise {@code false}.
     */
    public boolean isSignedIn() {
        // Firebase Auth automatically persists sessions, so if getCurrentUser() is not null,
        // the user has a valid persisted session. The currentUser object will be loaded
        // asynchronously by the auth state listener.
        return auth.getCurrentUser() != null;
    }
    
    /**
     * Checks if Firebase has a persisted session but user data hasn't been loaded yet.
     * This can happen on app startup when Firebase has a session but loadUserData() hasn't completed.
     *
     * @return {@code true} if Firebase has a user but currentUser is null, otherwise {@code false}.
     */
    public boolean hasFirebaseSessionButNoUserData() {
        return auth.getCurrentUser() != null && currentUser == null;
    }
    
    /**
     * Checks if automatic login is allowed for the current user.
     * Only entrants (USER role) are allowed to use automatic login.
     * Organizers and admins must log in each time.
     *
     * @return {@code true} if the user is an entrant (USER role), otherwise {@code false}.
     */
    public boolean isAutoLoginAllowed() {
        if (currentUser == null) {
            return false;
        }
        // Only allow auto-login for entrants (USER role)
        return currentUser.isUser();
    }

    /**
     * Returns the FirebaseAuth instance.
     *
     * @return The {@link FirebaseAuth} instance.
     */
    public FirebaseAuth getAuth() {
        return auth;
    }

    /**
     * Gets multiple users from their UIDs.
     *
     * @param userIds  List of user IDs to fetch.
     * @param callback Callback invoked with the resulting list or an error.
     */
    public void getUsersByIds(List<String> userIds, UsersCallback callback) {
        if (userIds == null || userIds.isEmpty()) {
            callback.onSuccess(new ArrayList<>());
            return;
        }

        List<User> users = new ArrayList<>();
        final int[] remaining = {userIds.size()};

        for (String userId : userIds) {
            db.collection("users").document(userId)
                .get()
                .addOnSuccessListener(documentSnapshot -> {
                    if (documentSnapshot.exists()) {
                        User user = documentSnapshotToUser(documentSnapshot);
                        users.add(user);
                    }
                    remaining[0]--;
                    if (remaining[0] == 0) {
                        callback.onSuccess(users);
                    }
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading user: " + userId, e);
                    remaining[0]--;
                    if (remaining[0] == 0) {
                        callback.onSuccess(users);
                    }
                });
        }
    }

    /**
     * Callback interface for authentication operations.
     */
    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String error);
    }

    /**
     * Callback interface for retrieving multiple users from Firestore.
     */
    public interface UsersCallback {
        void onSuccess(List<User> users);
        void onError(String error);
    }
}
