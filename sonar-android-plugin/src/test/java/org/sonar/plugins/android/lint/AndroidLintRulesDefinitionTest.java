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

import static org.fest.assertions.Assertions.assertThat;

import com.android.tools.lint.checks.BuiltinIssueRegistry;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.TextFormat;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;

import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.SubCharacteristics;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;

import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

/**
 * This class tests the AndroidLintRuleRepository class
 *
 * @author Florian Roncari
 */
public class AndroidLintRulesDefinitionTest {

  private static final Map<Category, String> SQALE_BY_LINT_CATEGORY = ImmutableMap.<Category, String>builder()
      .put(Category.SECURITY, SubCharacteristics.SECURITY_FEATURES)
      .put(Category.CORRECTNESS, SubCharacteristics.INSTRUCTION_RELIABILITY)
      .put(Category.PERFORMANCE, SubCharacteristics.EFFICIENCY_COMPLIANCE)
      .put(Category.RTL, SubCharacteristics.LANGUAGE_RELATED_PORTABILITY)
      .put(Category.MESSAGES, SubCharacteristics.INSTRUCTION_RELIABILITY)
      .put(Category.I18N, SubCharacteristics.LANGUAGE_RELATED_PORTABILITY)
      .put(Category.A11Y, SubCharacteristics.USABILITY_ACCESSIBILITY)
      .put(Category.USABILITY, "ToBeDefined")
      .put(Category.ICONS, "ToBeDefined")
      .put(Category.TYPOGRAPHY, "ToBeDefined")
      .build();



  @Test
  public void createRulesTest() {
    AndroidLintRulesDefinition rulesDefinition = new AndroidLintRulesDefinition(new RulesDefinitionXmlLoader());
    RulesDefinition.Context context = new RulesDefinition.Context();
    rulesDefinition.define(context);
    RulesDefinition.Repository repository = context.repository(AndroidLintRulesDefinition.REPOSITORY_KEY);
    List<RulesDefinition.Rule> rules = repository.rules();
    assertThat(rules.size()).isEqualTo(254);
    assertThat(rules.get(1).tags().size() > 0);

    List<RuleIssue> errorMessageOfMissingSqale = Lists.newArrayList();
    IssueRegistry registry = new BuiltinIssueRegistry();
    for (RulesDefinition.Rule rule : rules) {
      if(StringUtils.isEmpty(rule.debtSubCharacteristic())) {
        Issue issue = registry.getIssue(rule.key());
        //FIXME: Ignore rule with Usability category (or parent category) as long as this is not defined in the sqale model by default.
        if(!(Category.USABILITY.equals(issue.getCategory()) || Category.USABILITY.equals(issue.getCategory().getParent()))) {
          errorMessageOfMissingSqale.add(new RuleIssue(rule, issue));
        }
      }
    }
    Collections.sort(errorMessageOfMissingSqale, new Comparator<RuleIssue>() {
      @Override
      public int compare(RuleIssue o1, RuleIssue o2) {
        if (o1 == o2) return 0;
        if (o1.issue == null) return 1;
        if (o2.issue == null) return -1;

        String sqaleCategory1 = SQALE_BY_LINT_CATEGORY.get(o1.issue.getCategory());
        String sqaleCategory2 = SQALE_BY_LINT_CATEGORY.get(o2.issue.getCategory());

        if (sqaleCategory1 == sqaleCategory2) return 0;
        if (sqaleCategory1 == null) return 1;
        if (sqaleCategory2 == null) return -1;
        return sqaleCategory1.compareTo(sqaleCategory2);
      }
    });
    String currentCategory = null;
    for (RuleIssue ruleIssue : errorMessageOfMissingSqale) {
      String sqaleCategory = SQALE_BY_LINT_CATEGORY.get(ruleIssue.issue.getCategory());
      if (currentCategory == null || sqaleCategory == null || !sqaleCategory.equals(currentCategory)) {
        currentCategory = sqaleCategory;
        System.out.println("!_____________" + currentCategory);
      }
      System.out.println(getXml(ruleIssue));
    }
    assertThat(errorMessageOfMissingSqale).isEmpty();
  }

  private String getErrorMessage(RulesDefinition.Rule rule, Issue issue) {
    return StringUtils.rightPad("" + issue.getPriority(), 4)
        + StringUtils.rightPad(issue.getCategory().getFullName(), 22)
        + StringUtils.rightPad(SQALE_BY_LINT_CATEGORY.get(issue.getCategory()), 30)
        + StringUtils.rightPad(rule.key(), 30)
        + issue.getBriefDescription(TextFormat.TEXT);
  }

  private String getXml(RuleIssue ruleIssue) {
    return String.format("      <chc>\n" +
        "        <rule-repo>android-lint</rule-repo>\n" +
        "        <rule-key>%s</rule-key>\n" +
        "        <prop>\n" +
        "          <key>remediationFunction</key>\n" +
        "          <txt>CONSTANT_ISSUE</txt>\n" +
        "        </prop>\n" +
        "        <prop>\n" +
        "          <key>offset</key>\n" +
        "          <val>5</val>\n" +
        "          <txt>min</txt>\n" +
        "        </prop>\n" +
        "      </chc>", ruleIssue.rule.key());

  }

  private class RuleIssue {
    RulesDefinition.Rule rule;
    Issue issue;

    RuleIssue(RulesDefinition.Rule rule, Issue issue) {
      this.rule = rule;
      this.issue = issue;
    }

    @Override
    public String toString() {
      return getErrorMessage(rule, issue);
    }
  }

}
