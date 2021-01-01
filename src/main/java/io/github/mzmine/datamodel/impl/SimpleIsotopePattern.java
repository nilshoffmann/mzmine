/*
 * Copyright 2006-2020 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 * Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.datamodel.impl;

import java.nio.DoubleBuffer;
import javax.annotation.Nonnull;
import com.google.common.collect.Range;
import io.github.mzmine.datamodel.DataPoint;
import io.github.mzmine.datamodel.IsotopePattern;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.util.scans.ScanUtils;

/**
 * Simple implementation of IsotopePattern interface
 */
public class SimpleIsotopePattern implements IsotopePattern {

  private double mzValues[], intensityValues[];
  private int highestIsotope;
  private IsotopePatternStatus status;
  private String description;
  private Range<Double> mzRange;
  private String[] isotopeCompostion;

  public SimpleIsotopePattern(double mzValues[], double intensityValues[],
      IsotopePatternStatus status, String description, String[] isotopeCompostion) {

    this(mzValues, intensityValues, status, description);
    this.isotopeCompostion = isotopeCompostion;
  }

  public SimpleIsotopePattern(DataPoint dataPoints[], IsotopePatternStatus status,
      String description, String[] isotopeCompostion) {

    this(dataPoints, status, description);
    this.isotopeCompostion = isotopeCompostion;
  }


  public SimpleIsotopePattern(DataPoint dataPoints[], IsotopePatternStatus status,
      String description) {

    assert mzValues.length > 0;
    assert mzValues.length == intensityValues.length;

    highestIsotope = ScanUtils.findTopDataPoint(intensityValues);
    mzValues = new double[dataPoints.length];
    intensityValues = new double[dataPoints.length];
    for (int i = 0; i < dataPoints.length; i++) {
      mzValues[i] = dataPoints[i].getMZ();
      intensityValues[i] = dataPoints[i].getIntensity();
    }
    this.status = status;
    this.description = description;
    this.mzRange = ScanUtils.findMzRange(mzValues);
  }


  public SimpleIsotopePattern(double mzValues[], double intensityValues[],
      IsotopePatternStatus status, String description) {

    assert mzValues.length > 0;
    assert mzValues.length == intensityValues.length;

    highestIsotope = ScanUtils.findTopDataPoint(intensityValues);
    this.mzValues = mzValues;
    this.intensityValues = intensityValues;
    this.status = status;
    this.description = description;
    this.mzRange = ScanUtils.findMzRange(mzValues);
  }

  @Override
  public int getNumberOfDataPoints() {
    return mzValues.length;
  }

  @Override
  public @Nonnull IsotopePatternStatus getStatus() {
    return status;
  }

  @Override
  public @Nonnull int getBasePeak() {
    return highestIsotope;
  }

  @Override
  public @Nonnull String getDescription() {
    return description;
  }

  @Override
  public String toString() {
    return "Isotope pattern: " + description;
  }

  @Override
  @Nonnull
  public Range<Double> getDataPointMZRange() {
    return mzRange;
  }

  @Override
  public double getTIC() {
    return 0;
  }

  @Override
  public MassSpectrumType getSpectrumType() {
    return MassSpectrumType.CENTROIDED;
  }

  @Override
  public DoubleBuffer getMzValues() {
    return DoubleBuffer.wrap(mzValues);
  }

  @Override
  public DoubleBuffer getIntensityValues() {
    return DoubleBuffer.wrap(intensityValues);
  }


  public String getIsotopeComposition(int num) {
    if (isotopeCompostion != null && num < isotopeCompostion.length)
      return isotopeCompostion[num];
    return "";
  }

  public String[] getIsotopeCompositions() {
    if (isotopeCompostion != null)
      return isotopeCompostion;
    return null;
  }



}
