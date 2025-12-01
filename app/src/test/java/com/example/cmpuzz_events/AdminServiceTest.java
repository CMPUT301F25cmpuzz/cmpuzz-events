package com.example.cmpuzz_events;

import com.example.cmpuzz_events.models.user.User;
import org.junit.Before;
import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

/**
 * Tests the user data manipulation logic from AdminService in complete isolation.
 * Verifies that role-based filtering and user removal from lists work correctly.
 */
public class AdminServiceTest {

    private User testUser1, testUser2, testOrganizer;
    private List<User> allTestUsers;

    private final String TEST_USER_1_UID = "user_abc";
    private final String TEST_USER_2_UID = "user_def";
    private final String TEST_ORGANIZER_UID = "org_123";

    /**
     * Direct copy of the filtering logic from AdminService's getAllAccountsByRole method.
     * Filters a list of users based on a specified UserRole.
     */
    private List<User> filterUsersByRole(List<User> users, User.UserRole role) {
        List<User> filteredList = new ArrayList<>();
        if (users == null) {
            return filteredList;
        }
        for (User user : users) {
            if (user != null && user.getRole() == role) {
                filteredList.add(user);
            }
        }
        return filteredList;
    }

    /**
     * Direct copy of the removal logic from AdminService's removeUserFromAllEvents method.
     * Removes a user from a simple list of UIDs.
     */
    private List<String> removeUserFromList(List<String> list, String uidToRemove) {
        if (list == null || !list.contains(uidToRemove)) {
            return new ArrayList<>(list != null ? list : new ArrayList<>());
        }
        List<String> updatedList = new ArrayList<>(list);
        updatedList.remove(uidToRemove);
        return updatedList;
    }

    /**
     * Direct copy of the invitation removal logic from AdminService's removeUserFromAllEvents method.
     * Removes a user from a list of invitation maps.
     */
    private List<Map<String, Object>> removeUserFromInvitations(List<Map<String, Object>> invitations, String uidToRemove) {
        if (invitations == null) {
            return new ArrayList<>();
        }
        return invitations.stream()
                .filter(inv -> inv != null && !uidToRemove.equals(inv.get("userId")))
                .collect(Collectors.toList());
    }

    @Before
    public void setUp() {
        testUser1 = new User();
        testUser1.setUid(TEST_USER_1_UID);
        testUser1.setRole(User.UserRole.USER);

        testUser2 = new User();
        testUser2.setUid(TEST_USER_2_UID);
        testUser2.setRole(User.UserRole.USER);

        testOrganizer = new User();
        testOrganizer.setUid(TEST_ORGANIZER_UID);
        testOrganizer.setRole(User.UserRole.ORGANIZER);

        allTestUsers = new ArrayList<>(Arrays.asList(testUser1, testUser2, testOrganizer));
    }

    /**
     * Verifies that filtering a list by a role returns only matching users.
     */
    @Test
    public void testRoleFiltering() {
        List<User> result = filterUsersByRole(allTestUsers, User.UserRole.USER);
        assertEquals(2, result.size());

        List<String> resultUids = result.stream().map(User::getUid).collect(Collectors.toList());
        assertTrue(resultUids.contains(TEST_USER_1_UID));
        assertTrue(resultUids.contains(TEST_USER_2_UID));
    }

    /**
     * Verifies that filtering by a role with no matches returns an empty list.
     */
    @Test
    public void testRoleFilteringWithNoMatches() {
        List<User> result = filterUsersByRole(allTestUsers, User.UserRole.ADMIN);
        assertTrue(result.isEmpty());
    }

    /**
     * Verifies that a user is correctly removed from a simple list of UIDs.
     */
    @Test
    public void testUserRemovalFromUidList() {
        List<String> originalList = new ArrayList<>(Arrays.asList(TEST_USER_1_UID, TEST_ORGANIZER_UID, TEST_USER_2_UID));
        List<String> result = removeUserFromList(originalList, TEST_ORGANIZER_UID);

        assertEquals(2, result.size());
        assertFalse(result.contains(TEST_ORGANIZER_UID));
        assertTrue(result.contains(TEST_USER_1_UID));
    }

    /**
     * Verifies that a user is correctly removed from a list of invitation maps.
     */
    @Test
    public void testUserRemovalFromInvitations() {
        List<Map<String, Object>> originalInvitations = new ArrayList<>();
        Map<String, Object> inv1 = new HashMap<>();
        inv1.put("userId", TEST_USER_1_UID);
        originalInvitations.add(inv1);

        Map<String, Object> inv2 = new HashMap<>();
        inv2.put("userId", TEST_ORGANIZER_UID);
        originalInvitations.add(inv2);

        List<Map<String, Object>> result = removeUserFromInvitations(originalInvitations, TEST_ORGANIZER_UID);

        assertEquals(1, result.size());
        assertEquals(TEST_USER_1_UID, result.get(0).get("userId"));
    }
}
