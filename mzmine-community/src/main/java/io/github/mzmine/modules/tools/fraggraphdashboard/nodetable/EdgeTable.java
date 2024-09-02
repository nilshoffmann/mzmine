/*
 * Copyright (c) 2004-2024 The mzmine Development Team
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

package io.github.mzmine.modules.tools.fraggraphdashboard.nodetable;

import io.github.mzmine.gui.preferences.NumberFormats;
import io.github.mzmine.javafx.components.factories.TableColumns;
import io.github.mzmine.javafx.components.factories.TableColumns.ColumnAlignment;
import io.github.mzmine.main.ConfigService;
import io.github.mzmine.modules.tools.fraggraphdashboard.fraggraph.graphstream.SubFormulaEdge;
import io.github.mzmine.util.Comparators;
import java.text.ParseException;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableRow;
import javafx.scene.control.TableView;
import javafx.scene.control.cell.CheckBoxTableCell;

public class EdgeTable extends TableView<SubFormulaEdge> {

  private final NumberFormats formats = ConfigService.getGuiFormats();

  public EdgeTable() {
    setColumnResizePolicy(TableView.CONSTRAINED_RESIZE_POLICY_FLEX_NEXT_COLUMN);
    setRowFactory(table -> new TableRow<>() {
      {
        // unbind from old property, bind to new one to properly reflect the disable status
        itemProperty().addListener((_, old, n) -> {
          if (old != null) {
            disableProperty().unbind();
          }
          if (n != null) {
            disableProperty().bind(n.validProperty().not());
          }
        });
      }
    });

    TableColumn<SubFormulaEdge, Boolean> visible = TableColumns.createColumn("", 15, 35,
        SubFormulaEdge::visibleProperty);
    visible.setCellFactory(CheckBoxTableCell.forTableColumn(visible));

    TableColumn<SubFormulaEdge, Number> signal1 = TableColumns.createColumn("Signal 1", 70,
        formats.mzFormat(), ColumnAlignment.RIGHT, edge -> edge.smaller().calculatedMzProperty());

    TableColumn<SubFormulaEdge, Number> signal2 = TableColumns.createColumn("Signal 2", 70,
        formats.mzFormat(), ColumnAlignment.RIGHT, edge -> edge.larger().calculatedMzProperty());

    TableColumn<SubFormulaEdge, String> formulaDifference = TableColumns.createColumn(
        "Formula\ndiff.", 85, edge -> edge.lossFormulaStringProperty().map(str -> STR."-[\{str}]"));

    TableColumn<SubFormulaEdge, Number> massDifferenceAbs = TableColumns.createColumn(
        "Mass diff.\n(meas.)", 85, formats.mzFormat(), ColumnAlignment.RIGHT,
        SubFormulaEdge::measuredMassDiffProperty);

    TableColumn<SubFormulaEdge, String> massErrorAbs = new TableColumn<>("Δm/z\n(abs.)");
    massErrorAbs.getStyleClass().add("align-right-column");
    massErrorAbs.setCellValueFactory(
        cell -> cell.getValue().massErrorAbsProperty().map(formats::mz));
    massErrorAbs.setMinWidth(70);
    massErrorAbs.setComparator(
        (s1, s2) -> Comparators.COMPARE_ABS_NUMBER.compare(mzDoubleParser(s1), mzDoubleParser(s2)));

    TableColumn<SubFormulaEdge, String> massErrorPpm = new TableColumn<>("Δm/z\n(ppm)");
    massErrorPpm.getStyleClass().add("align-right-column");
    massErrorPpm.setCellValueFactory(
        cell -> cell.getValue().massErrorPpmProperty().map(formats::ppm));
    massErrorPpm.setMinWidth(70);
    massErrorPpm.setComparator(
        (s1, s2) -> Comparators.COMPARE_ABS_NUMBER.compare(mzDoubleParser(s1), mzDoubleParser(s2)));

    getColumns().addAll(visible, signal1, signal2, formulaDifference, massDifferenceAbs,
        massErrorAbs, massErrorPpm);
  }

  private Double mzDoubleParser(String diffStr) {
    if (diffStr == null || diffStr.isBlank()) {
      return null;
    }
    try {
      return formats.mzFormat().parse(diffStr).doubleValue();
    } catch (ParseException e) {
      return 0.0d;
    }
  }
}
