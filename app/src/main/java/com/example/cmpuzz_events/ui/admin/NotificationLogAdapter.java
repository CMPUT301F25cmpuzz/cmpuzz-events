package com.example.cmpuzz_events.ui.admin;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.models.notification.Notification;
import com.example.cmpuzz_events.models.user.User;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Adapter for displaying notification log entries in the admin view.
 * Shows organizer name, recipient name, event name, notification type, and timestamp.
 * Supports starring notifications as important.
 */
public class NotificationLogAdapter extends RecyclerView.Adapter<NotificationLogAdapter.LogEntryViewHolder> {
    
    private List<Notification> notifications;
    private Map<String, EventEntity> eventCache;
    private Map<String, User> userCache;
    private Set<String> starredNotificationIds;
    private OnStarClickListener starClickListener;
    
    public interface OnStarClickListener {
        void onStarClicked(String notificationId, boolean isStarred);
    }
    
    public NotificationLogAdapter(List<Notification> notifications) {
        this.notifications = notifications;
        this.eventCache = new HashMap<>();
        this.userCache = new HashMap<>();
        this.starredNotificationIds = new java.util.HashSet<>();
    }
    
    public void setNotifications(List<Notification> notifications, 
                                  Map<String, EventEntity> eventCache,
                                  Map<String, User> userCache) {
        this.notifications = notifications;
        this.eventCache = eventCache;
        this.userCache = userCache;
        notifyDataSetChanged();
    }
    
    public void setStarredNotificationIds(Set<String> starredIds) {
        this.starredNotificationIds = starredIds != null ? starredIds : new java.util.HashSet<>();
        notifyDataSetChanged();
    }
    
    public void setOnStarClickListener(OnStarClickListener listener) {
        this.starClickListener = listener;
    }
    
    @NonNull
    @Override
    public LogEntryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification_log, parent, false);
        return new LogEntryViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull LogEntryViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        boolean isStarred = notification.getId() != null && starredNotificationIds.contains(notification.getId());
        holder.bind(notification, eventCache, userCache, isStarred, starClickListener);
    }
    
    @Override
    public int getItemCount() {
        return notifications != null ? notifications.size() : 0;
    }
    
    static class LogEntryViewHolder extends RecyclerView.ViewHolder {
        private final ImageButton btnStar;
        private final TextView tvOrganizer;
        private final TextView tvRecipient;
        private final TextView tvEventName;
        private final TextView tvNotificationType;
        private final TextView tvMessage;
        private final TextView tvTimestamp;
        
        public LogEntryViewHolder(@NonNull View itemView) {
            super(itemView);
            btnStar = itemView.findViewById(R.id.btnStar);
            tvOrganizer = itemView.findViewById(R.id.tvOrganizer);
            tvRecipient = itemView.findViewById(R.id.tvRecipient);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvNotificationType = itemView.findViewById(R.id.tvNotificationType);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
        }
        
        public void bind(Notification notification, 
                        Map<String, EventEntity> eventCache,
                        Map<String, User> userCache,
                        boolean isStarred,
                        OnStarClickListener starClickListener) {
            
            // Setup star button
            if (btnStar != null) {
                // Update star icon based on starred state
                if (isStarred) {
                    btnStar.setImageResource(android.R.drawable.btn_star_big_on);
                    btnStar.setColorFilter(android.graphics.Color.parseColor("#FFD700")); // Gold color
                } else {
                    btnStar.setImageResource(android.R.drawable.btn_star_big_off);
                    btnStar.setColorFilter(android.graphics.Color.parseColor("#808080")); // Gray color
                }
                
                // Handle star click
                btnStar.setOnClickListener(v -> {
                    if (starClickListener != null && notification.getId() != null) {
                        starClickListener.onStarClicked(notification.getId(), !isStarred);
                    }
                });
            }
            // Get organizer name from event
            String organizerName = "Deleted Organizer";
            if (notification.getEventId() != null) {
                EventEntity event = eventCache.get(notification.getEventId());
                if (event != null && event.getOrganizerName() != null) {
                    organizerName = event.getOrganizerName();
                }
            }
            tvOrganizer.setText("Organizer: " + organizerName);
            
            // Get recipient name from user cache
            String recipientName = "Deleted User";
            if (notification.getUserId() != null) {
                User user = userCache.get(notification.getUserId());
                if (user != null) {
                    recipientName = user.getDisplayName() != null && !user.getDisplayName().isEmpty()
                            ? user.getDisplayName()
                            : (user.getUsername() != null && !user.getUsername().isEmpty()
                                    ? user.getUsername()
                                    : user.getEmail());
                }
            }
            tvRecipient.setText("Recipient: " + recipientName);
            
            // Event name
            String eventName = notification.getEventName() != null 
                    ? notification.getEventName() 
                    : "Deleted Event";
            tvEventName.setText("Event: " + eventName);
            
            // Notification type
            String typeString = notification.getType() != null 
                    ? notification.getType().getValue() 
                    : "unknown";
            tvNotificationType.setText("Type: " + typeString.toUpperCase());
            
            // Message
            tvMessage.setText(notification.getMessage() != null 
                    ? notification.getMessage() 
                    : "No message");
            
            // Timestamp
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    notification.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);
            tvTimestamp.setText(timeAgo);
        }
    }
}

