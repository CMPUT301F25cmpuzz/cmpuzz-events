package com.example.cmpuzz_events.auth;

import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;

import com.example.cmpuzz_events.models.User;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.ArrayList;
import java.util.List;

/**
 * Singleton class to manage authentication state across the app
 * Similar to Android Context pattern - provides global access to auth state
 */
public class AuthManager {
    private static final String TAG = "AuthManager";
    private static AuthManager instance;

    private final FirebaseAuth auth;
    private final FirebaseFirestore db;
    private User currentUser;
    private final List<AuthStateListener> listeners;

    public interface AuthStateListener {
        void onAuthStateChanged(User user);
    }

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

    public static synchronized AuthManager getInstance() {
        if (instance == null) {
            instance = new AuthManager();
        }
        return instance;
    }

    public void addAuthStateListener(AuthStateListener listener) {
        listeners.add(listener);
        // Immediately notify with current state
        listener.onAuthStateChanged(currentUser);
    }

    public void removeAuthStateListener(AuthStateListener listener) {
        listeners.remove(listener);
    }

    private void notifyListeners(User user) {
        for (AuthStateListener listener : listeners) {
            listener.onAuthStateChanged(user);
        }
    }

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

    private void loadUserData(String uid) {
        loadUserData(uid, null);
    }

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

    public void signOut() {
        auth.signOut();
        currentUser = null;
        notifyListeners(null);
    }

    public User getCurrentUser() {
        return currentUser;
    }

    public boolean isSignedIn() {
        return auth.getCurrentUser() != null && currentUser != null;
    }

    public FirebaseAuth getAuth() {
        return auth;
    }

    public interface AuthCallback {
        void onSuccess(User user);
        void onError(String error);
    }
}
