package com.example.cmpuzz_events.ui.event;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.models.notification.Notification;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;
import com.example.cmpuzz_events.service.INotificationService;
import com.example.cmpuzz_events.service.NotificationService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;

import java.util.ArrayList;
import java.util.List;

public class EventActionMenuFragment extends Fragment {

    private static final String TAG = "EventActionMenuFragment";
    private static final String ARG_EVENT = "event";
    private Event event;
    private NotificationService notificationService;
    private EventService eventService;

    public static EventActionMenuFragment newInstance(Event event) {
        EventActionMenuFragment fragment = new EventActionMenuFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EVENT, event);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            event = (Event) getArguments().getSerializable(ARG_EVENT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_event_action_menu, container, false);

        notificationService = NotificationService.getInstance();
        notificationService.setContext(requireContext().getApplicationContext());
        eventService = EventService.getInstance();

        // Setup toolbar
        MaterialToolbar toolbar = root.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Setup click listeners for all menu items
        setupClickListeners(root);

        return root;
    }

    private void setupClickListeners(View root) {
        // Now you have access to the event object!
        String eventTitle = event != null ? event.getTitle() : "Unknown";
        
        // Entrants section
        root.findViewById(R.id.cardCancelEntrants).setOnClickListener(v ->
                showToast("Cancel Entrants for: " + eventTitle));

        root.findViewById(R.id.cardViewDeclinedEntrants).setOnClickListener(v -> {
            if (event != null) {
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getEventId());
                Navigation.findNavController(root).navigate(
                    R.id.action_to_view_entrants,
                    bundle
                );
            }
        });

        root.findViewById(R.id.cardViewWaitlist).setOnClickListener(v -> {
            if (event != null) {
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getEventId());
                Navigation.findNavController(root).navigate(
                    R.id.action_to_view_entrants,
                    bundle
                );
            }
        });

        root.findViewById(R.id.cardViewInvitedEntrants).setOnClickListener(v -> {
            if (event != null) {
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getEventId());
                Navigation.findNavController(root).navigate(
                    R.id.action_to_view_entrants,
                    bundle
                );
            }
        });

        root.findViewById(R.id.cardViewAttendees).setOnClickListener(v -> {
            if (event != null) {
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getEventId());
                Navigation.findNavController(root).navigate(
                    R.id.action_to_view_entrants,
                    bundle
                );
            }
        });

        // Notifications section
        root.findViewById(R.id.cardNotifyDeclined).setOnClickListener(v -> 
                sendNotificationsToGroup("declined"));

        root.findViewById(R.id.cardNotifyWaitlist).setOnClickListener(v -> 
                sendNotificationsToGroup("waitlist"));

        root.findViewById(R.id.cardNotifyInvited).setOnClickListener(v -> 
                sendNotificationsToGroup("invited"));

        root.findViewById(R.id.cardNotifyAttendees).setOnClickListener(v -> 
                sendNotificationsToGroup("attendees"));
    }

    private void sendNotificationsToGroup(String group) {
        if (event == null) {
            showToast("Error: Event not found");
            return;
        }

        // Get full event entity with user lists
        eventService.getEvent(event.getEventId(), new IEventService.EventCallback() {
            @Override
            public void onSuccess(EventEntity eventEntity) {
                final List<String> userIds;
                final Notification.NotificationType notificationType;

                switch (group) {
                    case "waitlist":
                        userIds = eventEntity.getWaitlist() != null ? 
                                new ArrayList<>(eventEntity.getWaitlist()) : new ArrayList<>();
                        notificationType = Notification.NotificationType.WAITLISTED;
                        break;
                    case "invited":
                        userIds = new ArrayList<>();
                        if (eventEntity.getInvitations() != null) {
                            for (com.example.cmpuzz_events.models.event.Invitation inv : eventEntity.getInvitations()) {
                                if (inv.getUserId() != null && inv.isPending()) {
                                    userIds.add(inv.getUserId());
                                }
                            }
                        }
                        notificationType = Notification.NotificationType.INVITED;
                        break;
                    case "attendees":
                        userIds = eventEntity.getAttendees() != null ? 
                                new ArrayList<>(eventEntity.getAttendees()) : new ArrayList<>();
                        notificationType = Notification.NotificationType.CONFIRMED;
                        break;
                    case "declined":
                        userIds = eventEntity.getDeclined() != null ? 
                                new ArrayList<>(eventEntity.getDeclined()) : new ArrayList<>();
                        notificationType = Notification.NotificationType.DECLINED;
                        break;
                    default:
                        showToast("Invalid group");
                        return;
                }

                if (userIds.isEmpty()) {
                    showToast("No users in " + group + " group");
                    return;
                }

                // Send notifications
                notificationService.sendNotificationsToUsers(
                    userIds,
                    event.getEventId(),
                    event.getTitle(),
                    notificationType,
                    new INotificationService.VoidCallback() {
                        @Override
                        public void onSuccess() {
                            showToast("Notifications sent to " + group + " (" + userIds.size() + " users)");
                        }

                        @Override
                        public void onError(String error) {
                            showToast("Error sending notifications: " + error);
                            Log.e(TAG, "Error: " + error);
                        }
                    }
                );
            }

            @Override
            public void onError(String error) {
                showToast("Error loading event details");
                Log.e(TAG, "Error: " + error);
            }
        });
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
