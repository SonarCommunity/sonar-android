/*
 * Android :: IT
 * Copyright (C) 2013-2016 SonarSource SA
 * mailto:contact AT sonarsource DOT com
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
 * You should have received a copy of the GNU Lesser General Public License
 * along with this program; if not, write to the Free Software Foundation,
 * Inc., 51 Franklin Street, Fifth Floor, Boston, MA  02110-1301, USA.
 */
package com.sonar.it.android;

import static org.fest.assertions.Assertions.assertThat;
import static org.fest.assertions.Fail.fail;
import static org.junit.Assume.assumeTrue;

import com.google.common.base.Charsets;
import com.sonar.orchestrator.Orchestrator;
import com.sonar.orchestrator.build.SonarRunner;

import org.apache.commons.io.FileUtils;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.sonar.wsclient.Sonar;
import org.sonar.wsclient.services.Resource;
import org.sonar.wsclient.services.ResourceQuery;

import java.io.File;

public class AndroidTest {

  @ClassRule
  public static Orchestrator orchestrator = AndroidTestSuite.ORCHESTRATOR;

  private static Sonar sonar;

  @BeforeClass
  public static void inspect() throws Exception {
    sonar = orchestrator.getServer().getWsClient();
  }

  @Before
  public void truncateData() {
    orchestrator.resetData();
  }

  @Test
  public void sonar_android_sample_project() throws Exception {
    assumeTrue(AndroidTestSuite.isAtLeastPlugin1_1());
    SonarRunner analysis = SonarRunner.create()
      .setProfile("it-profile")
      .setProjectName("SonarAndroidSample")
      .setProjectKey("SonarAndroidSample")
      .setProjectVersion("1.0")
      .setSourceDirs("app/src/main")
      .setProjectDir(new File("projects/SonarAndroidSample"))
      .setProperty("sonar.android.lint.report", "app/lint-results.xml")
      .setProperty("sonar.import_unknown_files", "true");
    orchestrator.executeBuild(analysis);
    Resource project = sonar.find(ResourceQuery.createForMetrics("SonarAndroidSample", "violations"));

    int expectedViolations = 4;
    if (AndroidTestSuite.sonarqube_version_is_after_5_1()) {
      //After version 5.1 xml files will be indexed thanks to sonar.import_unknown_files parameter and so issues can be reported on them.
      expectedViolations = 8;
    }
    assertThat(project.getMeasureIntValue("violations")).isEqualTo(expectedViolations);
  }

  @Test
  public void should_exclude_source_files() {
    assumeTrue(AndroidTestSuite.isAtLeastPlugin1_1());
    SonarRunner analysis = SonarRunner.create()
      .setProfile("it-profile")
      .setProjectName("SonarAndroidSample")
      .setProjectKey("SonarAndroidSample")
      .setProjectVersion("1.0")
      .setSourceDirs("app/src/main")
      .setProjectDir(new File("projects/SonarAndroidSample"))
      .setProperty("skipTests", "true")
      .setProperty("sonar.global.exclusions", "**/TestViolations.java")
      .setProperty("sonar.android.lint.report", "app/lint-results.xml")
      .setProperty("sonar.import_unknown_files", "true");

    orchestrator.executeBuild(analysis);
    Resource project = sonar.find(ResourceQuery.createForMetrics("SonarAndroidSample", "violations"));

    int expectedViolations = 0;
    if (AndroidTestSuite.sonarqube_version_is_after_5_1()) {
      //After version 5.1 xml files will be indexed thanks to sonar.import_unknown_files parameter and so issues can be reported on them.
      expectedViolations = 4;
    }
    assertThat(project.getMeasureIntValue("violations")).isEqualTo(expectedViolations);
  }

  @Test
  public void should_export_profile() throws Exception {
    assumeTrue(AndroidTestSuite.isAtLeastPlugin1_1());
    //url : profiles/export?format=android-lint&language=java&name=Android%2520Lint
    String response = exportProfile("Android Lint");

    String expectedExport = FileUtils.readFileToString(new File("src/test/resources/lint-export.xml"));
    assertThat(response).isEqualTo(expectedExport);
  }

  private String exportProfile(String profileName) {
    String response;
    if(AndroidTestSuite.sonarqube_version_is_after_5_2()) {
      response = orchestrator.getServer().adminWsClient().get("api/qualityprofiles/export", "language", "java", "name", profileName, "exporterKey", "android-lint");
    } else {
      response = orchestrator.getServer().adminWsClient().get("profiles/export", "language", "java", "name", profileName, "format", "android-lint");
    }
    return response;
  }

  @Test
  @Ignore("Deactivated awaiting resolution of http://jira.sonarsource.com/browse/JC-145")
  public void should_run_lint_after_export_and_import_results() throws Exception {
    assumeTrue(AndroidTestSuite.isAtLeastPlugin1_1());
    String response = exportProfile("it-profile");
    File baseDir = new File("projects/SonarAndroidSample/app");
    FileUtils.write(new File(baseDir, "lint.xml"), response, Charsets.UTF_8);
    ProcessBuilder pb = new ProcessBuilder("CMD", "/C", "gradle lint");
    pb.directory(baseDir);
    pb.inheritIO();
    Process gradleProcess = pb.start();
    int exitStatus = gradleProcess.waitFor();
    if(exitStatus != 0) {
      fail("Failed to execute gradle lint.");
    }
    SonarRunner analysis = SonarRunner.create()
        .setProfile("it-profile")
        .setProjectName("SonarAndroidSample2")
        .setProjectKey("SonarAndroidSample2")
        .setProjectVersion("1.0")
        .setSourceDirs("src/main")
        .setProjectDir(baseDir)
        .setProperty("sonar.android.lint.report", "lint-report-build.xml")
        .setProperty("sonar.import_unknown_files", "true");
    orchestrator.executeBuild(analysis);
    Resource project = sonar.find(ResourceQuery.createForMetrics("SonarAndroidSample2", "violations"));

    assertThat(project.getMeasureIntValue("violations")).isEqualTo(2);

  }
}
