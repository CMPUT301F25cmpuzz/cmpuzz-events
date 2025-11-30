package com.example.cmpuzz_events.ui.home;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.widget.SearchView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.databinding.FragmentHomeBinding;
import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;
import com.example.cmpuzz_events.ui.event.Event;
import com.example.cmpuzz_events.utils.QRCodeGenerator;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment {

    private static final String TAG = "HomeFragment";
    private FragmentHomeBinding binding;
    private EventService eventService;
    private MyEventsAdapter adapter;

    // Lists to hold all events for filtering
    private List<Event> allEvents = new ArrayList<>();

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        eventService = EventService.getInstance();
        setupRecyclerView(root);
        setupSearchView();
        setupAvailabilityFilter();
        User currentUser = AuthManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.canManageEvents()) {
            binding.tvMyEventsTitle.setText("My Events");
            loadMyEvents();
        } else {
            binding.tvMyEventsTitle.setText("All Events");
            loadAllEvents();
        }

        return root;
    }

    private void setupAvailabilityFilter() {
        binding.availabilityFilterGroup.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                // when the radio buttons change, apply the new filters onto the allEvents list
                applyFilters();
            }
        });
    }

    private void setupRecyclerView(View root) {
        RecyclerView recyclerView = binding.recyclerViewMyEvents;
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        User currentUser = AuthManager.getInstance().getCurrentUser();
        boolean isOrganizer = (currentUser != null && currentUser.canManageEvents());
        adapter = new MyEventsAdapter(new ArrayList<>(), isOrganizer);
        adapter.setOnEventClickListener(new MyEventsAdapter.OnEventClickListener() {
            @Override
            public void onViewEventClick(Event event) {
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getEventId());
                Navigation.findNavController(root).navigate(
                        R.id.action_to_event_details,
                        bundle
                );
            }

            @Override
            public void onDrawAttendeesClick(Event event) {
                drawAttendeesForEvent(event);
            }

            @Override
            public void onOverflowClick(Event event, View anchorView) {
                showEventOptionsBottomSheet(event);
            }
        });

        recyclerView.setAdapter(adapter);
    }

    // search view added to allow user to filter events by title or description
    private void setupSearchView() {
        binding.eventSearchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                applyFilters();
                return false;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                applyFilters();
                return true;
            }
        });
    }


    /**
     * Filters the main event list based on the search query and availability radio buttons.
     */
    private void applyFilters() {
        // Null safety check - binding can be null if view is being destroyed
        if (binding == null) {
            return;
        }

        // Get the query and selected availability from the UI
        String query = binding.eventSearchView.getQuery().toString();
        int selectedAvailabilityId = binding.availabilityFilterGroup.getCheckedRadioButtonId();

        List<Event> filteredEvents = new ArrayList<>();
        String lowerCaseQuery = (query == null) ? "" : query.toLowerCase();

        for (Event event : allEvents) {
            // Filter by availability
            boolean availabilityMatch = false;
            int currentEntrantCount = (event.getEntrants() != null) ? event.getEntrants().size() : 0;
            int capacity = event.getCapacity();

            if (selectedAvailabilityId == R.id.radio_not_full) {
                availabilityMatch = (capacity == 0 || currentEntrantCount < capacity);
            } else if (selectedAvailabilityId == R.id.radio_full) {
                availabilityMatch = (capacity > 0 && currentEntrantCount >= capacity);
            } else {
                // "Any" case
                availabilityMatch = true;
            }

            // Filter by search query
            if (availabilityMatch) {
                if (lowerCaseQuery.isEmpty() ||
                        event.getTitle().toLowerCase().contains(lowerCaseQuery) ||
                        event.getDescription().toLowerCase().contains(lowerCaseQuery)) {
                    filteredEvents.add(event);
                }
            }
        }

        adapter.updateEvents(filteredEvents);
    }

    private void drawAttendeesForEvent(Event event) {
        Log.d(TAG, "Drawing attendees for event: " + event.getTitle());

        eventService.drawAttendees(event.getEventId(), null, new IEventService.VoidCallback() {
            @Override
            public void onSuccess() {
                Toast.makeText(getContext(),
                        "Successfully drew attendees for " + event.getTitle(),
                        Toast.LENGTH_SHORT).show();
                Log.d(TAG, "Attendees drawn successfully");
                loadMyEvents();
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(),
                        "Error drawing attendees: " + error,
                        Toast.LENGTH_LONG).show();
                Log.e(TAG, "Error drawing attendees: " + error);
            }
        });
    }

    /**
     * Fetches events organized by the current user and updates the UI.
     * <p>
     * This method first verifies that a user is logged in via {@link AuthManager} and that the
     * user has permissions to manage events (i.e., is an organizer). If these checks fail,
     * it returns early without fetching data.
     * <p>
     * If the checks pass, it calls the {@link EventService} to retrieve the events for the
     * current organizer's user ID.
     * <p>
     * On success, it clears the existing event lists, converts the retrieved {@link EventEntity}
     * objects into UI-specific {@link Event} models, and calls {@link #applyFilters()} to display
     * the data. The UI is updated to show the event list or an empty state view.
     * <p>
     * On error, it logs the issue, shows a toast notification, and updates the UI to display an
     * error message.
     *
     * @see AuthManager#getCurrentUser()
     * @see IEventService#getEventsForOrganizer(String, IEventService.EventListCallback)
     * @see #applyFilters()
     */

    private void loadMyEvents() {
        User currentUser = AuthManager.getInstance().getCurrentUser();

        if (currentUser == null) {
            Toast.makeText(getContext(), "Please log in", Toast.LENGTH_SHORT).show();
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            binding.recyclerViewMyEvents.setVisibility(View.GONE);
            return;
        }

        if (!currentUser.canManageEvents()) {
            Log.w(TAG, "User is not an organizer, skipping event load");
            binding.recyclerViewMyEvents.setVisibility(View.GONE);
            binding.tvEmptyState.setText("You do not have any events to manage.");
            binding.tvEmptyState.setVisibility(View.VISIBLE);
            return;
        }

        eventService.getEventsForOrganizer(currentUser.getUid(), new IEventService.EventListCallback() {
            @Override
            public void onSuccess(List<EventEntity> entities) {
                Log.d("HomeFragment", "Loaded " + entities.size() + " events");
                if (binding == null) {
                    Log.w(TAG, "HomeFragment view was destroyed. Ignoring event list response.");
                    return;
                }

                allEvents.clear();

                // Convert entities to UI Events
                for (EventEntity entity : entities) {
                    Event uiEvent = new Event(
                            entity.getEventId(),
                            entity.getTitle(),
                            entity.getDescription(),
                            entity.getCapacity(),
                            entity.getRegistrationStart(),
                            entity.getRegistrationEnd(),
                            entity.getOrganizerId(),
                            entity.getOrganizerName(),
                            entity.isGeolocationRequired(),
                            entity.getWaitlist()
                    );
                    uiEvent.setMaxEntrants(entity.getMaxEntrants());
                    uiEvent.setEntrants(entity.getEntrants());
                    uiEvent.setPosterUrl(entity.getPosterUrl());
                    allEvents.add(uiEvent);
                }

                applyFilters();

                if (allEvents.isEmpty()) {
                    binding.recyclerViewMyEvents.setVisibility(View.GONE);
                    binding.tvEmptyState.setText("You have not created any events.");
                    binding.tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    binding.recyclerViewMyEvents.setVisibility(View.VISIBLE);
                    binding.tvEmptyState.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                if (binding == null) {
                    Log.w(TAG, "HomeFragment view was destroyed. Ignoring error response.");
                    return;
                }

                Log.e("HomeFragment", "Error loading events: " + error);
                Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show();
                binding.tvEmptyState.setText("Could not load your events.");
                binding.tvEmptyState.setVisibility(View.VISIBLE);
                binding.recyclerViewMyEvents.setVisibility(View.GONE);
            }
        });
    }


    /**
     * Fetches all public events using proper UI layer (Event model).
     */
    private void loadAllEvents() {
        eventService.getAllEvents(new IEventService.UIEventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                if (binding == null) {
                    Log.w(TAG, "HomeFragment view was destroyed. Ignoring event list response.");
                    return;
                }

                Log.d(TAG, "Successfully loaded " + events.size() + " public events.");
                allEvents.clear();
                allEvents.addAll(events);

                applyFilters();

                if (adapter.getItemCount() == 0) {
                    binding.recyclerViewMyEvents.setVisibility(View.GONE);
                    binding.tvEmptyState.setText("No events available right now.");
                    binding.tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    binding.recyclerViewMyEvents.setVisibility(View.VISIBLE);
                    binding.tvEmptyState.setVisibility(View.GONE);
                }

                binding.eventSearchView.setVisibility(View.VISIBLE);
                binding.availabilityFilterGroup.setVisibility(View.VISIBLE);
            }

            @Override
            public void onError(String error) {
                if (binding == null) {
                    Log.w(TAG, "HomeFragment view was destroyed. Ignoring error response.");
                    return;
                }

                Log.e(TAG, "Error loading all public events: " + error);
                Toast.makeText(getContext(), "Error loading events: " + error, Toast.LENGTH_SHORT).show();

                binding.tvEmptyState.setText("Could not load events.");
                binding.tvEmptyState.setVisibility(View.VISIBLE);
                binding.recyclerViewMyEvents.setVisibility(View.GONE);

                binding.eventSearchView.setVisibility(View.GONE);
                binding.availabilityFilterGroup.setVisibility(View.GONE);
            }
        });
    }



    /**
     * Show bottom sheet with event options (Share and QR Code)
     */
    private void showEventOptionsBottomSheet(Event event) {
        BottomSheetDialog bottomSheet = new BottomSheetDialog(requireContext());
        View sheetView = getLayoutInflater().inflate(R.layout.bottom_sheet_event_options, null);
        bottomSheet.setContentView(sheetView);

        // Handle Share option
        View shareOption = sheetView.findViewById(R.id.layoutShareOption);
        shareOption.setOnClickListener(v -> {
            bottomSheet.dismiss();
            shareEvent(event);
        });

        // Handle QR Code option
        View qrCodeOption = sheetView.findViewById(R.id.layoutQrCodeOption);
        qrCodeOption.setOnClickListener(v -> {
            bottomSheet.dismiss();
            showQRCodeDialog(event);
        });

        bottomSheet.show();
    }

    /**
     * Share event using Android's share intent with deep link
     */
    private void shareEvent(Event event) {
        // Create deep link URL
        String deepLink = "cmpuzzevents://event/" + event.getEventId();

        String shareText = "Check out this event: " + event.getTitle() + "\n\n" +
                event.getDescription() + "\n\n" +
                "Tap to view details and enroll:\n" + deepLink;

        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        shareIntent.setType("text/plain");
        shareIntent.putExtra(Intent.EXTRA_SUBJECT, "Event: " + event.getTitle());
        shareIntent.putExtra(Intent.EXTRA_TEXT, shareText);

        startActivity(Intent.createChooser(shareIntent, "Share Event"));
    }

    /**
     * Show QR code dialog for the event
     */
    private void showQRCodeDialog(Event event) {
        Dialog dialog = new Dialog(requireContext());
        View dialogView = getLayoutInflater().inflate(R.layout.dialog_qr_code, null);
        dialog.setContentView(dialogView);
        dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);

        TextView tvEventTitle = dialogView.findViewById(R.id.tvEventTitle);
        ImageView imgQrCode = dialogView.findViewById(R.id.imgQrCode);
        View btnClose = dialogView.findViewById(R.id.btnClose);

        tvEventTitle.setText(event.getTitle());

        // Generate QR code
        // First, get the event entity to access the QR code URL
        eventService.getEvent(event.getEventId(), new IEventService.EventCallback() {
            @Override
            public void onSuccess(EventEntity eventEntity) {
                if (eventEntity != null && eventEntity.getQrCodeUrl() != null) {
                    String qrCodeUrl = eventEntity.getQrCodeUrl();
                    Bitmap qrBitmap = QRCodeGenerator.generateQRCode(qrCodeUrl, 512, 512);

                    if (qrBitmap != null) {
                        imgQrCode.setImageBitmap(qrBitmap);
                    } else {
                        Toast.makeText(getContext(), "Failed to generate QR code", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(getContext(), "QR code URL not available", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Error loading event details: " + error, Toast.LENGTH_SHORT).show();
            }
        });

        btnClose.setOnClickListener(v -> dialog.dismiss());

        dialog.show();
    }

    @Override
    public void onResume() {
        super.onResume();
        loadMyEvents();   // refresh events (and posterUrl) whenever you come back to Home
    }


    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
