package com.example.cmpuzz_events.ui.browse;

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

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.databinding.FragmentBrowseEventsBinding;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;
import com.example.cmpuzz_events.ui.event.Event;
import com.example.cmpuzz_events.ui.home.MyEventsAdapter;

import java.util.ArrayList;
import java.util.List;

public class BrowseEventsFragment extends Fragment {

    private FragmentBrowseEventsBinding binding;
    private EventService eventService;
    private MyEventsAdapter adapter;
    private static final String TAG = "BrowseEventsFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentBrowseEventsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        eventService = EventService.getInstance();
        
        // Setup RecyclerView
        binding.recyclerViewBrowseEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MyEventsAdapter(new ArrayList<>(), false); // false = user view
        
        // Set click listener to navigate to event details
        adapter.setOnEventClickListener(event -> {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", event.getEventId());
            Navigation.findNavController(root).navigate(
                R.id.action_browse_to_event_details,
                bundle
            );
        });
        
        binding.recyclerViewBrowseEvents.setAdapter(adapter);
        
        // Load all events
        loadAllEvents();
        
        return root;
    }

    private void loadAllEvents() {
        eventService.getAllEvents(new IEventService.UIEventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                Log.d(TAG, "Loaded " + events.size() + " events");
                adapter.updateEvents(events);
                
                // Show/hide empty state
                if (events.isEmpty()) {
                    binding.recyclerViewBrowseEvents.setVisibility(View.GONE);
                    binding.tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    binding.recyclerViewBrowseEvents.setVisibility(View.VISIBLE);
                    binding.tvEmptyState.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading events: " + error);
                Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show();
                binding.tvEmptyState.setVisibility(View.VISIBLE);
                binding.recyclerViewBrowseEvents.setVisibility(View.GONE);
            }
        });
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
