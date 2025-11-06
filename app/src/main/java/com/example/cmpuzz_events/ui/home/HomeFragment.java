package com.example.cmpuzz_events.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

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

    private FragmentHomeBinding binding;
    private EventService eventService;
    private MyEventsAdapter adapter;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        eventService = EventService.getInstance();
        
        // Setup RecyclerView
        RecyclerView recyclerView = binding.recyclerViewMyEvents;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MyEventsAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
        
        // Load user's events
        loadMyEvents();
        
        return root;
    }

    private void loadMyEvents() {
        User currentUser = AuthManager.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in", Toast.LENGTH_SHORT).show();
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
                        entity.isGeolocationRequired()
                    );
                    uiEvent.setMaxEntrants(entity.getMaxEntrants());
                    uiEvents.add(uiEvent);
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
