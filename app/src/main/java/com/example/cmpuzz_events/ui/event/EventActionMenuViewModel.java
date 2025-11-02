package com.example.cmpuzz_events.ui.event;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

public class EventActionMenuViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    public EventActionMenuViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("This is home fragment");
    }

    public LiveData<String> getText() {
        return mText;
    }
}

