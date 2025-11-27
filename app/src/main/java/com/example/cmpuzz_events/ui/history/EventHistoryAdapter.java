package com.example.cmpuzz_events.ui.history;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.models.event.EventEntity;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * A {@link RecyclerView.Adapter} responsible for binding a list of {@link EventEntity} objects
 * to a {@link RecyclerView}. This adapter is specifically designed to display a history of events,
 * showing key details for each event in a list format.
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
        holder.bind(event);
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
     * Clears the current list of events and replaces it with a new list.
     * After updating the data source, it notifies the attached observers that the data set has
     * changed, causing the UI to refresh.
     *
     * @param newEvents The new list of {@link EventEntity} objects to display.
     */
    public void updateEvents(List<EventEntity> newEvents) {
        this.eventList.clear();
        this.eventList.addAll(newEvents);
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
        }

        /**
         * Binds an {@link EventEntity} object to the views in the ViewHolder.
         * <p>
         * This method populates the UI elements with the data from the provided {@code EventEntity}.
         * It sets the event name, description, and registration end date. If the registration
         * end date is null, the date view will be hidden. It also calculates and displays
         * the number of current attendees and the event's capacity.
         *
         * @param event The {@link EventEntity} object containing the event data to display.
         *              This object is expected to be non-null.
         */
        public void bind(EventEntity event) {
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
        }
    }
}
