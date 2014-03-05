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
import org.mockito.Matchers;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.scan.filesystem.FileQuery;
import org.sonar.api.scan.filesystem.ModuleFileSystem;

import java.io.File;
import java.util.Arrays;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AndroidLintSensorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private AndroidLintSensor sensor;
  private AndroidLintExecutor executor;
  private RulesProfile rulesProfile;
  private Settings settings;

  private ModuleFileSystem fs;

  @Before
  public void prepare() {
    executor = mock(AndroidLintExecutor.class);
    rulesProfile = mock(RulesProfile.class);
    fs = mock(ModuleFileSystem.class);
    settings = new Settings();
    sensor = new AndroidLintSensor(rulesProfile, executor, fs, settings);
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
    when(rulesProfile.getActiveRulesByRepository(AndroidLintConstants.REPOSITORY_KEY)).thenReturn(Arrays.asList(new ActiveRule()));
    File basedir = temp.newFolder();
    new File(basedir, SdkConstants.ANDROID_MANIFEST_XML).createNewFile();
    when(fs.baseDir()).thenReturn(basedir);

    Project project = mock(Project.class);
    when(fs.files(Matchers.<FileQuery>any())).thenReturn(Collections.emptyList(), Lists.newArrayList(new File("MyClass.java")));
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void shouldOnlyRunOnAndroidModules() throws Exception {
    Project project = mock(Project.class);
    when(fs.files(Matchers.<FileQuery>any())).thenReturn(Lists.newArrayList(new File("MyClass.java")));
    when(rulesProfile.getActiveRulesByRepository(AndroidLintConstants.REPOSITORY_KEY)).thenReturn(Arrays.asList(new ActiveRule()));

    File basedir = temp.newFolder();
    when(fs.baseDir()).thenReturn(basedir);

    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    new File(basedir, SdkConstants.ANDROID_MANIFEST_XML).createNewFile();
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void shouldNotRunIfNoAndroidRule() throws Exception {
    Project project = mock(Project.class);
    when(fs.files(Matchers.<FileQuery>any())).thenReturn(Lists.newArrayList(new File("MyClass.java")));
    File basedir = temp.newFolder();
    when(fs.baseDir()).thenReturn(basedir);
    new File(basedir, SdkConstants.ANDROID_MANIFEST_XML).createNewFile();

    when(rulesProfile.getActiveRulesByRepository(AndroidLintConstants.REPOSITORY_KEY)).thenReturn(Collections.emptyList());
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    when(rulesProfile.getActiveRulesByRepository(AndroidLintConstants.REPOSITORY_KEY)).thenReturn(Arrays.asList(new ActiveRule()));
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void shouldOnlyRunOnAndroidModulesProjectPathSet() throws Exception {
    String projectPath = "AndroidProject";

    Project project = mock(Project.class);
    when(fs.files(Matchers.<FileQuery>any())).thenReturn(Lists.newArrayList(new File("MyClass.java")));
    when(rulesProfile.getActiveRulesByRepository(AndroidLintConstants.REPOSITORY_KEY)).thenReturn(Arrays.asList(new ActiveRule()));

    Settings settings = new Settings();
    settings.setProperty("sonar.androidLint.projectPath", projectPath);

    AndroidLintSensor sensor = new AndroidLintSensor(rulesProfile, executor, fs, settings);

    File basedir = temp.newFolder();
    when(fs.baseDir()).thenReturn(basedir);

    File projectDir = new File(basedir, projectPath);
    projectDir.mkdir();

    assertThat(sensor.shouldExecuteOnProject(project)).isEqualTo(false);

    new File(projectDir, SdkConstants.ANDROID_MANIFEST_XML).createNewFile();
    assertThat(sensor.shouldExecuteOnProject(project)).isEqualTo(true);
  }

}
