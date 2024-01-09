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
package io.github.mzmine.modules.dataprocessing.featdet_extract_mz_ranges;

import com.google.common.collect.Range;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess;
import io.github.mzmine.datamodel.data_access.EfficientDataAccess.ScanDataType;
import io.github.mzmine.datamodel.featuredata.impl.BuildingIonSeries;
import io.github.mzmine.datamodel.featuredata.impl.BuildingIonSeries.IntensityMode;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.logging.Logger;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * Task to extrat {@link BuildingIonSeries} that can be turned into IonTimeSeries etc.
 */
public class ExtractMzRangesIonSeriesTask extends AbstractTask {

  private static final Logger logger = Logger.getLogger(
      ExtractMzRangesIonSeriesTask.class.getName());
  private final RawDataFile dataFile;
  private final ScanSelection scanSelection;
  private final MZTolerance mzTol;
  private final List<Range<Double>> mzRanges;
  private final AbstractTask parentTask;
  private int processedScans, totalScans;
  private BuildingIonSeries[] ionSeries;

  public ExtractMzRangesIonSeriesTask(@NotNull RawDataFile dataFile,
      @NotNull ScanSelection scanSelection, @NotNull MZTolerance mzTol,
      @NotNull List<Range<Double>> mzRanges, @Nullable AbstractTask parentTask) {
    super(null, Instant.now());

    this.dataFile = dataFile;
    this.scanSelection = scanSelection;
    this.mzTol = mzTol;
    this.mzRanges = mzRanges;
    this.parentTask = parentTask;
  }

  @Override
  public double getFinishedPercentage() {
    return totalScans == 0 ? 0 : (double) processedScans / totalScans;
  }

  @Override
  public String getTaskDescription() {
    return "Extracting mz ranges";
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    if (mzRanges.isEmpty()) {
      setStatus(TaskStatus.FINISHED);
      logger.info("No mz ranges selected to extract");
      return;
    }

    var scans = List.of(scanSelection.getMatchingScans(dataFile));

    totalScans = scans.size();

    // No scans in selection range.
    if (totalScans == 0) {
      setStatus(TaskStatus.ERROR);
      final String msg = "No scans detected in scan selection for " + dataFile.getName();
      setErrorMessage(msg);
      return;
    }

    mzRanges.sort(Comparator.comparingDouble(Range::lowerEndpoint));

    ionSeries = extractIonSeries();

    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Will call the run on this thread if the task was still waiting
   *
   * @return
   */
  public BuildingIonSeries[] getResultingIonSeries() {
    if (getStatus() == TaskStatus.WAITING) {
      run();
    }
    return ionSeries;
  }

  public BuildingIonSeries[] extractIonSeries() {
    var dataAccess = EfficientDataAccess.of(dataFile, ScanDataType.MASS_LIST, scanSelection);
    // store data points for each range
    BuildingIonSeries[] chromatograms = new BuildingIonSeries[mzRanges.size()];
    for (int i = 0; i < chromatograms.length; i++) {
      chromatograms[i] = new BuildingIonSeries(dataAccess.getNumberOfScans(),
          IntensityMode.HIGHEST);
    }

    int currentScan = -1;
    while (dataAccess.nextScan() != null) {
      int currentTree = 0;
      currentScan++;
      processedScans++;

      // Canceled?
      if (isCanceled() || (parentTask != null && parentTask.isCanceled())) {
        return null;
      }
      // check value for tree and for all next trees in range
      int nDataPoints = dataAccess.getNumberOfDataPoints();
      for (int dp = 0; dp < nDataPoints; dp++) {
        double mz = dataAccess.getMzValue(dp);
        // all next trees
        for (int t = currentTree; t < mzRanges.size(); t++) {
          if (mz > mzRanges.get(t).upperEndpoint()) {
            // out of bounds for current tree
            currentTree++;
          } else if (mz < mzRanges.get(t).lowerEndpoint()) {
            break;
          } else {
            // found match
            double intensity = dataAccess.getIntensityValue(dp);
            chromatograms[t].addValue(currentScan, mz, intensity);
          }
        }
        // all trees done
        if (currentTree >= mzRanges.size()) {
          break;
        }
      }
    }
    return chromatograms;
  }

}
