package com.example.cmpuzz_events.ui.event;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class EventDetailsViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public EventDetailsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is the Event Details fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}

