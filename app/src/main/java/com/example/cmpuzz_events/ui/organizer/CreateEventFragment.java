package com.example.cmpuzz_events.ui.organizer;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.ui.event.Event;
import com.example.cmpuzz_events.databinding.CreateEventFragmentBinding;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;
import com.google.firebase.storage.FirebaseStorage;
import com.google.firebase.storage.StorageReference;
import com.google.firebase.storage.UploadTask;

import java.util.ArrayList;
import java.util.Date;
import java.util.Locale;
import java.util.UUID;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import android.net.Uri;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;

/**
 * Controller for the Organizer "Create Event" screen.
 * Connects UI inputs to EventService for Firebase storage.
 */
public class CreateEventFragment extends Fragment {

    private CreateEventFragmentBinding binding;
    private User currentUser;
    private EventService eventService;
    private AuthManager authManager;
    private Date registrationStart;
    private Date registrationEnd;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
    private Uri posterUri;
    private ActivityResultLauncher<String> pickPosterLauncher;


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        Log.d("CreateEventFragment", "Fragment loaded successfully");


        binding = CreateEventFragmentBinding.inflate(inflater, container, false);

        // Image picker for poster
        pickPosterLauncher = registerForActivityResult(
                new ActivityResultContracts.GetContent(),
                uri -> {
                    if (uri != null) {
                        posterUri = uri;
                        binding.ivEventImage.setImageURI(uri);  // preview
                    }
                }
        );


        View root = binding.getRoot();

        // Initialize services
        eventService = EventService.getInstance();
        authManager = AuthManager.getInstance();

        // Get current authenticated user
        currentUser = authManager.getCurrentUser();
        if (currentUser == null) {
            // Shouldn't happen if auth is required
            Log.w("CreateEventFragment", "No authenticated user found");
            Toast.makeText(requireContext(), "Please log in to create events", Toast.LENGTH_SHORT).show();
            return root;
        }

        // Check if user has permission to create events
        if (!currentUser.canManageEvents()) {
            Log.w("CreateEventFragment", "User does not have permission to create events");
            Toast.makeText(requireContext(), "You don't have permission to create events", Toast.LENGTH_SHORT).show();
            return root;
        }

        // Handle Finish button click
        binding.btnFinish.setOnClickListener(v -> handleCreateEvent());

        // Handle registration period picker
        binding.rowSetRegistrationPeriod.setOnClickListener(v -> {
            Log.d("CreateEventFragment", "Row clicked!");
            Toast.makeText(requireContext(), "Select registration dates", Toast.LENGTH_SHORT).show();
            showDatePicker();
        });

        // Enable/disable numeric inputs based on toggles
        binding.setAttendeesInput.setEnabled(binding.toggleSetAttendees.isChecked());
        binding.setEntrantsInput.setEnabled(binding.toggleMaxEntrants.isChecked());
        binding.setPriceInput.setEnabled(binding.togglePrice.isChecked());

