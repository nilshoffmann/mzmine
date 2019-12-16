/*
 * Copyright 2006-2020 The MZmine Development Team
 * 
 * This file is part of MZmine.
 * 
 * MZmine is free software; you can redistribute it and/or modify it under the terms of the GNU
 * General Public License as published by the Free Software Foundation; either version 2 of the
 * License, or (at your option) any later version.
 * 
 * MZmine is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without
 * even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * General Public License for more details.
 * 
 * You should have received a copy of the GNU General Public License along with MZmine; if not,
 * write to the Free Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA 02110-1301
 * USA
 */

package io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.results;

import java.text.NumberFormat;

import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.modules.visualization.spectra.simplespectra.datapointprocessing.datamodel.ProcessedDataPoint;

public class DPPIsotopicPeakResult extends DPPResult<ProcessedDataPoint> {

  private final int charge;
  private final String isotope;
  private static final NumberFormat format = MZmineCore.getConfiguration().getMZFormat();

  public DPPIsotopicPeakResult(ProcessedDataPoint peak, String isotope, int charge) {
    super(peak);
    this.isotope = isotope;
    this.charge = charge;
  }

  public int getCharge() {
    return charge;
  }

  public String getIsotope() {
    return isotope;
  }

  @Override
  public String toString() {
    return format.format(value.getMZ()) + " (" + isotope + ")";
  }

  @Override
  public ResultType getResultType() {
    return ResultType.ISOTOPICPEAK;
  }
}
