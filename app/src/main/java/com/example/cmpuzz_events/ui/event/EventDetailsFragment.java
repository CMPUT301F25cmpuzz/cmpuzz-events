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
import androidx.navigation.Navigation;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;
import com.google.android.material.button.MaterialButton;

public class EventDetailsFragment extends Fragment {

    private static final String TAG = "EventDetailsFragment";
    private static final String ARG_EVENT_ID = "eventId";

    private String eventId;

    private IEventService eventService;

    // Store the UI Event
    private Event currentEvent;
    
    // Views
    private TextView eventTitle;
    private TextView eventHost;
    private TextView eventAvailability;
    private TextView datePosted;
    private TextView descriptionText;
    private MaterialButton editButton;
    private MaterialButton shareButton;
    private MaterialButton viewMapButton;
    private MaterialButton joinButton;
    private TextView additionalActionsButton;
    private View dividerTop;
    private View dividerBottom;
    private TextView usersEnrolledTitle;

    public static EventDetailsFragment newInstance(String eventId) {
        EventDetailsFragment fragment = new EventDetailsFragment();
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
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_event_details, container, false);
        
        // Initialize views
        eventTitle = root.findViewById(R.id.event_title);
        eventHost = root.findViewById(R.id.event_host);
        eventAvailability = root.findViewById(R.id.event_availability);
        datePosted = root.findViewById(R.id.date_posted);
        descriptionText = root.findViewById(R.id.description_text);
        editButton = root.findViewById(R.id.edit_button);
        shareButton = root.findViewById(R.id.share_button);
        viewMapButton = root.findViewById(R.id.view_map_button);
        joinButton = root.findViewById(R.id.join_button);
        additionalActionsButton = root.findViewById(R.id.additional_actions_button);
        dividerTop = root.findViewById(R.id.divider_top);
        dividerBottom = root.findViewById(R.id.divider_bottom);
        usersEnrolledTitle = root.findViewById(R.id.users_enrolled_title);
        
        // Setup UI based on user role
        setupRoleBasedUI(root);
        
        loadEventDetails();
        
        return root;
    }

    private void setupRoleBasedUI(View root) {
        User currentUser = AuthManager.getInstance().getCurrentUser();
        boolean isOrganizer = currentUser != null && currentUser.canManageEvents();
        
        if (isOrganizer) {
            // Organizer view - show all management controls
            editButton.setVisibility(View.VISIBLE);
            shareButton.setVisibility(View.VISIBLE);
            viewMapButton.setVisibility(View.VISIBLE);
            additionalActionsButton.setVisibility(View.VISIBLE);
            dividerTop.setVisibility(View.VISIBLE);
            dividerBottom.setVisibility(View.VISIBLE);
            usersEnrolledTitle.setVisibility(View.VISIBLE);
            joinButton.setVisibility(View.GONE);
            
            // Navigate to Action Menu, and pass the event object
            additionalActionsButton.setOnClickListener(v -> {
                if (currentEvent != null) {
                    Bundle bundle = new Bundle();
                    bundle.putSerializable("event", currentEvent);
                    Navigation.findNavController(root).navigate(
                        R.id.action_to_event_action_menu,
                        bundle
                    );
                }
            });
            
        } else {
            // User view - show only Join button and essential info
            editButton.setVisibility(View.GONE);
            shareButton.setVisibility(View.GONE);
            viewMapButton.setVisibility(View.GONE);
            additionalActionsButton.setVisibility(View.GONE);
            dividerTop.setVisibility(View.GONE);
            dividerBottom.setVisibility(View.GONE);
            usersEnrolledTitle.setVisibility(View.GONE);
            joinButton.setVisibility(View.VISIBLE);
            
            // Join event functionality
            joinButton.setOnClickListener(v -> joinEvent());
        }
    }

    private void joinEvent() {
        User currentUser = AuthManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in to join events", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // Use device ID or user ID to join waitlist
        String deviceId = currentUser.getUid();
        
        eventService.joinEvent(eventId, deviceId, new IEventService.VoidCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Successfully joined event!", Toast.LENGTH_SHORT).show();
                // Reload event details to update button state
                loadEventDetails();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to join: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void leaveEvent() {
        User currentUser = AuthManager.getInstance().getCurrentUser();
        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in", Toast.LENGTH_SHORT).show();
            return;
        }
        
        String deviceId = currentUser.getUid();
        
        eventService.removeFromWaitlist(eventId, deviceId, new IEventService.VoidCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(), "Left the waitlist", Toast.LENGTH_SHORT).show();
                // Reload event details to update button state
                loadEventDetails();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Failed to leave: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void loadEventDetails() {
        if (eventId == null || eventId.isEmpty()) {
            Toast.makeText(getContext(), "Invalid event", Toast.LENGTH_SHORT).show();
            return;
        }

        eventService.getEvent(eventId, new IEventService.EventCallback() {
            @Override
            public void onSuccess(EventEntity eventEntity) {
                // Convert to UI Event and store
                currentEvent = new Event(
                    eventEntity.getEventId(),
                    eventEntity.getTitle(),
                    eventEntity.getDescription(),
                    eventEntity.getCapacity(),
                    eventEntity.getRegistrationStart(),
                    eventEntity.getRegistrationEnd(),
                    eventEntity.getOrganizerId(),
                    eventEntity.getOrganizerName(),
                    eventEntity.isGeolocationRequired()
                );
                currentEvent.setMaxEntrants(eventEntity.getMaxEntrants());
                
                displayEventDetails(eventEntity);
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading event: " + error);
                Toast.makeText(getContext(), "Failed to load event details", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void displayEventDetails(EventEntity event) {
        if (getActivity() == null) return;
        
        getActivity().runOnUiThread(() -> {
            eventTitle.setText(event.getTitle());
            // Display organizer name if available, otherwise fall back to ID
            String hostText = event.getOrganizerName() != null && !event.getOrganizerName().isEmpty()
                    ? "Hosted by: " + event.getOrganizerName()
                    : "Hosted by: " + event.getOrganizerId();
            eventHost.setText(hostText);
            descriptionText.setText(event.getDescription());
            
            // Format dates
            if (event.getRegistrationStart() != null) {
                datePosted.setText("Registration Start: " + event.getRegistrationStart());
            }
            
            // Display availability
            int capacity = event.getCapacity();
            eventAvailability.setText("Capacity: " + capacity);
            
            // Check if user is already in waitlist and update Join button
            User currentUser = AuthManager.getInstance().getCurrentUser();
            if (currentUser != null && event.getWaitlist() != null) {
                boolean isInWaitlist = event.getWaitlist().contains(currentUser.getUid());
                if (isInWaitlist) {
                    joinButton.setText("Leave Waitlist");
                    joinButton.setEnabled(true);
                    joinButton.setOnClickListener(v -> leaveEvent());
                } else {
                    joinButton.setText("Join Event");
                    joinButton.setEnabled(true);
                    joinButton.setOnClickListener(v -> joinEvent());
                }
            }
            
            // Setup buttons (placeholder functionality for now)
            editButton.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Edit functionality coming soon", Toast.LENGTH_SHORT).show();
            });
            
            shareButton.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Share functionality coming soon", Toast.LENGTH_SHORT).show();
            });
            
            viewMapButton.setOnClickListener(v -> {
                Toast.makeText(getContext(), "Map functionality coming soon", Toast.LENGTH_SHORT).show();
            });
        });
    }
}
