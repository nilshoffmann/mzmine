/*
 *  Copyright 2006-2020 The MZmine Development Team
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

package io.github.mzmine.modules.visualization.rawdataoverviewims;

import io.github.mzmine.datamodel.RawDataFile;
import io.github.mzmine.gui.mainwindow.MZmineTab;
import io.github.mzmine.main.MZmineCore;
import io.github.mzmine.parameters.ParameterSet;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import java.util.Set;

public class IMSRawDataOverviewTask extends AbstractTask {

  private RawDataFile file;

  public IMSRawDataOverviewTask(ParameterSet parameterSet) {
    file = parameterSet.getParameter(IMSRawDataOverviewParameters.rawDataFiles)
        .getValue()
        .getMatchingRawDataFiles()[0];
  }

  @Override
  public String getTaskDescription() {
    return "Visualising ion mobility data....";
  }

  @Override
  public double getFinishedPercentage() {
    return 0;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    MZmineTab tab = new IMSRawDataOverviewTab();
    tab.onRawDataFileSelectionChanged(Set.of(file));
    MZmineCore.getDesktop().addTab(tab);
    setStatus(TaskStatus.FINISHED);
  }
}
