package com.example.cmpuzz_events.models.event;

import java.util.ArrayList;

public class Invitations {
    private ArrayList<String> invited;
    // hashmap key - Device ID
    // hashmap value - Username
    // Declined - users that declined the invitation
    private ArrayList<String> declined;
    // hashmap key - Device ID
    // hashmap value - Username
    // Attendance - users that accepted the invitation
    private ArrayList<String> attendance;

    public Invitations() {
        this.invited = new ArrayList<>();
        this.declined = new ArrayList<>();
        this.attendance = new ArrayList<>();
    }

    public void UserAccept(String deviceID) {
        invited.remove(deviceID);
        attendance.remove(deviceID);
    }

    public void UserDecline(String deviceID) {
        invited.remove(deviceID);
        declined.add(deviceID);
    }
}
