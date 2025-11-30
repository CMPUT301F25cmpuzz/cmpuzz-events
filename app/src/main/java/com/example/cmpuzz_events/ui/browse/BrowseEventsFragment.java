package com.example.cmpuzz_events.ui.browse;

import android.app.Dialog;
import android.content.Intent;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.google.android.material.bottomsheet.BottomSheetDialog;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.databinding.FragmentBrowseEventsBinding;
import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;
import com.example.cmpuzz_events.ui.event.Event;
import com.example.cmpuzz_events.ui.home.MyEventsAdapter;
import com.example.cmpuzz_events.utils.QRCodeGenerator;

import java.util.ArrayList;
import java.util.List;

/**
 * A {@link Fragment} responsible for displaying a list of all available public events
 * It fetches event data using the {@link EventService} and presents it in a
 * {@link androidx.recyclerview.widget.RecyclerView}.
 */
public class BrowseEventsFragment extends Fragment {

    private FragmentBrowseEventsBinding binding;
    private EventService eventService;
    private MyEventsAdapter adapter;
    private static final String TAG = "BrowseEventsFragment";

    /**
     * inflates the layout, initilizes viewbinding,sets up recyclerview,
     * and initiates the data loading.
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     * any views in the fragment,
     * @param container If non-null, this is the parent view that the fragment's
     * UI should be attached to.  The fragment should not add the view itself,
     * but this can be used to generate the LayoutParams of the view.
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     * from a previous saved state as given here.
     *
     * @return View
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentBrowseEventsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        eventService = EventService.getInstance();
        
        // Setup RecyclerView
        binding.recyclerViewBrowseEvents.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new MyEventsAdapter(new ArrayList<>(), false); // false = user view
        
        // Set click listener to navigate to event details
        adapter.setOnEventClickListener(new MyEventsAdapter.OnEventClickListener() {
            /**
             * handles click events on an event item.
             * @param event The {@link Event} object that was click
             */
            @Override
            public void onViewEventClick(Event event) {
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getEventId());
                Navigation.findNavController(root).navigate(
                    R.id.action_browse_to_event_details,
                    bundle
                );
            }

            /**
             * Handles clicks on the "Draw Attendees" action. since users can't do this
             * nothing happens.
             * @param event The event associated with the action.
             */
            @Override
            public void onDrawAttendeesClick(Event event) {
                // Do nothing - users can't draw attendees
            }

            @Override
            public void onOverflowClick(Event event, View anchorView) {
                showEventOptionsBottomSheet(event);
            }
        });
        
        binding.recyclerViewBrowseEvents.setAdapter(adapter);
        
        // Load all events
        loadAllEvents();
        
        return root;
    }

    /**
     * Fetches the list of all events from the {@link EventService}.
     */
    private void loadAllEvents() {
        eventService.getAllEvents(new IEventService.UIEventListCallback() {
            @Override
            public void onSuccess(List<Event> events) {
                Log.d(TAG, "Loaded " + events.size() + " events");
                adapter.updateEvents(events);
                
                // Show/hide empty state
                if (events.isEmpty()) {
                    binding.recyclerViewBrowseEvents.setVisibility(View.GONE);
                    binding.tvEmptyState.setVisibility(View.VISIBLE);
                } else {
                    binding.recyclerViewBrowseEvents.setVisibility(View.VISIBLE);
                    binding.tvEmptyState.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading events: " + error);
                Toast.makeText(getContext(), "Error loading events", Toast.LENGTH_SHORT).show();
                binding.tvEmptyState.setVisibility(View.VISIBLE);
                binding.recyclerViewBrowseEvents.setVisibility(View.GONE);
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
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
