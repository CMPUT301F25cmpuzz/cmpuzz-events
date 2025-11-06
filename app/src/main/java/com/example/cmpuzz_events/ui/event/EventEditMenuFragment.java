package com.example.cmpuzz_events.ui.event;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;

import com.example.cmpuzz_events.databinding.FragmentEventEditMenuBinding;

import com.google.android.material.datepicker.MaterialDatePicker;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventEditMenuFragment extends Fragment {

    private FragmentEventEditMenuBinding binding;

    private EventDetailsViewModel sharedViewModel;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        sharedViewModel = new ViewModelProvider(requireActivity()).get(EventDetailsViewModel.class);

        binding = FragmentEventEditMenuBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // Listener for the "Limit Max Entrants" toggle
        binding.maxEntrantToggleButton.setOnCheckedChangeListener((buttonView, isChecked) -> {
            if (isChecked) {
                binding.limitTextFieldLayout.setVisibility(View.VISIBLE);
            } else {
                binding.limitTextFieldLayout.setVisibility(View.GONE);
                binding.entrantLimitTextField.setText("");
            }
        });

        // Listener for the "Edit Registration Period" button
        binding.editRegistrationPeriodButton.setOnClickListener(v -> {
            showDateRangePicker();
        });

        sharedViewModel.getRegistrationDateString().observe(getViewLifecycleOwner(), text -> {
            binding.editRegistrationPeriodButton.setText(text);
        });
    }

    private void showDateRangePicker() {
        MaterialDatePicker.Builder<androidx.core.util.Pair<Long, Long>> builder =
                MaterialDatePicker.Builder.dateRangePicker();

        builder.setTitleText("Select Registration Period");

        final MaterialDatePicker<androidx.core.util.Pair<Long, Long>> picker = builder.build();

        picker.addOnPositiveButtonClickListener(selection -> {
            // The 'selection' object contains the start and end dates in milliseconds (Long)
            Long startDateMs = selection.first;
            Long endDateMs = selection.second;

            sharedViewModel.setRegistrationDates(startDateMs, endDateMs);

            Toast.makeText(getContext(), "Dates Saved" ,Toast.LENGTH_SHORT).show();
        });

        picker.show(getChildFragmentManager(), "DATE_PICKER_TAG");
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
