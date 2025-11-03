package com.example.cmpuzz_events;

import java.util.ArrayList;
import java.util.HashMap;

public class Events {
    // hashmap key - Device ID
    // hashmap value - Username
    // Attendance - users that accepted the invitation
    private ArrayList<HashMap<String, String>> Attendance;
    // hashmap key - Device ID
    // hashmap value - Username
    // Waitlist - users that are waitlisted to the event
    private ArrayList<HashMap<String, String>> Waitlist;
    // hashmap key - Device ID
    // hashmap value - Username
    // Declined - users that declined the invitation
    private ArrayList<HashMap<String, String>> Declined;
    // hashmap key - Device ID
    // hashmap value - Username
    // Invited - users that are invited to the event from the waitlist
    private ArrayList<HashMap<String, String>> Invited;
    private String eventdetails;
    private Integer MAXWAITLISTSIZE;
    private Integer MAXATTENDEESIZE;
    private Integer EventID;

    public Events() {
        Attendance = new ArrayList<>();
        Waitlist = new ArrayList<>();
        Declined = new ArrayList<>();
        Invited = new ArrayList<>();
    }

    public ArrayList<String> GetAttendance() {
        ArrayList<String> result = new ArrayList<>();
        for (HashMap<String, String> entry : Attendance) {
            result.addAll(entry.values());
        }
        return result;
    }

    public ArrayList<String> GetWaitList() {
        ArrayList<String> result = new ArrayList<>();
        for (HashMap<String, String> entry : Waitlist) {
            result.addAll(entry.values());
        }
        return result;
    }

    public void AddAttendee(String deviceID, String name) {
        HashMap<String, String> map = new HashMap<>();
        map.put(deviceID, name);
        Attendance.add(map);
    }

    public void AddWaitlistee(String deviceID, String name) {
        HashMap<String, String> map = new HashMap<>();
        map.put(deviceID, name);
        Waitlist.add(map);
    }

    public void RemoveAttendee(String deviceID) {
        Attendance.removeIf(entry -> entry.containsKey(deviceID));
    }

    public void RemoveWaitlistee(String deviceID) {
        Waitlist.removeIf(entry -> entry.containsKey(deviceID));
    }

    public ArrayList<HashMap<String, String>> getAttendance() {
        return Attendance;
    }

    public void setAttendance(ArrayList<HashMap<String, String>> attendance) {
        Attendance = attendance;
    }

    public ArrayList<HashMap<String, String>> getWaitlist() {
        return Waitlist;
    }

    public void setWaitlist(ArrayList<HashMap<String, String>> waitlist) {
        Waitlist = waitlist;
    }

    public ArrayList<HashMap<String, String>> getDeclined() {
        return Declined;
    }

    public void setDeclined(ArrayList<HashMap<String, String>> declined) {
        Declined = declined;
    }

    public ArrayList<HashMap<String, String>> getInvited() {
        return Invited;
    }

    public void setInvited(ArrayList<HashMap<String, String>> invited) {
        Invited = invited;
    }

    public String GetEventDetails() {
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

    public Integer getEventID() {
        return EventID;
    }

    public void setEventID(Integer eventID) {
        EventID = eventID;
    }
}
