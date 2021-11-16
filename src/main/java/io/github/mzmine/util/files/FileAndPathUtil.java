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

package io.github.mzmine.util.files;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import javafx.stage.FileChooser.ExtensionFilter;
import org.apache.commons.io.FilenameUtils;

/**
 * Simple file operations
 *
 * @author Robin Schmid (robinschmid@uni-muenster.de)
 */
public class FileAndPathUtil {

  /**
   * Returns the real file path as path/filename.fileformat
   *
   * @param path   the file path (directory of filename)
   * @param name   filename
   * @param format a format starting with a dot for example: .pdf ; or without a dot: pdf
   * @return path/name.format
   */
  public static File getRealFilePath(File path, String name, String format) {
    return new File(getFolderOfFile(path), getRealFileName(name, format));
  }

  /**
   * Returns the real file path as path/filename.fileformat
   *
   * @param filepath a filepath with file name
   * @param format   a format starting with a dot for example: .pdf ; or without a dot: pdf
   * @return filepath.format (the format will be exchanged by this format)
   */
  public static File getRealFilePath(File filepath, String format) {
    return new File(filepath.getParentFile(), getRealFileName(filepath.getName(), format));
  }


  public static File getRealFilePathWithSuffix(File outputFile, String suffix, String format) {
    File out = eraseFormat(outputFile);
    return getRealFilePath(outputFile, out.getName() + suffix, format);
  }

  /**
   * Returns the real file name as filename.fileformat
   *
   * @param name   filename
   * @param format a format starting with a dot for example .pdf
   * @return name.format
   */
  public static String getRealFileName(String name, String format) {
    String result = FilenameUtils.removeExtension(name);
    result = addFormat(result, format);
    return result;
  }

  /**
   * Returns the real file name as filename.fileformat
   *
   * @param name   filename
   * @param format a format starting with a dot for example .pdf
   * @return filename.format
   */
  public static String getRealFileName(File name, String format) {
    return getRealFileName(name.getAbsolutePath(), format);
  }

  /**
   * @param f target file
   * @return The file extension or null.
   */
  public static String getExtension(File f) {
    int lastDot = f.getName().lastIndexOf(".");
    if (lastDot != -1) {
      return f.getName().substring(lastDot + 1);
    }
    return null;
  }

  /**
   * erases the format. "image.png" will be returned as "image" this method is used by
   * getRealFilePath and getRealFileName
   *
   * @return remove format from file
   */
  public static File eraseFormat(File f) {
    int lastDot = f.getName().lastIndexOf(".");
    if (lastDot != -1) {
      return new File(f.getParent(), f.getName().substring(0, lastDot));
    } else {
      return f;
    }
  }

  /**
   * Adds the format. "image" will be returned as "image.format" Maybe use erase format first. this
   * method is used by getRealFilePath and getRealFileName
   *
   * @param name   a file name without format (use {@link #eraseFormat(File)} before)
   * @param format the new format
   * @return name.format
   */
  public static String addFormat(String name, String format) {
    if (format.startsWith(".")) {
      return name + format;
    } else {
      return name + "." + format;
    }
  }

  /**
   * Returns the file if it is already a folder. Or the parent folder if the file is a data file
   *
   * @param file file or folder
   * @return the parameter file if it is a directory, otherwise its parent directory
   */
  public static File getFolderOfFile(File file) {
    if (file.isDirectory()) {
      return file;
    } else {
      return file.getParentFile();
    }
  }

  /**
   * Returns the file name from a given file. If file is a folder an empty String is returned
   *
   * @param file a path
   * @return the filename of a file
   */
  public static String getFileNameFromPath(File file) {
    if (file.isDirectory()) {
      return "";
    } else {
      return file.getAbsolutePath().substring(file.getAbsolutePath().lastIndexOf("\\") + 1);
    }
  }

  /**
   * Returns the file name from a given file. If the file is a folder an empty String is returned
   *
   * @return the filename from a path
   */
  public static String getFileNameFromPath(String file) {
    return getFileNameFromPath(new File(file));
  }

  /**
   * Creates a new directory.
   *
   * @param theDir target directory
   * @return true if directory existed or was created
   */
  public static boolean createDirectory(File theDir) {
    // if the directory does not exist, create it
    if (theDir.exists()) {
      return true;
    } else {
      try {
        return theDir.mkdirs();
      } catch (SecurityException se) {
        // handle it
      }
      return false;
    }
  }

