package com.example.cmpuzz_events.ui.home;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.cmpuzz_events.auth.AuthManager;
import com.example.cmpuzz_events.databinding.FragmentHomeBinding;
import com.example.cmpuzz_events.models.user.User;

public class HomeFragment extends Fragment {

    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        final TextView textView = binding.textHome;
        
        // Look how simple AuthManager makes.. It's DEPENDENCY INJECTION AND I LOVEEE ITTTT - Andy
        User currentUser = AuthManager.getInstance().getCurrentUser();
        if (currentUser != null && currentUser.getUsername() != null) {
            textView.setText("Welcome " + currentUser.getUsername() + "!");
        } else {
            homeViewModel.getText().observe(getViewLifecycleOwner(), textView::setText);
        }
        
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}