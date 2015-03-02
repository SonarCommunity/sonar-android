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
import com.google.common.collect.Lists;
import edu.emory.mathcs.backport.java.util.Collections;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.rules.ActiveRule;

import java.io.File;
import java.util.Arrays;
import java.util.TreeSet;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.*;

public class AndroidLintSensorTest {

    @Rule
    public TemporaryFolder temp = new TemporaryFolder();

    private AndroidLintSensor sensor;
    private AndroidLintExecutor executor;
    private RulesProfile rulesProfile;

    private FileSystem fs;

    @Before
    public void prepare() {
        executor = mock(AndroidLintExecutor.class);
        rulesProfile = mock(RulesProfile.class);
        fs = mock(FileSystem.class);
        sensor = new AndroidLintSensor(rulesProfile, executor, fs);
    }

    @Test
    public void shouldStartExecutor() {
        SensorContext sensorContext = mock(SensorContext.class);
        Project project = mock(Project.class);
        sensor.analyse(project, sensorContext);

        verify(executor).execute(sensorContext, project);

        // To improve coverage
        assertThat(sensor.toString()).isEqualTo("AndroidLintSensor");
    }

    @Test
    public void shouldOnlyRunOnJavaModules() throws Exception {
        when(rulesProfile.getActiveRulesByRepository(AndroidLintRuleRepository.REPOSITORY_KEY)).thenReturn(Arrays.asList(new ActiveRule()));
        File basedir = temp.newFolder();
        new File(basedir, SdkConstants.ANDROID_MANIFEST_XML).createNewFile();
        when(fs.baseDir()).thenReturn(basedir);

        Project project = mock(Project.class);

        TreeSet<String> expectedLanguages = new TreeSet<String>();
        expectedLanguages.add("java");
        when(fs.languages()).thenReturn(new TreeSet<String>(), expectedLanguages);
        assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
        assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    }

    @Test
    public void shouldRunWithAndroidManifestDetection() throws Exception {

        File basedir = temp.newFolder();

        Project project = new Project("Test");
        ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
        project.setFileSystem(fileSystem);
        when(fileSystem.getSourceDirs()).thenReturn(Lists.newArrayList(basedir));
        TreeSet<String> expectedLanguages = new TreeSet<String>();
        expectedLanguages.add("java");
        when(fs.languages()).thenReturn(expectedLanguages);
        when(rulesProfile.getActiveRulesByRepository(AndroidLintRuleRepository.REPOSITORY_KEY)).thenReturn(Arrays.asList(new ActiveRule()));

        when(fs.baseDir()).thenReturn(basedir);

        assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

        new File(basedir, SdkConstants.ANDROID_MANIFEST_XML).createNewFile();
        assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    }

    @Test
    public void shouldRunIfGradleProject() throws Exception {

        File basedir = temp.newFolder();

        Project project = new Project("Test");
        ProjectFileSystem fileSystem = mock(ProjectFileSystem.class);
        project.setFileSystem(fileSystem);
        when(fileSystem.getSourceDirs()).thenReturn(Lists.newArrayList(basedir));
        TreeSet<String> expectedLanguages = new TreeSet<String>();
        expectedLanguages.add("java");
        when(fs.languages()).thenReturn(expectedLanguages);
        when(rulesProfile.getActiveRulesByRepository(AndroidLintRuleRepository.REPOSITORY_KEY)).thenReturn(Arrays.asList(new ActiveRule()));

        when(fileSystem.getBasedir()).thenReturn(basedir);

        assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

        new File(basedir, "build.gradle").createNewFile();
        assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    }

    @Test
    public void shouldNotRunIfNoAndroidRule() throws Exception {
        Project project = mock(Project.class);
        TreeSet<String> expectedLanguages = new TreeSet<String>();
        expectedLanguages.add("java");
        when(fs.languages()).thenReturn(expectedLanguages);
        File basedir = temp.newFolder();
        when(fs.baseDir()).thenReturn(basedir);
        new File(basedir, SdkConstants.ANDROID_MANIFEST_XML).createNewFile();

        when(rulesProfile.getActiveRulesByRepository(AndroidLintRuleRepository.REPOSITORY_KEY)).thenReturn(Collections.emptyList());
        assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

        when(rulesProfile.getActiveRulesByRepository(AndroidLintRuleRepository.REPOSITORY_KEY)).thenReturn(Arrays.asList(new ActiveRule()));
        assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
    }

}
