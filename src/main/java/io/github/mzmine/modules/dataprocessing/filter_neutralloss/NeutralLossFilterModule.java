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

package io.github.mzmine.modules.dataprocessing.filter_neutralloss;

import io.github.mzmine.datamodel.MZmineProject;
import io.github.mzmine.datamodel.features.FeatureList;
import io.github.mzmine.modules.MZmineModuleCategory;
import io.github.mzmine.modules.MZmineProcessingModule;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.Task;
import io.github.mzmine.util.ExitCode;
import io.github.mzmine.util.MemoryMapStorage;
import java.util.Collection;
import org.jetbrains.annotations.NotNull;

public class NeutralLossFilterModule implements MZmineProcessingModule {

  private static final String MODULE_NAME = "Neutral loss filter";
  private static final String MODULE_DESCRIPTION =
      "Searches for neutral losses within a feature list.";

  @Override
  public @NotNull String getName() {
    return MODULE_NAME;
  }

  public @NotNull MZmineModuleCategory getModuleCategory() {
    return MZmineModuleCategory.FEATURELISTFILTERING;
  }

  @Override
  public @NotNull Class<? extends ParameterSet> getParameterSetClass() {
    return NeutralLossFilterParameters.class;
  }

  public @NotNull String getDescription() {
    return MODULE_DESCRIPTION;
  }

  @Override
  public @NotNull ExitCode runModule(@NotNull MZmineProject project,
      @NotNull ParameterSet parameters, @NotNull Collection<Task> tasks) {
    FeatureList peakLists[] = parameters.getParameter(NeutralLossFilterParameters.PEAK_LISTS)
        .getValue().getMatchingFeatureLists();

    final MemoryMapStorage storage = MemoryMapStorage.forFeatureList();
    for (FeatureList peakList : peakLists) {
      Task newTask = new NeutralLossFilterTask(project, peakList, parameters, storage);
      tasks.add(newTask);
    }
    return ExitCode.OK;
  }

}
