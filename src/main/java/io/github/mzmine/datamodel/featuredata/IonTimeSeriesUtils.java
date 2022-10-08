/*
 *  Copyright 2006-2022 The MZmine Development Team
 *
 *  This file is part of MZmine.
 *
 *  MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 *  General Public License as published by the Free Software Foundation; either version 2 of the
 *  License, or (at your option) any later version.
 *
 *  MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 *  the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General
 *  Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License along with MZmine; if not,
 *  write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 *  USA
 */

package io.github.mzmine.datamodel.featuredata;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.data_access.ScanDataAccess;
import io.github.mzmine.datamodel.featuredata.impl.SimpleIonTimeSeries;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.util.DataPointUtils;
import io.github.mzmine.util.MathUtils;
import io.github.mzmine.util.MemoryMapStorage;
import io.github.mzmine.util.RangeUtils;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class IonTimeSeriesUtils {

  private IonTimeSeriesUtils() {

  }

  /**
   * Remaps the values of the given series onto another set of scans to gain access to all RT
   * values, for example if the whole chromatogram including 0 intensities is required. This should
   * only be used for preview purposes, since buffers created by this method are not reused. If a
   * set of features is processed, a
   * {@link io.github.mzmine.datamodel.data_access.FeatureDataAccess} should be used. If access to
   * all intensity and rt values including zero intensities is required in array form,
   * {@link this#remapRtAxis(IonTimeSeries, List, double[], double[], double[])} can be used
   * instead.
   * <p></p>
   * Note that the use of this method removes the mobility dimension of a
   * {@link IonMobilogramTimeSeries}.
   *
   * @param series   The series.
   * @param newScans The scans. Have to contain all scans in the series.
   * @return A {@link SimpleIonTimeSeries} with the new rt values.
   * @throws IllegalStateException If newScans did not contain all scans in the series.
   */
  public static IonTimeSeries<Scan> remapRtAxis(@NotNull final IonTimeSeries<? extends Scan> series,
      @NotNull final List<? extends Scan> newScans) {
    final double[] newIntensities = new double[newScans.size()];
    final double[] newMzs = new double[newScans.size()];
    final double[] newRts = new double[newScans.size()];

    remapRtAxis(series, newScans, newRts, newIntensities, newMzs);

    return new SimpleIonTimeSeries(null, newMzs, newIntensities, (List<Scan>) newScans);
  }

  /**
   * Remaps the intensity, m/z and rt values of a feature onto a new set of scans, for example if
   * the whole chromatogram including 0 intensities is required. This method can be used in case
   * access to all rt and intensity values is required in array form. (e.g. for resolving or
   * smoothing).
   *
   * @param series             The series.
   * @param newScans           The new set of scans. Must contain all scans in the series.
   * @param outRtBuffer        A buffer to write the new rt values to. May not be null.
   * @param outIntensityBuffer A buffer to write the new intensity values to. May not be null.
   * @param outMzBuffer        A buffer to write m/z values to. May be null. A weighted average of
   *                           all existing m/z values will be used as a placeholder where intensity
   *                           == 0.
   * @throws IllegalStateException If newScans did not contain all scans in the series.
   */
  public static void remapRtAxis(@NotNull final IonTimeSeries<? extends Scan> series,
      @NotNull final List<? extends Scan> newScans, @NotNull final double[] outRtBuffer,
      @NotNull final double[] outIntensityBuffer, @Nullable final double[] outMzBuffer) {

    assert outIntensityBuffer.length == outRtBuffer.length;
    assert newScans.size() == outIntensityBuffer.length;
    assert outMzBuffer == null || outMzBuffer.length == outIntensityBuffer.length;
    assert series.getNumberOfValues() <= newScans.size();

    if (outMzBuffer != null) {
      final double avgMz = MathUtils.calcAvg(
          DataPointUtils.getDoubleBufferAsArray(series.getMZValueBuffer()));
      Arrays.fill(outMzBuffer, avgMz);
    }

    int seriesIndex = 0;

    for (int i = 0; i < newScans.size() && seriesIndex < series.getNumberOfValues(); i++) {
      outRtBuffer[i] = newScans.get(i).getRetentionTime();

      if (series.getSpectrum(seriesIndex).equals(newScans.get(i))) {
        outIntensityBuffer[i] = series.getIntensity(seriesIndex);
        if (outMzBuffer != null) {
          outMzBuffer[i] = series.getMZ(seriesIndex);
        }
        seriesIndex++;
      } else {
        outIntensityBuffer[i] = 0d;
      }
    }

    if (seriesIndex < series.getNumberOfValues()) {
      throw new IllegalStateException(
          "Incomplete rt remap. newScans did not contain all scans in the series.");
    }
  }

  public static <T extends IonTimeSeries<? extends Scan>> T normalizeToAvgTic(T series,
      @Nullable final MemoryMapStorage storage) {
    final List<? extends Scan> scans = series.getSpectra();
    final double[] intensities = new double[scans.size()];
    final double[] mzs = new double[scans.size()];

    final double avgTic = scans.stream().mapToDouble(Scan::getTIC).average()
        .orElseThrow(() -> new IllegalStateException("Cannot determine average TIC"));

    for (int i = 0; i < series.getNumberOfValues(); i++) {
      intensities[i] = series.getIntensity(i) / scans.get(i).getTIC() * avgTic;
    }

    series.getMzValues(mzs);

    return (T) series.copyAndReplace(storage, mzs, intensities);
  }

  /**
   * Extracts an extracted ion chromatogram from the given raw file. The peak closest to the center
   * of the given mz range will be used for every scan.
   * <p></p>
   * <b>Note</b>  that a new {@link ScanDataAccess} will be created on every call. If multiple
   * chromatograms are to be created, use
   * {@link IonTimeSeriesUtils#extractIonTimeSeries(ScanDataAccess, Range, Range, MemoryMapStorage)}
   * instead.
   *
   * @return A chromatogram across the whole RT range of the scan selection.
   */
  public static IonTimeSeries<Scan> extractIonTimeSeries(@NotNull RawDataFile file,
      @NotNull ScanSelection selection, @NotNull Range<Double> mzRange,
      @Nullable MemoryMapStorage storage) {
    final ScanDataAccess access = EfficientDataAccess.of(file, ScanDataType.CENTROID, selection);
    return extractIonTimeSeries(access, mzRange, null, storage);
  }

  /**
   * @see IonTimeSeriesUtils#extractIonTimeSeries(ScanDataAccess, Range, Range, MemoryMapStorage)
   */
  public static IonTimeSeries<Scan> extractIonTimeSeries(@NotNull RawDataFile file,
      @NotNull List<Scan> scans, @NotNull Range<Double> mzRange, @Nullable Range<Float> rtRange,
      @Nullable MemoryMapStorage storage) {
    final ScanDataAccess access = EfficientDataAccess.of(file, ScanDataType.CENTROID, scans);
    return extractIonTimeSeries(access, mzRange, rtRange, storage);
  }

  /**
   * Extracts an extracted ion chromatogram from the given raw file. The peak closest to the center
   * of the given mz range will be used for every scan in the given rtRange.
   *
   * @param access  THe scan data access.
   * @param mzRange The range of the m/z window. the m/z closest to the center will be used.
   * @param rtRange if null, all scans in the data access will be used.
   * @param storage The memory map storage.
   * @return A chromatogram.
   */
  public static IonTimeSeries<Scan> extractIonTimeSeries(@NotNull ScanDataAccess access,
      @NotNull Range<Double> mzRange, @Nullable Range<Float> rtRange,
      @Nullable MemoryMapStorage storage) {

    final double[] mzs = new double[access.getNumberOfScans()];
    final double[] intensities = new double[access.getNumberOfScans()];
    final List<Scan> scans = new ArrayList<>();

    access.reset();
    final double centerMz = RangeUtils.rangeCenter(mzRange);

    int i = 0;

    while (access.hasNextScan()) {
      final Scan scan = access.nextScan();

      if (rtRange != null && !rtRange.contains(scan.getRetentionTime())) {
        continue;
      }

      scans.add(scan);

      final int closestPeakIndex = access.binarySearch(centerMz, true);
      final double mz = access.getMzValue(closestPeakIndex);

      if (mzRange.contains(mz)) {
        mzs[i] = mz;
        intensities[i] = access.getIntensityValue(closestPeakIndex);
      } else {
        mzs[i] = 0;
        intensities[i] = 0;
      }

      i++;
    }

    return new SimpleIonTimeSeries(storage, mzs, intensities, scans);
  }


}