  /**
   * Sort an array of files These files have to start or end with a number
   *
   * @param files files to sort
   * @return sorted array of files
   */
  public static File[] sortFilesByNumber(File[] files, boolean reverse) {
    Comparator<File> fileNumberComparator = new Comparator<>() {
      private Boolean endsWithNumber = null;

      @Override
      public int compare(File o1, File o2) {
        try {
          if (endsWithNumber == null) {
            endsWithNumber = checkEndsWithNumber(o1.getName());
          }
          int n1 = extractNumber(o1, endsWithNumber);
          int n2 = extractNumber(o2, endsWithNumber);
          return Integer.compare(n1, n2);
        } catch (Exception e) {
          return o1.compareTo(o2);
        }
      }
    };
    if (reverse) {
      fileNumberComparator = fileNumberComparator.reversed();
    }
    Arrays.sort(files, fileNumberComparator);
    return files;
  }

  public static boolean isNumber(char c) {
    return (c >= '0' && c <= '9');
  }

  /**
   * @param file target file
   * @return true if the name ends with a number (e.g., file1.mzML=true)
   */
  public static boolean checkEndsWithNumber(File file) {
    return checkEndsWithNumber(file.getName());
  }

  /**
   * @param name filename
   * @return true if it ends with a number
   */
  public static boolean checkEndsWithNumber(String name) {
    // ends with number?
    int e = name.lastIndexOf('.');
    e = e == -1 ? name.length() : e;
    return isNumber(name.charAt(e - 1));
  }

  public static int extractNumber(File file, boolean endsWithNumber)
      throws IllegalArgumentException {
    return extractNumber(file.getName(), endsWithNumber);
  }

  public static int extractNumber(String name, boolean endsWithNumber)
      throws IllegalArgumentException {
    if (endsWithNumber) {
      // ends with number?
      int e = name.lastIndexOf('.');
      e = e == -1 ? name.length() : e;
      int f = e - 1;
      for (; f > 0; f--) {
        if (!isNumber(name.charAt(f))) {
          f++;
          break;
        }
      }
      //
      if (f < 0) {
        f = 0;
      }
      if (f - e == 0) {
        // there was no number
        throw new IllegalArgumentException(name + " does not end with number");
      }
      String number = name.substring(f, e);
      return Integer.parseInt(number);
    } else {
      int f = 0;
      for (; f < name.length(); f++) {
        if (!isNumber(name.charAt(f))) {
          break;
        }
      }
      if (f == 0) {
        // there was no number
        throw new IllegalArgumentException(name + " does not start with number");
      }
      String number = name.substring(0, f);
      return Integer.parseInt(number);
    }
  }

  /**
   * Lists all directories in directory f
   *
   * @param f directory
   * @return all sub directories
   */
  public static File[] getSubDirectories(File f) {
    return f.listFiles((current, name) -> new File(current, name).isDirectory());
  }

  // ###############################################################################################
  // search for files

  public static List<File[]> findFilesInDir(File dir, ExtensionFilter fileFilter) {
    String ext = fileFilter.getExtensions().get(0);
    if (ext.startsWith("*.")) {
      ext = ext.substring(2);
    }
    return findFilesInDir(dir, new FileNameExtFilter("", ext), true, false);
  }

  public static List<File[]> findFilesInDir(File dir, ExtensionFilter fileFilter,
      boolean searchSubdir) {
    String ext = fileFilter.getExtensions().get(0);
    if (ext.startsWith("*.")) {
      ext = ext.substring(2);
    }
    return findFilesInDir(dir, new FileNameExtFilter("", ext), searchSubdir, false);
  }

  /**
   * @param dir        parent directory
   * @param fileFilter filter files
   * @return list of all files in directory and sub directories
   */
  public static List<File[]> findFilesInDir(File dir, FileNameExtFilter fileFilter) {
    return findFilesInDir(dir, fileFilter, true, false);
  }

  public static List<File[]> findFilesInDir(File dir, FileNameExtFilter fileFilter,
      boolean searchSubdir) {
    return findFilesInDir(dir, fileFilter, searchSubdir, false);
  }

