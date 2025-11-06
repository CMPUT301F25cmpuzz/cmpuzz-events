package com.example.cmpuzz_events.ui.home;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.ui.event.Event;

public class EventViewHolder extends RecyclerView.ViewHolder {
    private final TextView tvEventName;
    private final TextView tvDescription;
    private final TextView tvMaxAttendees;
    private final Button btnView;

    public EventViewHolder(@NonNull View itemView) {
        super(itemView);
        tvEventName = itemView.findViewById(R.id.tvEventName);
        tvDescription = itemView.findViewById(R.id.tvDesc);
        tvMaxAttendees = itemView.findViewById(R.id.tvMaxAttendees);
        btnView = itemView.findViewById(R.id.btnView);
    }

    public void bind(Event event, MyEventsAdapter.OnEventClickListener listener) {
        tvEventName.setText(event.getTitle());
        tvDescription.setText(event.getDescription());
        tvMaxAttendees.setText("Maximum Attendees: " + event.getCapacity());
        
        // Set click listener for View Event button
        if (listener != null) {
            btnView.setOnClickListener(v -> listener.onViewEventClick(event));
        }
    }
}
