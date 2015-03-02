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
import com.android.tools.lint.client.api.*;
import com.android.tools.lint.detector.api.*;
import com.google.common.annotations.VisibleForTesting;
import com.google.common.collect.Sets;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.FileFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.BatchExtension;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.SonarException;
import org.sonar.api.utils.TimeProfiler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
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
    private IssueRegistry registry;

    public AndroidLintExecutor(RuleFinder ruleFinder, ModuleFileSystem fs, RulesProfile rulesProfile, ProjectClasspath projectClasspath) {
        this.ruleFinder = ruleFinder;
        this.fs = fs;
        this.rulesProfile = rulesProfile;
        this.projectClasspath = projectClasspath;
        registry = new BuiltinIssueRegistry();
    }

    @VisibleForTesting
    AndroidLintExecutor(RuleFinder ruleFinder, ModuleFileSystem fs, RulesProfile rulesProfile, ProjectClasspath projectClasspath, IssueRegistry registry) {
        this(ruleFinder, fs, rulesProfile, projectClasspath);
        this.registry = registry;
    }

    public void execute(SensorContext sensorContext, org.sonar.api.resources.Project project) {

        this.sensorContext = sensorContext;
        this.project = project;

        // Retrieve source files
        List<File> sourceFiles = new ArrayList<File>();
        for (File sourceDir : project.getFileSystem().getSourceDirs()) {
            sourceFiles.addAll(FileUtils.listFiles(sourceDir, FileFileFilter.FILE, TrueFileFilter.INSTANCE));
        }

        LintDriver driver = new LintDriver(registry, this);
        TimeProfiler profiler = new TimeProfiler().start("Execute Android Lint " + AndroidLintVersion.getVersion());
        driver.analyze(new LintRequest(this, sourceFiles));
        profiler.stop();

    }

    @Override
    public Configuration getConfiguration(Project project) {
        return new Configuration() {

            @Override
            public boolean isEnabled(Issue issue) {
                return rulesProfile.getActiveRule(AndroidLintRuleRepository.REPOSITORY_KEY, issue.getId()) != null;
            }

            @Override
            public void setSeverity(Issue issue, Severity severity) {
                //Allows to reassociate severity and issue. Not needed in SonarQube context this is handled by quality profile.
            }

            @Override
            public void ignore(Context context, Issue issue, Location location, String message, Object data) {
                //Allows to customize ignore/exclusion patterns. Not needed in Sonarqube context.
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
        Resource resource;
        if (location.getFile().isDirectory()) {
            resource = org.sonar.api.resources.Directory.fromIOFile(location.getFile(), project);
        } else {
            resource = org.sonar.api.resources.File.fromIOFile(location.getFile(), project);
        }
        resource = sensorContext.getResource(resource);
        if (resource == null || !"java".equals(resource.getLanguage().getKey())) {
            return Violation.create(rule, project);
        } else {
            return Violation.create(rule, resource);
        }
    }

    private Rule findRule(Issue issue) {
        Rule rule = ruleFinder.findByKey(AndroidLintRuleRepository.REPOSITORY_KEY, issue.getId());
        if (rule == null) {
            throw new SonarException("No Android Lint rule for key " + issue.getId());
        }
        if (!rule.isEnabled()) {
            throw new SonarException("Android Lint rule with key " + issue.getId() + " disabled");
        }
        return rule;
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
        try {
            Set<String> binaryDirPaths = Sets.newHashSet();
            for (File binaryDir : fs.binaryDirs()) {
                if (binaryDir.exists()) {
                    binaryDirPaths.add(binaryDir.getCanonicalPath());
                }
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
