package com.example.cmpuzz_events.ui.profile;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.ui.event.Event;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class EnrolledEventsAdapter extends RecyclerView.Adapter<EnrolledEventsAdapter.EnrolledEventViewHolder> {

    private List<EventWithStatus> eventsWithStatus;
    private OnEventActionListener listener;

    public static class EventWithStatus {
        public Event event;
        public String status; // "waitlist", "invited", "attending", "declined"
        
        public EventWithStatus(Event event, String status) {
            this.event = event;
            this.status = status;
        }
    }

    public interface OnEventActionListener {
        void onLeaveWaitlist(Event event);
        void onAcceptInvitation(Event event);
        void onDeclineInvitation(Event event);
        void onViewEvent(Event event);
    }

    public EnrolledEventsAdapter(List<EventWithStatus> eventsWithStatus) {
        this.eventsWithStatus = eventsWithStatus;
    }

    public void setOnEventActionListener(OnEventActionListener listener) {
        this.listener = listener;
    }

    public void updateEvents(List<EventWithStatus> newEventsWithStatus) {
        this.eventsWithStatus = newEventsWithStatus;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EnrolledEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_enrolled_event, parent, false);
        return new EnrolledEventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EnrolledEventViewHolder holder, int position) {
        holder.bind(eventsWithStatus.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return eventsWithStatus != null ? eventsWithStatus.size() : 0;
    }

    static class EnrolledEventViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvEventTitle;
        private final TextView tvEventDescription;
        private final TextView tvEventTime;
        private final TextView tvStatusBadge;
        private final Button btnActionPrimary;
        private final Button btnActionSecondary;
        private final View cardView;

        public EnrolledEventViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView;
            tvEventTitle = itemView.findViewById(R.id.tvEnrolledEventTitle);
            tvEventDescription = itemView.findViewById(R.id.tvEnrolledEventDescription);
            tvEventTime = itemView.findViewById(R.id.tvEnrolledEventTime);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            btnActionPrimary = itemView.findViewById(R.id.btnActionPrimary);
            btnActionSecondary = itemView.findViewById(R.id.btnActionSecondary);
        }

        public void bind(EventWithStatus eventWithStatus, OnEventActionListener listener) {
            Event event = eventWithStatus.event;
            String status = eventWithStatus.status;
            
            tvEventTitle.setText(event.getTitle());
            tvEventDescription.setText(event.getDescription());
            
            // Format time
            if (event.getRegistrationStart() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
                tvEventTime.setText(sdf.format(event.getRegistrationStart()));
            }

            // Configure UI based on status
            switch (status) {
                case "waitlist":
                    tvStatusBadge.setText("Waitlist");
                    tvStatusBadge.setBackgroundColor(Color.parseColor("#FF9800")); // Orange
                    btnActionPrimary.setText("Leave");
                    btnActionPrimary.setVisibility(View.VISIBLE);
                    btnActionSecondary.setVisibility(View.GONE);
                    btnActionPrimary.setOnClickListener(v -> {
                        if (listener != null) listener.onLeaveWaitlist(event);
                    });
                    break;
                    
                case "invited":
                    tvStatusBadge.setText("Invited");
                    tvStatusBadge.setBackgroundColor(Color.parseColor("#2196F3")); // Blue
                    btnActionPrimary.setText("Decline");
                    btnActionPrimary.setVisibility(View.VISIBLE);
                    btnActionSecondary.setText("Accept");
                    btnActionSecondary.setVisibility(View.VISIBLE);
                    btnActionPrimary.setOnClickListener(v -> {
                        if (listener != null) listener.onDeclineInvitation(event);
                    });
                    btnActionSecondary.setOnClickListener(v -> {
                        if (listener != null) listener.onAcceptInvitation(event);
                    });
                    break;
                    
                case "attending":
                    tvStatusBadge.setText("Attending");
                    tvStatusBadge.setBackgroundColor(Color.parseColor("#4CAF50")); // Green
                    btnActionPrimary.setVisibility(View.GONE);
                    btnActionSecondary.setVisibility(View.GONE);
                    break;
                    
                case "declined":
                    tvStatusBadge.setText("Declined");
                    tvStatusBadge.setBackgroundColor(Color.parseColor("#9E9E9E")); // Gray
                    btnActionPrimary.setVisibility(View.GONE);
                    btnActionSecondary.setVisibility(View.GONE);
                    break;
            }

            // Card click to view details
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewEvent(event);
                }
            });
        }
    }
}
