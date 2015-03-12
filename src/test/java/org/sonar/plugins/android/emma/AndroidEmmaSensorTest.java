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

package org.sonar.plugins.android.emma;

import org.junit.Before;
import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.batch.fs.internal.DefaultInputFile;
import org.sonar.api.config.Settings;
import org.sonar.api.measures.Metric;
import org.sonar.api.resources.Project;
import org.sonar.plugins.android.AndroidPlugin;
import org.sonar.plugins.java.api.JavaResourceLocator;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyDouble;
import static org.mockito.Matchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyZeroInteractions;
import static org.mockito.Mockito.when;

public class AndroidEmmaSensorTest {

  private Project project;
  private Settings settings;
  private SensorContext context;
  private JavaResourceLocator jrl;
  private DefaultFileSystem fs;

  @Before
  public void setUp() throws Exception {
    project = mock(Project.class);
    settings = new Settings();
    jrl = mock(JavaResourceLocator.class);
    fs = new DefaultFileSystem();
    context = mock(SensorContext.class);
  }

  @Test
  public void should_not_execute_if_report_directory_empty() {
    settings.setProperty(AndroidPlugin.EMMA_REPORT_DIR_PROPERTY, "");
    assertThat(new AndroidEmmaSensor(settings, jrl, fs).shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void should_not_execute_if_report_directory_not_empty_and_no_java_files() {
    settings.setProperty(AndroidPlugin.EMMA_REPORT_DIR_PROPERTY, "src/test/resources");
    assertThat(new AndroidEmmaSensor(settings, jrl, fs).shouldExecuteOnProject(project)).isFalse();
  }

  @Test
  public void should_execute_if_report_directory_not_empty() {
    settings.setProperty(AndroidPlugin.EMMA_REPORT_DIR_PROPERTY, "src/test/resources");
    fs.add(new DefaultInputFile("HelloWorld.java").setLanguage("java"));
    assertThat(new AndroidEmmaSensor(settings, jrl, fs).shouldExecuteOnProject(project)).isTrue();
  }

  @Test
  public void should_do_nothing_if_no_report() {
    DefaultInputFile dif = new DefaultInputFile("HelloWorld");
    dif.setAbsolutePath(this.getClass().getResource("/HelloWorld").getFile());
    fs.add(dif);
    fs.add(new DefaultInputFile("HelloWorld.java").setLanguage("java"));
    settings.setProperty(AndroidPlugin.EMMA_REPORT_DIR_PROPERTY, "HelloWorld");
    AndroidEmmaSensor androidEmmaSensor = new AndroidEmmaSensor(settings, jrl, fs);
    androidEmmaSensor.shouldExecuteOnProject(project);
    androidEmmaSensor.analyse(project, context);
    verifyZeroInteractions(context);
  }

  @Test
  public void should_process_emma_reports() throws Exception {
    settings.setProperty(AndroidPlugin.EMMA_REPORT_DIR_PROPERTY, getClass().getResource("/emma").getFile());
    DefaultInputFile buildConfig = new DefaultInputFile("org/example/BuildConfig.java");
    buildConfig.setLanguage("java");
    fs.add(buildConfig);
    DefaultInputFile exampleActivity = new DefaultInputFile("org/example/ExampleActivity.java");
    exampleActivity.setLanguage("java");
    fs.add(exampleActivity);
    when(jrl.findResourceByClassName(anyString())).thenReturn(mock(org.sonar.api.resources.File.class));
    AndroidEmmaSensor androidEmmaSensor = new AndroidEmmaSensor(settings, jrl, fs);
    androidEmmaSensor.shouldExecuteOnProject(project);
    androidEmmaSensor.analyse(project, context);
    verify(context, times(4)).saveMeasure(any(org.sonar.api.resources.File.class), any(Metric.class), anyDouble());
  }

  @Test
  public void should_handle_non_existing_directory() throws Exception {
    settings.setProperty(AndroidPlugin.EMMA_REPORT_DIR_PROPERTY, "foo");
    fs.add(new DefaultInputFile("HelloWorld.java").setLanguage("java"));
    AndroidEmmaSensor androidEmmaSensor = new AndroidEmmaSensor(settings, jrl, fs);
    androidEmmaSensor.shouldExecuteOnProject(project);
    androidEmmaSensor.analyse(project, context);
    verifyZeroInteractions(context);
  }

  @Test
  public void should_handle_refering_a_file() throws Exception {
    settings.setProperty(AndroidPlugin.EMMA_REPORT_DIR_PROPERTY, "emma/coverage.ec");
    DefaultInputFile dif = new DefaultInputFile("emma/coverage.ec");
    dif.setAbsolutePath(this.getClass().getResource("/emma/coverage.ec").getFile());
    fs.add(dif);
    fs.add(new DefaultInputFile("HelloWorld.java").setLanguage("java"));
    AndroidEmmaSensor androidEmmaSensor = new AndroidEmmaSensor(settings, jrl, fs);
    androidEmmaSensor.shouldExecuteOnProject(project);
    androidEmmaSensor.analyse(project, context);
    verifyZeroInteractions(context);
  }

  @Test
  public void test_toString() {
    assertThat(new AndroidEmmaSensor(new Settings(), jrl, fs).toString()).isEqualTo("AndroidEmmaSensor");
  }
}
