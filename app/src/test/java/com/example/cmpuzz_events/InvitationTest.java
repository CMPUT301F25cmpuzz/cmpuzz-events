package com.example.cmpuzz_events;

import static org.junit.Assert.*;

import com.example.cmpuzz_events.models.event.Invitation;

import org.junit.Before;
import org.junit.Test;

public class InvitationTest {

    private Invitation invitation;

    @Before
    public void setUp() {
        invitation = new Invitation("user123", "testuser");
    }

    @Test
    public void defaultState_isPending() {
        assertTrue(invitation.isPending());
        assertEquals(Invitation.InvitationStatus.PENDING, invitation.getStatus());
        assertFalse(invitation.isAccepted());
        assertFalse(invitation.isDeclined());
        assertNotNull(invitation.getInvitedAt());
        assertNull(invitation.getRespondedAt());
    }

    @Test
    public void accept_changesStateToAccepted() {
        invitation.accept();
        assertTrue(invitation.isAccepted());
        assertEquals(Invitation.InvitationStatus.ACCEPTED, invitation.getStatus());
        assertFalse(invitation.isPending());
        assertNotNull(invitation.getRespondedAt());
    }

    @Test
    public void decline_changesStateToDeclined() {
        invitation.decline();
        assertTrue(invitation.isDeclined());
        assertEquals(Invitation.InvitationStatus.DECLINED, invitation.getStatus());
        assertFalse(invitation.isPending());
        assertNotNull(invitation.getRespondedAt());
    }

    @Test
    public void cancel_changesStateToCancelled() {
        invitation.cancel();
        assertEquals(Invitation.InvitationStatus.CANCELLED, invitation.getStatus());
        assertFalse(invitation.isPending());
        assertNotNull(invitation.getRespondedAt());
    }

    @Test
    public void statusEnum_fromString() {
        assertEquals(Invitation.InvitationStatus.PENDING, Invitation.InvitationStatus.fromString("PENDING"));
        assertEquals(Invitation.InvitationStatus.ACCEPTED, Invitation.InvitationStatus.fromString("accepted"));
        assertEquals(Invitation.InvitationStatus.DECLINED, Invitation.InvitationStatus.fromString("DeClInEd"));
        assertEquals(Invitation.InvitationStatus.CANCELLED, Invitation.InvitationStatus.fromString("cancelled"));
        assertEquals(Invitation.InvitationStatus.PENDING, Invitation.InvitationStatus.fromString("invalid")); // Default
    }
}