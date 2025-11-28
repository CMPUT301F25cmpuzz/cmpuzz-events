package com.example.cmpuzz_events.ui.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.models.event.EventEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * A {@link RecyclerView.Adapter} responsible for binding a list of {@link EventEntity} objects
 * to a {@link RecyclerView}. This adapter is specifically designed to display a history of events
 * for which a user has registered, showing key details and the user's selection status for each event.
 *
 * @see EventHistoryViewHolder
 * @see EventEntity
 */
public class EventHistoryAdapter extends RecyclerView.Adapter<EventHistoryAdapter.EventHistoryViewHolder> {

    /**
     * The list of event entities that the adapter will display.
     */
    private final List<EventEntity> eventList;

    /**
     * The unique identifier of the current user, used to determine their selection status for each event.
     */
    private String currentUserId;

    /**
     * Constructs a new {@code EventHistoryAdapter}.
     *
     * @param eventList A non-null list of {@link EventEntity} objects to be managed and displayed by this adapter.
     */
    public EventHistoryAdapter(List<EventEntity> eventList) {
        this.eventList = eventList;
    }

    /**
     * Called when the {@link RecyclerView} needs a new {@link EventHistoryViewHolder} of the given type to represent an item.
     * <p>
     * This new ViewHolder should be constructed with a new View that can represent the items
     * of the given type. You can either create a new View manually or inflate it from an XML
     * layout file.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new {@link EventHistoryViewHolder} that holds a View for an event item.
     */
    @NonNull
    @Override
    public EventHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_event, parent, false);
        return new EventHistoryViewHolder(view);
    }

    /**
     * Called by {@link RecyclerView} to display the data at the specified position. This method
     * updates the contents of the {@link EventHistoryViewHolder#itemView} to reflect the item at the
     * given position.
     *
     * @param holder   The {@link EventHistoryViewHolder} which should be updated to represent the
     *                 contents of the item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull EventHistoryViewHolder holder, int position) {
        EventEntity event = eventList.get(position);
        holder.bind(event, currentUserId);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of events in {@link #eventList}.
     */
    @Override
    public int getItemCount() {
        return eventList.size();
    }

    /**
     * Clears the current list of events and replaces it with a new list, while also setting the current user's ID.
     * After updating the data source, it notifies the attached observers that the data set has
     * changed, causing the UI to refresh.
     *
     * @param newEvents The new list of {@link EventEntity} objects to display.
     * @param userId The unique identifier for the current user, necessary for determining selection status.
     */
    public void updateEvents(List<EventEntity> newEvents, String userId) {
        this.eventList.clear();
        this.eventList.addAll(newEvents);
        this.currentUserId = userId;
        notifyDataSetChanged();
    }

    /**
     * A {@link RecyclerView.ViewHolder} that describes an event item view and metadata about its place
     * within the {@link RecyclerView}. It holds the UI components for a single event history item.
     */
    class EventHistoryViewHolder extends RecyclerView.ViewHolder {
        /**
         * Displays the name or title of the event.
         */
        private final TextView tvEventName;
        /**
         * Displays the formatted registration end date of the event.
         */
        private final TextView tvEventDate;
        /**
         * Displays the count of attendees versus the total capacity.
         */
        private final TextView tvAttendeeCount;
        /**
         * Displays a short description of the event.
         */
        private final TextView tvDesc;
        /**
         * Displays the user's selection status for the event (e.g., "Selected", "Not Selected", "Pending Selection").
         */
        private final TextView tvSelectionStatus;

        /**
         * Constructs a new {@code EventHistoryViewHolder}.
         *
         * @param itemView The view for a single item in the RecyclerView, inflated from a layout resource.
         */
        public EventHistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvEventDate = itemView.findViewById(R.id.tvEventDate);
            tvAttendeeCount = itemView.findViewById(R.id.tvAttendeeCount);
            tvDesc = itemView.findViewById(R.id.tvDesc);
            tvSelectionStatus = itemView.findViewById(R.id.tvSelectionStatus);
        }

        /**
         * Binds an {@link EventEntity} object to the views in the ViewHolder and determines user-specific status.
         * <p>
         * This method populates the UI elements with data from the provided {@code EventEntity}.
         * It sets the event name, description, date, and attendee count. Crucially, it calculates
         * and displays the user's registration status ("Selected," "Not Selected," or "Pending Selection")
         * based on their presence in the event's attendee or waitlist and whether selections have been finalized.
         *
         * @param event The {@link EventEntity} object containing the event data to display.
         * @param userId The unique identifier of the current user, used to determine their status.
         */
        public void bind(EventEntity event, String userId) {
            Context context = itemView.getContext();

            tvEventName.setText(event.getTitle());
            tvDesc.setText(event.getDescription());

            Date registrationEndDate = event.getRegistrationEnd();

            if (registrationEndDate != null) {
                SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
                String formattedDate = dateFormat.format(registrationEndDate);

                tvEventDate.setText(formattedDate);
                tvEventDate.setVisibility(View.VISIBLE);
                itemView.findViewById(R.id.date_container).setVisibility(View.VISIBLE);
            } else {
                tvEventDate.setVisibility(View.GONE);
                itemView.findViewById(R.id.date_container).setVisibility(View.GONE);
            }

            int currentAttendees = (event.getAttendees() != null) ? event.getAttendees().size() : 0;
            int capacity = event.getCapacity();
            String attendeeText;
            if (capacity > 0) {
                attendeeText = context.getString(R.string.attendee_count_with_capacity, currentAttendees, capacity);
            } else {
                attendeeText = context.getString(R.string.attendee_count_no_capacity, currentAttendees);
            }
            tvAttendeeCount.setText(attendeeText);

            boolean isAttendee = event.getAttendees() != null && event.getAttendees().contains(userId);
            boolean isWaitlisted = event.getWaitlist() != null && event.getWaitlist().contains(userId);
            boolean selectionsFinalized = event.isSelectionsFinalized();

            tvSelectionStatus.setVisibility(View.VISIBLE);

            if (isAttendee) {
                tvSelectionStatus.setText(R.string.status_selected);
                tvSelectionStatus.setBackground(ContextCompat.getDrawable(context, R.drawable.status_background_selected));
            } else if (isWaitlisted) {
                if (selectionsFinalized) {
                    tvSelectionStatus.setText(R.string.status_not_selected);
                    tvSelectionStatus.setBackground(ContextCompat.getDrawable(context, R.drawable.status_background_not_selected));
                } else {
                    tvSelectionStatus.setText(R.string.status_pending);
                    tvSelectionStatus.setBackground(ContextCompat.getDrawable(context, R.drawable.status_background_pending));
                }
            } else {
                tvSelectionStatus.setVisibility(View.GONE);
            }
        }
    }
}
