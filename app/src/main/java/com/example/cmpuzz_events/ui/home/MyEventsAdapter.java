package com.example.cmpuzz_events.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.ui.event.Event;

import java.util.List;

public class MyEventsAdapter extends RecyclerView.Adapter<EventViewHolder> {

    private List<Event> events;
    private OnEventClickListener listener;
    private boolean isOrganizerView;

    public interface OnEventClickListener {
        void onViewEventClick(Event event);
        void onDrawAttendeesClick(Event event);
        void onOverflowClick(Event event, View anchorView);
    }

    public MyEventsAdapter(List<Event> events, boolean isOrganizerView) {
        this.events = events;
        this.isOrganizerView = isOrganizerView;
    }

    public void setOnEventClickListener(OnEventClickListener listener) {
        this.listener = listener;
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
        holder.bind(events.get(position), listener, isOrganizerView);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }
}
