package com.example.cmpuzz_events.ui.home;

import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.content.Context;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

import com.example.cmpuzz_events.Entrant;
import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.ui.event.Event;

public class EventViewHolder extends RecyclerView.ViewHolder {
    private final TextView tvEventName;
    private final TextView tvDescription;
    private final TextView tvMaxAttendees;
    private final Button btnView;
    private final Button btnDraw;
    private final TextView tvWaitlistCount;

    public EventViewHolder(@NonNull View itemView) {
        super(itemView);
        tvEventName = itemView.findViewById(R.id.tvEventName);
        tvDescription = itemView.findViewById(R.id.tvDesc);
        tvMaxAttendees = itemView.findViewById(R.id.tvMaxAttendees);
        btnView = itemView.findViewById(R.id.btnView);
        btnDraw = itemView.findViewById(R.id.btnDraw);
        tvWaitlistCount = itemView.findViewById(R.id.tvWaitlistCount);
    }

    public void bind(Event event, MyEventsAdapter.OnEventClickListener listener, boolean isOrganizerView) {
        tvEventName.setText(event.getTitle());
        tvDescription.setText(event.getDescription());
        tvMaxAttendees.setText("Maximum Attendees: " + event.getCapacity());

        // Ability for user to see waitlist count
        Context context = itemView.getContext();
        List<String> waitlist = event.getWaitingList();
        int waitlistCount = (waitlist != null) ? waitlist.size() : 0;
        if (waitlistCount >= 0) {
            tvWaitlistCount.setVisibility(View.VISIBLE);
            tvWaitlistCount.setText(context.getString(R.string.waitlist_count_format, waitlistCount));
        } else {
            tvWaitlistCount.setVisibility(View.GONE);
        }
        
        // Show/hide Draw button based on role
        if (isOrganizerView) {
            btnDraw.setVisibility(View.VISIBLE);
        } else {
            btnDraw.setVisibility(View.GONE);
        }
        
        // Set click listeners
        if (listener != null) {
            btnView.setOnClickListener(v -> listener.onViewEventClick(event));
            btnDraw.setOnClickListener(v -> listener.onDrawAttendeesClick(event));
        }
    }
}
