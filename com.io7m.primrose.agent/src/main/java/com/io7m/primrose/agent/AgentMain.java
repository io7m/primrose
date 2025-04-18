/*
 * Copyright © 2023 Mark Raynsford <code@io7m.com> https://www.io7m.com
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


package com.io7m.primrose.agent;

import jnr.posix.POSIX;
import jnr.posix.POSIXFactory;
import org.apache.commons.compress.archivers.tar.TarArchiveInputStream;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.logging.ConsoleHandler;
import java.util.logging.LogRecord;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

import static java.nio.file.StandardCopyOption.ATOMIC_MOVE;
import static java.nio.file.StandardCopyOption.REPLACE_EXISTING;
import static java.nio.file.StandardOpenOption.CREATE;
import static java.nio.file.StandardOpenOption.TRUNCATE_EXISTING;

/**
 * A command-line configuration agent.
 */

public final class AgentMain
{
  private static final Logger LOG =
    Logger.getLogger("agent");

  static {
    LOG.setUseParentHandlers(false);

    final ConsoleHandler handler = new ConsoleHandler();
    handler.setFormatter(new SimpleFormatter()
    {
      @Override
      public String format(
        final LogRecord record)
      {
        return String.format(
          "%s %s%n",
          record.getLevel(),
          record.getMessage()
        );
      }
    });
    LOG.addHandler(handler);
  }

  private final boolean isDryRun;
  private final ArrayList<String> removals;
  private POSIX posix;
  private SecureRandom rng;

  private AgentMain(
    final boolean inIsDryRun)
  {
    this.isDryRun = inIsDryRun;
    this.removals = new ArrayList<String>();
  }

  /**
   * A command-line configuration agent.
   *
   * @param args The arguments
   */

  public static void main(
    final String[] args)
  {
    System.exit(
      new AgentMain(
        Objects.equals(System.getenv("PRIMROSE_AGENT_DRY_RUN"), "true")
      ).run()
    );
  }

  private static List<String> readAgentInstructions(
    final TarArchiveInputStream tar)
    throws IOException
  {
    final var entry = tar.getNextTarEntry();
    if (entry == null) {
      throw new IOException("No META-INF/PRIMROSE/AGENT.TXT");
    }
    if (!Objects.equals(entry.getName(), "META-INF/PRIMROSE/AGENT.TXT")) {
      throw new IOException("First entry must be META-INF/PRIMROSE/AGENT.TXT");
    }

    final var lines =
      new ArrayList<String>();
    final var items =
      new String(tar.readAllBytes(), StandardCharsets.UTF_8)
        .split("\\n");

    for (final var item : items) {
      final var trimmed = item.trim();
      if (!trimmed.isEmpty()) {
        lines.add(trimmed);
      }
    }
    return List.copyOf(lines);
  }

  private static byte[] readAgentJar(
    final TarArchiveInputStream tar)
    throws IOException
  {
    final var entry = tar.getNextTarEntry();
    if (entry == null) {
      throw new IOException("No META-INF/PRIMROSE/AGENT.JAR");
    }
    if (!Objects.equals(entry.getName(), "META-INF/PRIMROSE/AGENT.JAR")) {
      throw new IOException("Second entry must be META-INF/PRIMROSE/AGENT.JAR");
    }
    return tar.readAllBytes();
  }

  /**
   * Run the agent.
   *
   * @return The exit code
   */

  public int run()
  {
    if (this.isDryRun) {
      LOG.info("DRY RUN");
    }

    try {
      this.posix =
        POSIXFactory.getPOSIX();
      this.rng =
        SecureRandom.getInstanceStrong();
    } catch (final Exception e) {
      LOG.severe(e.getMessage());
      e.printStackTrace(System.err);
      return 1;
    }

    final var root = Paths.get("/");

    try (var buffered = new BufferedInputStream(System.in, 65536)) {
      try (var tar = new TarArchiveInputStream(buffered)) {
        this.processAgentInstructions(readAgentInstructions(tar));
        final var jar = readAgentJar(tar);

        while (true) {
          final var entry = tar.getNextTarEntry();
          if (entry == null) {
            break;
          }

          final var uid = entry.getLongUserId();
          final var gid = entry.getLongGroupId();
          final var perm = entry.getMode();

          final var path =
            root.resolve(entry.getName())
              .toAbsolutePath();

          if (entry.isDirectory()) {
            this.createDirectory(uid, gid, perm, path);
          } else if (entry.isFile()) {
            this.createFileFromStream(tar, uid, gid, perm, path);
          }
        }

        for (final var removal : this.removals) {
          this.executeRemoval(root.resolve(removal));
        }

        this.createDirectory(0L, 0L, 0700, Paths.get("/agent"));
        this.createFile(jar, 0L, 0L, 0600, Paths.get("/agent/agent.jar"));
        return 0;
      }
    } catch (final Exception e) {
      LOG.severe(e.getMessage());
      e.printStackTrace(System.err);
      return 1;
    }
  }

  private void executeRemoval(
    final Path resolve)
    throws IOException
  {
    LOG.info("rm %s".formatted(resolve));
    Files.deleteIfExists(resolve);
  }

  private void createFileFromStream(
    final InputStream tar,
    final long uid,
    final long gid,
    final int perm,
    final Path path)
    throws IOException
  {
    this.createFile(
      tar.readAllBytes(),
      uid,
      gid,
      perm,
      path
    );
  }

  private void createFile(
    final byte[] data,
    final long uid,
    final long gid,
    final int perm,
    final Path path)
    throws IOException
  {
    LOG.info(
      "write %s %s %s %s"
        .formatted(
          path,
          Long.toUnsignedString(uid),
          Long.toUnsignedString(gid),
          Integer.toUnsignedString(perm, 8)
        )
    );

    if (this.isDryRun) {
      return;
    }

    final var token = new byte[8];
    this.rng.nextBytes(token);
    final var tokenText =
      HexFormat.of().formatHex(token);

    final var pathTemp =
      path.resolveSibling("%s.%s".formatted(path.getFileName(), tokenText));
    final var pathTempText =
      pathTemp.toString();
    final var pathText =
      path.toString();

    try (var output =
           Files.newOutputStream(pathTemp, TRUNCATE_EXISTING, CREATE)) {
      this.posix.chown(pathTempText, (int) uid, (int) gid);
      this.posix.chmod(pathTempText, perm);

      output.write(data);
      output.flush();

      Files.move(pathTemp, path, REPLACE_EXISTING, ATOMIC_MOVE);
      this.posix.chown(pathText, (int) uid, (int) gid);
      this.posix.chmod(pathText, perm);
    }
  }

  private void createDirectory(
    final long uid,
    final long gid,
    final int perm,
    final Path path)
    throws IOException
  {
    LOG.info(
      "mkdir %s %s %s %s"
        .formatted(
          path,
          Long.toUnsignedString(uid),
          Long.toUnsignedString(gid),
          Integer.toUnsignedString(perm, 8)
        )
    );

    if (this.isDryRun) {
      return;
    }

    Files.createDirectories(path);
    this.posix.chown(path.toString(), (int) uid, (int) gid);
    this.posix.chmod(path.toString(), perm);
  }

  private void processAgentInstructions(
    final List<String> instructions)
  {
    for (final var instruction : instructions) {
      LOG.info("AGENT %s".formatted(instruction));

      if (instruction.startsWith("REMOVE ")) {
        this.removals.add(instruction.substring(7));
      }
    }
  }
}
