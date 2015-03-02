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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.Sensor;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;

import java.io.File;

public class AndroidLintSensor implements Sensor {

    private static final Logger LOG = LoggerFactory.getLogger(AndroidLintSensor.class);

    private RulesProfile profile;

    private AndroidLintExecutor executor;

    private final FileSystem fileSystem;

    public AndroidLintSensor(RulesProfile profile, AndroidLintExecutor executor, FileSystem fs) {
        this.profile = profile;
        this.executor = executor;
        this.fileSystem = fs;
    }

    @Override
    public void analyse(Project project, SensorContext sensorContext) {
        executor.execute(sensorContext, project);
    }

    @Override
    public boolean shouldExecuteOnProject(Project project) {


        return fileSystem.languages().contains("java")
                && !profile.getActiveRulesByRepository(AndroidLintRuleRepository.REPOSITORY_KEY).isEmpty()
                && (hasAndroidManifest(project) || isGradleProject(project));

    }

    private boolean hasAndroidManifest(Project project) {
        boolean result = new File(fileSystem.baseDir(), SdkConstants.ANDROID_MANIFEST_XML).exists();
        if (!result) {
            for (File sourceDir : project.getFileSystem().getSourceDirs()) {
                if (new File(sourceDir, SdkConstants.ANDROID_MANIFEST_XML).exists()) {
                    return true;
                }
            }
        }
        return result;
    }

    private boolean isGradleProject(Project project) {

        return new File(project.getFileSystem().getBasedir(), "build.gradle").exists();
    }

    @Override
    public String toString() {
        return this.getClass().getSimpleName();
    }
}
