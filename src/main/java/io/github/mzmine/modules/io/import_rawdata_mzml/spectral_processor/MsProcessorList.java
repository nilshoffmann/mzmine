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

package io.github.mzmine.modules.io.import_rawdata_mzml.spectral_processor;

import io.github.mzmine.datamodel.Scan;
import io.github.mzmine.modules.io.import_rawdata_mzml.spectral_processor.processors.MassDetectorMsProcessor;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Contains multiple steps to process
 */
public class MsProcessorList implements MsProcessor {

  private final List<MsProcessor> processors = new ArrayList<>();

  public MsProcessorList(List<MsProcessor> processors) {
    this.processors.addAll(processors);
  }

  @Override
  public SimpleSpectralArrays processScan(final Scan scan, SimpleSpectralArrays spectrum) {
    for (final MsProcessor processor : processors) {
      spectrum = processor.processScan(scan, spectrum);
    }
    return spectrum;
  }

  @Override
  public String description() {
    return "Processing steps:\n" + processors.stream().map(MsProcessor::description)
        .collect(Collectors.joining("\n"));
  }

  /**
   * Loops through all processors to find {@link MassDetectorMsProcessor}
   *
   * @return true if MassDetectorMsProcessor is in list
   */
  public boolean containsMassDetection() {
    return stream().anyMatch(step -> step instanceof MassDetectorMsProcessor);
  }

  public Stream<MsProcessor> stream() {
    return processors.stream();
  }
}
