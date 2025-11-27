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

    @Override
    public int getItemViewType(int position) {
        Notification n = notifications.get(position);

        // Waitlist-loss notifications use special layout
        if (n.getType() == Notification.NotificationType.WAITLISTED) {
            return 1; // lost lottery
        }

        return 0; // default notification layout
    }

    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        int layout = (viewType == 1)
                ? R.layout.waitlisted_notification        // lost lottery layout
                : R.layout.item_notification;             // default layout

        View view = LayoutInflater.from(parent.getContext())
                .inflate(layout, parent, false);

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

        // Extra for waitlisted layout
        private final TextView waitlistedEventName;

        public NotificationViewHolder(@NonNull View itemView) {
            super(itemView);

            // Default layout IDs (may be null if waitlisted_notification.xml is used)
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvMessage = itemView.findViewById(R.id.tvNotificationMessage);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            unreadIndicator = itemView.findViewById(R.id.unreadIndicator);
            layoutActionButtons = itemView.findViewById(R.id.layoutActionButtons);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnDecline = itemView.findViewById(R.id.btnDecline);

            // Waitlisted lottery-loss layout IDs
            waitlistedEventName = itemView.findViewById(R.id.waitlisted_notification_event_name);
        }

        public void bind(Notification notification, OnNotificationActionListener listener) {

            // LOST LOTTERY NOTIFICATION LAYOUT
            if (waitlistedEventName != null) {
                waitlistedEventName.setText(notification.getEventName());
                return; // DON'T bind anything else for this layout
            }

            // NORMAL NOTIFICATIONS (existing behaviour)
            tvTitle.setText(notification.getTitle());
            tvEventName.setText(notification.getEventName());
            tvMessage.setText(notification.getMessage());

            // relative time
            CharSequence timeAgo = DateUtils.getRelativeTimeSpanString(
                    notification.getTimestamp(),
                    System.currentTimeMillis(),
                    DateUtils.MINUTE_IN_MILLIS);
            tvTimestamp.setText(timeAgo);

            // unread dot
            unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);

            // Accept/Decline buttons ONLY for INVITED
            if (notification.getType() == Notification.NotificationType.INVITED && !notification.isRead()) {
                layoutActionButtons.setVisibility(View.VISIBLE);

                btnAccept.setOnClickListener(v -> {
                    if (listener != null) listener.onAcceptInvitation(notification);
                });
                btnDecline.setOnClickListener(v -> {
                    if (listener != null) listener.onDeclineInvitation(notification);
                });

            } else {
                layoutActionButtons.setVisibility(View.GONE);
            }

            // Entire card click
            itemView.setOnClickListener(v -> {
                if (listener != null) listener.onNotificationClick(notification);
            });
        }
    }
}
