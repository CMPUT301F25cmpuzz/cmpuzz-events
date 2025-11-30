package com.example.cmpuzz_events.ui.settings;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;

import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.auth.LoginActivity;
import com.example.cmpuzz_events.databinding.FragmentSettingsBinding;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.service.INotificationService;
import com.example.cmpuzz_events.service.NotificationService;
import com.example.cmpuzz_events.service.ProfileService;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SharedPreferences preferences;
    private NotificationService notificationService;
    private ProfileService profileService;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        preferences = requireContext().getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
        notificationService = NotificationService.getInstance();
        notificationService.setContext(requireContext().getApplicationContext());
        profileService = new ProfileService();
        currentUser = AuthManager.getInstance().getCurrentUser();

        // Setup toolbar
        MaterialToolbar toolbar = binding.toolbar;
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        setupNotificationsToggle();
        setupDeleteAccount();

        return root;
    }

    private void setupNotificationsToggle() {
        if (currentUser == null) {
            return;
        }

        // Load saved preference from service
        notificationService.getNotificationPreference(currentUser.getUid(), 
            new INotificationService.NotificationPreferenceCallback() {
                @Override
                public void onSuccess(boolean enabled) {
                    // Set the switch WITHOUT triggering the listener
                    binding.switchNotifications.setOnCheckedChangeListener(null);
                    binding.switchNotifications.setChecked(enabled);
                    
                    // Also save to local preferences for fallback
                    preferences.edit().putBoolean("notifications_enabled", enabled).apply();
                    
                    // NOW set up the listener after loading the initial value
                    binding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        // Save to local preferences
                        preferences.edit().putBoolean("notifications_enabled", isChecked).apply();
                        
                        // Save via service
                        notificationService.updateNotificationPreference(currentUser.getUid(), isChecked,
                            new INotificationService.VoidCallback() {
                                @Override
                                public void onSuccess() {
                                    String message = isChecked ? "Notifications enabled" : "Notifications disabled";
                                    Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                                }

                                @Override
                                public void onError(String error) {
                                    Toast.makeText(getContext(), "Error saving preference", Toast.LENGTH_SHORT).show();
                                    // Revert the switch
                                    binding.switchNotifications.setOnCheckedChangeListener(null);
                                    binding.switchNotifications.setChecked(!isChecked);
                                    setupNotificationsToggle(); // Re-setup
                                }
                            });
                    });
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Error loading preference", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void setupDeleteAccount() {
        binding.cardDeleteAccount.setOnClickListener(v -> showDeleteAccountConfirmation());
    }

    private void showDeleteAccountConfirmation() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Delete Account")
                .setMessage("Are you sure you want to delete your account? This action is PERMANENT and cannot be undone. All your data will be deleted.")
                .setPositiveButton("DELETE", (dialog, which) -> deleteAccount())
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void deleteAccount() {
        if (currentUser == null) {
            Toast.makeText(getContext(), "No user signed in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Show progress
        Toast.makeText(getContext(), "Deleting account...", Toast.LENGTH_SHORT).show();

        profileService.deleteCurrentAccount(currentUser.getUid())
                .addOnSuccessListener(aVoid -> {
                    Toast.makeText(getContext(), "Account deleted successfully", Toast.LENGTH_SHORT).show();
                    
                    // Sign out (auth is already cleared by delete, but this cleans up state)
                    AuthManager.getInstance().signOut();
                    
                    // Navigate back to login
                    if (getActivity() != null) {
                        Intent intent = new Intent(getActivity(), LoginActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        getActivity().finish();
                    }
                })
                .addOnFailureListener(e -> {
                    String errorMsg = e.getMessage();
                    if (errorMsg != null && errorMsg.toLowerCase().contains("recent")) {
                        Toast.makeText(getContext(), "Please sign in again before deleting your account", Toast.LENGTH_LONG).show();
                    } else {
                        Toast.makeText(getContext(), "Failed to delete account: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
