package com.example.cmpuzz_events.ui.event;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

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
import com.google.android.material.button.MaterialButton;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment to view different categories of entrants:
 * - Waitlist: Users waiting for invitation
 * - Invited: Users who have been sent invitations
 * - Attendees: Users who accepted invitations
 */
public class ViewEntrantsFragment extends Fragment {

    private static final String TAG = "ViewEntrantsFragment";
    private static final String ARG_EVENT_ID = "eventId";
    
    private String eventId;
    private EventEntity currentEvent;
    
    private TabLayout tabLayout;
    private RecyclerView recyclerView;
    private EnrolledUsersAdapter adapter;
    private TextView emptyStateText;
    private MaterialButton drawReplacementButton;
    private boolean isOrganizer;
    private boolean isDrawingReplacement = false;
    private IEventService eventService;

    public static ViewEntrantsFragment newInstance(String eventId) {
        ViewEntrantsFragment fragment = new ViewEntrantsFragment();
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
        eventService = EventService.getInstance();
        User currentUser = AuthManager.getInstance().getCurrentUser();
        isOrganizer = currentUser != null && currentUser.canManageEvents();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_view_entrants, container, false);
        
        // Initialize views
        tabLayout = root.findViewById(R.id.tabLayout);
        recyclerView = root.findViewById(R.id.recyclerView);
        emptyStateText = root.findViewById(R.id.tvEmptyState);
        drawReplacementButton = root.findViewById(R.id.btnDrawReplacement);
        drawReplacementButton.setOnClickListener(v -> drawReplacementEntrant());
        
        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EnrolledUsersAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
        
        // Setup tabs
        tabLayout.addTab(tabLayout.newTab().setText("Waitlist"));
        tabLayout.addTab(tabLayout.newTab().setText("Invited"));
        tabLayout.addTab(tabLayout.newTab().setText("Attendees"));
        tabLayout.addTab(tabLayout.newTab().setText("Declined"));
        
        // Tab selection listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadEntrantsForTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });
        
        // Load event data
        loadEventData();
        
        return root;
    }

    private void loadEventData() {
        eventService.getEvent(eventId, new IEventService.EventCallback() {
            @Override
            public void onSuccess(EventEntity event) {
                currentEvent = event;
                int position = tabLayout != null ? tabLayout.getSelectedTabPosition() : 0;
                if (position < 0) {
                    position = 0;
                }
                loadEntrantsForTab(position);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading event: " + error);
                showEmptyState("Error loading event data");
            }
        });
    }

    private void loadEntrantsForTab(int position) {
        updateReplacementButton(position);
        if (currentEvent == null) return;

        switch (position) {
            case 0: // Waitlist
                loadWaitlist();
                break;
            case 1: // Invited
                loadInvited();
                break;
            case 2: // Attendees
                loadAttendees();
                break;
            case 3: // Declined
                loadDeclined();
                break;
        }
    }

    private void loadWaitlist() {
        List<String> waitlist = currentEvent.getWaitlist();
        if (waitlist == null || waitlist.isEmpty()) {
            showEmptyState("No users in waitlist");
            return;
        }

        loadUsers(waitlist);
    }

    private void loadInvited() {
        List<Invitation> invitations = currentEvent.getInvitations();
        if (invitations == null || invitations.isEmpty()) {
            showEmptyState("No invited users");
            return;
        }

        // Extract user IDs from invitations
        List<String> userIds = new ArrayList<>();
        for (Invitation invitation : invitations) {
            if (invitation.isPending()) {
                userIds.add(invitation.getUserId());
            }
        }

        if (userIds.isEmpty()) {
            showEmptyState("No pending invitations");
            return;
        }

        loadUsers(userIds);
    }

    private void loadAttendees() {
        List<String> attendees = currentEvent.getAttendees();
        if (attendees == null || attendees.isEmpty()) {
            showEmptyState("No attendees yet");
            return;
        }

        loadUsers(attendees);
    }

    private void loadDeclined() {
        List<String> declined = currentEvent.getDeclined();
        if (declined == null || declined.isEmpty()) {
            showEmptyState("No declined users");
            return;
        }

        loadUsers(declined);
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

    private void updateReplacementButton(int tabPosition) {
        if (drawReplacementButton == null) return;

        if (!isOrganizer || tabPosition != 3) {
            drawReplacementButton.setVisibility(View.GONE);
            return;
        }

        drawReplacementButton.setVisibility(View.VISIBLE);
        boolean hasWaitlist = currentEvent != null &&
                currentEvent.getWaitlist() != null &&
                !currentEvent.getWaitlist().isEmpty();
        boolean hasDeclined = currentEvent != null &&
                currentEvent.getDeclined() != null &&
                !currentEvent.getDeclined().isEmpty();
        drawReplacementButton.setEnabled(hasWaitlist && hasDeclined && !isDrawingReplacement);
    }

    private void drawReplacementEntrant() {
        if (eventId == null || isDrawingReplacement) {
            return;
        }
        if (currentEvent == null || currentEvent.getDeclined() == null || currentEvent.getDeclined().isEmpty()) {
            showToast("No declined entrants to replace.");
            return;
        }
        if (currentEvent.getWaitlist() == null || currentEvent.getWaitlist().isEmpty()) {
            showToast("Waitlist is empty - no replacements available.");
            return;
        }
        isDrawingReplacement = true;
        updateReplacementButton(tabLayout != null ? tabLayout.getSelectedTabPosition() : 3);

        eventService.drawReplacementAttendee(eventId, new IEventService.VoidCallback() {
            @Override
            public void onSuccess() {
                isDrawingReplacement = false;
                showToast("Replacement invitation sent.");
                loadEventData();
            }

            @Override
            public void onError(String error) {
                isDrawingReplacement = false;
                showToast(error != null ? error : "Unable to draw replacement.");
                updateReplacementButton(tabLayout != null ? tabLayout.getSelectedTabPosition() : 3);
            }
        });
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
