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

/**
 * This adapter is responsible for displaying a list of events a user has enrolled in.
 * It shows each event's details and status, like "attending" or "waitlisted,"
 * and provides actions the user can take.
 */
public class EnrolledEventsAdapter extends RecyclerView.Adapter<EnrolledEventsAdapter.EnrolledEventViewHolder> {

    private List<EventWithStatus> eventsWithStatus;
    private OnEventActionListener listener;

    /**
     * A helper class that bundles an Event object with its current enrollment status.
     * This makes it easier to manage the event's state within the adapter.
     */
    public static class EventWithStatus {
        public Event event;
        public String status; // "waitlist", "invited", "attending", "declined"

        /**
         * Creates a new instance of EventWithStatus.
         *
         * @param event  The event object.
         * @param status The user's status for this event.
         */
        public EventWithStatus(Event event, String status) {
            this.event = event;
            this.status = status;
        }
    }

    /**
     * An interface for handling user actions on an event item.
     * This lets the fragment or activity respond to button clicks.
     */
    public interface OnEventActionListener {
        /**
         * Called when the user decides to leave the waitlist for an event.
         * @param event The event to leave.
         */
        void onLeaveWaitlist(Event event);
        /**
         * Called when the user accepts an invitation to an event.
         * @param event The event to accept.
         */
        void onAcceptInvitation(Event event);
        /**
         * Called when the user declines an invitation to an event.
         * @param event The event to decline.
         */
        void onDeclineInvitation(Event event);
        /**
         * Called when the user taps on an event to see more details.
         * @param event The event to view.
         */
        void onViewEvent(Event event);
    }

    /**
     * Initializes the adapter with a list of events and their statuses.
     *
     * @param eventsWithStatus The initial list of events to display.
     */
    public EnrolledEventsAdapter(List<EventWithStatus> eventsWithStatus) {
        this.eventsWithStatus = eventsWithStatus;
    }

    /**
     * Sets up the listener that will handle user actions, like button clicks.
     *
     * @param listener The listener to be notified of events.
     */
    public void setOnEventActionListener(OnEventActionListener listener) {
        this.listener = listener;
    }

    /**
     * Updates the list of events displayed by the adapter and refreshes the view.
     *
     * @param newEventsWithStatus The new list of events to show.
     */
    public void updateEvents(List<EventWithStatus> newEventsWithStatus) {
        this.eventsWithStatus = newEventsWithStatus;
        notifyDataSetChanged();
    }

    /**
     * Called when the RecyclerView needs a new ViewHolder to represent an item.
     * This is where we inflate the layout for a single event item.
     *
     * @param parent The ViewGroup into which the new View will be added.
     * @param viewType The view type of the new View.
     * @return A new EnrolledEventViewHolder that holds the View for an event item.
     */
    @NonNull
    @Override
    public EnrolledEventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_enrolled_event, parent, false);
        return new EnrolledEventViewHolder(view);
    }

    /**
     * Called by the RecyclerView to display the data at a specific position.
     * This method updates the contents of the ViewHolder to reflect the event at the given position.
     *
     * @param holder The ViewHolder which should be updated.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull EnrolledEventViewHolder holder, int position) {
        holder.bind(eventsWithStatus.get(position), listener);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of events in the list.
     */
    @Override
    public int getItemCount() {
        return eventsWithStatus != null ? eventsWithStatus.size() : 0;
    }

    /**
     * The ViewHolder class that describes an item view and its metadata within the RecyclerView.
     * Each ViewHolder instance holds the UI elements for a single enrolled event.
     */
    static class EnrolledEventViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvEventTitle;
        private final TextView tvEventDescription;
        private final TextView tvEventTime;
        private final TextView tvStatusBadge;
        private final Button btnActionPrimary;
        private final Button btnActionSecondary;
        private final View cardView;

        /**
         * Sets up the ViewHolder by finding all the necessary views in the layout.
         *
         * @param itemView The view for a single item in the list.
         */
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

        /**
         * Binds the event data to the views. It sets the text, colors, and button visibility
         * based on the event's status and wires up the click listeners.
         *
         * @param eventWithStatus The event and its status to display.
         * @param listener The listener to handle actions.
         */
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
