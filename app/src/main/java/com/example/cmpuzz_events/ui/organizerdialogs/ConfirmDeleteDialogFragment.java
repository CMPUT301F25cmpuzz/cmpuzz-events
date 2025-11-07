package com.example.cmpuzz_events.ui.organizerdialogs;

import android.app.Dialog;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.android.material.dialog.MaterialAlertDialogBuilder;

public class ConfirmDeleteDialogFragment extends DialogFragment {

    public static final String REQUEST_KEY = "confirm_delete_request";
    public static final String RESULT_CONFIRMED = "confirmed";

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        return new MaterialAlertDialogBuilder(requireContext())
                .setTitle("Delete profile?")
                .setMessage("Are you sure you want to delete your profile?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    Bundle result = new Bundle();
                    result.putBoolean(RESULT_CONFIRMED, true);
                    getParentFragmentManager().setFragmentResult(REQUEST_KEY, result);
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss())
                .create();
    }
}
