package com.example.cmpuzz_events;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;

import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.models.user.User;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.navigation.ui.AppBarConfiguration;
import androidx.navigation.ui.NavigationUI;

import com.example.cmpuzz_events.databinding.ActivityMainBinding;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private NavController navController;
    private static final String TAG = "MainActivity";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setupNavigationForUserRole();
        
        // Handle deep links from QR codes
        handleDeepLink(getIntent());
    }
    
    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        setIntent(intent);
        handleDeepLink(intent);
    }
    
    /**
     * Handle deep links from QR codes (cmpuzzevents://event/{eventId})
     */
    private void handleDeepLink(Intent intent) {
        Uri data = intent.getData();
        if (data != null && "cmpuzzevents".equals(data.getScheme())) {
            String host = data.getHost();
            if ("event".equals(host)) {
                // Extract event ID from the path
                String eventId = data.getLastPathSegment();
                if (eventId != null && !eventId.isEmpty()) {
                    Log.d(TAG, "Deep link detected for event: " + eventId);
                    navigateToEventDetails(eventId);
                }
            }
        }
    }
    
    /**
     * Navigate to event details screen
     */
    private void navigateToEventDetails(String eventId) {
        // Wait for navigation controller to be ready
        if (navController != null) {
            Bundle bundle = new Bundle();
            bundle.putString("eventId", eventId);
            navController.navigate(R.id.action_to_event_details, bundle);
        } else {
            // If nav controller isn't ready yet, post the navigation
            binding.getRoot().post(() -> {
                navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
                Bundle bundle = new Bundle();
                bundle.putString("eventId", eventId);
                navController.navigate(R.id.action_to_event_details, bundle);
            });
        }
    }

    private void setupNavigationForUserRole() {
        User currentUser = AuthManager.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            Log.e(TAG, "No user logged in");
            // TODO: Redirect to login
            return;
        }

        BottomNavigationView navView = binding.navView;
        navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
        
        // Setup navigation based on user role
        if (currentUser.canManageEvents()) {
            // ORGANIZER or ADMIN - show organizer navigation
            Log.d(TAG, "Setting up ORGANIZER navigation");
            navView.getMenu().clear();
            navView.inflateMenu(R.menu.bottom_nav_menu_organizer);
            
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.navigation_home,R.id.navigation_history ,R.id.navigation_dashboard, R.id.navigation_notifications)
                    .build();
            
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(navView, navController);
            
        } else {
            // REGULAR USER - show user navigation  
            Log.d(TAG, "Setting up USER navigation");
            navView.getMenu().clear();
            navView.inflateMenu(R.menu.bottom_nav_menu_user);
            
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.navigation_browse,R.id.navigation_history ,R.id.navigation_profile, R.id.navigation_notifications)
                    .build();
            
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(navView, navController);
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        return navController.navigateUp() || super.onSupportNavigateUp();
    }

    @Override
    public void onBackPressed() {
        if (!navController.popBackStack()) {
            super.onBackPressed();
        }
    }
}
