package com.example.cmpuzz_events.service;

import com.example.cmpuzz_events.models.user.User;

import java.util.List;

public interface IAdminService {

    interface UIAccountListCallback {
        void onSuccess(List<User> accounts);
        void onError(String error);
    }
    void getAllAccountsByRole(User.UserRole role, UIAccountListCallback callback);
}
