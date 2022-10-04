/*
 * Copyright (c) 2004-2022 The MZmine Development Team
 *
 * Permission is hereby granted, free of charge, to any person
 * obtaining a copy of this software and associated documentation
 * files (the "Software"), to deal in the Software without
 * restriction, including without limitation the rights to use,
 * copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the
 * Software is furnished to do so, subject to the following
 * conditions:
 *
 * The above copyright notice and this permission notice shall be
 * included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES
 * OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND
 * NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT
 * HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY,
 * WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING
 * FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 */

package io.github.mzmine.modules.dataanalysis.projectionplots;

import io.github.mzmine.parameters.UserParameter;

public class ColoringType {

  public static final ColoringType NOCOLORING = new ColoringType("No coloring");

  public static final ColoringType COLORBYFILE = new ColoringType("Color by file");

  private String name;
  private UserParameter<?, ?> parameter;

  public ColoringType(String name) {
    this.name = name;
  }

  public ColoringType(UserParameter<?, ?> parameter) {
    this("Color by parameter " + parameter.getName());
    this.parameter = parameter;
  }

  public boolean isByParameter() {
    return parameter != null;
  }

  public UserParameter<?, ?> getParameter() {
    return parameter;
  }

  public boolean equals(Object obj) {
    if (!(obj instanceof ColoringType))
      return false;
    return name.equals(((ColoringType) obj).name);
  }

  public String toString() {
    return name;
  }

}
