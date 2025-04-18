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


package com.io7m.primrose.core;

import com.io7m.jdeferthrow.core.ExceptionTracker;
import com.io7m.jproperties.JProperties;
import com.io7m.jproperties.JPropertyIncorrectType;
import com.io7m.jproperties.JPropertyNonexistent;
import com.io7m.primrose.core.internal.PrAgentInstructions;
import com.io7m.primrose.core.internal.PrGroupIdentifier;
import com.io7m.primrose.core.internal.PrMap;
import com.io7m.primrose.core.internal.PrOwnership;
import com.io7m.primrose.core.internal.PrUserGroupAssociation;
import com.io7m.primrose.core.internal.PrUserIdentifier;
import com.io7m.primrose.core.internal.PrUsers;
import com.io7m.primrose.core.internal.PrValueData;
import freemarker.template.Configuration;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateException;
import org.apache.commons.io.FilenameUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Properties;

/**
 * A processor.
 */

public final class PrProcessor
{
  private static final Logger LOG =
    LoggerFactory.getLogger(PrProcessor.class);

  private final PrConfiguration configuration;
  private final HashMap<String, PrOwnership> ownership;
  private final PrMap userIds;
  private final PrMap groupIds;
  private PrValueData data;
  private PrUsers users;
  private PrOwnership ownershipDefault;

  private PrProcessor(
    final PrConfiguration inConfiguration)
  {
    this.configuration =
      Objects.requireNonNull(inConfiguration, "configuration");
    this.ownership =
      new HashMap<>();
    this.userIds =
      new PrMap();
    this.groupIds =
      new PrMap();
  }

  /**
   * Create a new processor.
   *
   * @param configuration The configuration
   *
   * @return The processor
   */

  public static PrProcessor create(
    final PrConfiguration configuration)
  {
    Objects.requireNonNull(configuration, "configuration");
    return new PrProcessor(configuration);
  }

  private static void processFileCreateDirectory(
    final Path outputFile)
    throws IOException
  {
    LOG.debug("CreateDirectory {}", outputFile);
    Files.createDirectories(outputFile);
  }

  /**
   * Run the processor.
   *
   * @throws PrException On errors
   */

  public void run()
    throws PrException
  {
    this.loadValues();
    this.loadUsers();
    this.loadOwnership();
    this.processFiles();
    this.processArchive();
  }

  private void processArchive()
    throws PrException
  {
    final var ins =
      new PrAgentInstructions(
        this.ownershipDefault,
        this.ownership.values()
          .stream()
          .toList(),
        this.configuration.outputDirectory(),
        this.configuration.outputArchive()
      );

    try {
      ins.execute();
    } catch (final Exception e) {
      throw new PrException(
        e.getMessage(),
        e,
        Map.ofEntries(
          Map.entry("Output", this.configuration.outputArchive().toString())
        ),
        Optional.empty(),
        "error-archive",
        List.of()
      );
    }
  }

  private void loadOwnership()
    throws PrException
  {
    this.ownership.clear();
    this.ownershipDefault = null;

    final var tracker =
      new ExceptionTracker<PrException>();

    final var ownerFile = this.configuration.ownershipFile();
    try (final var stream = Files.lines(ownerFile)) {
      final var lines =
        stream.map(String::trim)
          .filter(s -> !s.startsWith("#"))
          .filter(s -> !s.isEmpty())
          .toList();

      for (final var line : lines) {
        final var parts = List.of(line.split("\\s+"));
        if (parts.size() != 5) {
          throw new PrException(
            "Unparseable ownership line.",
            Map.ofEntries(
              Map.entry("Line", line),
              Map.entry("File", ownerFile.toString())
            ),
            "error-ownership"
          );
        }

        final var name =
          parts.get(0);
        final var user =
          parts.get(1);
        final var group =
          parts.get(2);
        final var file =
          Integer.parseUnsignedInt(parts.get(3), 8);
        final var directory =
          Integer.parseUnsignedInt(parts.get(4), 8);

        final var userId =
          this.users.usersByName()
            .get(user);
        final var groupId =
          this.users.groupsByName()
            .get(group);

        if (userId == null) {
          throw new PrException(
            "Nonexistent user.",
            Map.ofEntries(
              Map.entry("User", user),
              Map.entry("File", ownerFile.toString())
            ),
            "error-user"
          );
        }

        if (groupId == null) {
          throw new PrException(
            "Nonexistent group.",
            Map.ofEntries(
              Map.entry("Group", group),
              Map.entry("File", ownerFile.toString())
            ),
            "error-group"
          );
        }

        final var owner =
          new PrOwnership(name, userId, groupId, file, directory);

        if (this.ownershipDefault == null) {
          this.ownershipDefault = owner;
        }

        this.ownership.put(name, owner);
      }
    } catch (final IOException e) {
      tracker.addException(
        this.fileException(e, ownerFile, "error-ownership")
      );
    }

    if (this.ownershipDefault == null) {
      tracker.addException(
        new PrException(
          "Must specify at least one ownership entry.",
          Map.ofEntries(
            Map.entry("File", ownerFile.toString())
          ),
          "error-ownership"
        )
      );
    }

    tracker.throwIfNecessary();
  }

