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

package io.github.mzmine.modules.dataanalysis.heatmaps;

import java.util.Collection;

import javax.annotation.Nonnull;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.PeakList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineRunnableModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;

public class HeatMapModule implements MZmineRunnableModule {

  private static final String MODULE_NAME = "Heat map plot";
  private static final String MODULE_DESCRIPTION = "Heat map plot.";

  @Override
  public @Nonnull String getName() {
    return MODULE_NAME;
  }

  @Override
  public @Nonnull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  @Nonnull
  public ExitCode runModule(@Nonnull MZmineProject project, @Nonnull ParameterSet parameters,
      @Nonnull Collection<Task> tasks) {

    PeakList selectedDataset =
        parameters.getParameter(HeatMapParameters.peakLists).getValue().getMatchingPeakLists()[0];

    HeatMapTask heatMapTask = new HeatMapTask(project, selectedDataset, parameters);

    tasks.add(heatMapTask);

    return ExitCode.OK;

  }

  public @Nonnull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.DATAANALYSIS;
  }

  @Override
  public @Nonnull Class<? extends ParameterSet> getParameterSetClass() {
    return HeatMapParameters.class;
  }

}
