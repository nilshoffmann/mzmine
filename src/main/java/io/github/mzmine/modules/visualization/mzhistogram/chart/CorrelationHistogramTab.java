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

package io.github.mzmine.modules.visualization.mzhistogram.chart;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.ModularFeatureList;
import io.github.mzmine.gui.chartbasics.ChartLogicsFX;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.modules.visualization.mzhistogram.ScanMzHistogramParameters;
import io.github.mzmine.parameters.ParameterSet;
import it.unimi.dsi.fastutil.doubles.DoubleArrayList;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.Priority;
import org.jetbrains.annotations.NotNull;
import org.jfree.chart.axis.ValueAxis;
import org.jfree.chart.fx.ChartViewer;
import org.jfree.chart.plot.XYPlot;
import org.jfree.data.xy.XYDataset;

/**
 * Enhanced version. Use arrows to jump to the next or previous distribution
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class CorrelationHistogramTab extends MZmineTab implements ActionListener {

  //private final Scene mainScene;
  private final BorderPane mainPane;
  private final ModularFeatureList flist;
  protected HistogramPanel histo;
  protected HistogramPanel histoDeltaNeutralMass;
  private CheckBox cbKeepSameXaxis;

  // scan counter
  private int processedScans, totalScans;

  // parameters
  private double binWidth;

  private HistogramData data;
  private HistogramData dataDeltaNeutralMass;

  public CorrelationHistogramTab(ModularFeatureList flist, DoubleArrayList deltaMZList,
      DoubleArrayList deltaMZToNeutralMassList, String title, String xLabel,
      ParameterSet parameters) {
    super(title, true, false);

    this.flist = flist;
    binWidth = parameters.getParameter(ScanMzHistogramParameters.binWidth).getValue();
    data = new HistogramData(deltaMZList.toDoubleArray());
    dataDeltaNeutralMass = new HistogramData(deltaMZToNeutralMassList.toDoubleArray());

    mainPane = new BorderPane();
    //mainScene.getStylesheets()
    //    .addAll(MZmineCore.getDesktop().getMainWindow().getScene().getStylesheets());

    histo = new HistogramPanel(xLabel, data, binWidth);

    histoDeltaNeutralMass = new HistogramPanel(xLabel + " (neutral mass)", dataDeltaNeutralMass,
        binWidth);

    //setMinWidth(1050);
    //setMinHeight(700);
    //setScene(mainScene);
    setContent(mainPane);

    GridPane gridPane = new GridPane();
    gridPane.add(histo, 0, 0);
    gridPane.add(histoDeltaNeutralMass, 1, 0);
    GridPane.setVgrow(histo, Priority.ALWAYS);
    GridPane.setVgrow(histoDeltaNeutralMass, Priority.ALWAYS);
    mainPane.setCenter(gridPane);

    // Add the Windows menu
    //WindowsMenu.addWindowsMenu(mainScene);
    addKeyBindings();
  }

  private void addKeyBindings() {
    FlowPane pnJump = new FlowPane();

    cbKeepSameXaxis = new CheckBox("keep same x-axis length");
    pnJump.getChildren().add(cbKeepSameXaxis);

    Button btnPrevious = new Button("<");
    btnPrevious.setTooltip(new Tooltip("Jump to previous distribution (use left arrow"));
    btnPrevious.setOnAction(e -> jumpToPrevFeature());
    pnJump.getChildren().add(btnPrevious);

    Button btnNext = new Button(">");
    btnNext.setTooltip(new Tooltip("Jump to previous distribution (use right arrow"));
    btnNext.setOnAction(e -> jumpToNextFeature());
    pnJump.getChildren().add(btnNext);
    mainPane.setBottom(pnJump);
  }

  @Override
  public void actionPerformed(final ActionEvent event) {
    final String command = event.getActionCommand();
    if ("PREVIOUS_PEAK".equals(command)) {
      jumpToPrevFeature();
    } else if ("NEXT_PEAK".equals(command)) {
      jumpToNextFeature();
    }
  }

  /**
   * tries to find the next local maximum to jump to the prev peak
   */
  private void jumpToPrevFeature() {
    for (var histo : getHistoPanel()) {
      XYPlot plot = getXYPlot(histo);
      if (plot == null) {
        return;
      }

      XYDataset data = plot.getDataset(0);
      // get center of zoom
      ValueAxis x = plot.getDomainAxis();
      double mid = (x.getUpperBound() + x.getLowerBound()) / 2;

      boolean started = false;

      for (int i = data.getItemCount(0) - 1; i >= 1; i--) {
        double mz = data.getXValue(0, i);
        if (mz < mid) {
          // wait for y to be 0 to start the search for a new peak
          if (!started) {
            if (data.getYValue(0, i) == 0) {
              started = true;
            }
          } else {
            // intensity drops?
            if (data.getYValue(0, i - 1) != 0 && data.getYValue(0, i) >= 5
                && data.getYValue(0, i - 1) < data.getYValue(0, i)) {
              // peak found with max at i
              setZoomAroundFeatureAt(i, histo);
              return;
            }
          }
        }
      }
    }
  }

  /**
   * tries to find the next local maximum to jump to the prev peak
   */
  private void jumpToNextFeature() {

    for (var histo : getHistoPanel()) {
      XYPlot plot = getXYPlot(histo);
      if (plot == null) {
        return;
      }

      XYDataset data = plot.getDataset(0);
      // get center of zoom
      ValueAxis x = plot.getDomainAxis();
      // mid of range
      double mid = (x.getUpperBound() + x.getLowerBound()) / 2;

      boolean started = false;

      for (int i = 0; i < data.getItemCount(0) - 1; i++) {
        double mz = data.getXValue(0, i);
        if (mz > mid) {
          // wait for y to be 0 to start the search for a new peak
          if (!started) {
            if (data.getYValue(0, i) == 0) {
              started = true;
            }
          } else {
            // intensity drops?
            if (data.getYValue(0, i + 1) != 0 && data.getYValue(0, i) >= 5
                && data.getYValue(0, i + 1) < data.getYValue(0, i)) {
              // peak found with max at i
              setZoomAroundFeatureAt(i, histo);
              return;
            }
          }
        }
      }
    }
  }

  /**
   * Set zoom factor around peak at data point i
   *
   * @param i
   */
  private void setZoomAroundFeatureAt(int i, HistogramPanel histo) {
    XYPlot plot = getXYPlot(histo);
    if (plot == null) {
      return;
    }

    XYDataset data = plot.getDataset(0);

    // keep same domain axis range length
    boolean keepRange = cbKeepSameXaxis.isSelected();

    // find lower bound (where y=0)
    double lower = data.getXValue(0, i);
    for (int x = i; x >= 0; x--) {
      if (data.getYValue(0, x) == 0) {
        lower = data.getXValue(0, x);
        break;
      }
    }
    // find upper bound /where y=0)
    double upper = data.getXValue(0, i);
    for (int x = i; x < data.getItemCount(0); x++) {
      if (data.getYValue(0, x) == 0) {
        upper = data.getXValue(0, x);
        break;
      }
    }

    if (keepRange) {
      // set constant range zoom
      double length = plot.getDomainAxis().getRange().getLength();
      plot.getDomainAxis().setRangeAboutValue(data.getXValue(0, i), length);
    } else {
      // set range directly around peak
      plot.getDomainAxis().setRange(lower, upper);
    }

    // auto gaussian fit
    if (histo.isGaussianFitEnabled()) {
      // find
      histo.setGaussianFitRange(lower, upper);
    }
    // auto range y
    ChartLogicsFX.autoRangeAxis(histo.getChartPanel());
  }

  private XYPlot getXYPlot(HistogramPanel histo) {
    ChartViewer chart = histo.getChartPanel();
    if (chart != null) {
      return chart.getChart().getXYPlot();
    } else {
      return null;
    }
  }

  public CheckBox getCbKeepSameXaxis() {
    return cbKeepSameXaxis;
  }

  public HistogramPanel[] getHistoPanel() {
    return new HistogramPanel[]{histo, histoDeltaNeutralMass};
  }

  public int getTotalScans() {
    return totalScans;
  }

  public int getProcessedScans() {
    return processedScans;
  }

  @NotNull
  @Override
  public Collection<? extends RawDataFile> getRawDataFiles() {
    return Collections.emptyList();
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getFeatureLists() {
    return List.of(flist);
  }

  @NotNull
  @Override
  public Collection<? extends FeatureList> getAlignedFeatureLists() {
    return Collections.emptyList();
  }

  @Override
  public void onRawDataFileSelectionChanged(Collection<? extends RawDataFile> rawDataFiles) {
  }

  @Override
  public void onFeatureListSelectionChanged(Collection<? extends FeatureList> featureLists) {

  }

  @Override
  public void onAlignedFeatureListSelectionChanged(
      Collection<? extends FeatureList> featureLists) {

  }
}
