package com.example.cmpuzz_events.ui.settings;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.databinding.FragmentSettingsBinding;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.service.INotificationService;
import com.example.cmpuzz_events.service.NotificationService;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SharedPreferences preferences;
    private NotificationService notificationService;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        preferences = requireContext().getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
        notificationService = NotificationService.getInstance();
        notificationService.setContext(requireContext().getApplicationContext());
        currentUser = AuthManager.getInstance().getCurrentUser();

        // Setup toolbar
        MaterialToolbar toolbar = binding.toolbar;
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        setupNotificationsToggle();

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

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
