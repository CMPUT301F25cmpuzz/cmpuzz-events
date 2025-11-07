package com.example.cmpuzz_events.ui.event;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.models.event.Invitation;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment to view all entrants for an event (waitlist + invited + attendees)
 * Excludes declined users
 */
public class ViewAllEntrantsFragment extends Fragment {

    private static final String TAG = "ViewAllEntrantsFragment";
    private static final String ARG_EVENT_ID = "eventId";
    
    private String eventId;
    private RecyclerView recyclerView;
    private EnrolledUsersAdapter adapter;
    private TextView emptyStateText;

    public static ViewAllEntrantsFragment newInstance(String eventId) {
        ViewAllEntrantsFragment fragment = new ViewAllEntrantsFragment();
        Bundle args = new Bundle();
        args.putString(ARG_EVENT_ID, eventId);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            eventId = getArguments().getString(ARG_EVENT_ID);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_view_all_entrants, container, false);
        
        // Initialize views
        recyclerView = root.findViewById(R.id.recyclerView);
        emptyStateText = root.findViewById(R.id.tvEmptyState);
        
        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EnrolledUsersAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
        
        // Load all entrants
        loadAllEntrants();
        
        return root;
    }

    private void loadAllEntrants() {
        EventService.getInstance().getEvent(eventId, new IEventService.EventCallback() {
            @Override
            public void onSuccess(EventEntity event) {
                // Combine all user IDs: waitlist + invited + attendees (exclude declined)
                List<String> allUserIds = new ArrayList<>();
                
                // Add waitlist users
                if (event.getWaitlist() != null) {
                    allUserIds.addAll(event.getWaitlist());
                }
                
                // Add users with pending invitations
                if (event.getInvitations() != null) {
                    for (Invitation invitation : event.getInvitations()) {
                        String userId = invitation.getUserId();
                        if (!allUserIds.contains(userId)) {
                            allUserIds.add(userId);
                        }
                    }
                }
                
                // Add attendees (users who accepted invitations)
                if (event.getAttendees() != null) {
                    for (String userId : event.getAttendees()) {
                        if (!allUserIds.contains(userId)) {
                            allUserIds.add(userId);
                        }
                    }
                }
                
                // Load all users
                if (allUserIds.isEmpty()) {
                    showEmptyState("No entrants yet");
                    return;
                }
                
                loadUsers(allUserIds);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading event: " + error);
                showEmptyState("Error loading event data");
            }
        });
    }

    private void loadUsers(List<String> userIds) {
        AuthManager.getInstance().getUsersByIds(userIds, new AuthManager.UsersCallback() {
            @Override
            public void onSuccess(List<User> users) {
                if (users.isEmpty()) {
                    showEmptyState("No users found");
                } else {
                    adapter.updateUsers(users);
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyStateText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading users: " + error);
                showEmptyState("Error loading users");
            }
        });
    }

    private void showEmptyState(String message) {
        adapter.updateUsers(new ArrayList<>());
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText(message);
    }
}
