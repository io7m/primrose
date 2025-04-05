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

import com.io7m.seltzer.api.SStructuredError;
import com.io7m.seltzer.api.SStructuredErrorExceptionType;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

/**
 * The type of exceptions raised by the API.
 */

public final class PrException
  extends Exception
  implements SStructuredErrorExceptionType<String>
{
  private final Map<String, String> attributes;
  private final Optional<String> remediatingAction;
  private final String errorCode;
  private final List<SStructuredError<String>> extraErrors;

  /**
   * Construct an exception.
   *
   * @param message             The message
   * @param inAttributes        The attributes
   * @param inRemediatingAction The remediating action
   * @param inErrorCode         The error code
   * @param inExtraErrors       Extra errors
   */

  public PrException(
    final String message,
    final Map<String, String> inAttributes,
    final Optional<String> inRemediatingAction,
    final String inErrorCode,
    final List<SStructuredError<String>> inExtraErrors)
  {
    super(Objects.requireNonNull(message, "message"));
    this.attributes =
      Objects.requireNonNull(inAttributes, "attributes");
    this.remediatingAction =
      Objects.requireNonNull(inRemediatingAction, "remediatingAction");
    this.errorCode =
      Objects.requireNonNull(inErrorCode, "errorCode");
    this.extraErrors =
      Objects.requireNonNull(inExtraErrors, "extraErrors");
  }

  /**
   * Construct an exception.
   *
   * @param message             The message
   * @param cause               The cause
   * @param inAttributes        The attributes
   * @param inRemediatingAction The remediating action
   * @param inErrorCode         The error code
   * @param inExtraErrors       Extra errors
   */

  public PrException(
    final String message,
    final Throwable cause,
    final Map<String, String> inAttributes,
    final Optional<String> inRemediatingAction,
    final String inErrorCode,
    final List<SStructuredError<String>> inExtraErrors)
  {
    super(
      Objects.requireNonNull(message, "message"),
      Objects.requireNonNull(cause, "cause")
    );
    this.attributes =
      Objects.requireNonNull(inAttributes, "attributes");
    this.remediatingAction =
      Objects.requireNonNull(inRemediatingAction, "remediatingAction");
    this.errorCode =
      Objects.requireNonNull(inErrorCode, "errorCode");
    this.extraErrors =
      Objects.requireNonNull(inExtraErrors, "extraErrors");
  }

  /**
   * Construct an exception.
   *
   * @param message      The message
   * @param cause        The cause
   * @param inAttributes The attributes
   * @param inErrorCode  The error code
   */

  public PrException(
    final String message,
    final Throwable cause,
    final Map<String, String> inAttributes,
    final String inErrorCode)
  {
    this(
      message,
      cause,
      inAttributes,
      Optional.empty(),
      inErrorCode,
      List.of());
  }

  /**
   * Construct an exception.
   *
   * @param message      The message
   * @param inAttributes The attributes
   * @param inErrorCode  The error code
   */

  public PrException(
    final String message,
    final Map<String, String> inAttributes,
    final String inErrorCode)
  {
    this(message, inAttributes, Optional.empty(), inErrorCode, List.of());
  }

  /**
   * Construct an exception.
   *
   * @param message     The message
   * @param cause       The cause
   * @param inErrorCode The error code
   */

  public PrException(
    final String message,
    final Throwable cause,
    final String inErrorCode)
  {
    this(message, cause, Map.of(), Optional.empty(), inErrorCode, List.of());
  }

  /**
   * Construct an exception.
   *
   * @param message     The message
   * @param inErrorCode The error code
   */

  public PrException(
    final String message,
    final String inErrorCode)
  {
    this(message, Map.of(), Optional.empty(), inErrorCode, List.of());
  }

  /**
   * Construct an exception.
   *
   * @param message       The message
   * @param inErrorCode   The error code
   * @param inExtraErrors Extra errors
   */

  public PrException(
    final String message,
    final String inErrorCode,
    final List<SStructuredError<String>> inExtraErrors)
  {
    this(message, Map.of(), Optional.empty(), inErrorCode, inExtraErrors);
  }

  /**
   * @return Any extra errors associated with the exception
   */

  public List<SStructuredError<String>> extraErrors()
  {
    return this.extraErrors;
  }

  @Override
  public String errorCode()
  {
    return this.errorCode;
  }

  @Override
  public Map<String, String> attributes()
  {
    return this.attributes;
  }

  @Override
  public Optional<String> remediatingAction()
  {
    return this.remediatingAction;
  }

  @Override
  public Optional<Throwable> exception()
  {
    return Optional.of(this);
  }
}
