package com.example.cmpuzz_events.ui.history;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.databinding.FragmentHistoryBinding;
import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Fragment} that displays a history of events that the current user has registered for,
 * including their selection status (selected, not selected, or pending).
 * <p>
 * This fragment retrieves event data using {@link EventService} and displays it in a
 * {@link RecyclerView} managed by an {@link EventHistoryAdapter}. It handles user authentication
 * status via {@link AuthManager}.
 *
 * @see EventHistoryAdapter
 * @see EventService
 * @see AuthManager
 */
public class EventHistoryFragment extends Fragment {

    /**
     * Tag for logging purposes, used to identify logs originating from this class.
     */
    private static final String TAG = "EventHistoryFragment";

    /**
     * View binding instance for this fragment, providing direct access to the views
     * defined in the {@code fragment_history.xml} layout. It is nulled out in {@link #onDestroyView()}.
     */
    private FragmentHistoryBinding binding;

    /**
     * Service layer for fetching event-related data from the backend.
     */
    private EventService eventService;

    /**
     * Adapter for the RecyclerView, responsible for binding event data to the list items.
     */
    private EventHistoryAdapter eventHistoryAdapter;

    /**
     * Called to have the fragment instantiate its user interface view.
     * <p>
     * This method inflates the fragment's layout, initializes view binding, sets up the
     * {@link RecyclerView}, and initiates the process of loading the event history.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state.
     * @return The View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        eventService = EventService.getInstance();

        setupRecyclerView();
        loadEventHistory();

        return root;
    }

    /**
     * Initializes the RecyclerView, setting its layout manager and attaching the {@link EventHistoryAdapter}.
     */
    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.recyclerViewHistory;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventHistoryAdapter = new EventHistoryAdapter(new ArrayList<>());
        recyclerView.setAdapter(eventHistoryAdapter);
    }

    /**
     * Fetches and displays the history of all events the current user has registered for.
     * <p>
     * It first checks if a user is logged in using {@link AuthManager}. If not, it displays a
     * message and an empty state view. If logged in, it calls the {@link EventService} to get all
     * events the user is involved in (as an attendee or on a waitlist). It then passes this list,
     * along with the user's ID, to the {@link EventHistoryAdapter} for display. It handles both
     * success and error cases from the service call, updating the UI accordingly.
     */
    private void loadEventHistory() {
        User currentUser = AuthManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in to see your history.", Toast.LENGTH_SHORT).show();
            binding.emptyStateView.setVisibility(View.VISIBLE);
            binding.recyclerViewHistory.setVisibility(View.GONE);
            return;
        }

        String currentUserId = currentUser.getUid();

        eventService.getEventsForUserWithEntities(currentUserId, new IEventService.EventListCallback() {
            @Override
            public void onSuccess(List<EventEntity> allInvolvedEvents) {
                Log.d(TAG, "Found " + allInvolvedEvents.size() + " events in user's history.");

                if (allInvolvedEvents.isEmpty()) {
                    binding.recyclerViewHistory.setVisibility(View.GONE);
                    binding.emptyStateView.setVisibility(View.VISIBLE);
                } else {
                    binding.recyclerViewHistory.setVisibility(View.VISIBLE);
                    binding.emptyStateView.setVisibility(View.GONE);
                    eventHistoryAdapter.updateEvents(allInvolvedEvents, currentUserId);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading event history: " + error);
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                binding.emptyStateView.setVisibility(View.VISIBLE);
                binding.recyclerViewHistory.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Called when the view previously created by {@link #onCreateView} has been detached from the fragment.
     * <p>
     * This is where the fragment cleans up resources associated with its view. The binding
     * is set to null to avoid memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
