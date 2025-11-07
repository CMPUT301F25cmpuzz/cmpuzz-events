package com.example.cmpuzz_events.ui.notifications;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.databinding.FragmentNotificationsBinding;
import com.example.cmpuzz_events.models.notification.Notification;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;
import com.example.cmpuzz_events.service.INotificationService;
import com.example.cmpuzz_events.service.NotificationService;

import java.util.List;

public class NotificationsFragment extends Fragment {

    private static final String TAG = "NotificationsFragment";
    private FragmentNotificationsBinding binding;
    private NotificationAdapter adapter;
    private NotificationService notificationService;
    private EventService eventService;
    private User currentUser;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentNotificationsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        notificationService = NotificationService.getInstance();
        eventService = EventService.getInstance();
        currentUser = AuthManager.getInstance().getCurrentUser();

        setupRecyclerView();
        loadNotifications();

        return root;
    }

    private void setupRecyclerView() {
        adapter = new NotificationAdapter();
        binding.recyclerViewNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        binding.recyclerViewNotifications.setAdapter(adapter);

        adapter.setOnNotificationActionListener(new NotificationAdapter.OnNotificationActionListener() {
            @Override
            public void onAcceptInvitation(Notification notification) {
                handleAcceptInvitation(notification);
            }

            @Override
            public void onDeclineInvitation(Notification notification) {
                handleDeclineInvitation(notification);
            }

            @Override
            public void onNotificationClick(Notification notification) {
                markAsRead(notification);
            }
        });
    }

    private void loadNotifications() {
        if (currentUser == null) {
            return;
        }

        notificationService.getNotificationsForUser(currentUser.getUid(), 
            new INotificationService.NotificationListCallback() {
                @Override
                public void onSuccess(List<Notification> notifications) {
                    Log.d(TAG, "Loaded " + notifications.size() + " notifications");
                    adapter.setNotifications(notifications);
                    
                    if (notifications.isEmpty()) {
                        binding.recyclerViewNotifications.setVisibility(View.GONE);
                        binding.tvEmptyState.setVisibility(View.VISIBLE);
                    } else {
                        binding.recyclerViewNotifications.setVisibility(View.VISIBLE);
                        binding.tvEmptyState.setVisibility(View.GONE);
                    }
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error loading notifications: " + error);
                    Toast.makeText(getContext(), "Error loading notifications", Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void handleAcceptInvitation(Notification notification) {
        eventService.respondToInvitation(notification.getEventId(), currentUser.getUid(), true,
            new IEventService.VoidCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Invitation accepted!", Toast.LENGTH_SHORT).show();
                    
                    // Notify organizer
                    notifyOrganizerOfResponse(notification, true);
                    
                    // Remove notification from list
                    loadNotifications();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void handleDeclineInvitation(Notification notification) {
        eventService.respondToInvitation(notification.getEventId(), currentUser.getUid(), false,
            new IEventService.VoidCallback() {
                @Override
                public void onSuccess() {
                    Toast.makeText(getContext(), "Invitation declined", Toast.LENGTH_SHORT).show();
                    
                    // Notify organizer
                    notifyOrganizerOfResponse(notification, false);
                    
                    // Remove notification from list
                    loadNotifications();
                }

                @Override
                public void onError(String error) {
                    Toast.makeText(getContext(), "Error: " + error, Toast.LENGTH_SHORT).show();
                }
            });
    }

    private void notifyOrganizerOfResponse(Notification notification, boolean accepted) {
        // Get the event to find the organizer
        eventService.getUIEventById(notification.getEventId(), new IEventService.UIEventCallback() {
            @Override
            public void onSuccess(com.example.cmpuzz_events.ui.event.Event event) {
                String userName = currentUser.getDisplayName() != null ? 
                                currentUser.getDisplayName() : "A user";
                
                notificationService.notifyOrganizerOfResponse(
                    event.getOrganizerId(),
                    userName,
                    notification.getEventId(),
                    notification.getEventName(),
                    accepted,
                    new INotificationService.VoidCallback() {
                        @Override
                        public void onSuccess() {
                            Log.d(TAG, "Organizer notified of response");
                        }

                        @Override
                        public void onError(String error) {
                            Log.e(TAG, "Error notifying organizer: " + error);
                        }
                    }
                );
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error getting event: " + error);
            }
        });
    }

    private void markAsRead(Notification notification) {
        if (!notification.isRead()) {
            notificationService.markAsRead(notification.getId(), new INotificationService.VoidCallback() {
                @Override
                public void onSuccess() {
                    // Refresh to update UI
                    loadNotifications();
                }

                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error marking as read: " + error);
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
