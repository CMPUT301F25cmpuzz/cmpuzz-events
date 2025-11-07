package com.example.cmpuzz_events.ui.organizer;

import android.app.DatePickerDialog;
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
import java.util.Locale;
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.util.Calendar;

/**
 * Controller for the Organizer "Create Event" screen.
 * Connects UI inputs to EventService for Firebase storage.
 */
public class CreateEventFragment extends Fragment {

    private CreateEventFragmentBinding binding;
    private User currentUser;
    private EventService eventService;
    private AuthManager authManager;
    private Date registrationStart;
    private Date registrationEnd;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());

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

        // Handle registration period picker
        binding.rowSetRegistrationPeriod.setOnClickListener(v -> {
            Log.d("CreateEventFragment", "Row clicked!");
            Toast.makeText(requireContext(), "Select registration dates", Toast.LENGTH_SHORT).show();
            showDatePicker();
        });

        // Enable/disable numeric inputs based on toggles
        binding.setAttendeesInput.setEnabled(binding.toggleSetAttendees.isChecked());
        binding.setEntrantsInput.setEnabled(binding.toggleMaxEntrants.isChecked());

        // When "Set Attendees" toggle changes
        binding.toggleSetAttendees.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.setAttendeesInput.setEnabled(isChecked);
            if (!isChecked) {
                binding.setAttendeesInput.setText("");
            }
        });

        // When "Limit Max Entrants" toggle changes
        binding.toggleMaxEntrants.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.setEntrantsInput.setEnabled(isChecked);
            if (!isChecked) {
                binding.setEntrantsInput.setText("");
            }
        });

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

        // Get values from UI
        String capacityStr = binding.setAttendeesInput.getText().toString().trim();
        String maxEntrantsStr = binding.setEntrantsInput.getText().toString().trim();

        // Validate required fields
        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (capacityStr.isEmpty() || maxEntrantsStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter attendee and entrant limits", Toast.LENGTH_SHORT).show();
            return;
        }
        if (registrationStart == null || registrationEnd == null) {
            Toast.makeText(requireContext(), "Please set a registration period", Toast.LENGTH_SHORT).show();
            return;
        }

        int capacity;
        int maxEntrants;
        try {
            capacity = Integer.parseInt(capacityStr);
            maxEntrants = Integer.parseInt(maxEntrantsStr);
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        if (registrationEnd.before(registrationStart)) {
            Toast.makeText(requireContext(), "End date cannot be before start date", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use selected registration period
        Date start = registrationStart;
        Date end = registrationEnd;

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

    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        // Start date picker
        DatePickerDialog startPicker = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar startDate = Calendar.getInstance();
                    startDate.set(year, month, dayOfMonth, 0, 0, 0);
                    registrationStart = startDate.getTime();

                    // Now open the end date picker
                    DatePickerDialog endPicker = new DatePickerDialog(
                            requireContext(),
                            (view2, year2, month2, day2) -> {
                                Calendar endDate = Calendar.getInstance();
                                endDate.set(year2, month2, day2, 0, 0, 0);
                                registrationEnd = endDate.getTime();

                                // Update text in the summary field
                                String summary = "Start: " + dateFormat.format(registrationStart)
                                        + " → End: " + dateFormat.format(registrationEnd);
                                binding.tvRegistrationSummary.setText(summary);
                            },
                            startDate.get(Calendar.YEAR),
                            startDate.get(Calendar.MONTH),
                            startDate.get(Calendar.DAY_OF_MONTH)
                    );
                    endPicker.setTitle("Select Registration End Date");
                    endPicker.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        startPicker.setTitle("Select Registration Start Date");
        startPicker.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
