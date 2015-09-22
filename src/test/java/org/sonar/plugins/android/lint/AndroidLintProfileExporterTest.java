/*
 * SonarQube Android Lint Plugin
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

import com.google.common.collect.Lists;
import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.CharUtils;
import org.custommonkey.xmlunit.Diff;
import org.custommonkey.xmlunit.XMLUnit;
import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rule.RuleKey;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.rules.RulePriority;
import org.sonar.api.rules.RuleQuery;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;
import org.sonar.api.utils.ValidationMessages;
import org.xml.sax.SAXException;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.List;

import static org.fest.assertions.Assertions.assertThat;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AndroidLintProfileExporterTest {
  private final static int RULES_COUNT = 197;

  @Test
  public void mime_type_is_xml() throws Exception {
    assertThat(new AndroidLintProfileExporter().getMimeType()).isEqualTo("text/xml; charset=utf-8");
  }

  @Test
  public void test_exporting_profile() throws Exception {
    StringWriter sw = new StringWriter();
    RuleFinder ruleFinder = mock(RuleFinder.class);
    List<RulesDefinition.Rule> rules = createRules();
    when(ruleFinder.findAll(any(RuleQuery.class))).thenReturn(createAPIRule(rules));
    RulesProfile rulesProfileWithActiveRules = createRulesProfileWithActiveRules(rules);
    new AndroidLintProfileExporter().exportProfile(rulesProfileWithActiveRules, sw);
    String output = sw.toString();
    assertThat(nbOfIssues(output)).isEqualTo(RULES_COUNT);
    assertXmlAreSimilar(output, "exporter/lint.xml");
  }

  @Test
  public void rules_not_activated_should_have_severity_ignore() throws Exception {
    StringWriter sw = new StringWriter();
    RuleFinder ruleFinder = mock(RuleFinder.class);
    List<RulesDefinition.Rule> rules = createRules();
    when(ruleFinder.findAll(any(RuleQuery.class))).thenReturn(createAPIRule(rules));
    new AndroidLintProfileExporter().exportProfile(RulesProfile.create(), sw);
    String output = sw.toString();
    assertThat(nbOfIssues(output)).isEqualTo(RULES_COUNT);
    assertXmlAreSimilar(output, "exporter/lint-ignore.xml");
  }

  private int nbOfIssues(String output) {
    int count = 0;
    int lastIndex = 0;
    while (lastIndex != -1) {
      lastIndex = output.indexOf("issue", lastIndex);
      if (lastIndex != -1) {
        count++;
        lastIndex += "issue".length();
      }
    }
    return count;
  }

  protected RulesProfile createRulesProfileWithActiveRules(List<RulesDefinition.Rule> rules) {
    RulesProfile profile = RulesProfile.create();
    for (RulesDefinition.Rule rule : rules) {
      // Deactivate first three rules for testing purpose.
      profile.activateRule(Rule.create("android-lint", rule.key()), RulePriority.valueOf(rule.severity()));
    }
    return profile;
  }

  private List<RulesDefinition.Rule> createRules() {
    RulesDefinition.Context context = new RulesDefinition.Context();
    new AndroidLintRulesDefinition(new RulesDefinitionXmlLoader()).define(context);
    return context.repository("android-lint").rules();
  }

  private List<Rule> createAPIRule(List<RulesDefinition.Rule> rules) {
    List<Rule> apiRules = Lists.newArrayList();
    for (RulesDefinition.Rule rule : rules) {
      Rule rule1 = Rule.create(rule.repository().key(), rule.key());
      rule1.setSeverity(RulePriority.valueOf(rule.severity()));
      apiRules.add(rule1);
    }
    return apiRules;
  }


  @Test
  public void export_reimport_should_end_up_with_same_quality_profile() throws Exception {
    StringWriter sw = new StringWriter();
    RuleFinder ruleFinder = mock(RuleFinder.class);
    List<RulesDefinition.Rule> rules = createRules();
    when(ruleFinder.findAll(any(RuleQuery.class))).thenReturn(createAPIRule(rules));
    RulesProfile rulesProfileWithActiveRules = createRulesProfileWithActiveRules(rules);
    new AndroidLintProfileExporter().exportProfile(rulesProfileWithActiveRules, sw);
    StringReader sr = new StringReader(sw.toString());
    when(ruleFinder.findByKey(any(RuleKey.class))).then(new Answer<Rule>() {
      @Override
      public Rule answer(InvocationOnMock invocationOnMock) throws Throwable {
        RuleKey ruleKey = (RuleKey) invocationOnMock.getArguments()[0];
        return Rule.create(ruleKey.repository(), ruleKey.rule(), ruleKey.rule());
      }
    });
    RulesProfile importProfile = new AndroidLintProfileImporter(ruleFinder).importProfile(sr, ValidationMessages.create());

    assertThat(importProfile).isEqualTo(rulesProfileWithActiveRules);
    assertThat(importProfile.getActiveRules()).hasSize(rulesProfileWithActiveRules.getActiveRules().size());
    assertThat(importProfile.getActiveRules()).contains(rulesProfileWithActiveRules.getActiveRules().toArray());
  }

  protected void assertXmlAreSimilar(String actualContent, String expectedFileName) throws SAXException, IOException {
    File expectedContent = FileUtils.toFile(getClass().getResource("/" + expectedFileName));
    assertSimilarXml(expectedContent, actualContent);
  }

  private void assertSimilarXml(File expectedFile, String xml) throws SAXException, IOException {
    XMLUnit.setIgnoreWhitespace(true);
    Reader reader = new FileReader(expectedFile);
    Diff diff = XMLUnit.compareXML(reader, xml);
    String message = "Diff: " + diff.toString() + CharUtils.LF + "XML: " + xml;
    assertTrue(message, diff.similar());
  }

}
