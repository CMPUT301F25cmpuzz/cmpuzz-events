package com.example.cmpuzz_events.ui.admin;

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
import com.example.cmpuzz_events.ui.event.EnrolledUsersAdapter;
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
public class BrowseUsersFragment extends Fragment {

    private static final String TAG = "ViewEntrantsFragment";
    private RecyclerView recyclerView;
    private UserListAdapter adapter;
    private TextView emptyStateText;
    private List<User> users;
    private boolean isAdmin;

//    public static BrowseUsersFragment newInstance(String eventId) {
//        BrowseUsersFragment fragment = new BrowseUsersFragment();
//        Bundle args = new Bundle();
//        fragment.setArguments(args);
//        return fragment;
//    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User currentUser = AuthManager.getInstance().getCurrentUser();
        isAdmin = currentUser != null && currentUser.isAdmin();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_view_entrants, container, false);
        
        // Initialize views
        recyclerView = root.findViewById(R.id.recyclerView);
        emptyStateText = root.findViewById(R.id.tvEmptyState);
        
        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserListAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);
        
        return root;
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