  public static List<File[]> findFilesInDir(File dir, FileNameExtFilter fileFilter,
      boolean searchSubdir, boolean filesInSeparateFolders) {
    File[] subDir = FileAndPathUtil.getSubDirectories(dir);
    // result: each vector element stands for one img
    List<File[]> list = new ArrayList<>();
    // add all files as first image
    // sort all files and return them
    File[] files = dir.listFiles(fileFilter);
    files = FileAndPathUtil.sortFilesByNumber(files, false);
    if (files != null && files.length > 0) {
      list.add(files);
    }

    if (subDir == null || subDir.length <= 0 || !searchSubdir) {
      // no subdir end directly
      return list;
    } else {
      // sort dirs
      subDir = FileAndPathUtil.sortFilesByNumber(subDir, false);
      // go in all sub and subsub... folders to find files
      if (filesInSeparateFolders) {
        findFilesInSubDirSeparatedFolders(dir, subDir, list, fileFilter);
      } else {
        findFilesInSubDir(subDir, list, fileFilter);
      }
      // return as array (unsorted because they are sorted folder wise)
      return list;
    }
  }

  /**
   * go into all subfolders and find all files and go in further subfolders files stored in separate
   * folders. one line in one folder
   *
   * @param dirs musst be sorted!
   * @param list
   */
  private static void findFilesInSubDirSeparatedFolders(File parent, File[] dirs,
      List<File[]> list, FileNameExtFilter fileFilter) {
    // go into folder and find files
    List<File> img = null;
    // each file in one folder
    for (File dir : dirs) {
      // find all suiting files
      File[] subFiles = FileAndPathUtil.sortFilesByNumber(dir.listFiles(fileFilter), false);
      // if there are some suiting files in here directory has been found!
      // create image of these
      // dirs
      if (subFiles.length > 0) {
        if (img == null) {
          img = new ArrayList<>();
        }
        // put them into the list
        img.addAll(Arrays.asList(subFiles));
      } else {
        // search in subfolders for data
        // find all subfolders, sort them and do the same iterative
        File[] subDir =
            FileAndPathUtil.sortFilesByNumber(FileAndPathUtil.getSubDirectories(dir), false);
        // call this method
        findFilesInSubDirSeparatedFolders(dir, subDir, list, fileFilter);
      }
    }
    // add to list
    if (img != null && img.size() > 0) {
      list.add(img.toArray(File[]::new));
    }
  }

  /**
   * Go into all sub-folders and find all files files stored one image in one folder!
   *
   * @param dirs musst be sorted!
   * @param list
   */
  private static void findFilesInSubDir(File[] dirs, List<File[]> list,
      FileNameExtFilter fileFilter) {
    // All files in one folder
    for (File dir : dirs) {
      // find all suiting files
      File[] subFiles = FileAndPathUtil.sortFilesByNumber(dir.listFiles(fileFilter), false);
      // put them into the list
      if (subFiles != null && subFiles.length > 0) {
        list.add(subFiles);
      }
      // find all subfolders, sort them and do the same iterative
      File[] subDir = FileAndPathUtil
          .sortFilesByNumber(FileAndPathUtil.getSubDirectories(dir), false);
      // call this method
      findFilesInSubDir(subDir, list, fileFilter);
    }
  }

  /**
   * The Path of the Jar.
   *
   * @return jar path
   */
  public static File getPathOfJar() {
    /*
     * File f = new File(System.getProperty("java.class.path")); File dir =
     * f.getAbsoluteFile().getParentFile(); return dir;
     */
    try {
      File jar = new File(FileAndPathUtil.class.getProtectionDomain().getCodeSource().getLocation()
          .toURI().getPath());
      return jar.getParentFile();
    } catch (Exception ex) {
      return new File("");
    }
  }

  public static File getUniqueFilename(final File parent, final String fileName) {
    final File dir = parent.isDirectory() ? parent : parent.getParentFile();
    final File file = new File(dir + File.separator + fileName);

    if (!file.exists()) {
      return file;
    }

    final String extension = getExtension(file);
    final File noExtension = eraseFormat(file);

    int i = 1;
    File uniqueFile = new File(
        noExtension.getAbsolutePath() + "(" + i + ")." + extension);
    while (uniqueFile.exists()) {
      i++;
      uniqueFile = new File(
          noExtension.getAbsolutePath() + "(" + i + ")." + extension);
    }
    return uniqueFile;
  }
}
