package com.example.cmpuzz_events;

import java.util.ArrayList;
import java.util.HashMap;

public class Events {
    private HashMap<String, String> entrants;
    private String eventdetails;

    public Events() {
        entrants = new HashMap<>();
    }
    public ArrayList<String> GetEntrants(){
        return new ArrayList<>(entrants.values());
    }
    public void AddEntrant(String deviceID, String name){
        if (!name.isEmpty()) {
            entrants.put(deviceID, name);
        }
    }
    public void RemoveEntrant(String deviceID) {
        entrants.remove(deviceID);
    }
    public String GetEventDetails(){
        return eventdetails;
    }
    public void SetEventDetails(String str){
        eventdetails = str;
    }
}
