package com.example.cmpuzz_events.ui.home;

import android.view.View;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.ui.event.Event;

public class EventViewHolder extends RecyclerView.ViewHolder {
    private final TextView tvEventName;
    private final TextView tvDescription;
    private final TextView tvMaxAttendees;

    public EventViewHolder(@NonNull View itemView) {
        super(itemView);
        tvEventName = itemView.findViewById(R.id.tvEventName);
        tvDescription = itemView.findViewById(R.id.tvDesc);
        tvMaxAttendees = itemView.findViewById(R.id.tvMaxAttendees);
    }

    public void bind(Event event) {
        tvEventName.setText(event.getTitle());
        tvDescription.setText(event.getDescription());
        tvMaxAttendees.setText("Maximum Attendees: " + event.getCapacity());
    }
}
