package com.example.cmpuzz_events.ui.event;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class EventEditMenuViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public EventEditMenuViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is the Event Edit Menu fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}

