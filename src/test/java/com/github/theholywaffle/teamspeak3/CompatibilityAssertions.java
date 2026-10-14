package com.github.theholywaffle.teamspeak3;

import static org.junit.jupiter.api.Assertions.*;

import com.github.theholywaffle.teamspeak3.api.wrapper.Permission;
import java.util.List;

final class CompatibilityAssertions {
    private CompatibilityAssertions() {}

    static Permission permission(
            List<Permission> assignments, String name, int value, boolean negated, boolean skipped) {
        var matches = assignments.stream().filter(p -> p.name().equals(name)).toList();
        assertEquals(1, matches.size(), "Exactly one assignment for " + name);
        var permission = matches.getFirst();
        assertEquals(value, permission.value());
        assertEquals(negated, permission.negated());
        assertEquals(skipped, permission.skipped());
        return permission;
    }
}
