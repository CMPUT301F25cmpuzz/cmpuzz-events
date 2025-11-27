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
 * A {@link Fragment} that displays a history of events for which the current user
 * has registered and that have already concluded.
 * <p>
 * This fragment retrieves past event data using the {@link EventService} and displays
 * it in a {@link RecyclerView}. It handles UI states for when the list is populated,
 * when it's empty, and when an error occurs during data fetching.
 */
public class EventHistoryFragment extends Fragment {

    private static final String TAG = "EventHistoryFragment";

    /**
     * View binding instance for this fragment's layout, providing type-safe access to views.
     */
    private FragmentHistoryBinding binding;

    /**
     * Service responsible for all event-related data operations.
     */
    private EventService eventService;

    /**
     * Adapter for the RecyclerView that displays the list of past events.
     */
    private EventHistoryAdapter eventHistoryAdapter;

    /**
     * Called to have the fragment instantiate its user interface view.
     * This is where layout inflation and view binding initialization occurs.
     *
     * @param inflater           The LayoutInflater object that can be used to inflate any views in the fragment.
     * @param container          If non-null, this is the parent view that the fragment's UI should be attached to.
     * @param savedInstanceState If non-null, this fragment is being re-constructed from a previous saved state as given here.
     * @return Return the View for the fragment's UI, or null.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHistoryBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        eventService = EventService.getInstance();

        setupRecyclerView();
        loadRegistrationHistory();

        return root;
    }

    /**
     * Initializes the RecyclerView with a {@link LinearLayoutManager} and sets up the
     * {@link EventHistoryAdapter}.
     */
    private void setupRecyclerView() {
        RecyclerView recyclerView = binding.recyclerViewHistory;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        eventHistoryAdapter = new EventHistoryAdapter(new ArrayList<>());
        recyclerView.setAdapter(eventHistoryAdapter);
    }

    /**
     * Fetches the registration history for the currently logged-in user from the {@link EventService}.
     * It handles the UI updates based on the success or failure of the data fetching operation.
     * If no user is logged in, it displays a toast message and the empty state view.
     */
    private void loadRegistrationHistory() {
        User currentUser = AuthManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "You must be logged in to see your history.", Toast.LENGTH_SHORT).show();
            binding.emptyStateView.setVisibility(View.VISIBLE);
            binding.recyclerViewHistory.setVisibility(View.GONE);
            return;
        }

        binding.recyclerViewHistory.setVisibility(View.VISIBLE);
        binding.emptyStateView.setVisibility(View.GONE);

        eventService.getRegistrationHistory(currentUser.getUid(), new IEventService.RegistrationHistoryCallback() {
            /**
             * Handles the successful retrieval of past event data.
             *
             * @param pastEvents A list of {@link EventEntity} objects representing past events.
             */
            @Override
            public void onSuccess(List<EventEntity> pastEvents) {
                Log.d(TAG, "Successfully loaded " + pastEvents.size() + " past events.");

                if (pastEvents.isEmpty()) {
                    binding.recyclerViewHistory.setVisibility(View.GONE);
                    binding.emptyStateView.setVisibility(View.VISIBLE);
                } else {
                    binding.recyclerViewHistory.setVisibility(View.VISIBLE);
                    binding.emptyStateView.setVisibility(View.GONE);
                    eventHistoryAdapter.updateEvents(pastEvents);
                }
            }

            /**
             * Handles errors that occur during the data fetching process.
             *
             * @param error A string describing the error.
             */
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading registration history: " + error);
                Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_LONG).show();
                binding.emptyStateView.setVisibility(View.VISIBLE);
                binding.recyclerViewHistory.setVisibility(View.GONE);
            }
        });
    }

    /**
     * Called when the view previously created by {@link #onCreateView} has been detached from the fragment.
     * This is where we clean up the binding reference to avoid memory leaks.
     */
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
