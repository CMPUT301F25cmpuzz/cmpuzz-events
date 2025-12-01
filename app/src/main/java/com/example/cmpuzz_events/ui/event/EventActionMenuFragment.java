package com.example.cmpuzz_events.ui.event;

import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.models.event.EventEntity;
import com.example.cmpuzz_events.models.notification.Notification;
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;
import com.example.cmpuzz_events.service.INotificationService;
import com.example.cmpuzz_events.service.NotificationService;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.card.MaterialCardView;
import java.util.concurrent.atomic.AtomicBoolean;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.List;

public class EventActionMenuFragment extends Fragment {

    private static final String TAG = "EventActionMenuFragment";
    private static final String ARG_EVENT = "event";
    private Event event;
    private NotificationService notificationService;
    private EventService eventService;

    public static EventActionMenuFragment newInstance(Event event) {
        EventActionMenuFragment fragment = new EventActionMenuFragment();
        Bundle args = new Bundle();
        args.putSerializable(ARG_EVENT, event);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            event = (Event) getArguments().getSerializable(ARG_EVENT);
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_event_action_menu, container, false);

        notificationService = NotificationService.getInstance();
        notificationService.setContext(requireContext().getApplicationContext());
        eventService = EventService.getInstance();

        // Setup toolbar
        MaterialToolbar toolbar = root.findViewById(R.id.toolbar);
        toolbar.setNavigationOnClickListener(v -> {
            if (getActivity() != null) {
                getActivity().onBackPressed();
            }
        });

        // Setup click listeners for all menu items
        setupClickListeners(root);

