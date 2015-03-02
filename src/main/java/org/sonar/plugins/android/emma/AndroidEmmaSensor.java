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

import org.apache.commons.lang.StringUtils;
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
    private final Settings settings;
    private final FileSystem fileSystem;
    private String emmaReportDirectory;

    public AndroidEmmaSensor(Settings settings, JavaResourceLocator javaResourceLocator, FileSystem fileSystem) {
        this.javaResourceLocator = javaResourceLocator;
        this.settings = settings;
        this.fileSystem = fileSystem;
    }

    @Override
    public boolean shouldExecuteOnProject(Project project) {
        emmaReportDirectory = settings.getString(AndroidPlugin.EMMA_REPORT_DIR_PROPERTY);

        return !StringUtils.isEmpty(emmaReportDirectory) && fileSystem.hasFiles(fileSystem.predicates().hasLanguage("java"));
    }

    @Override
    public void analyse(Project project, SensorContext context) {
        File reportsPath = project.getFileSystem().resolvePath(emmaReportDirectory);
        if (reportsPath == null) {
            LOGGER.warn("Directory {} not found on file system", emmaReportDirectory);
            return;
        }
        if (!reportsPath.exists() || !reportsPath.isDirectory()) {
            LOGGER.warn("Emma reports not found in {}", reportsPath);
            return;
        }

        LOGGER.info("Parse reports: " + reportsPath);
        new AndroidEmmaProcessor(reportsPath, javaResourceLocator, context).process();
    }

    @Override
    public String toString() {
        return getClass().getSimpleName();
    }

}
