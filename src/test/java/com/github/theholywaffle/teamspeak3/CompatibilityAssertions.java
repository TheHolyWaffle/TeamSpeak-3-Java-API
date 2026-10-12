package com.github.theholywaffle.teamspeak3;

import com.github.theholywaffle.teamspeak3.api.wrapper.Permission;
import java.util.List;
import static org.junit.jupiter.api.Assertions.*;

final class CompatibilityAssertions {
    private CompatibilityAssertions() { }

    static Permission permission(List<Permission> assignments, String name, int value, boolean negated, boolean skipped) {
        var matches = assignments.stream().filter(p -> p.getName().equals(name)).toList();
        assertEquals(1, matches.size(), "Exactly one assignment for " + name);
        var permission = matches.getFirst();
        assertEquals(value, permission.getValue());
        assertEquals(negated, permission.isNegated());
        assertEquals(skipped, permission.isSkipped());
        return permission;
    }
}
