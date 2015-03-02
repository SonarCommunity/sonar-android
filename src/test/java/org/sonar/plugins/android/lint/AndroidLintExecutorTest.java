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

import com.android.tools.lint.checks.ApiDetector;
import com.android.tools.lint.checks.BuiltinIssueRegistry;
import com.android.tools.lint.checks.InvalidPackageDetector;
import com.android.tools.lint.checks.TypoDetector;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.Severity;
import com.google.common.collect.Lists;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.junit.Before;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.sonar.api.batch.ProjectClasspath;
import org.sonar.api.batch.SensorContext;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.resources.Language;
import org.sonar.api.resources.Project;
import org.sonar.api.resources.ProjectFileSystem;
import org.sonar.api.resources.Resource;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.Violation;
import org.sonar.api.scan.filesystem.ModuleFileSystem;
import org.sonar.api.utils.SonarException;

import java.io.File;
import java.net.URISyntaxException;
import java.util.Arrays;
import java.util.List;

import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.argThat;
import static org.mockito.Mockito.eq;
import static org.mockito.Mockito.*;

public class AndroidLintExecutorTest {

    @org.junit.Rule
    public ExpectedException thrown = ExpectedException.none();

    private AndroidLintExecutor executor;
    private RulesProfile rulesProfile;
    private ModuleFileSystem fs;
    private Project project;

    @Before
    public void prepare() throws Exception {
        project = new Project("key");
        ProjectFileSystem pfs = mock(ProjectFileSystem.class);
        when(pfs.getBasedir()).thenReturn(new File(this.getClass().getResource("/HelloWorld").toURI()));
        project.setFileSystem(pfs);
        fs = mock(ModuleFileSystem.class);
        rulesProfile = mock(RulesProfile.class);
        ProjectClasspath projectClasspath = mock(ProjectClasspath.class);
        RuleFinder ruleFinder = mock(RuleFinder.class);
        IssueRegistry registry = new IssueRegistry() {
            @Override
            public List<Issue> getIssues() {
                List<Issue> issues = Lists.newArrayList(new BuiltinIssueRegistry().getIssues());
                issues.remove(ApiDetector.UNSUPPORTED);
                issues.remove(ApiDetector.INLINED);
                issues.remove(ApiDetector.OVERRIDE);
                issues.remove(InvalidPackageDetector.ISSUE);
                issues.remove(TypoDetector.ISSUE);
                return issues;
            }
        };
        executor = new AndroidLintExecutor(ruleFinder, fs, rulesProfile, projectClasspath, registry);
        when(fs.baseDir()).thenReturn(new File(this.getClass().getResource("/HelloWorld").toURI()));
        when(pfs.getSourceDirs()).thenReturn(Arrays.asList(new File(this.getClass().getResource("/HelloWorld").toURI())));
        when(fs.sourceDirs()).thenReturn(Arrays.asList(new File(this.getClass().getResource("/HelloWorld/src").toURI())));
        when(fs.binaryDirs()).thenReturn(Arrays.asList(new File(this.getClass().getResource("/HelloWorld/bin").toURI())));
        when(projectClasspath.getElements()).thenReturn(Arrays.asList(new File(this.getClass().getResource("/HelloWorld/bin").toURI())));
        ActiveRule activeRule = mock(ActiveRule.class);
        when(rulesProfile.getActiveRule(eq(AndroidLintRuleRepository.REPOSITORY_KEY), anyString())).thenReturn(activeRule);
        Rule rule = Rule.create(AndroidLintRuleRepository.REPOSITORY_KEY, "foo");
        when(ruleFinder.findByKey(eq(AndroidLintRuleRepository.REPOSITORY_KEY), anyString())).thenReturn(rule);
    }

    @Test
    public void lintExecutionTest() throws URISyntaxException {
        SensorContext sensorContext = mock(SensorContext.class);
        Resource xmlResource = org.sonar.api.resources.File.create("foo.xml", "foo.xml", getLang("xml"), false);
        Resource javaResource = org.sonar.api.resources.File.create("foo.java", "foo.java", getLang("java"), false);
        when(sensorContext.getResource(any(Resource.class))).thenReturn(xmlResource).thenReturn(xmlResource).thenReturn(javaResource);
        executor.execute(sensorContext, project);

        verify(sensorContext, times(3)).saveViolation(argThat(new MatchViolationResource(javaResource)));
        verify(sensorContext, times(2)).saveViolation(argThat(new MatchViolationResource(project)));
    }

    @Test
    public void shouldNotCreateViolationWhenRuleIsDisabled() {
        when(rulesProfile.getActiveRule(eq(AndroidLintRuleRepository.REPOSITORY_KEY), anyString())).thenReturn(null);
        SensorContext sensorContext = mock(SensorContext.class);
        when(sensorContext.getResource(any(Resource.class))).thenReturn(org.sonar.api.resources.File.create("foo"));
        executor.execute(sensorContext, project);

        verify(sensorContext, never()).saveViolation(any(Violation.class));
    }

    @Test
    public void testSonarExclusions() {
        SensorContext sensorContext = mock(SensorContext.class);
        when(sensorContext.getResource(any(Resource.class))).thenReturn(null).thenReturn(org.sonar.api.resources.File.create("foo.xml", "foo.xml", getLang("xml"), false));
        executor.execute(sensorContext, project);
        verify(sensorContext, times(5)).saveViolation(any(Violation.class));
    }

    @Test
    public void testLog() {
        executor.log(Severity.ERROR, null, "Something %s", "arg");
        executor.log(Severity.FATAL, null, "Something %s", "arg");
        executor.log(Severity.IGNORE, null, "Something %s", "arg");
        executor.log(Severity.INFORMATIONAL, new SonarException(), "Something %s", "arg");
        executor.log(Severity.WARNING, null, "Something %s", "arg");
    }

    private Language getLang(final String key) {
        return new Language() {
            @Override
            public String getKey() {
                return key;
            }

            @Override
            public String getName() {
                return key;
            }

            @Override
            public String[] getFileSuffixes() {
                return new String[]{key};
            }
        };
    }

    private static class MatchViolationResource extends BaseMatcher<Violation> {

        private final Resource resource;

        private MatchViolationResource(Resource resource) {
            this.resource = resource;
        }

        @Override
        public boolean matches(Object item) {
            if (item instanceof Violation) {
                return ((Violation) item).getResource().equals(resource);
            }
            return false;
        }

        @Override
        public void describeTo(Description description) {

        }
    }
}
