package com.example.cmpuzz_events.ui.admin;

import android.os.Bundle;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.models.notification.Notification;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;
import com.example.cmpuzz_events.service.INotificationService;
import com.example.cmpuzz_events.service.NotificationService;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Admin-only fragment to view a log of all notifications sent to entrants by organizers.
 * Shows organizer name, recipient name, event name, notification type, and timestamp.
 */
public class NotificationLogFragment extends Fragment {

    private static final String TAG = "NotificationLogFragment";
    
    private RecyclerView recyclerView;
    private NotificationLogAdapter adapter;
    private TextView emptyStateText;
    private Spinner filterSpinner;
    private NotificationService notificationService;
    private EventService eventService;
    private User currentUser;
    
    // Cache for events and users to avoid repeated queries
    private Map<String, EventEntity> eventCache = new HashMap<>();
    private Map<String, User> userCache = new HashMap<>();
    
    // Store all notifications for filtering
    private List<Notification> allNotifications = new ArrayList<>();
    
    // Track starred/important notifications (using notification IDs)
    private Set<String> starredNotificationIds = new HashSet<>();
    
    // Filter options
    private enum FilterOption {
        MOST_RECENT("Most Recent"),
        OLDEST("Oldest"),
        ACCEPTED("Accepted"),
        DECLINED("Declined"),
        WAITLISTED("Waitlisted"),
        IMPORTANT("Important");
        
        private final String displayName;
        
        FilterOption(String displayName) {
            this.displayName = displayName;
        }
        
        public String getDisplayName() {
            return displayName;
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_notification_log, container, false);
        
        notificationService = NotificationService.getInstance();
        notificationService.setContext(requireContext().getApplicationContext());
        eventService = EventService.getInstance();
        currentUser = AuthManager.getInstance().getCurrentUser();
        
        // Check if user is admin
        if (currentUser == null || !currentUser.isAdmin()) {
            Log.w(TAG, "Non-admin user attempted to access notification log");
            return root;
        }
        
        recyclerView = root.findViewById(R.id.recyclerViewLog);
        emptyStateText = root.findViewById(R.id.tvEmptyState);
        filterSpinner = root.findViewById(R.id.spinnerFilter);
        
        // Hide action bar completely to prevent it from blocking the filter
        if (getActivity() instanceof AppCompatActivity) {
            AppCompatActivity activity = (AppCompatActivity) getActivity();
            if (activity.getSupportActionBar() != null) {
                activity.getSupportActionBar().hide();
            }
        }
        
        adapter = new NotificationLogAdapter(new ArrayList<>());
        adapter.setOnStarClickListener((notificationId, isStarred) -> {
            // Toggle starred state
            if (isStarred) {
                starredNotificationIds.add(notificationId);
            } else {
                starredNotificationIds.remove(notificationId);
            }
            // Update adapter with new starred set
            adapter.setStarredNotificationIds(starredNotificationIds);
            // Reapply filter if "Important" is selected
            if (filterSpinner.getSelectedItemPosition() == FilterOption.IMPORTANT.ordinal()) {
                applyFilter();
            }
        });
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        recyclerView.setAdapter(adapter);
        
        setupFilterSpinner();
        loadNotificationLog();
        
        return root;
    }
    
    private void loadNotificationLog() {
        notificationService.getAllNotifications(new INotificationService.NotificationListCallback() {
            @Override
            public void onSuccess(List<Notification> notifications) {
                Log.d(TAG, "Loaded " + notifications.size() + " notifications for log");
                
                if (notifications.isEmpty()) {
                    showEmptyState("No notifications found");
                    return;
                }
                
                // Load event and user data for all notifications
                loadNotificationDetails(notifications);
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading notifications: " + error);
                showEmptyState("Error loading notification log");
            }
        });
    }
    
    private void loadNotificationDetails(List<Notification> notifications) {
        // Collect unique event IDs and user IDs
        List<String> eventIds = new ArrayList<>();
        List<String> userIds = new ArrayList<>();
        
        for (Notification notification : notifications) {
            if (notification.getEventId() != null && !eventIds.contains(notification.getEventId())) {
                eventIds.add(notification.getEventId());
            }
            if (notification.getUserId() != null && !userIds.contains(notification.getUserId())) {
                userIds.add(notification.getUserId());
            }
        }
        
        // Store all notifications for filtering
        allNotifications = notifications;
        
        // Load events to get organizer info
        loadEvents(eventIds, () -> {
            // Load users to get recipient names
            loadUsers(userIds, () -> {
                // Set starred IDs in adapter
                adapter.setStarredNotificationIds(starredNotificationIds);
                // Apply current filter and update adapter
                applyFilter();
            });
        });
    }
    
