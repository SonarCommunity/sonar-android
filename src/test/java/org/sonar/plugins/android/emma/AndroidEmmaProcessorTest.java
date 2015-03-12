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

import org.junit.Test;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.measures.CoreMetrics;
import org.sonar.plugins.java.api.JavaResourceLocator;

import java.io.File;

import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

public class AndroidEmmaProcessorTest {

  @Test
  public void process_should_read_emma_files() throws Exception {
    File dir = new File(getClass().getResource("/emma").getFile());

    SensorContext context = mock(SensorContext.class);
    org.sonar.api.resources.File exampleActivity = mock(org.sonar.api.resources.File.class);
    org.sonar.api.resources.File buildConfig = mock(org.sonar.api.resources.File.class);
    JavaResourceLocator jrl = mock(JavaResourceLocator.class);
    when(jrl.findResourceByClassName("org.example.ExampleActivity")).thenReturn(exampleActivity);
    when(jrl.findResourceByClassName("org.example.BuildConfig")).thenReturn(buildConfig);
    new AndroidEmmaProcessor(dir, jrl, context).process();

    verify(context).saveMeasure(eq(exampleActivity),
        eq(CoreMetrics.LINES_TO_COVER),
        eq(7d));
    verify(context).saveMeasure(eq(exampleActivity),
        eq(CoreMetrics.UNCOVERED_LINES),
        eq(1d));
    verify(context).saveMeasure(eq(buildConfig),
        eq(CoreMetrics.LINES_TO_COVER),
        eq(1d));
    verify(context).saveMeasure(eq(buildConfig),
        eq(CoreMetrics.UNCOVERED_LINES),
        eq(1d));
  }

  @Test
  public void process_should_log_files_in_error() throws Exception {
    File dir = new File(getClass().getResource("/emma").getFile());
    SensorContext context = mock(SensorContext.class);
    JavaResourceLocator jrl = mock(JavaResourceLocator.class);
    org.sonar.api.resources.File file = mock(org.sonar.api.resources.File.class);
    when(jrl.findResourceByClassName("org.example.BuildConfig")).thenReturn(file);
    new AndroidEmmaProcessor(dir, jrl, context).process();
    verify(context).saveMeasure(eq(file),
        eq(CoreMetrics.LINES_TO_COVER),
        eq(1d));
    verify(context).saveMeasure(eq(file),
        eq(CoreMetrics.UNCOVERED_LINES),
        eq(1d));
  }
}
