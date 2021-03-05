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
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301 USA
 */

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.MassSpectrumType;
import io.github.mzmine.datamodel.PolarityType;
import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.datamodel.featuredata.IonTimeSeries;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.datamodel.features.FeatureListRow;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.MZmineProcessingStep;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ADAPChromatogramBuilderParameters;
import io.github.mzmine.modules.dataprocessing.featdet_adapchromatogrambuilder.ModularADAPChromatogramBuilderModule;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.MassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetector;
import io.github.mzmine.modules.dataprocessing.featdet_massdetection.centroid.CentroidMassDetectorParameters;
import io.github.mzmine.modules.impl.MZmineProcessingStepImpl;
import io.github.mzmine.modules.io.import_all_data_files.AdvancedSpectraImportParameters;
import io.github.mzmine.modules.io.import_all_data_files.AllSpectralDataImportModule;
import io.github.mzmine.modules.io.import_all_data_files.AllSpectralDataImportParameters;
import io.github.mzmine.parameters.parametertypes.selectors.RawDataFilesSelectionType;
import io.github.mzmine.parameters.parametertypes.selectors.ScanSelection;
import io.github.mzmine.parameters.parametertypes.tolerances.MZTolerance;
import java.io.File;
import java.util.Comparator;
import java.util.logging.Logger;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.MethodOrderer.OrderAnnotation;
import org.junit.jupiter.api.Order;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.junit.jupiter.api.TestMethodOrder;

/**
 * {@link Lifecycle#PER_CLASS} creates only one test instance of this class and executes everything
 * in sequence. As we are using data import, chromatogram building, ... Only with this option the
 * init (@BeforeAll) and tearDown method are not static.
 *
 * @author Robin Schmid (https://github.com/robinschmid)
 */
@DisplayName("Test Feature Finding")
@TestInstance(Lifecycle.PER_CLASS)
@TestMethodOrder(OrderAnnotation.class)
public class FeatureFindingTest {

  private static final Logger logger = Logger.getLogger(FeatureFindingTest.class.getName());
  private static MZmineProject project;

  /**
   * Init MZmine core in headless mode with the options -r (keep running) and -m (keep in memory)
   */
  @BeforeAll
  public void init() {
    logger.info("Running MZmine");
    MZmineCore.main(new String[]{"-r", "-m", "all"});
    logger.info("Getting project");
    project = MZmineCore.getProjectManager().getCurrentProject();
  }


  @Test
  @Order(1)
  @DisplayName("Test advanced data import of mzML and mzXML with mass detection")
  void dataImportTest() throws InterruptedException {
    File[] files = new File[]{new File(FeatureFindingTest.class.getClassLoader()
        .getResource("rawdatafiles/DOM_a.mzML").getFile()),
        new File(FeatureFindingTest.class.getClassLoader()
            .getResource("rawdatafiles/DOM_b.mzXML").getFile())};

    AllSpectralDataImportParameters paramDataImport = new AllSpectralDataImportParameters();
    paramDataImport.setParameter(AllSpectralDataImportParameters.fileNames, files);
    paramDataImport.setParameter(AllSpectralDataImportParameters.advancedImport, true);
    AdvancedSpectraImportParameters advancedImport = paramDataImport
        .getParameter(AllSpectralDataImportParameters.advancedImport).getEmbeddedParameters();
    advancedImport.setParameter(AdvancedSpectraImportParameters.msMassDetection, true);
    advancedImport.setParameter(AdvancedSpectraImportParameters.ms2MassDetection, true);
    // create centroid mass detectors
    advancedImport.getParameter(AdvancedSpectraImportParameters.msMassDetection)
        .getEmbeddedParameter().setValue(createCentroidMassDetector(1E5));
    advancedImport.getParameter(AdvancedSpectraImportParameters.ms2MassDetection)
        .getEmbeddedParameter().setValue(createCentroidMassDetector(0));

    logger.info("Testing advanced data import of mzML and mzXML with direct mass detection");
    boolean finished = MZmineTestUtil
        .callModuleWithTimeout(30, AllSpectralDataImportModule.class, paramDataImport);

    // should have finished by now
    assertTrue(finished, "Time out during file read task. Not finished in time.");
    assertEquals(2, project.getDataFiles().length);
    // sort by name
    project.getRawDataFiles().sort(Comparator.comparing(RawDataFile::getName));
    int filesTested = 0;
    for (RawDataFile raw : project.getRawDataFiles()) {
      // check all scans and mass lists
      for (Scan scan : raw.getScans()) {
        assertNotNull(scan);
        assertNotNull(scan.getMassList());
      }
      switch (raw.getName()) {
        case "DOM_a.mzML" -> {
          // num of scans, ms1, ms2
          assertEquals(521, raw.getNumOfScans());
          assertEquals(87, raw.getScanNumbers(1).size());
          assertEquals(434, raw.getScanNumbers(2).size());
          // number of data points
          assertEquals(2400, raw.getMaxRawDataPoints());
          assertEquals(2400, raw.getMaxCentroidDataPoints());
          // check two scans
          Scan scan = raw.getScan(0);
          assertEquals(1, scan.getMSLevel());
          assertNotEquals(MassSpectrumType.PROFILE, scan.getSpectrumType()); // not
          assertEquals(794, scan.getBasePeakIndex());
          assertEquals(PolarityType.POSITIVE, scan.getPolarity());
          // MS2 scan
          Scan scanMS2 = raw.getScan(1);
          assertEquals(2, scanMS2.getMSLevel());
          assertNotEquals(MassSpectrumType.PROFILE, scanMS2.getSpectrumType()); // not
          assertEquals(221, scanMS2.getBasePeakIndex());
          assertEquals(PolarityType.POSITIVE, scanMS2.getPolarity());
          filesTested++;
        }
        case "DOM_b.mzXML" -> {
          // num of scans, ms1, ms2
          assertEquals(521, raw.getNumOfScans());
          assertEquals(87, raw.getScanNumbers(1).size());
          assertEquals(434, raw.getScanNumbers(2).size());
          // number of data points
          assertEquals(2410, raw.getMaxRawDataPoints());
          assertEquals(2410, raw.getMaxCentroidDataPoints());
          // check two scans
          Scan scan = raw.getScan(1);
          assertEquals(1, scan.getMSLevel());
          assertNotEquals(MassSpectrumType.PROFILE, scan.getSpectrumType()); // not
          assertEquals(203, scan.getBasePeakIndex());
          assertEquals(PolarityType.POSITIVE, scan.getPolarity());
          // MS2 scan
          Scan scanMS2 = raw.getScan(2);
          assertEquals(2, scanMS2.getMSLevel());
          assertNotEquals(MassSpectrumType.PROFILE, scanMS2.getSpectrumType()); // not
          assertEquals(239, scanMS2.getBasePeakIndex());
          assertEquals(PolarityType.POSITIVE, scanMS2.getPolarity());
          filesTested++;
        }
      }
    }
    // both files tested
    assertEquals(2, filesTested);
  }

