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
import java.util.List;
import java.util.Locale;

/**
 * A {@link RecyclerView.Adapter} that binds a list of {@link EventEntity} objects
 * to a RecyclerView, displaying the event history.
 * <p>
 * Each item in the list is rendered using the `item_history_event.xml` layout.
 */
public class EventHistoryAdapter extends RecyclerView.Adapter<EventHistoryAdapter.EventHistoryViewHolder> {

    /**
     * The list of past events to be displayed by the adapter.
     */
    private final List<EventEntity> pastEvents;

    /**
     * A formatter to display dates in a user-friendly format (e.g., "Jan 1, 2024").
     */
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());

    /**
     * Constructs an EventHistoryAdapter.
     *
     * @param pastEvents An initial list of events to display. Can be empty.
     */
    public EventHistoryAdapter(List<EventEntity> pastEvents) {
        this.pastEvents = pastEvents;
    }

    /**
     * Called when RecyclerView needs a new {@link EventHistoryViewHolder} of the given type to represent
     * an item.
     *
     * @param parent   The ViewGroup into which the new View will be added after it is bound to
     *                 an adapter position.
     * @param viewType The view type of the new View.
     * @return A new EventHistoryViewHolder that holds a View of the given view type.
     */
    @NonNull
    @Override
    public EventHistoryViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_history_event, parent, false);
        return new EventHistoryViewHolder(view);
    }

    /**
     * Called by RecyclerView to display the data at the specified position.
     * This method updates the contents of the {@link EventHistoryViewHolder#itemView} to reflect the item at the given
     * position.
     *
     * @param holder   The EventHistoryViewHolder which should be updated to represent the contents of the
     *                 item at the given position in the data set.
     * @param position The position of the item within the adapter's data set.
     */
    @Override
    public void onBindViewHolder(@NonNull EventHistoryViewHolder holder, int position) {
        EventEntity event = pastEvents.get(position);
        holder.bind(event);
    }

    /**
     * Returns the total number of items in the data set held by the adapter.
     *
     * @return The total number of items in this adapter.
     */
    @Override
    public int getItemCount() {
        return pastEvents.size();
    }

    /**
     * Updates the list of events displayed by the adapter and notifies the RecyclerView
     * to refresh its view.
     *
     * @param newEvents The new list of {@link EventEntity} objects to display.
     */
    public void updateEvents(List<EventEntity> newEvents) {
        this.pastEvents.clear();
        this.pastEvents.addAll(newEvents);
        notifyDataSetChanged();
    }

    /**
     * A {@link RecyclerView.ViewHolder} that describes an item view and metadata about its place
     * within the RecyclerView. It holds the UI components for a single history item.
     */
    class EventHistoryViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvEventName;
        private final TextView tvEventDate;
        private final TextView tvAttendeeCount;
        private final TextView tvDesc;

        /**
         * Constructs a new EventHistoryViewHolder.
         *
         * @param itemView The view for a single list item, inflated from `item_history_event.xml`.
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
         *
         * @param event The event data to display.
         */
        public void bind(EventEntity event) {
            Context context = itemView.getContext();

            tvEventName.setText(event.getTitle());
            tvDesc.setText(event.getDescription());

            if (event.getRegistrationEnd() != null) {
                String formattedDate = dateFormat.format(event.getRegistrationEnd());
                tvEventDate.setText(formattedDate);
            } else {
                tvEventDate.setText(R.string.date_not_available);
            }

            int currentEntrants = (event.getEntrants() != null) ? event.getEntrants().size() : 0;
            int capacity = event.getCapacity();
            String attendeeText;
            if (capacity > 0) {
                attendeeText = context.getString(R.string.attendee_count_with_capacity, currentEntrants, capacity);
            } else {
                attendeeText = context.getString(R.string.attendee_count_no_capacity, currentEntrants);
            }
            tvAttendeeCount.setText(attendeeText);
        }
    }
}