    private void setupFilterSpinner() {
        // Create array of filter option display names
        String[] filterOptions = new String[FilterOption.values().length];
        for (int i = 0; i < FilterOption.values().length; i++) {
            filterOptions[i] = FilterOption.values()[i].getDisplayName();
        }
        
        // Create adapter for spinner using custom layouts
        ArrayAdapter<String> spinnerAdapter = new ArrayAdapter<>(
                requireContext(),
                R.layout.spinner_item_filter,
                filterOptions
        );
        spinnerAdapter.setDropDownViewResource(R.layout.spinner_dropdown_item_filter);
        filterSpinner.setAdapter(spinnerAdapter);
        
        // Set default selection to "Most Recent"
        filterSpinner.setSelection(0);
        
        // Handle filter selection
        filterSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                applyFilter();
            }
            
            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // Do nothing
            }
        });
    }
    
    private void applyFilter() {
        if (allNotifications.isEmpty()) {
            showEmptyState("No notifications found");
            return;
        }
        
        FilterOption selectedFilter = FilterOption.values()[filterSpinner.getSelectedItemPosition()];
        List<Notification> filteredNotifications = new ArrayList<>(allNotifications);
        
        switch (selectedFilter) {
            case MOST_RECENT:
                // Already sorted by timestamp descending (most recent first)
                // No additional filtering needed
                break;
                
            case OLDEST:
                // Sort by timestamp ascending (oldest first)
                filteredNotifications.sort((n1, n2) -> Long.compare(n1.getTimestamp(), n2.getTimestamp()));
                break;
                
            case ACCEPTED:
                // Filter by ACCEPTED type
                filteredNotifications.removeIf(n -> 
                    n.getType() == null || n.getType() != Notification.NotificationType.ACCEPTED);
                // Sort by timestamp descending
                filteredNotifications.sort((n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                break;
                
            case DECLINED:
                // Filter by DECLINED type
                filteredNotifications.removeIf(n -> 
                    n.getType() == null || n.getType() != Notification.NotificationType.DECLINED);
                // Sort by timestamp descending
                filteredNotifications.sort((n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                break;
                
            case WAITLISTED:
                // Filter by WAITLISTED type
                filteredNotifications.removeIf(n -> 
                    n.getType() == null || n.getType() != Notification.NotificationType.WAITLISTED);
                // Sort by timestamp descending
                filteredNotifications.sort((n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                break;
                
            case IMPORTANT:
                // Filter by starred notifications
                filteredNotifications.removeIf(n -> 
                    n.getId() == null || !starredNotificationIds.contains(n.getId()));
                // Sort by timestamp descending
                filteredNotifications.sort((n1, n2) -> Long.compare(n2.getTimestamp(), n1.getTimestamp()));
                break;
        }
        
        if (filteredNotifications.isEmpty()) {
            showEmptyState("No notifications match the selected filter");
        } else {
            adapter.setNotifications(filteredNotifications, eventCache, userCache);
            adapter.setStarredNotificationIds(starredNotificationIds);
            recyclerView.setVisibility(View.VISIBLE);
            emptyStateText.setVisibility(View.GONE);
        }
    }
    
    private void loadEvents(List<String> eventIds, Runnable onComplete) {
        if (eventIds.isEmpty()) {
            onComplete.run();
            return;
        }
        
        int[] loadedCount = {0};
        int totalEvents = eventIds.size();
        
        for (String eventId : eventIds) {
            eventService.getEvent(eventId, new IEventService.EventCallback() {
                @Override
                public void onSuccess(EventEntity event) {
                    eventCache.put(eventId, event);
                    loadedCount[0]++;
                    if (loadedCount[0] == totalEvents) {
                        onComplete.run();
                    }
                }
                
                @Override
                public void onError(String error) {
                    Log.e(TAG, "Error loading event " + eventId + ": " + error);
                    loadedCount[0]++;
                    if (loadedCount[0] == totalEvents) {
                        onComplete.run();
                    }
                }
            });
        }
    }
    
    private void loadUsers(List<String> userIds, Runnable onComplete) {
        if (userIds.isEmpty()) {
            onComplete.run();
            return;
        }
        
        AuthManager.getInstance().getUsersByIds(userIds, new AuthManager.UsersCallback() {
            @Override
            public void onSuccess(List<User> users) {
                for (User user : users) {
                    userCache.put(user.getUid(), user);
                }
                onComplete.run();
            }
            
            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading users: " + error);
                onComplete.run();
            }
        });
    }
    
    private void showEmptyState(String message) {
        adapter.setNotifications(new ArrayList<>(), new HashMap<>(), new HashMap<>());
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText(message);
    }
}