        return root;
    }

    private void setupClickListeners(View root) {
        // Now you have access to the event object!
        String eventTitle = event != null ? event.getTitle() : "Unknown";

        // Entrants section
        root.findViewById(R.id.cardCancelEntrants).setOnClickListener(v -> {
            if (event != null) {
                cancelPendingInvitations(event.getEventId());
            }
        });


        root.findViewById(R.id.cardViewDeclinedEntrants).setOnClickListener(v -> {
            if (event != null) {
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getEventId());
                Navigation.findNavController(root).navigate(
                    R.id.action_to_view_entrants,
                    bundle
                );
            }
        });

        root.findViewById(R.id.cardViewWaitlist).setOnClickListener(v -> {
            if (event != null) {
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getEventId());
                Navigation.findNavController(root).navigate(
                    R.id.action_to_view_entrants,
                    bundle
                );
            }
        });

        root.findViewById(R.id.cardViewInvitedEntrants).setOnClickListener(v -> {
            if (event != null) {
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getEventId());
                Navigation.findNavController(root).navigate(
                    R.id.action_to_view_entrants,
                    bundle
                );
            }
        });

        root.findViewById(R.id.cardViewAttendees).setOnClickListener(v -> {
            if (event != null) {
                Bundle bundle = new Bundle();
                bundle.putString("eventId", event.getEventId());
                Navigation.findNavController(root).navigate(
                    R.id.action_to_view_entrants,
                    bundle
                );
            }
        });

        root.findViewById(R.id.cardExportEnrolledEntrants).setOnClickListener(v -> {exportEnrolledEntrants();});


        // Notifications section
        root.findViewById(R.id.cardNotifyDeclined).setOnClickListener(v -> 
                sendNotificationsToGroup("declined"));

        root.findViewById(R.id.cardNotifyWaitlist).setOnClickListener(v -> 
                sendNotificationsToGroup("waitlist"));

        root.findViewById(R.id.cardNotifyInvited).setOnClickListener(v -> 
                sendNotificationsToGroup("invited"));

        root.findViewById(R.id.cardNotifyAttendees).setOnClickListener(v -> 
                sendNotificationsToGroup("attendees"));
    }

    private void sendNotificationsToGroup(String group) {
        if (event == null) {
            showToast("Error: Event not found");
            return;
        }

        // Get full event entity with user lists
        eventService.getEvent(event.getEventId(), new IEventService.EventCallback() {
            @Override
            public void onSuccess(EventEntity eventEntity) {
                final List<String> userIds;
                final Notification.NotificationType notificationType;

                switch (group) {
                    case "waitlist":
                        userIds = eventEntity.getWaitlist() != null ? 
                                new ArrayList<>(eventEntity.getWaitlist()) : new ArrayList<>();
                        notificationType = Notification.NotificationType.WAITLISTED;
                        break;
                    case "invited":
                        userIds = new ArrayList<>();
                        if (eventEntity.getInvitations() != null) {
                            for (com.example.cmpuzz_events.models.event.Invitation inv : eventEntity.getInvitations()) {
                                if (inv.getUserId() != null && inv.isPending()) {
                                    userIds.add(inv.getUserId());
                                }
                            }
                        }
                        notificationType = Notification.NotificationType.INVITED;
                        break;
                    case "attendees":
                        userIds = eventEntity.getAttendees() != null ? 
                                new ArrayList<>(eventEntity.getAttendees()) : new ArrayList<>();
                        notificationType = Notification.NotificationType.CONFIRMED;
                        break;
                    case "declined":
                        userIds = eventEntity.getDeclined() != null ? 
                                new ArrayList<>(eventEntity.getDeclined()) : new ArrayList<>();
                        notificationType = Notification.NotificationType.DECLINED;
                        break;
                    default:
                        showToast("Invalid group");
                        return;
                }

                if (userIds.isEmpty()) {
                    showToast("No users in " + group + " group");
                    return;
                }

                // Send notifications
                notificationService.sendNotificationsToUsers(
                    userIds,
                    event.getEventId(),
                    event.getTitle(),
                    notificationType,
                    new INotificationService.VoidCallback() {
                        @Override
                        public void onSuccess() {
                            showToast("Notifications sent to " + group + " (" + userIds.size() + " users)");
                        }

                        @Override
                        public void onError(String error) {
                            showToast("Error sending notifications: " + error);
                            Log.e(TAG, "Error: " + error);
                        }
                    }
                );
            }

            @Override
            public void onError(String error) {
                showToast("Error loading event details");
                Log.e(TAG, "Error: " + error);
            }
        });
    }

    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Exports a list of enrolled entrants into a CSV file into the public Downloads directory of the Android device.
     *
     * @param context  The application context to access the ContentResolver.
     * @param eeList   The list of enrolled entrants to be exported.
     * @param filename The desired name of the file (without the .csv extension).
     */
    public void csvExport(Context context, List<String> eeList, String filename) throws IOException {
        // used to put the file within the downloads folder of the android device
        ContentResolver resolver = context.getContentResolver();
        ContentValues contentValues = new ContentValues();
        contentValues.put(MediaStore.MediaColumns.DISPLAY_NAME, filename + ".csv");
        contentValues.put(MediaStore.MediaColumns.MIME_TYPE, "text/csv");
        contentValues.put(MediaStore.MediaColumns.RELATIVE_PATH, Environment.DIRECTORY_DOWNLOADS);
        Uri collection = MediaStore.Files.getContentUri(MediaStore.VOLUME_EXTERNAL_PRIMARY);
        Uri fileUri = resolver.insert(collection, contentValues);

        if (fileUri == null) {
            throw new IOException("Failed to create new MediaStore record.");
        }

        try (OutputStream outputStream = resolver.openOutputStream(fileUri);
             BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream))) {

            if (outputStream == null) {
                throw new IOException("Failed to open output stream for " + fileUri);
            }

            for (String entrant : eeList) {
                bufferedWriter.write(entrant);
                bufferedWriter.newLine();
            }

        } catch (IOException e) {
            throw e;
        }

    }

    /**
     * Fetches event details to get the attendee list and initiates the CSV export process.
     */
    private void exportEnrolledEntrants() {
        eventService.getEvent(event.getEventId(), new IEventService.EventCallback() {
            @Override
            public void onSuccess(EventEntity eventEntity) {
                List<String> eeList = eventEntity.getAttendees() != null ? new ArrayList<>(eventEntity.getAttendees()) : new ArrayList<>();
                if (eeList.isEmpty()) {
                    showToast("No enrolled entrants to export.");
                    return;
                }

                String filename = "enrolled_entrants_for_" + eventEntity.getTitle();

                try {
                    csvExport(requireContext(), eeList, filename);
                    showToast("Successfully exported " + eeList.size() + " confirmed entrants to CSV.");
                } catch (IOException e) {
                    showToast("Export failed.");
                    Log.e(TAG, "CSV export failed for event: " + eventEntity.getTitle(), e);
                }
            }

            @Override
            public void onError(String error) {
                showToast("Error loading data for export.");
                Log.e(TAG, "Error fetching event for export: " + error);
            }
        });
    }
    /**
     * Cancels all pending invitations for a given event.
     *
     * @param eventId The ID of the event to process.
     */
    private void cancelPendingInvitations(String eventId) {
        // Fetch the event to find users who haven't responded.
        eventService.getEvent(eventId, new IEventService.EventCallback() {
            @Override
            public void onSuccess(EventEntity eventEntity) {
                final List<String> pendingInvitees = new ArrayList<>();
                if (eventEntity.getInvitations() != null) {
                    for (com.example.cmpuzz_events.models.event.Invitation inv : eventEntity.getInvitations()) {
                        if (inv.isPending()) {
                            pendingInvitees.add(inv.getUserId());
                        }
                    }
                }

                if (pendingInvitees.isEmpty()) {
                    showToast("No pending invitations to cancel.");
                    return;
                }

                final int totalToCancel = pendingInvitees.size();
                final int[] successCount = {0};
                final AtomicBoolean hasErrorOccurred = new AtomicBoolean(false);

                // cancelling each user's invitation individually.
                for (String userId : pendingInvitees) {
                    eventService.cancelInvitation(eventId, userId, new IEventService.VoidCallback() {
                        @Override
                        public void onSuccess() {
                            successCount[0]++;
                            if (successCount[0] == totalToCancel && !hasErrorOccurred.get()) {
                                handleBulkNotificationDeletion(pendingInvitees, eventId, totalToCancel);
                            }
                        }

                        @Override
                        public void onError(String error) {
                            if (!hasErrorOccurred.getAndSet(true)) {
                                Log.e(TAG, "Error canceling an invitation: " + error);
                                showToast("An error occurred while updating the event.");
                            }
                        }
                    });
                }
            }

            @Override
            public void onError(String error) {
                showToast("Error fetching event details: " + error);
            }
        });
    }

    /**
     * Deletes the original "INVITED" notifications for the specified users.
     * bulk deletion for all users.
     * @param userIds       The users whose notifications should be deleted.
     * @param eventId       The event associated with the notifications.
     * @param totalCanceled The count of canceled invitations for the confirmation message.
     */
    private void handleBulkNotificationDeletion(List<String> userIds, String eventId, int totalCanceled) {
        notificationService.deleteNotificationsForUsers(userIds, eventId, new INotificationService.VoidCallback() {
            @Override
            public void onSuccess() {
                showToast("Successfully canceled " + totalCanceled + " pending invitation(s).");
                Log.d(TAG, "Bulk deletion of associated notifications was successful.");
            }

            @Override
            public void onError(String error) {
                showToast("Canceled " + totalCanceled + " invitation(s), but failed to clear all notifications.");
                Log.e(TAG, "Bulk deletion of notifications failed: " + error);
            }
        });
    }




}
