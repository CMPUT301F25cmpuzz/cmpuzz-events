package com.example.cmpuzz_events;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.auth.LoginActivity;
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
    private AuthManager.AuthStateListener authStateListener;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Check if this is a manual login (user explicitly logged in)
        boolean isManualLogin = getIntent().getBooleanExtra("is_manual_login", false);
        checkAuthenticationAndSetup(isManualLogin);
    }
    
    /**
     * Checks authentication state and sets up navigation.
     * If user is not authenticated, redirects to LoginActivity.
     * If Firebase has a session but user data isn't loaded, waits for it.
     * Only blocks auto-login for organizers/admins; manual logins are allowed for all roles.
     *
     * @param isManualLogin {@code true} if the user manually logged in, {@code false} if this is an auto-login attempt
     */
    private void checkAuthenticationAndSetup(boolean isManualLogin) {
        AuthManager authManager = AuthManager.getInstance();
        
        // If no Firebase session at all, redirect to login
        if (!authManager.isSignedIn()) {
            Log.d(TAG, "No Firebase session, redirecting to login");
            redirectToLogin();
            return;
        }
        
        // If Firebase has a session but user data isn't loaded yet, wait for it
        if (authManager.hasFirebaseSessionButNoUserData()) {
            Log.d(TAG, "Firebase session exists but user data not loaded, waiting...");
            authStateListener = user -> {
                if (user != null) {
                    // User data loaded
                    // If this is a manual login, allow all roles through
                    // If this is an auto-login, only allow entrants
                    if (isManualLogin || authManager.isAutoLoginAllowed()) {
                        setupNavigationForUserRole();
                    } else {
                        // Auto-login attempt for organizer/admin - sign them out and redirect to login
                        Log.d(TAG, "Auto-login blocked for Organizer/Admin, signing out and redirecting to login");
                        authManager.signOut();
                        redirectToLogin();
                    }
                } else {
                    // User data failed to load, redirect to login
                    Log.e(TAG, "Failed to load user data, redirecting to login");
                    redirectToLogin();
                }
            };
            authManager.addAuthStateListener(authStateListener);
            return;
        }
        
        // User is authenticated and data is loaded
        // If this is a manual login, allow all roles through
        // If this is an auto-login, only allow entrants
        if (isManualLogin || authManager.isAutoLoginAllowed()) {
            setupNavigationForUserRole();
        } else {
            // Auto-login attempt for organizer/admin - sign them out and redirect to login
            Log.d(TAG, "Auto-login blocked for Organizer/Admin, signing out and redirecting to login");
            authManager.signOut();
            redirectToLogin();
        }
    }
    
    /**
     * Redirects to LoginActivity and finishes this activity.
     */
    private void redirectToLogin() {
        Intent intent = new Intent(this, LoginActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void setupNavigationForUserRole() {
        User currentUser = AuthManager.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            Log.e(TAG, "No user logged in in setupNavigationForUserRole");
            redirectToLogin();
            return;
        }

        Log.d(TAG, "Setting up navigation for user: " + currentUser.getDisplayName() + 
              ", role: " + currentUser.getRole() + 
              ", canManageEvents: " + currentUser.canManageEvents());

        BottomNavigationView navView = binding.navView;
        
        try {
            navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
            Log.d(TAG, "NavController found successfully");
        } catch (Exception e) {
            Log.e(TAG, "Error finding NavController: " + e.getMessage(), e);
            // Try again after a short delay
            navView.postDelayed(() -> {
                try {
                    navController = Navigation.findNavController(this, R.id.nav_host_fragment_activity_main);
                    setupNavigationForUserRole();
                } catch (Exception e2) {
                    Log.e(TAG, "Error finding NavController on retry: " + e2.getMessage(), e2);
                }
            }, 100);
            return;
        }
        
        // Setup navigation based on user role
        if (currentUser.canManageEvents()) {
            // ORGANIZER or ADMIN - show organizer navigation
            Log.d(TAG, "Setting up ORGANIZER/ADMIN navigation");
            navView.getMenu().clear();
            navView.inflateMenu(R.menu.bottom_nav_menu_organizer);
            
            // Show notification log tab only for admins
            if (currentUser.isAdmin()) {
                navView.getMenu().findItem(R.id.navigation_notification_log).setVisible(true);
                Log.d(TAG, "Admin user - showing notification log tab");
            } else {
                navView.getMenu().findItem(R.id.navigation_notification_log).setVisible(false);
                Log.d(TAG, "Organizer user - hiding notification log tab");
            }
            
            AppBarConfiguration appBarConfiguration = new AppBarConfiguration.Builder(
                    R.id.navigation_home,R.id.navigation_history ,R.id.navigation_dashboard, R.id.navigation_notifications)
                    .build();
            
            // Add notification log to app bar config if admin
            if (currentUser.isAdmin()) {
                appBarConfiguration = new AppBarConfiguration.Builder(
                        R.id.navigation_home, R.id.navigation_dashboard, 
                        R.id.navigation_notifications, R.id.navigation_notification_log)
                        .build();
            }
            
            NavigationUI.setupActionBarWithNavController(this, navController, appBarConfiguration);
            NavigationUI.setupWithNavController(navView, navController);
            
            Log.d(TAG, "Navigation UI setup complete, attempting to navigate to home");
            
            // Explicitly navigate to home fragment for organizers/admins
            // Post to ensure NavHostFragment is fully initialized
            navView.post(() -> {
                try {
                    // Check current destination and navigate if needed
                    if (navController.getCurrentDestination() == null) {
                        Log.d(TAG, "Current destination is null, navigating to navigation_home");
                        navController.navigate(R.id.navigation_home);
                    } else {
                        int currentId = navController.getCurrentDestination().getId();
                        Log.d(TAG, "Current destination ID: " + currentId);
                        if (currentId != R.id.navigation_home) {
                            Log.d(TAG, "Navigating from " + currentId + " to navigation_home");
                            navController.navigate(R.id.navigation_home);
                        } else {
                            Log.d(TAG, "Already at navigation_home destination");
                        }
                    }
                } catch (Exception e) {
                    Log.e(TAG, "Error navigating to navigation_home: " + e.getMessage(), e);
                    e.printStackTrace();
                }
            });
            
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
        if (navController != null && !navController.popBackStack()) {
            super.onBackPressed();
        }
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Remove auth state listener when activity is destroyed
        if (authStateListener != null) {
            AuthManager.getInstance().removeAuthStateListener(authStateListener);
        }
    }
}
