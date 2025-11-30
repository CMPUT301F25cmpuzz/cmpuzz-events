package com.example.cmpuzz_events.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.databinding.FragmentHomeBinding;
import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;
import com.example.cmpuzz_events.ui.event.Event;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private EventService eventService;
    private MyEventsAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // Check if user should see this fragment
        User currentUser = AuthManager.getInstance().getCurrentUser();
        if (currentUser == null || !currentUser.canManageEvents()) {
            // This fragment should only be visible to organizers
            Log.w(TAG, "HomeFragment accessed by non-organizer user");
            return root;
        }

        eventService = EventService.getInstance();
        
        // Setup RecyclerView
        RecyclerView recyclerView = binding.recyclerViewMyEvents;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MyEventsAdapter(new ArrayList<>(), true); // true = organizer view
        
        // Set click listener for viewing event details
        adapter.setOnEventClickListener(new MyEventsAdapter.OnEventClickListener() {
            @Override
            public void onViewEventClick(Event event) {
                // Navigate to EventDetailsFragment
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getEventId());
                Navigation.findNavController(root).navigate(
                    R.id.action_to_event_details,
                    bundle
                );
            }

            @Override
            public void onDrawAttendeesClick(Event event) {
                drawAttendeesForEvent(event);
            }
        });
        
        recyclerView.setAdapter(adapter);
        
        // Load user's events
        loadMyEvents();
        
        return root;
    }

    private void drawAttendeesForEvent(Event event) {
        Log.d(TAG, "Drawing attendees for event: " + event.getTitle());
        
        // Call the service to draw attendees (null = use capacity or random)
        eventService.drawAttendees(event.getEventId(), null, new IEventService.VoidCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), 
                    "Successfully drew attendees for " + event.getTitle(), 
                    Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Attendees drawn successfully");
                // Optionally reload the events list to show updated state
                loadMyEvents();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), 
                    "Error drawing attendees: " + error, 
                    Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error drawing attendees: " + error);
            }
        });
    }

    private void loadMyEvents() {
        User currentUser = AuthManager.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in", Toast.LENGTH_SHORT).show();
            return;
        }

        // Only load events for organizers/admins
        if (!currentUser.canManageEvents()) {
            Log.w(TAG, "User is not an organizer, skipping event load");
            return;
        }

        // Get events for current user (organizer)
        eventService.getEventsForOrganizer(currentUser.getUid(), new IEventService.EventListCallback() {
            @Override
            public void onSuccess(List<EventEntity> events) {
                Log.d("HomeFragment", "Loaded " + events.size() + " events");
                
                // Convert to UI Events
                List<Event> uiEvents = new ArrayList<>();
                for (EventEntity entity : events) {
                    Event uiEvent = new Event(
                        entity.getEventId(),
                        entity.getTitle(),
                        entity.getDescription(),
                        entity.getCapacity(),
                        entity.getRegistrationStart(),
                        entity.getRegistrationEnd(),
                        entity.getOrganizerId(),
                        entity.getOrganizerName(),
                        entity.isGeolocationRequired()
                    );
                    uiEvent.setMaxEntrants(entity.getMaxEntrants());
                    uiEvent.setPosterUrl(entity.getPosterUrl());
                    uiEvents.add(uiEvent);
                }
                for (Event uiEvent : uiEvents) {
                    Log.d("HomeFragment", "UI Event " + uiEvent.getTitle() +
                            " posterUrl = " + uiEvent.getPosterUrl());
                }
                adapter.updateEvents(uiEvents);
                
                // Show/hide empty state
                if (uiEvents.isEmpty()) {
                    binding.recyclerViewMyEvents.setVisibility(View.GONE);
                    binding.tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    binding.recyclerViewMyEvents.setVisibility(View.VISIBLE);
                    binding.tvEmptyState.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                Log.e("HomeFragment", "Error loading events: " + error);
                Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show();
                binding.tvEmptyState.setVisibility(View.VISIBLE);
                binding.recyclerViewMyEvents.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMyEvents();   // refresh events (and posterUrl) whenever you come back to Home
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
