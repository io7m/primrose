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

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.BooleanNode;
import com.fasterxml.jackson.databind.node.NumericNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.fasterxml.jackson.databind.node.TextNode;
import com.io7m.primrose.core.PrException;
import freemarker.template.SimpleScalar;
import freemarker.template.TemplateModel;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

import static com.fasterxml.jackson.core.JsonParser.Feature.ALLOW_COMMENTS;

/**
 * The value data.
 */

public final class PrValueData
{
  private final TemplateModel data;

  private PrValueData(
    final TemplateModel inData)
  {
    this.data = Objects.requireNonNull(inData, "data");
  }

  /**
   * @return The template data
   */

  public TemplateModel data()
  {
    return this.data;
  }

  /**
   * Open the value data from the file.
   *
   * @param file The file
   *
   * @return The value data
   *
   * @throws PrException On errors
   */

  public static PrValueData open(
    final Path file)
    throws PrException
  {
    try {
      final var mapper =
        JsonMapper.builder()
          .enable(ALLOW_COMMENTS)
          .build();

      final ObjectNode tree;
      try (final var stream = Files.newInputStream(file)) {
        tree = (ObjectNode) mapper.readTree(stream);
      }

      return processTree(tree);
    } catch (final Exception e) {
      throw new PrException(
        e.getMessage(),
        e,
        Map.ofEntries(
          Map.entry("File", file.toString())
        ),
        Optional.empty(),
        "error-json",
        List.of()
      );
    }
  }

  private static PrValueData processTree(
    final ObjectNode tree)
  {
    return new PrValueData(processNode(tree));
  }

  private static TemplateModel processNode(
    final JsonNode node)
  {
    return switch (node) {
      case final ObjectNode tree -> {
        final var m = new PrMap();
        final var names = tree.fieldNames();
        while (names.hasNext()) {
          final var name = names.next();
          m.put(name, processNode(tree.get(name)));
        }
        yield m;
      }
      case final ArrayNode array -> {
        final var m = new PrList();
        for (int index = 0; index < array.size(); ++index) {
          m.put(
            index,
            processNode(array.get(index))
          );
        }
        yield m;
      }
      case final TextNode text -> {
        yield new SimpleScalar(text.textValue());
      }
      case final NumericNode numeric -> {
        yield new SimpleScalar(numeric.asText());
      }
      case final BooleanNode bool -> {
        yield new SimpleScalar(bool.asText());
      }
      default -> {
        throw new IllegalStateException("Unexpected value: " + node);
      }
    };
  }
}
