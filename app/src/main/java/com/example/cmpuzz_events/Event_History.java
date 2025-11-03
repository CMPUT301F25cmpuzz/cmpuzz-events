package com.example.cmpuzz_events;

import java.util.ArrayList;
import java.util.HashMap;

public class Event_History {
    // key String in the hashmap holds the ID of the event
    // value String in the hashmap holds whether the user was selected or not (US 01.02.03)
    private ArrayList<HashMap<String, String>> ShowHistory;

    public Event_History() {
        ShowHistory = new ArrayList<>();
    }
    public void addEvent(String eventName, String selectionStatus) {
        HashMap<String, String> eventEntry = new HashMap<>();
        eventEntry.put(eventName, selectionStatus);
        ShowHistory.add(eventEntry);
    }
    public void removeEvent(String eventName) {
        ShowHistory.removeIf(event -> event.containsKey(eventName));
    }
}
