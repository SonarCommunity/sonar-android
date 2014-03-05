/*
 * SonarQube Android Plugin
 * Copyright (C) 2013 SonarSource and Jerome Van Der Linden, Stephane Nicolas, Florian Roncari, Thomas Bores
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

import com.android.SdkConstants;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.ModuleFileSystem;

import java.io.File;

public class AndroidLintSensor implements Sensor {

  private RulesProfile profile;
  private AndroidLintExecutor executor;
  private ModuleFileSystem fs;
  private Settings settings;

  public AndroidLintSensor(RulesProfile profile, AndroidLintExecutor executor, ModuleFileSystem fs, Settings settings) {
    this.profile = profile;
    this.executor = executor;
    this.fs = fs;
    this.settings = settings;
  }

  @Override
  public void analyse(Project project, SensorContext sensorContext) {
    executor.execute(sensorContext, project);
  }

  @Override
  public boolean shouldExecuteOnProject(Project project) {
    String projectFullPath = fs.baseDir().getPath();
    String projectRelPath = settings.getString(AndroidLintConstants.PROJECT_PATH_PROPERTY);
    if (projectRelPath != null) {
      projectFullPath += "/" + projectRelPath;
    }
    return !fs.files(FileQuery.onSource().onLanguage("java")).isEmpty()
        && !profile.getActiveRulesByRepository(AndroidLintConstants.REPOSITORY_KEY).isEmpty()
        && new File(projectFullPath, SdkConstants.ANDROID_MANIFEST_XML).exists();
  }

  @Override
  public String toString() {
    return this.getClass().getSimpleName();
  }
}
