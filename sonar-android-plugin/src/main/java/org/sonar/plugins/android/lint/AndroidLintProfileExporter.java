/*
 * SonarQube Android Lint Plugin
 * Copyright (C) 2013-2016 SonarSource SA and Jerome Van Der Linden, Stephane Nicolas, Florian Roncari, Thomas Bores
 * sonarqube@googlegroups.com
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
package org.sonar.plugins.android.lint;

import com.android.tools.lint.detector.api.Severity;
import com.google.common.base.Charsets;
import com.google.common.collect.Lists;
import com.google.common.net.MediaType;

import org.apache.commons.lang.StringUtils;
import org.codehaus.staxmate.SMInputFactory;
import org.codehaus.staxmate.in.SMHierarchicCursor;
import org.codehaus.staxmate.in.SMInputCursor;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.profiles.ProfileExporter;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.api.rules.RulePriority;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Writer;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;

public class AndroidLintProfileExporter extends ProfileExporter {

  private static final Logger LOGGER = LoggerFactory.getLogger(AndroidLintProfileExporter.class);
  private final String rulesXmlPath;

  private Collection<String> ruleKeys;

  /**
   * Constructor to be used on batch side as ProfileExporter is a batch extension and thus might not
   * be injected RuleFinder.
   */
  public AndroidLintProfileExporter() {
    this(AndroidLintRulesDefinition.RULES_XML_PATH);
  }

  /**
   * Used for testing.
   */
  AndroidLintProfileExporter(String rulesXmlPath) {
    super(AndroidLintRulesDefinition.REPOSITORY_KEY, AndroidLintRulesDefinition.REPOSITORY_NAME);
    this.rulesXmlPath = rulesXmlPath;
    ruleKeys = Lists.newArrayList();
    loadRuleKeys();
    setSupportedLanguages("java", "xml");
    setMimeType(MediaType.XML_UTF_8.toString());
  }

  private void loadRuleKeys() {
    XMLInputFactory xmlFactory = XMLInputFactory.newInstance();
    xmlFactory.setProperty(XMLInputFactory.IS_COALESCING, Boolean.TRUE);
    xmlFactory.setProperty(XMLInputFactory.IS_NAMESPACE_AWARE, Boolean.FALSE);
    // just so it won't try to load DTD in if there's DOCTYPE
    xmlFactory.setProperty(XMLInputFactory.SUPPORT_DTD, Boolean.FALSE);
    xmlFactory.setProperty(XMLInputFactory.IS_VALIDATING, Boolean.FALSE);
    SMInputFactory inputFactory = new SMInputFactory(xmlFactory);
    InputStream inputStream = getClass().getResourceAsStream(rulesXmlPath);
    InputStreamReader reader = new InputStreamReader(inputStream, Charsets.UTF_8);
    try {
      SMHierarchicCursor rootC = inputFactory.rootElementCursor(reader);
      rootC.advance(); // <rules>

      SMInputCursor rulesC = rootC.childElementCursor("rule");
      while (rulesC.getNext() != null) {
        // <rule>
        SMInputCursor cursor = rulesC.childElementCursor();
        while (cursor.getNext() != null) {
          if (StringUtils.equalsIgnoreCase("key", cursor.getLocalName())) {
            String key = StringUtils.trim(cursor.collectDescendantText(false));
            ruleKeys.add(key);
          }
        }
      }

    } catch (XMLStreamException e) {
      throw new IllegalStateException("XML is not valid", e);
    }
  }

  @Override
  public void exportProfile(RulesProfile profile, Writer writer) {
    Serializer serializer = new Persister();
    try {
      serializer.write(createLintProfile(profile.getActiveRules()), writer);
    } catch (Exception e) {
      LOGGER.error("Could not export lint profile", e);
    }
  }

  private LintProfile createLintProfile(List<ActiveRule> activeRules) {
    LintProfile profile = new LintProfile();
    Map<String, RulePriority> activeKeys = new HashMap<>();
    List<LintIssue> issues = Lists.newArrayList();
    for (ActiveRule rule : activeRules) {
      activeKeys.put(rule.getRuleKey(), rule.getSeverity());
    }
    for (String ruleKey : ruleKeys) {
      issues.add(getLintIssue(ruleKey, activeKeys));
    }
    // ensure order of issues in output, sort by key.
    Collections.sort(issues, new IssueComparator());
    profile.issues = issues;
    return profile;
  }

  private static class IssueComparator implements Comparator<LintIssue> {
    @Override
    public int compare(LintIssue o1, LintIssue o2) {
      return o1.id.compareTo(o2.id);
    }
  }

  @Root(name = "lint", strict = false)
  static class LintProfile {
    @ElementList(inline = true)
    List<LintIssue> issues;
  }
  @Root(name = "issue", strict = false)
  static class LintIssue {
    @Attribute
    String id;
    @Attribute(required = false)
    String severity;
    @Attribute(required = false)
    Integer priority;

    public LintIssue() {
      // No arg constructor used by profile importer
    }

    public LintIssue(String ruleKey, String severity, @Nullable Integer priority) {
      this.id = ruleKey;
      this.severity = severity;
      this.priority = priority;
    }
  }

  private LintIssue getLintIssue(String key, Map<String, RulePriority> activeKeys) {
    if (!activeKeys.containsKey(key)) {
      return new LintIssue(key, Severity.IGNORE.getDescription(), null);
    }

    String lintSeverity = "";
    RulePriority severity = activeKeys.get(key);
    if (severity.equals(RulePriority.BLOCKER)) {
      lintSeverity = Severity.FATAL.getDescription();
    }
    if (severity.equals(RulePriority.CRITICAL)) {
      lintSeverity = Severity.ERROR.getDescription();
    }
    if (severity.equals(RulePriority.MAJOR)) {
      lintSeverity = Severity.ERROR.getDescription();
    }
    if (severity.equals(RulePriority.MINOR)) {
      lintSeverity = Severity.WARNING.getDescription();
    }
    if (severity.equals(RulePriority.INFO)) {
      lintSeverity = Severity.INFORMATIONAL.getDescription();
    }
    return new LintIssue(key, lintSeverity, null);
  }
}
