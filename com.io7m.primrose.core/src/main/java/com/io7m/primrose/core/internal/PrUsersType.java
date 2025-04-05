/*
 * Copyright © 2025 Mark Raynsford <code@io7m.com> https://www.io7m.com
 *
 * Permission to use, copy, modify, and/or distribute this software for any
 * purpose with or without fee is hereby granted, provided that the above
 * copyright notice and this permission notice appear in all copies.
 *
 * THE SOFTWARE IS PROVIDED "AS IS" AND THE AUTHOR DISCLAIMS ALL WARRANTIES
 * WITH REGARD TO THIS SOFTWARE INCLUDING ALL IMPLIED WARRANTIES OF
 * MERCHANTABILITY AND FITNESS. IN NO EVENT SHALL THE AUTHOR BE LIABLE FOR ANY
 * SPECIAL, DIRECT, INDIRECT, OR CONSEQUENTIAL DAMAGES OR ANY DAMAGES
 * WHATSOEVER RESULTING FROM LOSS OF USE, DATA OR PROFITS, WHETHER IN AN
 * ACTION OF CONTRACT, NEGLIGENCE OR OTHER TORTIOUS ACTION, ARISING OUT OF OR
 * IN CONNECTION WITH THE USE OR PERFORMANCE OF THIS SOFTWARE.
 */


package com.io7m.primrose.core.internal;

import com.io7m.immutables.styles.ImmutablesStyleType;
import org.immutables.value.Value;

import java.math.BigInteger;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

/**
 * The users database.
 */

@ImmutablesStyleType
@Value.Immutable
public interface PrUsersType
{
  /**
   * @return The groups
   */

  List<PrGroupIdentifier> groups();

  /**
   * @return The users
   */

  List<PrUserIdentifier> users();

  /**
   * @return The user/group associations
   */

  List<PrUserGroupAssociation> userGroups();

  /**
   * @return The users by name
   */

  @Value.Derived
  default Map<String, PrUserIdentifier> usersByName()
  {
    final var m = new HashMap<String, PrUserIdentifier>();
    for (final var user : this.users()) {
      final var userName = user.name();
      if (m.containsKey(userName)) {
        throw new IllegalArgumentException(
          "Duplicate user name: %s".formatted(userName)
        );
      }
      m.put(userName, user);
    }
    return Map.copyOf(m);
  }

  /**
   * @return The users by ID
   */

  @Value.Derived
  default Map<BigInteger, PrUserIdentifier> usersByID()
  {
    final var m = new HashMap<BigInteger, PrUserIdentifier>();
    for (final var user : this.users()) {
      final var userId = user.userId();
      if (m.containsKey(userId)) {
        throw new IllegalArgumentException(
          "Duplicate user ID: %s".formatted(userId)
        );
      }
      m.put(userId, user);
    }
    return Map.copyOf(m);
  }

  /**
   * @return The groups by name
   */

  @Value.Derived
  default Map<String, PrGroupIdentifier> groupsByName()
  {
    final var m = new HashMap<String, PrGroupIdentifier>();
    for (final var group : this.groups()) {
      final var groupName = group.name();
      if (m.containsKey(groupName)) {
        throw new IllegalArgumentException(
          "Duplicate group name: %s".formatted(groupName)
        );
      }
      m.put(groupName, group);
    }
    return Map.copyOf(m);
  }

  /**
   * @return The groups by ID
   */

  @Value.Derived
  default Map<BigInteger, PrGroupIdentifier> groupsByID()
  {
    final var m = new HashMap<BigInteger, PrGroupIdentifier>();
    for (final var group : this.groups()) {
      final var groupId = group.groupId();
      if (m.containsKey(groupId)) {
        throw new IllegalArgumentException(
          "Duplicate group ID: %s".formatted(groupId)
        );
      }
      m.put(groupId, group);
    }
    return Map.copyOf(m);
  }

  /**
   * @return The group members
   */

  @Value.Derived
  default Map<PrGroupIdentifier, Set<PrUserIdentifier>> groupMembers()
  {
    final var m = new HashMap<PrGroupIdentifier, Set<PrUserIdentifier>>();
    for (final var group : this.userGroups()) {
      var members = m.get(group.group());
      if (members == null) {
        members = new TreeSet<>();
      }
      members.add(group.user());
      m.put(group.group(), members);
    }
    return Map.copyOf(m);
  }
}
