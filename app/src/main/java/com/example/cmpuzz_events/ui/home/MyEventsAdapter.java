package com.example.cmpuzz_events.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.ui.event.Event;

import java.util.List;

public class MyEventsAdapter extends RecyclerView.Adapter<MyEventsAdapter.EventViewHolder> {

    private List<Event> events;

    public MyEventsAdapter(List<Event> events) {
        this.events = events;
    }

    public void updateEvents(List<Event> newEvents) {
        this.events = newEvents;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public EventViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_event, parent, false);
        return new EventViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull EventViewHolder holder, int position) {
        Event event = events.get(position);
        holder.bind(event);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EventViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvEventName;
        private final TextView tvDescription;
        private final TextView tvMaxAttendees;

        public EventViewHolder(@NonNull View itemView) {
            super(itemView);
            tvEventName = itemView.findViewById(R.id.tvEventName);
            tvDescription = itemView.findViewById(R.id.tvDescription);
            tvMaxAttendees = itemView.findViewById(R.id.tvMaxAttendees);
        }

        public void bind(Event event) {
            tvEventName.setText(event.getTitle());
            tvDescription.setText(event.getDescription());
            tvMaxAttendees.setText("Maximum Attendees: " + event.getCapacity());
        }
    }
}
