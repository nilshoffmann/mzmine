/*
 * Copyright (c) 2004-2024 The MZmine Development Team
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

package io.github.mzmine.datamodel.otherdetectors;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.featuredata.IntensityTimeSeries;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.ChromatogramType;
import java.util.List;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public interface OtherDataFile {

  @NotNull
  RawDataFile getCorrespondingRawDataFile();

  default boolean hasTimeSeries() {
    return !getTimeSeries().isEmpty();
  }

  default boolean hasSpectra() {
    return !getSpectra().isEmpty();
  }

  default int getNumberOfSpectra() {
    return getSpectra().size();
  }

  default int getNumberOfTimeSeries() {
    return getTimeSeries().size();
  }

  @NotNull
  List<@NotNull OtherSpectrum> getSpectra();

  RawDataFile getRawDataFile();

  @NotNull
  IntensityTimeSeries getTimeSeries(int index);

  @NotNull
  List<@NotNull IntensityTimeSeries> getTimeSeries();

  String getSpectraDomainLabel();

  String getSpectraDomainUnit();

  String getSpectraRangeLabel();

  String getSpectraRangeUnit();

  String getTimeSeriesDomainLabel();

  String getTimeSeriesDomainUnit();

  String getTimeSeriesRangeLabel();

  String getTimeSeriesRangeUnit();

  @NotNull
  String getDescription();

  /**
   * @return The chromatograms in this data file or null if this file does not contain
   * chromatograms.
   */
  @Nullable
  ChromatogramType getChromatogramType();
}
