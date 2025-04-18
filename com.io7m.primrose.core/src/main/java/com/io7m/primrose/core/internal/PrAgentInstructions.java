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

import org.apache.commons.compress.archivers.tar.TarArchiveEntry;
import org.apache.commons.compress.archivers.tar.TarArchiveOutputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Objects;

import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;
import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.BIGNUMBER_POSIX;
import static org.apache.commons.compress.archivers.tar.TarArchiveOutputStream.LONGFILE_POSIX;

/**
 * A generator for agent instruction archives.
 */

public final class PrAgentInstructions
{
  private static final Logger LOG =
    LoggerFactory.getLogger(PrAgentInstructions.class);

  private final Path outputFile;
  private final PrOwnership ownershipDefault;
  private final List<PrOwnership> ownership;
  private final Path inputDirectory;
  private final ArrayList<String> removals;
  private final HashMap<String, PrOwnership> ownershipMap;

  /**
   * A generator for agent instruction archives.
   *
   * @param inInputDirectory   The input directory
   * @param inOutputFile       The output file
   * @param inOwnership        The ownership
   * @param inOwnershipDefault The default ownership
   */

  public PrAgentInstructions(
    final PrOwnership inOwnershipDefault,
    final List<PrOwnership> inOwnership,
    final Path inInputDirectory,
    final Path inOutputFile)
  {
    this.ownershipDefault =
      Objects.requireNonNull(inOwnershipDefault, "inOwnershipDefault");
    this.ownership =
      Objects.requireNonNull(inOwnership, "inOwnership");
    this.inputDirectory =
      Objects.requireNonNull(inInputDirectory, "inInputDirectory");
    this.outputFile =
      Objects.requireNonNull(inOutputFile, "inOutputFile");

    this.removals =
      new ArrayList<>();
    this.ownershipMap =
      new HashMap<>();
  }

  /**
   * Execute the instruction generator.
   *
   * @throws Exception On errors
   */

  public void execute()
    throws Exception
  {
    this.parseOwnership();

    try (var out = Files.newOutputStream(
      this.outputFile,
      TRUNCATE_EXISTING,
      CREATE)) {
      try (var tarOut = new TarArchiveOutputStream(out, "UTF-8")) {
        tarOut.setLongFileMode(LONGFILE_POSIX);
        tarOut.setBigNumberMode(BIGNUMBER_POSIX);
        this.createTar(tarOut);
      }
    }
  }

  private void parseOwnership()
  {
    for (final var info : this.ownership) {
      this.ownershipMap.put(info.name(), info);
    }
  }

  private void createTar(
    final TarArchiveOutputStream tar)
    throws IOException
  {
    this.createTarAgentInstructions(tar);
    createTarAgentJar(tar);
    this.createTarDirectories(tar);
    this.createTarFiles(tar);
  }

  private static void createTarAgentJar(
    final TarArchiveOutputStream tar)
    throws IOException
  {
    final var entry = new TarArchiveEntry("META-INF/PRIMROSE/AGENT.JAR");
    entry.setMode(0);
    entry.setUserId(0);
    entry.setGroupId(0);
    LOG.debug("Archive: Entry {}", entry.getName());

    final byte[] jarBytes;
    try (var stream = PrAgentInstructions.class.getResourceAsStream(
      "/com/io7m/primrose/core/internal/agent.jar")) {
      jarBytes = stream.readAllBytes();
    }

    entry.setSize(Integer.toUnsignedLong(jarBytes.length));

    tar.putArchiveEntry(entry);
    tar.write(jarBytes);
    tar.closeArchiveEntry();
  }

  private void createTarAgentInstructions(
    final TarArchiveOutputStream tar)
    throws IOException
  {
    final var entry = new TarArchiveEntry("META-INF/PRIMROSE/AGENT.TXT");
    entry.setMode(0);
    entry.setUserId(0);
    entry.setGroupId(0);
    LOG.debug("Archive: Entry {}", entry.getName());

    final var lines = new ArrayList<String>();
    for (final var removal : this.removals) {
      lines.add("REMOVE %s".formatted(removal));
    }
    final var data =
      String.join("\n", lines)
        .getBytes(StandardCharsets.UTF_8);

    entry.setSize(Integer.toUnsignedLong(data.length));

    tar.putArchiveEntry(entry);
    tar.write(data);
    tar.closeArchiveEntry();
  }

  private void createTarFiles(
    final TarArchiveOutputStream tar)
    throws IOException
  {
    try (var stream = Files.walk(this.inputDirectory)) {
      final var files =
        stream.filter(Files::isRegularFile)
          .sorted()
          .toList();

      for (final var file : files) {
        final var relative =
          this.inputDirectory.relativize(file);

        if (relative.toString().isEmpty()) {
          continue;
        }

        final var info =
          this.findOwnershipFor(relative);
        final var uid =
          info.user().userId().longValueExact();
        final var gid =
          info.group().groupId().longValueExact();

        final var entry =
          new TarArchiveEntry(file, relative.toString());
        LOG.debug("Archive: Entry {}", entry.getName());

        entry.setMode(info.modeForFile());
        entry.setUserId(uid);
        entry.setGroupId(gid);

        LOG.debug(
          "{} (tar {} (uid {} gid {} perm 0{})",
          file,
          relative,
          uid,
          gid,
          Integer.toUnsignedString(info.modeForFile(), 8)
        );

        tar.putArchiveEntry(entry);
        try (var input = Files.newInputStream(file)) {
          input.transferTo(tar);
        }
        tar.closeArchiveEntry();
      }
    }
  }

  private PrOwnership findOwnershipFor(
    final Path file)
  {
    var pathPointer = file;

    while (pathPointer != null) {
      final var info = this.ownershipMap.get(pathPointer.toString());
      if (info == null) {
        pathPointer = pathPointer.getParent();
        continue;
      }
      return info;
    }

    return this.ownershipDefault;
  }

  private void createTarDirectories(
    final TarArchiveOutputStream tar)
    throws IOException
  {
    try (var stream = Files.walk(this.inputDirectory)) {
      final var directories =
        stream.filter(Files::isDirectory)
          .toList();

      for (final var directory : directories) {
        final var relative =
          this.inputDirectory.relativize(directory);

        if (relative.toString().isEmpty()) {
          continue;
        }

        final var info =
          this.findOwnershipFor(relative);
        final var uid =
          info.user().userId().longValueExact();
        final var gid =
          info.group().groupId().longValueExact();

        LOG.debug(
          "{} (tar {} (uid {} gid {} perm 0{})",
          directory,
          relative,
          uid,
          gid,
          Integer.toUnsignedString(info.modeForDirectory(), 8)
        );

        final var entry = new TarArchiveEntry(directory, relative.toString());
        LOG.debug("Archive: Entry {}", entry.getName());

        entry.setMode(info.modeForDirectory());
        entry.setUserId(uid);
        entry.setGroupId(gid);

        tar.putArchiveEntry(entry);
        tar.closeArchiveEntry();
      }
    }
  }
}
