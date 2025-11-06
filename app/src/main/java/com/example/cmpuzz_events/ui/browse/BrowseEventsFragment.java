package com.example.cmpuzz_events.ui.browse;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;

import com.example.cmpuzz_events.databinding.FragmentBrowseEventsBinding;

public class BrowseEventsFragment extends Fragment {

    private FragmentBrowseEventsBinding binding;
    private static final String TAG = "BrowseEventsFragment";

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentBrowseEventsBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        // TODO: Show all events that users can join
        TextView textView = new TextView(getContext());
        textView.setText("Browse Events - Coming Soon!");
        textView.setTextSize(20);
        
        return root;
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
