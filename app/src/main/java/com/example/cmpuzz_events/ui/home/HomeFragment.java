package com.example.cmpuzz_events.ui.home;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioGroup;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;

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
import java.util.stream.Collectors;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private EventService eventService;
    private MyEventsAdapter adapter;

    // List to hold all events for filtering
    private List<Event> allEvents = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        User currentUser = AuthManager.getInstance().getCurrentUser();
        if (currentUser == null || !currentUser.canManageEvents()) {
            Log.w(TAG, "HomeFragment accessed by non-organizer user");
            return root;
        }

        eventService = EventService.getInstance();

        setupRecyclerView(root);
        setupSearchView();
        setupAvailabilityFilter();

        loadMyEvents();

        return root;
    }

    private void setupAvailabilityFilter() {
        binding.availabilityFilterGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // when the radio buttons change, apply the new filters onto the allEvents list
                applyFilters();
            }
        });
    }

    private void setupRecyclerView(View root) {
        RecyclerView recyclerView = binding.recyclerViewMyEvents;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MyEventsAdapter(new ArrayList<>(), true);

        adapter.setOnEventClickListener(new MyEventsAdapter.OnEventClickListener() {
            @Override
            public void onViewEventClick(Event event) {
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
    }

    // search view added to allow user to filter events by title or description
    private void setupSearchView() {
        binding.eventSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                applyFilters();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                applyFilters();
                return true;
            }
        });
    }

    // method for applying all filters such as search and availability from radio buttons
    private void applyFilters() {
        // Get the query and selected availability from the UI
        String query = binding.eventSearchView.getQuery().toString();
        int selectedAvailabilityId = binding.availabilityFilterGroup.getCheckedRadioButtonId();

        List<Event> filteredEvents = new ArrayList<>();
        String lowerCaseQuery = (query == null) ? "" : query.toLowerCase();

        // Apply filters to the list of all events
        for (Event event : allEvents) {
            // Check if the event matches the selected availability
            boolean availabilityMatch = false;
            if (selectedAvailabilityId == R.id.radio_not_full) {
                if (event.getMaxEntrants() < event.getCapacity()) {
                    availabilityMatch = true;
                }
            } else if (selectedAvailabilityId == R.id.radio_full) {
                if (event.getMaxEntrants() >= event.getCapacity()) {
                    availabilityMatch = true;
                }
            } else {
                availabilityMatch = true;
            }
            // lastly checking the query matching
            if (availabilityMatch) {
                if (lowerCaseQuery.isEmpty() ||
                        event.getTitle().toLowerCase().contains(lowerCaseQuery) || event.getDescription().toLowerCase().contains(lowerCaseQuery)) {
                    filteredEvents.add(event);
                }
            }
        }
        // update the view's adapter with the filtered events
        adapter.updateEvents(filteredEvents);
    }

    private void drawAttendeesForEvent(Event event) {
        Log.d(TAG, "Drawing attendees for event: " + event.getTitle());

        eventService.drawAttendees(event.getEventId(), null, new IEventService.VoidCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(),
                        "Successfully drew attendees for " + event.getTitle(),
                        Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Attendees drawn successfully");
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

        if (!currentUser.canManageEvents()) {
            Log.w(TAG, "User is not an organizer, skipping event load");
            return;
        }

        eventService.getEventsForOrganizer(currentUser.getUid(), new IEventService.EventListCallback() {
            @Override
            public void onSuccess(List<EventEntity> events) {
                Log.d("HomeFragment", "Loaded " + events.size() + " events");

                allEvents.clear();
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
                    allEvents.add(uiEvent);
                }

                applyFilters();

                if (allEvents.isEmpty()) {
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
