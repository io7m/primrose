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

import freemarker.template.TemplateModel;
import freemarker.template.TemplateSequenceModel;

import java.util.ArrayList;

/**
 * A list in template form.
 */

public final class PrList implements TemplateSequenceModel
{
  private final ArrayList<TemplateModel> items =
    new ArrayList<>();

  /**
   * A list in template form.
   */

  public PrList()
  {

  }

  /**
   * Add a value to the list.
   *
   * @param index The index
   * @param value The value
   */

  public void put(
    final int index,
    final TemplateModel value)
  {
    if (index < this.items.size()) {
      this.items.set(index, value);
    } else {
      this.items.add(index, value);
    }
  }

  /**
   * Clear the list.
   */

  public void clear()
  {
    this.items.clear();
  }

  @Override
  public TemplateModel get(
    final int index)
  {
    return this.items.get(index);
  }

  @Override
  public int size()
  {
    return this.items.size();
  }
}
