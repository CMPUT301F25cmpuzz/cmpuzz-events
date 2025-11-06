package com.example.cmpuzz_events;

import java.util.ArrayList;
import java.util.HashMap;

public class Events {
    private ArrayList<String> waitlist;
    private String eventdetails;
    private static Integer MAXWAITLISTSIZE;
    private static Integer MAXATTENDEESIZE;
    private String eventID;

    public Events() {
        this.waitlist = new ArrayList<>();
    }

    public void addWaitlistee(String deviceID) {
        waitlist.add(deviceID);
    }
    public void removeWaitlistee(String deviceID) {
        waitlist.remove(deviceID);
    }
    public ArrayList<String> getWaitlist() {
        return waitlist;
    }
    public void setWaitlist(ArrayList<String> waitlist) {
        this.waitlist = waitlist;
    }
    public String getEventDetails() {
        return eventdetails;
    }
    public void SetEventDetails(String str) {
        eventdetails = str;
    }
    public Integer getMAXWAITLISTSIZE() {
        return MAXWAITLISTSIZE;
    }
    public void setMAXWAITLISTSIZE(Integer integer) {
        MAXWAITLISTSIZE = integer;
    }
    public Integer getMAXATTENDEESIZE() {
        return MAXATTENDEESIZE;
    }
    public void setMAXATTENDEESIZE(Integer integer) {
        MAXATTENDEESIZE = integer;
    }
    public String getEventID() {
        return eventID;
    }
    public void setEventID(String eventID) {
        this.eventID = eventID;
    }
}
