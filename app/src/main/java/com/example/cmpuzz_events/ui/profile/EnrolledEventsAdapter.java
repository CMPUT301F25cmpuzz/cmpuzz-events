package com.example.cmpuzz_events.ui.profile;

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

    private List<Event> events;
    private OnEventActionListener listener;

    public interface OnEventActionListener {
        void onLeaveEvent(Event event);
        void onViewEvent(Event event);
    }

    public EnrolledEventsAdapter(List<Event> events) {
        this.events = events;
    }

    public void setOnEventActionListener(OnEventActionListener listener) {
        this.listener = listener;
    }

    public void updateEvents(List<Event> newEvents) {
        this.events = newEvents;
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
        holder.bind(events.get(position), listener);
    }

    @Override
    public int getItemCount() {
        return events.size();
    }

    static class EnrolledEventViewHolder extends RecyclerView.ViewHolder {
        private final TextView tvEventTitle;
        private final TextView tvEventDescription;
        private final TextView tvEventTime;
        private final Button btnLeave;
        private final View cardView;

        public EnrolledEventViewHolder(@NonNull View itemView) {
            super(itemView);
            cardView = itemView;
            tvEventTitle = itemView.findViewById(R.id.tvEnrolledEventTitle);
            tvEventDescription = itemView.findViewById(R.id.tvEnrolledEventDescription);
            tvEventTime = itemView.findViewById(R.id.tvEnrolledEventTime);
            btnLeave = itemView.findViewById(R.id.btnLeaveEnrolled);
        }

        public void bind(Event event, OnEventActionListener listener) {
            tvEventTitle.setText(event.getTitle());
            tvEventDescription.setText(event.getDescription());
            
            // Format time
            if (event.getRegistrationStart() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy • hh:mm a", Locale.getDefault());
                tvEventTime.setText(sdf.format(event.getRegistrationStart()));
            }

            // Leave button
            btnLeave.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onLeaveEvent(event);
                }
            });

            // Card click to view details
            cardView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onViewEvent(event);
                }
            });
        }
    }
}