  private void loadUsers()
    throws PrException
  {
    this.userIds.clear();
    this.groupIds.clear();

    final var usersProps =
      new Properties();
    final var groupsProps =
      new Properties();
    final var membersProps =
      new Properties();

    final var tracker =
      new ExceptionTracker<PrException>();

    final var usersFile =
      this.parseUserProps(usersProps, tracker);
    final var groupsFile =
      this.parseGroupProps(groupsProps, tracker);
    final var membersFile =
      this.parseMembersProps(membersProps, tracker);

    final var builder = PrUsers.builder();
    this.buildUsers(usersProps, builder, tracker, usersFile);
    this.buildGroups(groupsProps, builder, tracker, groupsFile);
    this.buildMembers(builder, membersProps, tracker, membersFile);

    tracker.throwIfNecessary();
    this.users = builder.build();

    for (final var user : this.users.users()) {
      this.userIds.put(
        user.name(),
        new SimpleScalar(user.userId().toString())
      );
    }
    for (final var group : this.users.groups()) {
      this.groupIds.put(
        group.name(),
        new SimpleScalar(group.groupId().toString())
      );
    }
  }

  private void buildMembers(
    final PrUsers.Builder builder,
    final Properties membersProps,
    final ExceptionTracker<PrException> tracker,
    final Path groupsFile)
  {
    final var map0 = builder.build();
    for (final var groupName : membersProps.stringPropertyNames()) {
      try {
        final var userNames =
          Arrays.stream(
              JProperties.getString(membersProps, groupName)
                .split("\\s+")
            )
            .map(String::trim)
            .toList();

        final var groupId =
          map0.groupsByName().get(groupName);
        if (groupId == null) {
          tracker.addException(
            new PrException(
              "Nonexistent group.",
              Map.ofEntries(
                Map.entry("Group", groupName)
              ),
              "error-integrity"
            )
          );
          continue;
        }

        for (final var user : userNames) {
          final var userId = map0.usersByName().get(user);
          if (userId == null) {
            tracker.addException(
              new PrException(
                "Nonexistent user specified in group.",
                Map.ofEntries(
                  Map.entry("Group", groupName),
                  Map.entry("User", user)
                ),
                "error-integrity"
              )
            );
            continue;
          }
          builder.addUserGroups(new PrUserGroupAssociation(userId, groupId));
        }
      } catch (final JPropertyNonexistent e) {
        tracker.addException(
          this.propertyNonexistentException(e, groupsFile, "error-group")
        );
      }
    }
  }

  private void buildGroups(
    final Properties groupsProps,
    final PrUsers.Builder builder,
    final ExceptionTracker<PrException> tracker,
    final Path groupsFile)
  {
    for (final var groupName : groupsProps.stringPropertyNames()) {
      try {
        builder.addGroups(
          new PrGroupIdentifier(
            JProperties.getBigInteger(groupsProps, groupName),
            groupName
          )
        );
      } catch (final JPropertyNonexistent e) {
        tracker.addException(
          this.propertyNonexistentException(e, groupsFile, "error-group")
        );
      } catch (final JPropertyIncorrectType e) {
        tracker.addException(
          this.propertyTypeException(e, groupsFile, "error-group")
        );
      }
    }
  }

  private void buildUsers(
    final Properties usersProps,
    final PrUsers.Builder builder,
    final ExceptionTracker<PrException> tracker,
    final Path usersFile)
  {
    for (final var userName : usersProps.stringPropertyNames()) {
      try {
        builder.addUsers(
          new PrUserIdentifier(
            JProperties.getBigInteger(usersProps, userName),
            userName
          )
        );
      } catch (final JPropertyNonexistent e) {
        tracker.addException(
          this.propertyNonexistentException(e, usersFile, "error-user")
        );
      } catch (final JPropertyIncorrectType e) {
        tracker.addException(
          this.propertyTypeException(e, usersFile, "error-user")
        );
      }
    }
  }

  private Path parseMembersProps(
    final Properties membersProps,
    final ExceptionTracker<PrException> tracker)
  {
    final var membersFile = this.configuration.groupMembersFile();
    try (final var stream = Files.newInputStream(membersFile)) {
      membersProps.load(stream);
    } catch (final IOException e) {
      tracker.addException(
        this.fileException(e, membersFile, "error-members")
      );
    }
    return membersFile;
  }

