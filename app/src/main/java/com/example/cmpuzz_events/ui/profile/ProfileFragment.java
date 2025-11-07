package com.example.cmpuzz_events.ui.profile;

import android.content.Intent;
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
import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.databinding.FragmentProfileBinding;
import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.models.event.Invitation;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;
import com.example.cmpuzz_events.ui.event.Event;
import com.example.cmpuzz_events.ui.profile.EnrolledEventsAdapter.EventWithStatus;

import java.util.ArrayList;
import java.util.List;

public class ProfileFragment extends Fragment {

    private FragmentProfileBinding binding;
    private EventService eventService;
    private EnrolledEventsAdapter adapter;
    private static final String TAG = "ProfileFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentProfileBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        eventService = EventService.getInstance();

        // Display user info
        User currentUser = AuthManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            binding.tvUserName.setText(currentUser.getDisplayName());
            binding.tvUserEmail.setText(currentUser.getEmail());
            binding.tvUserRole.setText("Role: " + currentUser.getRole().getRoleName());
            
            // Setup enrolled events RecyclerView
            setupEnrolledEvents(root, currentUser);
        }

        // Setup logout button
        binding.btnLogout.setOnClickListener(v -> logout());

        return root;
    }

    private void setupEnrolledEvents(View root, User currentUser) {
        binding.recyclerViewEnrolledEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new EnrolledEventsAdapter(new ArrayList<>());
        
        adapter.setOnEventActionListener(new EnrolledEventsAdapter.OnEventActionListener() {
            @Override
            public void onLeaveWaitlist(Event event) {
                leaveWaitlist(event, currentUser);
            }

            @Override
            public void onAcceptInvitation(Event event) {
                acceptInvitation(event, currentUser);
            }

            @Override
            public void onDeclineInvitation(Event event) {
                declineInvitation(event, currentUser);
            }

            @Override
            public void onViewEvent(Event event) {
                // Navigate to event details
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getEventId());
                Navigation.findNavController(root).navigate(
                    R.id.action_profile_to_event_details,
                    bundle
                );
            }
        });
        
        binding.recyclerViewEnrolledEvents.setAdapter(adapter);
        loadEnrolledEvents(currentUser.getUid());
    }

    private void loadEnrolledEvents(String userId) {
        // Use getEventsForUserWithEntities to get full event data with invitations
        eventService.getEventsForUserWithEntities(userId, new IEventService.EventListCallback() {
            @Override
            public void onSuccess(List<EventEntity> entities) {
                Log.d(TAG, "Loaded " + entities.size() + " events for user");
                
                // Convert to EventWithStatus
                List<EventWithStatus> eventsWithStatus = new ArrayList<>();
                for (EventEntity entity : entities) {
                    Event uiEvent = convertToUIEvent(entity);
                    String status = determineUserStatus(entity, userId);
                    eventsWithStatus.add(new EventWithStatus(uiEvent, status));
                }
                
                adapter.updateEvents(eventsWithStatus);
                
                if (eventsWithStatus.isEmpty()) {
                    binding.recyclerViewEnrolledEvents.setVisibility(View.GONE);
                    binding.tvEmptyEnrolled.setVisibility(View.VISIBLE);
                } else {
                    binding.recyclerViewEnrolledEvents.setVisibility(View.VISIBLE);
                    binding.tvEmptyEnrolled.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading events: " + error);
                Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private Event convertToUIEvent(EventEntity entity) {
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
        return uiEvent;
    }

    private String determineUserStatus(EventEntity entity, String userId) {
        // Check if user has an invitation first
        if (entity.getInvitations() != null) {
            for (Invitation inv : entity.getInvitations()) {
                if (inv.getUserId() != null && inv.getUserId().equals(userId)) {
                    if (inv.isAccepted()) {
                        return "attending";
                    } else if (inv.isPending()) {
                        return "invited";
                    }
                    // If declined, still show as invited (they can see they declined)
                    return "invited";
                }
            }
        }
        
        // If not invited, must be in waitlist
        return "waitlist";
    }

    private void leaveWaitlist(Event event, User currentUser) {
        eventService.removeFromWaitlist(event.getEventId(), currentUser.getUid(), new IEventService.VoidCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Left waitlist for " + event.getTitle(), Toast.LENGTH_SHORT).show();
                loadEnrolledEvents(currentUser.getUid());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to leave: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void acceptInvitation(Event event, User currentUser) {
        eventService.respondToInvitation(event.getEventId(), currentUser.getUid(), true, new IEventService.VoidCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Accepted invitation to " + event.getTitle(), Toast.LENGTH_SHORT).show();
                loadEnrolledEvents(currentUser.getUid());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to accept: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void declineInvitation(Event event, User currentUser) {
        eventService.respondToInvitation(event.getEventId(), currentUser.getUid(), false, new IEventService.VoidCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Declined invitation to " + event.getTitle(), Toast.LENGTH_SHORT).show();
                loadEnrolledEvents(currentUser.getUid());
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to decline: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void logout() {
        // Clear user session
        AuthManager.getInstance().signOut();
        
        // Show toast
        Toast.makeText(getContext(), "Logged out successfully", Toast.LENGTH_SHORT).show();
        
        // Redirect to LoginActivity
        Intent intent = new Intent(getActivity(), com.example.cmpuzz_events.auth.LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        
        if (getActivity() != null) {
            getActivity().finish();
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
