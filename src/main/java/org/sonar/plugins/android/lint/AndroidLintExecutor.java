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

import com.android.annotations.NonNull;
import com.android.tools.lint.LintCliXmlParser;
import com.android.tools.lint.LombokParser;
import com.android.tools.lint.checks.BuiltinIssueRegistry;
import com.android.tools.lint.client.api.Configuration;
import com.android.tools.lint.client.api.IDomParser;
import com.android.tools.lint.client.api.IJavaParser;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.client.api.LintClient;
import com.android.tools.lint.client.api.LintDriver;
import com.android.tools.lint.client.api.LintRequest;
import com.android.tools.lint.detector.api.Context;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.LintUtils;
import com.android.tools.lint.detector.api.Location;
import com.android.tools.lint.detector.api.Project;
import com.android.tools.lint.detector.api.Severity;
import com.google.common.collect.Sets;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.config.Settings;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.scan.filesystem.PathResolver;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TimeProfiler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;

public class AndroidLintExecutor extends LintClient implements BatchExtension {

  private static final Logger LOG = LoggerFactory.getLogger(AndroidLintExecutor.class);
  private ModuleFileSystem fs;
  private SensorContext sensorContext;
  private org.sonar.api.resources.Project project;
  private RuleFinder ruleFinder;
  private RulesProfile rulesProfile;
  private ProjectClasspath projectClasspath;
  private PathResolver resolver;
  private Settings settings;

  public AndroidLintExecutor(RuleFinder ruleFinder, ModuleFileSystem fs, RulesProfile rulesProfile, ProjectClasspath projectClasspath, PathResolver resolver, Settings settings) {
    this.ruleFinder = ruleFinder;
    this.fs = fs;
    this.rulesProfile = rulesProfile;
    this.projectClasspath = projectClasspath;
    this.resolver = resolver;
    this.settings = settings;
  }

  public void execute(SensorContext sensorContext, org.sonar.api.resources.Project project) {
    this.sensorContext = sensorContext;
    this.project = project;
    String projectFullPath = fs.baseDir().getPath();
    String projectRelPath = settings.getString(AndroidLintConstants.PROJECT_PATH_PROPERTY);
    if (projectRelPath != null) {
      projectFullPath += "/" + projectRelPath;
    }
    File projectDir = new File(projectFullPath);
    IssueRegistry registry = new BuiltinIssueRegistry();
    LintDriver driver = new LintDriver(registry, this);

    TimeProfiler profiler = new TimeProfiler().start("Execute Android Lint " + AndroidLintVersion.getVersion());
    driver.analyze(new LintRequest(this, Arrays.asList(projectDir)));
    profiler.stop();
  }

  @Override
  public Configuration getConfiguration(Project project) {
    return new Configuration() {

      @Override
      public boolean isEnabled(Issue issue) {
        return rulesProfile.getActiveRule(AndroidLintConstants.REPOSITORY_KEY, issue.getId()) != null;
      }

      @Override
      public void setSeverity(Issue issue, Severity severity) {
      }

      @Override
      public void ignore(Context context, Issue issue, Location location, String message, Object data) {
      }
    };
  }

  @Override
  public void report(Context context, Issue issue, Severity severity, Location location, String message, Object data) {
    Rule rule = findRule(issue);

    Violation violation = createViolation(location, rule);

    if (violation != null) {
      int line = location.getStart() != null ? location.getStart().getLine() + 1 : 0;
      if (line > 0) {
        violation.setLineId(line);
      }
      violation.setMessage(message);
      sensorContext.saveViolation(violation);
    }
  }

  private Violation createViolation(Location location, Rule rule) {
    Violation violation;
    if (location.getFile().isDirectory()) {
      violation = Violation.create(rule, new org.sonar.api.resources.Directory(getRelativePath(location.getFile())));
    } else {
      violation = Violation.create(rule, org.sonar.api.resources.File.fromIOFile(location.getFile(), project));
    }
    return violation;
  }

  private Rule findRule(Issue issue) {
    Rule rule = ruleFinder.findByKey(AndroidLintConstants.REPOSITORY_KEY, issue.getId());
    if (rule == null) {
      throw new SonarException("No Android Lint rule for key " + issue.getId());
    }
    if (!rule.isEnabled()) {
      throw new SonarException("Android Lint rule with key " + issue.getId() + " disabled");
    }
    return rule;
  }

  private String getRelativePath(File file) {
    return fs.baseDir().toURI().relativize(file.toURI()).getPath();
  }

  @Override
  public void log(Severity severity, Throwable exception, String format, Object... args) {
    String msg = null;
    if (format != null) {
      msg = String.format(format, args);
    }
    switch (severity) {
      case FATAL:
      case ERROR:
        LOG.error(msg, exception);
        break;
      case WARNING:
        LOG.warn(msg, exception);
        break;
      case INFORMATIONAL:
        LOG.info(msg, exception);
        break;
      case IGNORE:
      default:
        LOG.debug(msg, exception);
        break;
    }

  }

  @Override
  @NonNull
  protected ClassPathInfo getClassPath(@NonNull Project project) {
    List<File> sources = fs.sourceDirs();
    List<File> classes = fs.binaryDirs();
    List<File> libraries = new ArrayList<File>();
    boolean hasExistingBinaryDir = false;

    try {

      Set<String> binaryDirPaths = Sets.newHashSet();
      for (File binaryDir : fs.binaryDirs()) {
        if (binaryDir.exists()) {
          hasExistingBinaryDir = true;
          binaryDirPaths.add(binaryDir.getCanonicalPath());
        }
      }
      if (!hasExistingBinaryDir) {
        throw new SonarException("Android Lint needs sources to be compiled. "
          + "Please build project before executing SonarQube and check the location of compiled classes.");
      }

      for (File file : projectClasspath.getElements()) {
        if (file.isFile() || !binaryDirPaths.contains(file.getCanonicalPath())) {
          libraries.add(file);
        }
      }
    } catch (IOException e) {
      throw new SonarException("Unable to configure project classpath", e);
    }

    return new ClassPathInfo(sources, classes, libraries);
  }

  @Override
  public IDomParser getDomParser() {
    return new LintCliXmlParser();
  }

  @Override
  public IJavaParser getJavaParser() {
    return new LombokParser();
  }

  @Override
  public String readFile(File file) {
    try {
      return LintUtils.getEncodedString(this, file);
    } catch (IOException e) {
      return ""; //$NON-NLS-1$
    }
  }
}
