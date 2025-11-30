package com.example.cmpuzz_events.ui.event;

import android.os.Bundle;
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
import com.example.cmpuzz_events.service.EventService;
import com.example.cmpuzz_events.service.IEventService;
import com.example.cmpuzz_events.service.ProfileService;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.List;
import java.util.Map;
/**
 * A fragment that displays a Google Map with markers for the locations of event entrants.
 */
public class EventMapFragment extends Fragment implements OnMapReadyCallback {

    private String eventId;
    private GoogleMap googleMap;
    private ProfileService profileService;

    /**
     * Retrieves the event ID from fragment arguments upon creation.
     */
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        profileService = new ProfileService();
        if (getArguments() != null) {
            eventId = getArguments().getString("eventId");
        }
    }
    /**
     * Inflates the layout, sets up the back button, and initializes the map fragment.
     * @return The root view for the fragment's UI.
     */
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_event_map, container, false);

        // Handle Back Button
        root.findViewById(R.id.btn_back).setOnClickListener(v -> Navigation.findNavController(v).navigateUp());

        // Initialize Map
        SupportMapFragment mapFragment = (SupportMapFragment) getChildFragmentManager().findFragmentById(R.id.fragmentEventMap);
        if (mapFragment != null) {
            mapFragment.getMapAsync(this);
        }
        return root;
    }
    /**
     * Callback triggered when the map is ready to be used.
     */
    @Override
    public void onMapReady(@NonNull GoogleMap map) {
        this.googleMap = map;
        loadEntrantLocations();
    }
    /**
     * Fetches the event details to get entrant locations and places markers on the map.
     */
    private void loadEntrantLocations() {
        if (eventId == null) return;

        EventService.getInstance().getEvent(eventId, new IEventService.EventCallback() {
            @Override
            public void onSuccess(EventEntity event) {
                Map<String, List<Double>> locations = event.getEntrantLocations();

                if (locations == null || locations.isEmpty()) {
                    Toast.makeText(getContext(), "No entrant locations found.", Toast.LENGTH_SHORT).show();
                    return;
                }

                LatLng lastLocation = null;

                for (Map.Entry<String, List<Double>> entry : locations.entrySet()) {
                    List<Double> coords = entry.getValue();
                    if (coords != null && coords.size() >= 2) {
                        Double lat = coords.get(0);
                        Double lon = coords.get(1);
                        String userId = entry.getKey();
                        LatLng position = new LatLng(lat, lon);

                        profileService.getDisplayNameById(userId).addOnSuccessListener(displayName -> {
                            String markerTitle = displayName != null ? displayName : "Entrant ID: " + userId;
                            googleMap.addMarker(new MarkerOptions()
                                    .position(position)
                                    .title(markerTitle));
                        });
                        lastLocation = position;
                    }
                }

                // Move camera to the last added marker
                if (lastLocation != null) {
                    googleMap.moveCamera(CameraUpdateFactory.newLatLngZoom(lastLocation, 10));
                }
            }

            @Override
            public void onError(String error) {
                Toast.makeText(getContext(), "Error loading map data: " + error, Toast.LENGTH_SHORT).show();
            }
        });
    }
}