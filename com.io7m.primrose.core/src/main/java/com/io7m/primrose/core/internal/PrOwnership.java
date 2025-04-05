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

import java.util.Objects;

/**
 * The ownership of a set of files.
 *
 * @param name             The base file
 * @param user             The user
 * @param group            The group
 * @param modeForFile      The POSIX permissions mode for files
 * @param modeForDirectory The POSIX permissions mode for directories
 */

public record PrOwnership(
  String name,
  PrUserIdentifier user,
  PrGroupIdentifier group,
  int modeForFile,
  int modeForDirectory)
{
  /**
   * The ownership of a set of files.
   *
   * @param name             The base file
   * @param user             The user
   * @param group            The group
   * @param modeForFile      The POSIX permissions mode for files
   * @param modeForDirectory The POSIX permissions mode for directories
   */

  public PrOwnership
  {
    Objects.requireNonNull(name, "name");
    Objects.requireNonNull(user, "user");
    Objects.requireNonNull(group, "group");
  }
}
