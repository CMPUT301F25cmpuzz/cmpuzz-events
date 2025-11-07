package com.example.cmpuzz_events.service;

import com.example.cmpuzz_events.ui.event.Event;

public class OrganizerService implements IOrganizerService{
    public void clearEntrants(Event event)
    {
        event.setWaitlist();
    }
}
