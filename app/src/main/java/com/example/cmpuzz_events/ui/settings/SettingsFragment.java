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
import com.google.android.material.appbar.MaterialToolbar;
import com.google.firebase.firestore.FirebaseFirestore;

public class SettingsFragment extends Fragment {

    private FragmentSettingsBinding binding;
    private SharedPreferences preferences;
    private FirebaseFirestore db;
    private User currentUser;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentSettingsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        preferences = requireContext().getSharedPreferences("user_preferences", Context.MODE_PRIVATE);
        db = FirebaseFirestore.getInstance();
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

        // Load saved preference from Firestore
        db.collection("users").document(currentUser.getUid())
            .get()
            .addOnSuccessListener(documentSnapshot -> {
                boolean notificationsEnabled = documentSnapshot.getBoolean("notificationsEnabled") != null 
                    ? documentSnapshot.getBoolean("notificationsEnabled") : true;
                
                // Set the switch WITHOUT triggering the listener
                binding.switchNotifications.setOnCheckedChangeListener(null);
                binding.switchNotifications.setChecked(notificationsEnabled);
                
                // Also save to local preferences for fallback
                preferences.edit().putBoolean("notifications_enabled", notificationsEnabled).apply();
                
                // NOW set up the listener after loading the initial value
                binding.switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
                    // Save to local preferences
                    preferences.edit().putBoolean("notifications_enabled", isChecked).apply();
                    
                    // Save to Firestore
                    db.collection("users").document(currentUser.getUid())
                        .update("notificationsEnabled", isChecked)
                        .addOnSuccessListener(aVoid -> {
                            String message = isChecked ? "Notifications enabled" : "Notifications disabled";
                            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
                        })
                        .addOnFailureListener(e -> {
                            Toast.makeText(getContext(), "Error saving preference", Toast.LENGTH_SHORT).show();
                            // Revert the switch
                            binding.switchNotifications.setOnCheckedChangeListener(null);
                            binding.switchNotifications.setChecked(!isChecked);
                            binding.switchNotifications.setOnCheckedChangeListener((buttonView2, isChecked2) -> {
                                // Re-attach listener after reverting
                                setupNotificationsToggle();
                            });
                        });
                });
            });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
