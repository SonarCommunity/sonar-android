/*
 * SonarQube Android Plugin
 * Copyright (C) 2013 SonarSource and Jerome Van Der Linden, Stephane Nicolas, Florian Roncari, Thomas Bores, Jordan Hansen
 * dev@sonar.codehaus.org
 *
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU Lesser General Public
 * License as published by the Free Software Foundation; either
 * version 3 of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this program; if not, write to the Free Software
 * Foundation, Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02
 */
package org.sonar.plugins.android.lint;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.plugins.android.AndroidPlugin;

import java.io.File;

public class AndroidLintSensor implements Sensor {
  private static final Logger LOGGER = LoggerFactory.getLogger(AndroidLintSensor.class);

  private RulesProfile profile;
  private final ResourcePerspectives perspectives;
  private FileSystem fs;

  private final File lintReport;

  public AndroidLintSensor(Settings settings, RulesProfile profile, ResourcePerspectives perspectives, FileSystem fs) {
    this.profile = profile;
    this.perspectives = perspectives;
    this.fs = fs;
    this.lintReport = getFile(settings.getString(AndroidPlugin.LINT_REPORT_PROPERTY));
  }

  @Override
  public void analyse(Project project, SensorContext sensorContext) {
    new AndroidLintProcessor(profile, perspectives, fs).process(lintReport);
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    return lintReport != null && lintReport.exists();
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }

  private File getFile(String path) {
    try {
      File file = new File(path);
      if (!file.isAbsolute()) {
        file = new File(fs.baseDir(), path).getCanonicalFile();
      }
      return file;
    } catch (Exception e) {
      LOGGER.warn("Unable to resolve path", e);
    }
    return null;
  }
}
