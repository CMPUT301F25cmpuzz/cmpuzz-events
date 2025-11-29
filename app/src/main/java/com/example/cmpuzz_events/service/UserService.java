package com.example.cmpuzz_events.service;

import android.util.Log;

import com.example.cmpuzz_events.models.user.User;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class UserService implements IUserService{
    private static final String TAG = "UserService";
    private static final String COLLECTION_USERS = "users";
    private static UserService instance;
    private final FirebaseFirestore db;

    public UserService() {
        db = FirebaseFirestore.getInstance();
    }

    public static synchronized UserService getInstance()
    {
        if (instance == null)
        {
            instance = new UserService();
        }
        return instance;
    }

    public void getAllUsers(UIUserListCallback callback)
    {
        db.collection(COLLECTION_USERS)
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    List<User> userList = new ArrayList<>();
                    for(QueryDocumentSnapshot doc : queryDocumentSnapshots)
                    {
                        User user = documentToUser(doc);
                        userList.add(user);
                    }
                    Log.d(TAG, "Retrieved " + userList.size() + " users!");
                    callback.onSuccess(userList);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error retreiving users", e);
                    callback.onError(e.getMessage());
                });
    }

    private User documentToUser(DocumentSnapshot doc)
    {
        User user = new User();
        user.setUid(doc.getString("uid"));
        user.setEmail(doc.getString("email"));
        user.setUsername(doc.getString("username"));
        user.setRole(User.UserRole.fromString(doc.getString("role")));
        user.setDisplayName(doc.getString("username"));
        user.setCreatedAt(doc.getLong("createdAt"));

        return user;
    }
}