  private Path parseGroupProps(
    final Properties groupsProps,
    final ExceptionTracker<PrException> tracker)
  {
    final var groupsFile = this.configuration.groupsFile();
    try (final var stream = Files.newInputStream(groupsFile)) {
      groupsProps.load(stream);
    } catch (final IOException e) {
      tracker.addException(
        this.fileException(e, groupsFile, "error-groups")
      );
    }
    return groupsFile;
  }

  private Path parseUserProps(
    final Properties usersProps,
    final ExceptionTracker<PrException> tracker)
  {
    final var usersFile = this.configuration.usersFile();
    try (final var stream = Files.newInputStream(usersFile)) {
      usersProps.load(stream);
    } catch (final IOException e) {
      tracker.addException(
        this.fileException(e, usersFile, "error-users")
      );
    }
    return usersFile;
  }

  private PrException propertyNonexistentException(
    final JPropertyNonexistent e,
    final Path file,
    final String errorCode)
  {
    return new PrException(
      e.getMessage(),
      e,
      Map.ofEntries(
        Map.entry("File", file.toString())
      ),
      Optional.empty(),
      errorCode,
      List.of()
    );
  }

  private PrException propertyTypeException(
    final JPropertyIncorrectType e,
    final Path file,
    final String errorCode)
  {
    return new PrException(
      e.getMessage(),
      e,
      Map.ofEntries(
        Map.entry("File", file.toString())
      ),
      Optional.empty(),
      errorCode,
      List.of()
    );
  }

  private PrException fileException(
    final IOException e,
    final Path file,
    final String errorCode)
  {
    return new PrException(
      e.getMessage(),
      e,
      Map.ofEntries(
        Map.entry("File", file.toString())
      ),
      Optional.empty(),
      errorCode,
      List.of()
    );
  }

  private void processFiles()
    throws PrException
  {
    final var tracker =
      new ExceptionTracker<PrException>();

    try (final var inputStream = Files.walk(this.configuration.inputDirectory())) {
      final var inputFiles = inputStream.toList();

      for (final var inputFile : inputFiles) {
        try {
          this.processFile(inputFile);
        } catch (final PrException e) {
          tracker.addException(e);
        } catch (final Exception e) {
          tracker.addException(
            new PrException(e.getMessage(), e, "error-exception")
          );
        }
      }
    } catch (final Exception e) {
      tracker.addException(
        new PrException(e.getMessage(), e, "error-exception")
      );
    }

    tracker.throwIfNecessary();
  }

  private void processFile(
    final Path inputFile)
    throws PrException
  {
    final var inputRelative =
      this.configuration.inputDirectory()
        .relativize(inputFile);

    LOG.debug("Process {}", inputFile);

    final var outputFile =
      this.configuration.outputDirectory()
        .resolve(inputRelative);

    try {
      if (Files.isDirectory(inputFile)) {
        processFileCreateDirectory(outputFile);
        return;
      }

      if (inputFile.getFileName().toString().endsWith(".primrose")) {
        this.processFileTemplate(inputFile, outputFile);
        return;
      }

      this.processFileCopy(inputFile, outputFile);
    } catch (final IOException e) {
      throw new PrException(
        e.getMessage(),
        e,
        Map.ofEntries(
          Map.entry("InputFile", inputFile.toString()),
          Map.entry("OutputFile", outputFile.toString())
        ),
        Optional.empty(),
        "error-process",
        List.of()
      );
    }
  }

  private void processFileCopy(
    final Path inputFile,
    final Path outputFile)
    throws IOException
  {
    LOG.debug("Copy {} -> {}", inputFile, outputFile);

    Files.copy(
      inputFile,
      outputFile,
      StandardCopyOption.REPLACE_EXISTING
    );
  }

  private void processFileTemplate(
    final Path inputFile,
    final Path outputFile)
    throws IOException, PrException
  {
    final var filesystem =
      outputFile.getFileSystem();
    final var outputWithoutExtension =
      FilenameUtils.removeExtension(outputFile.toString());
    final var outputPath =
      filesystem.getPath(outputWithoutExtension);

    LOG.debug("ProcessTemplate {} -> {}", inputFile, outputPath);

    final var cfg = new Configuration(Configuration.VERSION_2_3_34);
    cfg.setDirectoryForTemplateLoading(inputFile.getParent().toFile());
    final var template = cfg.getTemplate(inputFile.getFileName().toString());

    final var templateData = new PrMap();
    templateData.put("Data", this.data.data());
    templateData.put("UserID", this.userIds);
    templateData.put("GroupID", this.groupIds);

    try (final var writer = Files.newBufferedWriter(outputPath)) {
      template.process(templateData, writer);
    } catch (final TemplateException e) {
      throw new PrException(
        e.getMessage(),
        e,
        Map.ofEntries(
          Map.entry("InputFile", inputFile.toString()),
          Map.entry("OutputFile", outputFile.toString())
        ),
        "error-template"
      );
    }
  }

  private void loadValues()
    throws PrException
  {
    this.data = PrValueData.open(this.configuration.valuesFile());
  }
}
