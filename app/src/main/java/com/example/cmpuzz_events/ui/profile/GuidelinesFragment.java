package com.example.cmpuzz_events.ui.profile;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import com.example.cmpuzz_events.R;

/**
 * A simple {@link Fragment} subclass that displays guidelines.
 * This fragment is responsible for inflating and displaying the UI
 * defined in the {@code fragment_guidelines.xml} layout file.
 */
public class GuidelinesFragment extends Fragment {

    /**
     * Default constructor for the fragment.
     * It is required for the Android framework to instantiate the fragment.
     */
    public GuidelinesFragment() {
    }

    /**
     * Called to have the fragment instantiate its user interface view. [6]
     * This is optional, and non-graphical fragments can return null. This will be called between
     * onCreate(Bundle) and onActivityCreated(Bundle). [6]
     *
     * @param inflater The LayoutInflater object that can be used to inflate
     *                 any views in the fragment. [6]
     * @param container If non-null, this is the parent view that the fragment's
     *                  UI should be attached to. The fragment should not add the view itself,
     *                  but this can be used to generate the LayoutParams of the view. [6]
     * @param savedInstanceState If non-null, this fragment is being re-constructed
     *                           from a previous saved state as given here. [6]
     * @return Return the View for the fragment's UI, or null.
     */
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_guidelines, container, false);
    }
}
