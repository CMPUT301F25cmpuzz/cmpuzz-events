package com.example.cmpuzz_events.ui.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.cmpuzz_events.R;
import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.models.user.User;
import com.example.cmpuzz_events.service.IAdminService;
import com.google.android.material.tabs.TabLayout;

import java.util.ArrayList;
import java.util.List;

/**
 * Fragment to view different categories of entrants:
 * - Waitlist: Users waiting for invitation
 * - Invited: Users who have been sent invitations
 * - Attendees: Users who accepted invitations
 */
public class BrowseUsersFragment extends Fragment {

    private static final String TAG = "ViewUsersFragment";
    private RecyclerView recyclerView;
    private UserListAdapter adapter;
    private TextView emptyStateText;
    private List<User> users;
    private boolean isAdmin;
    private TabLayout tabLayout;
    private IAdminService adminService;



    public static BrowseUsersFragment newInstance(String eventId) {
        BrowseUsersFragment fragment = new BrowseUsersFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        User currentUser = AuthManager.getInstance().getCurrentUser();
        isAdmin = currentUser != null && currentUser.isAdmin();
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_view_entrants, container, false);

        // Initialize views
        tabLayout = root.findViewById(R.id.tabLayout);
        recyclerView = root.findViewById(R.id.recyclerView);
        emptyStateText = root.findViewById(R.id.tvEmptyState);

        // Setup RecyclerView
        recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new UserListAdapter(new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // Setup tabs
        tabLayout.addTab(tabLayout.newTab().setText("Users"));
        tabLayout.addTab(tabLayout.newTab().setText("Organizers"));

        // Tab selection listener
        tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                loadEntrantsForTab(tab.getPosition());
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        return root;
    }

    private void loadEntrantsForTab(int position) {
        User.UserRole role;
        switch (position) {
            case 0:
                role = User.UserRole.USER;
                break;
            case 1:
                role = User.UserRole.ORGANIZER;
                break;
            default:
                role = User.UserRole.USER;
                break;
        }
        loadUserList(role);
    }

    private void loadUserList(User.UserRole role) {
        adminService.getAllAccountsByRole(role, new IAdminService.UIAccountListCallback() {
            @Override
            public void onSuccess(List<User> users) {
                if (users.isEmpty()) {
                    showEmptyState("No users found");
                } else {
                    adapter.updateUsers(users);
                    recyclerView.setVisibility(View.VISIBLE);
                    emptyStateText.setVisibility(View.GONE);
                }
            }

            @Override
            public void onError(String error) {
                Log.e(TAG, "Error loading users: " + error);
                showEmptyState("Error loading users");
            }
        });
    }

    private void showEmptyState(String message) {
        adapter.updateUsers(new ArrayList<>());
        recyclerView.setVisibility(View.GONE);
        emptyStateText.setVisibility(View.VISIBLE);
        emptyStateText.setText(message);
    }


    private void showToast(String message) {
        if (getContext() != null) {
            Toast.makeText(getContext(), message, Toast.LENGTH_SHORT).show();
        }
    }
}
