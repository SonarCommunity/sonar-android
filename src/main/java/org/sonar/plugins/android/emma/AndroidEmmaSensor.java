/*
 * Sonar, open source software quality management tool.
 * Copyright (C) 2009 SonarSource
 * mailto:contact AT sonarsource DOT com
 *
 * Sonar is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * Sonar is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with Sonar; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */

package org.sonar.plugins.android.emma;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.CoverageExtension;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.config.Settings;
import org.sonar.api.resources.Project;
import org.sonar.plugins.android.AndroidPlugin;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;

public class AndroidEmmaSensor implements Sensor, CoverageExtension {

  private static final Logger LOGGER = LoggerFactory.getLogger(AndroidEmmaSensor.class);
  private final JavaResourceLocator javaResourceLocator;
  private final FileSystem fileSystem;
  private final File emmaReportDirectory;

  public AndroidEmmaSensor(Settings settings, JavaResourceLocator javaResourceLocator, FileSystem fileSystem) {
    this.javaResourceLocator = javaResourceLocator;
    this.fileSystem = fileSystem;
    this.emmaReportDirectory = getFile(settings.getString(AndroidPlugin.EMMA_REPORT_DIR_PROPERTY));
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return fileSystem.hasFiles(fileSystem.predicates().hasLanguage("java"));
  }

  @Override
  public void analyse(Project project, SensorContext context) {
    if (emmaReportDirectory == null) {
      LOGGER.warn("Directory {} not found on file system", emmaReportDirectory);
      return;
    }
    if (!emmaReportDirectory.exists() || !emmaReportDirectory.isDirectory()) {
      LOGGER.warn("Emma reports not found in {}", emmaReportDirectory);
      return;
    }

    LOGGER.info("Parse reports: " + emmaReportDirectory.getPath());
    new AndroidEmmaProcessor(emmaReportDirectory, javaResourceLocator, context).process();
  }

  @Override
  public String toString() {
    return getClass().getSimpleName();
  }

  private File getFile(String path) {
    try {
      File file = new File(path);
      if (!file.isAbsolute()) {
        file = new File(fileSystem.baseDir(), path).getCanonicalFile();
      }
      return file;
    } catch (Exception e) {
      LOGGER.warn("Unable to resolve path", e);
    }
    return null;
  }
}
