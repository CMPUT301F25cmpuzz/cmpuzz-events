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

import com.example.cmpuzz_events.databinding.FragmentSettingsBinding;
import com.google.android.material.appbar.MaterialToolbar;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SharedPreferences preferences;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        preferences = requireContext().getSharedPreferences("user_preferences", Context.MODE_PRIVATE);

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
        // Load saved preference
        boolean notificationsEnabled = preferences.getBoolean("notifications_enabled", true);
        binding.switchNotifications.setChecked(notificationsEnabled);

        // Handle toggle changes
        binding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            preferences.edit().putBoolean("notifications_enabled", isChecked).apply();

            String message = isChecked ? "Notifications enabled" : "Notifications disabled";
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
