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


package com.io7m.primrose.cmdline.internal;

import com.io7m.primrose.core.PrConfiguration;
import com.io7m.primrose.core.PrException;
import com.io7m.primrose.core.PrProcessor;
import com.io7m.quarrel.core.QCommandContextType;
import com.io7m.quarrel.core.QCommandMetadata;
import com.io7m.quarrel.core.QCommandStatus;
import com.io7m.quarrel.core.QCommandType;
import com.io7m.quarrel.core.QParameterNamed1;
import com.io7m.quarrel.core.QParameterNamedType;
import com.io7m.quarrel.core.QStringType;
import com.io7m.quarrel.ext.logback.QLogback;

import java.nio.file.Path;
import java.util.List;
import java.util.Optional;

/**
 * The compile command.
 */

public final class PrCmdCompile implements QCommandType
{
  private static final QParameterNamed1<Path> INPUT_DIRECTORY =
    new QParameterNamed1<>(
      "--input-directory",
      List.of(),
      new QStringType.QConstant("The input directory."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<Path> OUTPUT_DIRECTORY =
    new QParameterNamed1<>(
      "--output-directory",
      List.of(),
      new QStringType.QConstant("The output directory."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<Path> OUTPUT_ARCHIVE =
    new QParameterNamed1<>(
      "--output-archive",
      List.of(),
      new QStringType.QConstant("The output archive."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<Path> USERS_FILE =
    new QParameterNamed1<>(
      "--users",
      List.of(),
      new QStringType.QConstant("The users file."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<Path> GROUPS_FILE =
    new QParameterNamed1<>(
      "--groups",
      List.of(),
      new QStringType.QConstant("The groups file."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<Path> GROUP_MEMBERS_FILE =
    new QParameterNamed1<>(
      "--group-members",
      List.of(),
      new QStringType.QConstant("The group members file."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<Path> VALUES_FILE =
    new QParameterNamed1<>(
      "--values",
      List.of(),
      new QStringType.QConstant("The values file."),
      Optional.empty(),
      Path.class
    );

  private static final QParameterNamed1<Path> OWNERSHIP_FILE =
    new QParameterNamed1<>(
      "--ownership",
      List.of(),
      new QStringType.QConstant("The ownership file."),
      Optional.empty(),
      Path.class
    );

  private final QCommandMetadata metadata;

  /**
   * The compile command.
   */

  public PrCmdCompile()
  {
    this.metadata =
      new QCommandMetadata(
        "compile",
        new QStringType.QConstant("Compile the configuration."),
        Optional.empty()
      );
  }

  @Override
  public List<QParameterNamedType<?>> onListNamedParameters()
  {
    return QLogback.plusParameters(
      List.of(
        GROUPS_FILE,
        GROUP_MEMBERS_FILE,
        INPUT_DIRECTORY,
        OUTPUT_ARCHIVE,
        OUTPUT_DIRECTORY,
        OWNERSHIP_FILE,
        USERS_FILE,
        VALUES_FILE
      )
    );
  }

  @Override
  public QCommandStatus onExecute(
    final QCommandContextType context)
    throws PrException
  {
    QLogback.configure(context);

    final var pr =
      PrProcessor.create(
        PrConfiguration.builder()
          .setOwnershipFile(context.parameterValue(OWNERSHIP_FILE))
          .setGroupMembersFile(context.parameterValue(GROUP_MEMBERS_FILE))
          .setGroupsFile(context.parameterValue(GROUPS_FILE))
          .setUsersFile(context.parameterValue(USERS_FILE))
          .setValuesFile(context.parameterValue(VALUES_FILE))
          .setInputDirectory(context.parameterValue(INPUT_DIRECTORY))
          .setOutputDirectory(context.parameterValue(OUTPUT_DIRECTORY))
          .setOutputArchive(context.parameterValue(OUTPUT_ARCHIVE))
          .build()
      );

    pr.run();
    return QCommandStatus.SUCCESS;
  }

  @Override
  public QCommandMetadata metadata()
  {
    return this.metadata;
  }
}
