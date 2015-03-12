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

import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.internal.DefaultFileSystem;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Project;
import org.sonar.plugins.android.AndroidPlugin;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Mockito.mock;

public class AndroidLintSensorTest {

  @Rule
  public TemporaryFolder temp = new TemporaryFolder();

  private RulesProfile rulesProfile;
  private Settings settings;
  private ResourcePerspectives perspectives;

  private FileSystem fs;

  @Before
  public void prepare() {
    rulesProfile = mock(RulesProfile.class);
    settings = new Settings();
    fs = new DefaultFileSystem();
    perspectives = mock(ResourcePerspectives.class);
  }

  @Test
  public void testShouldExecuteOnProject() throws Exception {
    Project project = mock(Project.class);

    AndroidLintSensor sensor = new AndroidLintSensor(settings, rulesProfile, perspectives, fs);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    settings.setProperty(AndroidPlugin.LINT_REPORT_PROPERTY, AndroidPlugin.LINT_REPORT_PROPERTY_DEFAULT);
    sensor = new AndroidLintSensor(settings, rulesProfile, perspectives, fs);
    assertThat(sensor.shouldExecuteOnProject(project)).isFalse();

    settings.setProperty(AndroidPlugin.LINT_REPORT_PROPERTY, getClass().getResource("/lint-report.xml").getFile());
    sensor = new AndroidLintSensor(settings, rulesProfile, perspectives, fs);
    assertThat(sensor.shouldExecuteOnProject(project)).isTrue();

    settings.removeProperty(AndroidPlugin.LINT_REPORT_PROPERTY);
  }
}
