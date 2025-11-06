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

import com.example.cmpuzz_events.ui.event.Event;
import com.example.cmpuzz_events.models.user.Organizer;
import com.example.cmpuzz_events.databinding.CreateEventFragmentBinding;

import java.util.Date;

/**
 * Controller for the Organizer "Create Event" screen.
 * Connects UI inputs to the Organizer backend logic.
 */
public class CreateEventFragment extends Fragment {

    private CreateEventFragmentBinding binding;
    private Organizer organizer;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Log.d("CreateEventFragment", "Fragment loaded successfully");


        binding = CreateEventFragmentBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Temporary organizer (later replace with logged-in organizer from Firebase)
        organizer = new Organizer("org001", "Kaustubh", "kaustubh@example.com");

        // Handle Finish button click
        binding.btnFinish.setOnClickListener(v -> handleCreateEvent());

        return root;
    }

    /**
     * Collects all user inputs and creates an Event via Organizer backend.
     */
    private void handleCreateEvent() {
        String title = binding.etEventName.getText().toString().trim();
        String description = binding.etEventDescription.getText().toString().trim();
        boolean geoRequired = binding.toggleGeolocation.isChecked();

        // Temporary demo: capacity = 20, registration = now → +7 days
        int capacity = 20;
        Date start = new Date();
        Date end = new Date(System.currentTimeMillis() + (7L * 24 * 60 * 60 * 1000));

        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("CreateEventFragment",
                "Creating event: " + title + ", GeoRequired=" + geoRequired);

        organizer.createEvent(title, description, capacity, start, end, geoRequired);

        for (Event e : organizer.getCreatedEvents()) {
            Log.d("Organizer", "Stored event: " + e.getTitle());
        }

        Toast.makeText(requireContext(), "Event created successfully!", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