        // When "Set Attendees" toggle changes
        binding.toggleSetAttendees.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.setAttendeesInput.setEnabled(isChecked);
            if (!isChecked) {
                binding.setAttendeesInput.setText("");
            }
        });

        // When "Limit Max Entrants" toggle changes
        binding.toggleMaxEntrants.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.setEntrantsInput.setEnabled(isChecked);
            if (!isChecked) {
                binding.setEntrantsInput.setText("");
            }
        });

        // When "Set Price" toggle changes
        binding.togglePrice.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.setPriceInput.setEnabled(isChecked);
            if (!isChecked) {
                binding.setPriceInput.setText("");
            }
        });

        binding.btnUploadImage.setOnClickListener(v -> {
            pickPosterLauncher.launch("image/*");   // open gallery
        });

        return root;
    }

    /**
     * Collects all user inputs and creates an Event via EventService and saves to Firebase.
     */
    private void handleCreateEvent() {
        Log.d("CreateEventFragment", "handleCreateEvent called");
        
        if (currentUser == null) {
            Log.e("CreateEventFragment", "Current user is null!");
            Toast.makeText(requireContext(), "Please log in to create events", Toast.LENGTH_SHORT).show();
            return;
        }

        Log.d("CreateEventFragment", "Current user: " + currentUser.getEmail() + ", Role: " + currentUser.getRole());

        String title = binding.etEventName.getText().toString().trim();
        String description = binding.etEventDescription.getText().toString().trim();
        boolean geoRequired = binding.toggleGeolocation.isChecked();

        // Get values from UI
        String capacityStr = binding.setAttendeesInput.getText().toString().trim();
        String maxEntrantsStr = binding.setEntrantsInput.getText().toString().trim();
        String priceStr = binding.setPriceInput.getText().toString().trim();

        // Validate required fields
        if (title.isEmpty() || description.isEmpty()) {
            Toast.makeText(requireContext(), "Please fill all fields", Toast.LENGTH_SHORT).show();
            return;
        }
        if (capacityStr.isEmpty() || maxEntrantsStr.isEmpty()) {
            Toast.makeText(requireContext(), "Please enter attendee and entrant limits", Toast.LENGTH_SHORT).show();
            return;
        }
        if (registrationStart == null || registrationEnd == null) {
            Toast.makeText(requireContext(), "Please set a registration period", Toast.LENGTH_SHORT).show();
            return;
        }

        int capacity;
        int maxEntrants;
        Double price = null;
        try {
            capacity = Integer.parseInt(capacityStr);
            maxEntrants = Integer.parseInt(maxEntrantsStr);
            if (!priceStr.isEmpty()) {
                price = Double.parseDouble(priceStr);
                if (price < 0) {
                    Toast.makeText(requireContext(), "Price cannot be negative", Toast.LENGTH_SHORT).show();
                    return;
                }
            }
        } catch (NumberFormatException e) {
            Toast.makeText(requireContext(), "Please enter valid numbers", Toast.LENGTH_SHORT).show();
            return;
        }

        if (registrationEnd.before(registrationStart)) {
            Toast.makeText(requireContext(), "End date cannot be before start date", Toast.LENGTH_SHORT).show();
            return;
        }

        // Use selected registration period
        Date start = registrationStart;
        Date end = registrationEnd;

        Log.d("CreateEventFragment",
                "Creating event: " + title + " by " + currentUser.getDisplayName() + " (" + currentUser.getUid() + ")");

        // Create UI Event object with current user's UID and name as organizer
        String eventId = UUID.randomUUID().toString();
        Event uiEvent = new Event(eventId, title, description, capacity,
                start, end, currentUser.getUid(), currentUser.getDisplayName(), geoRequired, new ArrayList<>());
        uiEvent.setMaxEntrants(maxEntrants);
        uiEvent.setPrice(price);

        // CASE 1: No poster selected -> just create event as before
        if (posterUri == null) {
            eventService.createEvent(uiEvent, new IEventService.EventCallback() {
                @Override
                public void onSuccess(com.example.cmpuzz_events.models.event.EventEntity event) {
                    Log.d("CreateEventFragment", "Event saved to Firebase (no poster): " + event.getEventId());
                    Toast.makeText(requireContext(), "Event created successfully!", Toast.LENGTH_SHORT).show();
                    clearForm();
                }

                @Override
                public void onError(String error) {
                    Log.e("CreateEventFragment", "Error saving event: " + error);
                    Toast.makeText(requireContext(), "Failed to create event: " + error, Toast.LENGTH_LONG).show();
                }
            });
            return;
        }

        // CASE 2: Poster selected -> copy to local cache, then upload
        Log.d("CreateEventFragment", "Original posterUri = " + posterUri);

        Uri uploadUri;

        try {
            // 1. Copy content:// URI into a real file in cache
            java.io.InputStream in =
                    requireContext().getContentResolver().openInputStream(posterUri);

            if (in == null) {
                Toast.makeText(requireContext(), "Could not open image data", Toast.LENGTH_LONG).show();
                Log.e("CreateEventFragment", "InputStream from posterUri is null");
                return;
            }

            java.io.File outFile = new java.io.File(
                    requireContext().getCacheDir(),
                    "event_poster_" + eventId + ".jpg"
            );
            java.io.OutputStream out = new java.io.FileOutputStream(outFile);

            byte[] buffer = new byte[8 * 1024];
            int len;
            while ((len = in.read(buffer)) != -1) {
                out.write(buffer, 0, len);
            }
            in.close();
            out.flush();
            out.close();

            uploadUri = android.net.Uri.fromFile(outFile);
            Log.d("CreateEventFragment", "Copied image to: " + uploadUri);

        } catch (Exception e) {
            Log.e("CreateEventFragment", "Failed to copy image to cache", e);
            Toast.makeText(requireContext(), "Failed to read selected image", Toast.LENGTH_LONG).show();
            return;
        }

// 2. Now upload the local file
        com.google.firebase.storage.StorageReference storageRef =
                com.google.firebase.storage.FirebaseStorage.getInstance()
                        .getReference()
                        .child("event_posters/" + eventId + ".jpg");

        com.google.firebase.storage.UploadTask uploadTask = storageRef.putFile(uploadUri);

        uploadTask
                .addOnSuccessListener(taskSnapshot -> {
                    storageRef.getDownloadUrl().addOnSuccessListener(uri -> {
                        String downloadUrl = uri.toString();
                        Log.d("CreateEventFragment", "Poster uploaded. URL = " + downloadUrl);

                        uiEvent.setPosterUrl(downloadUrl);

                        eventService.createEvent(uiEvent, new IEventService.EventCallback() {
                            @Override
                            public void onSuccess(com.example.cmpuzz_events.models.event.EventEntity event) {
                                Log.d("CreateEventFragment",
                                        "Event saved to Firebase with poster: " + event.getEventId());
                                Toast.makeText(requireContext(),
                                        "Event created successfully!", Toast.LENGTH_SHORT).show();
                                clearForm();
                            }

                            @Override
                            public void onError(String error) {
                                Log.e("CreateEventFragment", "Error saving event: " + error);
                                Toast.makeText(requireContext(),
                                        "Failed to create event: " + error, Toast.LENGTH_LONG).show();
                            }
                        });
                    }).addOnFailureListener(e -> {
                        Log.e("CreateEventFragment", "Failed to get download URL", e);
                        Toast.makeText(requireContext(),
                                "Failed to get image URL", Toast.LENGTH_LONG).show();
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e("CreateEventFragment", "Image upload failed", e);
                    Toast.makeText(requireContext(),
                            "Image upload failed: " + e.getMessage(), Toast.LENGTH_LONG).show();
                });
    }

    // helper to clean the form (optional)
    private void clearForm() {
        binding.etEventName.setText("");
        binding.etEventDescription.setText("");
        binding.toggleGeolocation.setChecked(false);
        binding.setAttendeesInput.setText("");
        binding.setEntrantsInput.setText("");
        binding.setPriceInput.setText("");
        binding.togglePrice.setChecked(false);
        binding.tvRegistrationSummary.setText(" ");
        binding.ivEventImage.setImageDrawable(null);
        posterUri = null;
    }


    private void showDatePicker() {
        Calendar calendar = Calendar.getInstance();

        // Start date picker
        DatePickerDialog startPicker = new DatePickerDialog(
                requireContext(),
                (view, year, month, dayOfMonth) -> {
                    Calendar startDate = Calendar.getInstance();
                    startDate.set(year, month, dayOfMonth, 0, 0, 0);
                    registrationStart = startDate.getTime();

                    // Now open the end date picker
                    DatePickerDialog endPicker = new DatePickerDialog(
                            requireContext(),
                            (view2, year2, month2, day2) -> {
                                Calendar endDate = Calendar.getInstance();
                                endDate.set(year2, month2, day2, 0, 0, 0);
                                registrationEnd = endDate.getTime();

                                // Update text in the summary field
                                String summary = "Start: " + dateFormat.format(registrationStart)
                                        + " → End: " + dateFormat.format(registrationEnd);
                                binding.tvRegistrationSummary.setText(summary);
                            },
                            startDate.get(Calendar.YEAR),
                            startDate.get(Calendar.MONTH),
                            startDate.get(Calendar.DAY_OF_MONTH)
                    );
                    endPicker.setTitle("Select Registration End Date");
                    endPicker.show();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );

        startPicker.setTitle("Select Registration Start Date");
        startPicker.show();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
