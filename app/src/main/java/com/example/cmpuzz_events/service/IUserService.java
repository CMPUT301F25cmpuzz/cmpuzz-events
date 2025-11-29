package com.example.cmpuzz_events.service;

import com.example.cmpuzz_events.models.user.User;

import java.util.List;

public interface IUserService {

    interface UIUserListCallback {
        void onSuccess(List<User> users);
        void onError(String error);
    }
    void getAllUsers(UIUserListCallback callback);
}
