package com.example.cmpuzz_events.ui.notifications;

import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.models.notification.Notification;

import java.util.ArrayList;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.NotificationViewHolder> {

    private List<Notification> notifications;
    private OnNotificationActionListener listener;

    public interface OnNotificationActionListener {
        void onAcceptInvitation(Notification notification);
        void onDeclineInvitation(Notification notification);
        void onNotificationClick(Notification notification);
    }

    public NotificationAdapter() {
        this.notifications = new ArrayList<>();
    }

    public void setNotifications(List<Notification> notifications) {
        this.notifications = notifications;
        notifyDataSetChanged();
    }

    public void setOnNotificationActionListener(OnNotificationActionListener listener) {
        this.listener = listener;
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = notifications.get(position);
        holder.bind(notification, listener);
    }

    @Override
    public int getItemCount() {
        return notifications.size();
    }

    static class NotificationViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvTitle;
        private final TextView tvEventName;
        private final TextView tvMessage;
        private final TextView tvTimestamp;
        private final View unreadIndicator;
        private final LinearLayout layoutActionButtons;
        private final Button btnAccept;
        private final Button btnDecline;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
            layoutActionButtons = itemView.findViewById(R.id.layoutActionButtons);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);
        }

        public void bind(Notification notification, OnNotificationActionListener listener) {
            tvTitle.setText(notification.getTitle());
            tvEventName.setText(notification.getEventName());
            tvMessage.setText(notification.getMessage());
            
            // Show relative time
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    notification.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);
            tvTimestamp.setText(timeAgo);
            
            // Show unread indicator
            unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
            
            // Show action buttons only for INVITED notifications
            // Hide them if notification is read (means they already responded)
            if (notification.getType() == Notification.NotificationType.INVITED && !notification.isRead()) {
                layoutActionButtons.setVisibility(View.VISIBLE);
                btnAccept.setEnabled(true);
                btnDecline.setEnabled(true);
                
                btnAccept.setOnClickListener(v -> {
                    if (listener != null) {
                        // Disable buttons immediately to prevent double-clicks
                        btnAccept.setEnabled(false);
                        btnDecline.setEnabled(false);
                        listener.onAcceptInvitation(notification);
                    }
                });
                
                btnDecline.setOnClickListener(v -> {
                    if (listener != null) {
                        // Disable buttons immediately to prevent double-clicks
                        btnAccept.setEnabled(false);
                        btnDecline.setEnabled(false);
                        listener.onDeclineInvitation(notification);
                    }
                });
            } else {
                layoutActionButtons.setVisibility(View.GONE);
            }
            
            // Click listener for the entire card
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onNotificationClick(notification);
                }
            });
        }
    }
}