  @Test
  @Order(2)
  @DisplayName("Test ADAP chromatogram builder")
  void chromatogramBuilderTest() throws InterruptedException {
    String suffix = "chrom";

    ADAPChromatogramBuilderParameters paramChrom = new ADAPChromatogramBuilderParameters();
    paramChrom.getParameter(ADAPChromatogramBuilderParameters.dataFiles).setValue(
        RawDataFilesSelectionType.ALL_FILES);
    paramChrom.setParameter(ADAPChromatogramBuilderParameters.scanSelection, new ScanSelection(1));
    paramChrom.setParameter(ADAPChromatogramBuilderParameters.minimumScanSpan, 4);
    paramChrom.setParameter(ADAPChromatogramBuilderParameters.mzTolerance, new MZTolerance(0.002, 10));
    paramChrom.setParameter(ADAPChromatogramBuilderParameters.startIntensity, 3E5);
    paramChrom.setParameter(ADAPChromatogramBuilderParameters.IntensityThresh2, 1E5);
    paramChrom.setParameter(ADAPChromatogramBuilderParameters.suffix, suffix);

    logger.info("Testing ADAPChromatogramBuilder");
    boolean finished = MZmineTestUtil
        .callModuleWithTimeout(30, ModularADAPChromatogramBuilderModule.class, paramChrom);

    // should have finished by now
    assertTrue(finished, "Time out during ADAP chromatogram builder. Not finished in time.");

    assertEquals(project.getFeatureLists().size(), 2);
    // test feature lists
    int filesTested = 0;
    for(FeatureList flist : project.getFeatureLists()) {
      assertEquals(1, flist.getNumberOfRawDataFiles());
      assertEquals(1, flist.getAppliedMethods().size());
      // check default sorting of rows
      // assertTrue(MZmineTestUtil.isSorted(flist));

      if(flist.getName().equals("DOM_a.mzML "+suffix)) {
        assertEquals(1011, flist.getNumberOfRows());
        // check number of chromatogram scans (equals MS1 scans)
        assertEquals(87, flist.getSeletedScans(flist.getRawDataFile(0)).size());

        // check random row
        FeatureListRow row = flist.getRow(100);
        assertEquals(flist, row.getFeatureList());
        assertEquals(101, row.getID());
        assertTrue(row.getAverageMZ()>249.206);
        assertTrue(row.getAverageRT()>8.03);
        assertTrue(row.getAverageHeight()>320000);
        assertTrue(row.getAverageArea()>18354);

        IonTimeSeries<? extends Scan> data = row.getFeatures().get(0).getFeatureData();
        assertEquals(6, data.getNumberOfValues());
        assertEquals(6, data.getSpectra().size());

        filesTested++;
      }
      else if(flist.getName().equals("DOM_b.mzXML "+suffix)) {
        assertEquals(1068, flist.getNumberOfRows());
        // check number of chromatogram scans (equals MS1 scans)
        assertEquals(87, flist.getSeletedScans(flist.getRawDataFile(0)).size());

        filesTested++;
      }
    }
    // both files tested
    assertEquals(2, filesTested);
  }

  @AfterAll
  public void tearDown() {
    // System.exit in tests are bad
    // MZmineCore.exit();
  }

  private MZmineProcessingStep<MassDetector> createCentroidMassDetector(double noise) {
    CentroidMassDetector detect = new CentroidMassDetector();
    CentroidMassDetectorParameters param = new CentroidMassDetectorParameters();
    param.setParameter(CentroidMassDetectorParameters.noiseLevel, noise);
    return new MZmineProcessingStepImpl<>(detect, param);
  }
}
