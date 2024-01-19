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

package io.github.mzmine.modules.io.import_rawdata_mzml.msdk;

import io.github.msdk.MSDKException;
import io.github.mzmine.modules.io.import_rawdata_all.spectral_processor.ScanImportProcessorConfig;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLParser;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.data.MzMLRawDataFile;
import io.github.mzmine.modules.io.import_rawdata_mzml.msdk.util.FileMemoryMapper;
import io.github.mzmine.taskcontrol.AbstractTask;
import io.github.mzmine.taskcontrol.TaskStatus;
import io.github.mzmine.util.MemoryMapStorage;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.zip.DataFormatException;
import javolution.text.CharArray;
import javolution.xml.internal.stream.XMLStreamReaderImpl;
import javolution.xml.stream.XMLStreamConstants;
import javolution.xml.stream.XMLStreamException;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

/**
 * This class contains methods which parse data in MzML format from {@link File File},
 * {@link Path Path} or {@link InputStream InputStream} <br> scans will be parsed, and the values
 * pre-loaded. Scans can be filtered out.
 */
public class MzMLFileImportMethod extends AbstractTask {

  private static final Logger logger = Logger.getLogger(MzMLFileImportMethod.class.getName());
  private final File mzMLFile;
  private final InputStream inputStream;
  private MzMLParser parser;

  private MzMLRawDataFile newRawFile;
  private final @NotNull ScanImportProcessorConfig scanProcessorConfig;
  private final MemoryMapStorage storage;

  /**
   * Read file
   */
  public MzMLFileImportMethod(@NotNull Instant moduleCallDate, File mzMLFile,
      MemoryMapStorage storage, @NotNull ScanImportProcessorConfig scanProcessorConfig) {
    this(moduleCallDate, mzMLFile, null, storage, scanProcessorConfig);
  }


  /**
   * Read stream, e.g., from thermo RAW file parser
   *
   * @param inputStream an {@link InputStream InputStream} which contains data in MzML format.
   */
  public MzMLFileImportMethod(@NotNull Instant moduleCallDate, InputStream inputStream,
      MemoryMapStorage storage, @NotNull ScanImportProcessorConfig scanProcessorConfig) {
    this(moduleCallDate, null, inputStream, storage, scanProcessorConfig);
  }


  /**
   * Read file or stream. One is null
   */
  private MzMLFileImportMethod(@NotNull Instant moduleCallDate, File mzMLFile,
      InputStream inputStream, @Nullable MemoryMapStorage storage,
      @NotNull ScanImportProcessorConfig scanProcessorConfig) {
    super(storage, moduleCallDate);
    this.mzMLFile = mzMLFile;
    this.inputStream = inputStream;
    this.storage = storage;
    this.scanProcessorConfig = scanProcessorConfig;
  }

  @Override
  public void run() {
    setStatus(TaskStatus.PROCESSING);
    try {
      parseMzMl();
    } catch (MSDKException e) {
      var name = newRawFile == null ? "" : newRawFile.getName();
      setErrorMessage("Error during parsing of mzML/raw file " + name);
      setStatus(TaskStatus.ERROR);
      return;
    }
    setStatus(TaskStatus.FINISHED);
  }

  /**
   * Parse the MzML data and return the parsed data
   *
   * @return a {@link MzMLRawDataFile MzMLRawDataFile} object containing the parsed data
   */
  public MzMLRawDataFile parseMzMl() throws MSDKException {
    try {

      InputStream is = null;

      if (mzMLFile != null) {
        logger.finest("Began parsing file: " + mzMLFile.getAbsolutePath());
        is = FileMemoryMapper.mapToMemory(mzMLFile);
      } else if (inputStream != null) {
        logger.finest("Began parsing file from stream");
        is = inputStream;
      } else {
        throw new MSDKException("Invalid input");
      }
      // It's ok to directly create this particular reader, this class is `public final`
      // and we precisely want that fast UFT-8 reader implementation
      final XMLStreamReaderImpl xmlStreamReader = new XMLStreamReaderImpl();
      xmlStreamReader.setInput(is, "UTF-8");

      this.parser = new MzMLParser(this, storage, scanProcessorConfig);
      this.newRawFile = parser.getMzMLRawFile();

      int eventType;
      try {
        do {
          // check if parsing has been cancelled?
          if (isCanceled()) {
            return null;
          }

          eventType = xmlStreamReader.next();

          switch (eventType) {
            case XMLStreamConstants.START_ELEMENT -> {
              final CharArray openingTagName = xmlStreamReader.getLocalName();
              parser.processOpeningTag(xmlStreamReader, openingTagName);
            }
            case XMLStreamConstants.END_ELEMENT -> {
              final CharArray closingTagName = xmlStreamReader.getLocalName();
              parser.processClosingTag(xmlStreamReader, closingTagName);
            }

//            processCharacters method is not used in the moment
//            might be returned if new random access xml parser is introduced
//            case XMLStreamConstants.CHARACTERS:
//              parser.processCharacters(xmlStreamReader);
//              break;
          }

        } while (eventType != XMLStreamConstants.END_DOCUMENT);

      } catch (DataFormatException e) {
        throw new RuntimeException(e);
      } finally {
        if (xmlStreamReader != null) {
          xmlStreamReader.close();
        }
      }
      logger.finest("Parsing Complete");
    } catch (IOException | XMLStreamException e) {
      logger.log(Level.WARNING, "Error while loading mzML/RAW file " + e.getMessage(), e);
      throw (new MSDKException(e));
    }

    return newRawFile;
  }


  @Override
  public String getTaskDescription() {
    return newRawFile == null ? "" : "Parsing mzML file from " + newRawFile.getName();
  }

  public MzMLRawDataFile getResult() {
    return newRawFile;
  }

  /**
   * @return a {@link File File} instance of the MzML source if being read from a file <br> null if
   * the MzML source is an {@link InputStream InputStream}
   */
  @Nullable
  public File getMzMLFile() {
    return mzMLFile;
  }

  @Override
  public double getFinishedPercentage() {
    return parser == null ? 0 : parser.getFinishedPercentage();
  }
}
