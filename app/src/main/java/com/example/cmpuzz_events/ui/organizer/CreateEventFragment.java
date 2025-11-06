package com.example.cmpuzz_events.ui.organizer;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.ui.event.Event;
import com.example.cmpuzz_events.databinding.CreateEventFragmentBinding;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;

import java.util.Date;
import java.util.UUID;

/**
 * Controller for the Organizer "Create Event" screen.
 * Connects UI inputs to EventService for Firebase storage.
 */
public class CreateEventFragment extends Fragment {

    private CreateEventFragmentBinding binding;
    private User currentUser;
    private EventService eventService;
    private AuthManager authManager;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Log.d("CreateEventFragment", "Fragment loaded successfully");


        binding = CreateEventFragmentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Initialize services
        eventService = EventService.getInstance();
        authManager = AuthManager.getInstance();

        // Get current authenticated user
        currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            // Shouldn't happen if auth is required
            Log.w("CreateEventFragment", "No authenticated user found");
            Toast.makeText(requireContext(), "Please log in to create events", Toast.LENGTH_SHORT).show();
            return root;
        }

        // Check if user has permission to create events
        if (!currentUser.canManageEvents()) {
            Log.w("CreateEventFragment", "User does not have permission to create events");
            Toast.makeText(requireContext(), "You don't have permission to create events", Toast.LENGTH_SHORT).show();
            return root;
        }

        // Handle Finish button click
        binding.btnFinish.setOnClickListener(v -> handleCreateEvent());

        return root;
    }

    /**
     * Collects all user inputs and creates an Event via EventService and saves to Firebase.
     */
    private void handleCreateEvent() {
        Log.d("CreateEventFragment", "handleCreateEvent called");
        
        if (currentUser == null) {
            Log.e("CreateEventFragment", "Current user is null!");
            Toast.makeText(requireContext(), "Please log in to create events", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("CreateEventFragment", "Current user: " + currentUser.getEmail() + ", Role: " + currentUser.getRole());

        String title = binding.etEventName.getText().toString().trim();
        String description = binding.etEventDescription.getText().toString().trim();
        boolean geoRequired = binding.toggleGeolocation.isChecked();

        // Temporary demo: capacity = 20, registration = now → +7 days
        // TODO: Get these from UI inputs
        int capacity = 20;        // Max attendees (attendance limit)
        int maxEntrants = 100;    // Max people who can enroll
        Date start = new Date();
        Date end = new Date(System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000));

        if (title.isEmpty() || description.isEmpty()) {
            Log.w("CreateEventFragment", "Title or description is empty");
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("CreateEventFragment",
                "Creating event: " + title + " by " + currentUser.getDisplayName() + " (" + currentUser.getUid() + ")");

        // Create UI Event object with current user's UID and name as organizer
        String eventId = UUID.randomUUID().toString();
        Event uiEvent = new Event(eventId, title, description, capacity,
                start, end, currentUser.getUid(), currentUser.getDisplayName(), geoRequired);
        uiEvent.setMaxEntrants(maxEntrants);

        // Save to Firebase via EventService
        eventService.createEvent(uiEvent, new IEventService.EventCallback() {
            @Override
            public void onSuccess(com.example.cmpuzz_events.models.event.EventEntity event) {
                Log.d("CreateEventFragment", "Event saved to Firebase: " + event.getEventId());
                Toast.makeText(requireContext(), "Event created successfully!", Toast.LENGTH_SHORT).show();
                
                // Optional: Navigate back or clear form
                binding.etEventName.setText("");
                binding.etEventDescription.setText("");
                binding.toggleGeolocation.setChecked(false);
            }

            @Override
            public void onError(String error) {
                Log.e("CreateEventFragment", "Error saving event: " + error);
                Toast.makeText(requireContext(), "Failed to create event: " + error, Toast.LENGTH_LONG).show();
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
