/*
 * Copyright 2006-2021 The MZmine Development Team
 *
 * This file is part of MZmine.
 *
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 *
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even
 * the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA.
 *
 */

package io.github.mzmine.modules.io.export_features_metaboanalyst;

import io.github.mzmine.datamodel.FeatureIdentity;
import io.github.mzmine.datamodel.features.Feature;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.time.Instant;
import java.util.Arrays;
import java.util.Date;
import java.util.regex.Pattern;
import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.parameters.UserParameter;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import org.jetbrains.annotations.NotNull;

class MetaboAnalystExportTask extends AbstractTask {

  private static final String fieldSeparator = ",";

  private final MZmineProject project;
  private final FeatureList[] featureLists;
  private String plNamePattern = "{}";
  private int processedRows = 0, totalRows = 0;

  // parameter values
  private File fileName;
  private UserParameter<?, ?> groupParameter;

  MetaboAnalystExportTask(MZmineProject project, ParameterSet parameters, @NotNull Instant moduleCallDate) {
    super(null, moduleCallDate); // no new data stored -> null

    this.project = project;
    this.featureLists = parameters.getParameter(MetaboAnalystExportParameters.featureLists).getValue()
        .getMatchingFeatureLists();

    fileName = parameters.getParameter(MetaboAnalystExportParameters.filename).getValue();
    groupParameter =
        parameters.getParameter(MetaboAnalystExportParameters.groupParameter).getValue();

  }

  @Override
  public double getFinishedPercentage() {
    if (totalRows == 0) {
      return 0;
    }
    return (double) processedRows / (double) totalRows;
  }

  @Override
  public String getTaskDescription() {
    return "Exporting feature list(s) " + Arrays.toString(featureLists)
        + " to MetaboAnalyst CSV file(s)";
  }

  @Override
  public void run() {

    setStatus(TaskStatus.PROCESSING);

    // Shall export several files?
    boolean substitute = fileName.getPath().contains(plNamePattern);

    // Total number of rows
    for (FeatureList featureList : featureLists) {
      totalRows += featureList.getNumberOfRows();
    }

    // Process feature lists
    for (FeatureList featureList : featureLists) {

      // Filename
      File curFile = fileName;
      if (substitute) {
        // Cleanup from illegal filename characters
        String cleanPlName = featureList.getName().replaceAll("[^a-zA-Z0-9.-]", "_");
        // Substitute
        String newFilename =
            fileName.getPath().replaceAll(Pattern.quote(plNamePattern), cleanPlName);
        curFile = new File(newFilename);
      }

      // Check the feature list for MetaboAnalyst requirements
      boolean checkResult = checkFeatureList(featureList);
      if (checkResult == false) {
        MZmineCore.getDesktop().displayErrorMessage("Feature list " + featureList.getName()
            + " does not conform to MetaboAnalyst requirement: at least 3 samples (raw data files) in each group");
      }

      try {

        // Open file
        FileWriter writer = new FileWriter(curFile);

        // Get number of rows
        totalRows = featureList.getNumberOfRows();

        exportFeatureList(featureList, writer);

        // Close file
        writer.close();

      } catch (Exception e) {
        e.printStackTrace();
        setStatus(TaskStatus.ERROR);
        setErrorMessage("Could not export feature list to file " + curFile + ": " + e.getMessage());
        return;
      }
    }

    if (getStatus() == TaskStatus.PROCESSING)
      setStatus(TaskStatus.FINISHED);

  }

  private boolean checkFeatureList(FeatureList featureList) {

    // Check if each sample group has at least 3 samples
    final RawDataFile rawDataFiles[] = featureList.getRawDataFiles().toArray(RawDataFile[]::new);
    for (RawDataFile file : rawDataFiles) {
      final String fileValue = String.valueOf(project.getParameterValue(groupParameter, file));
      int count = 0;
      for (RawDataFile countFile : rawDataFiles) {
        final String countValue =
            String.valueOf(project.getParameterValue(groupParameter, countFile));
        if (countValue.equals(fileValue))
          count++;
      }
      if (count < 3)
        return false;
    }
    return true;
  }

  private void exportFeatureList(FeatureList featureList, FileWriter writer) throws IOException {

    final RawDataFile rawDataFiles[] = featureList.getRawDataFiles().toArray(RawDataFile[]::new);

    // Buffer for writing
    StringBuffer line = new StringBuffer();

    // Write sample (raw data file) names
    line.append("\"Sample\"");
    for (RawDataFile file : rawDataFiles) {
      // Cancel?
      if (isCanceled()) {
        return;
      }

      line.append(fieldSeparator);
      final String value = file.getName().replace('"', '\'');
      line.append("\"");
      line.append(value);
      line.append("\"");
    }

    line.append("\n");

    // Write grouping parameter values
    line.append("\"");
    line.append(groupParameter.getName().replace('"', '\''));
    line.append("\"");

    for (RawDataFile file : rawDataFiles) {

      // Cancel?
      if (isCanceled()) {
        return;
      }

      line.append(fieldSeparator);
      String value = String.valueOf(project.getParameterValue(groupParameter, file));
      value = value.replace('"', '\'');
      line.append("\"");
      line.append(value);
      line.append("\"");
    }

    line.append("\n");
    writer.write(line.toString());

    // Write data rows
    for (FeatureListRow featureListRow : featureList.getRows()) {

      // Cancel?
      if (isCanceled()) {
        return;
      }

      // Reset the buffer
      line.setLength(0);

      final String rowName = generateUniqueFeatureListRowName(featureListRow);

      line.append("\"" + rowName + "\"");

      for (RawDataFile dataFile : rawDataFiles) {
        line.append(fieldSeparator);

        Feature feature = featureListRow.getFeature(dataFile);
        if (feature != null) {
          final double area = feature.getArea();
          line.append(String.valueOf(area));
        }
      }

      line.append("\n");
      writer.write(line.toString());

      processedRows++;
    }
  }

  /**
   * Generates a unique name for each feature list row
   */
  private String generateUniqueFeatureListRowName(FeatureListRow row) {

    final double mz = row.getAverageMZ();
    final double rt = row.getAverageRT();
    final int rowId = row.getID();

    String generatedName = rowId + "/" + MZmineCore.getConfiguration().getMZFormat().format(mz)
        + "mz/" + MZmineCore.getConfiguration().getRTFormat().format(rt) + "min";
    FeatureIdentity featureIdentity = row.getPreferredFeatureIdentity();

    if (featureIdentity == null)
      return generatedName;

    String idName = featureIdentity.getPropertyValue(FeatureIdentity.PROPERTY_NAME);

    if (idName == null)
      return generatedName;

    idName = idName.replace('"', '\'');
    generatedName = generatedName + " (" + idName + ")";

    return generatedName;

  }

}
