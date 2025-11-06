package com.example.cmpuzz_events.ui.event;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class EventDetailsViewModel extends ViewModel {

    private final MutableLiveData<String> mText;

    private final MutableLiveData<Long> startDate = new MutableLiveData<>();
    private final MutableLiveData<Long> endDate = new MutableLiveData<>();

    private final MutableLiveData<String> registrationDateString = new MutableLiveData<>();

    public EventDetailsViewModel() {
        mText = new MutableLiveData<>();
        mText.setValue("Event Details");

        registrationDateString.setValue("Set Registration Period");
    }

    public LiveData<String> getText() {
        return mText;
    }

    public void setRegistrationDates(Long startDateMs, Long endDateMs) {
        startDate.setValue(startDateMs);
        endDate.setValue(endDateMs);

        // Format the dates and update the string LiveData
        SimpleDateFormat sdf = new SimpleDateFormat("MMM dd, yyyy", Locale.getDefault());
        String start = sdf.format(new Date(startDateMs));
        String end = sdf.format(new Date(endDateMs));
        registrationDateString.setValue("From: " + start + "\nTo: " + end);
    }

    public LiveData<String> getRegistrationDateString() {
        return registrationDateString;
    }
}

