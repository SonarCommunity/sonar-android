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

import com.android.tools.lint.checks.BuiltinIssueRegistry;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Issue;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Lists;
import org.apache.commons.lang.StringUtils;
import org.junit.Test;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinition.SubCharacteristics;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.fest.assertions.Assertions.assertThat;

/**
 * This class tests the AndroidLintRuleRepository class
 *
 * @author Florian Roncari
 */
public class AndroidLintRulesDefinitionTest {
    private final static int RULES_COUNT = 197;
  private static final Map<Category, String> SQALE_BY_LINT_CATEGORY = ImmutableMap.<Category, String>builder()
      .put(Category.SECURITY, SubCharacteristics.SECURITY_FEATURES)
      .put(Category.CORRECTNESS, SubCharacteristics.INSTRUCTION_RELIABILITY)
      .put(Category.PERFORMANCE, SubCharacteristics.EFFICIENCY_COMPLIANCE)
      .put(Category.RTL, SubCharacteristics.USABILITY_ACCESSIBILITY)
      .put(Category.MESSAGES, SubCharacteristics.INSTRUCTION_RELIABILITY)
      .put(Category.USABILITY, "ToBeDefined")
      .put(Category.ICONS, "ToBeDefined")
      .put(Category.TYPOGRAPHY, "ToBeDefined")
      .build();



  @Test
  public void createRulesTest() {
    RulesDefinition rulesDefinition = new AndroidLintRulesDefinition(new RulesDefinitionXmlLoader());
    RulesDefinition.Context context = new RulesDefinition.Context();
    rulesDefinition.define(context);
    RulesDefinition.Repository repository = context.repository(AndroidLintRulesDefinition.REPOSITORY_KEY);
    List<RulesDefinition.Rule> rules = repository.rules();
    assertThat(rules.size()).isEqualTo(RULES_COUNT);

    List<String> errorMessageOfMissingSqale = Lists.newArrayList();
    IssueRegistry registry = new BuiltinIssueRegistry();
    for (RulesDefinition.Rule rule : rules) {
      if(StringUtils.isEmpty(rule.debtSubCharacteristic())) {
        Issue issue = registry.getIssue(rule.key());
        //FIXME: Ignore rule with Usability category (or parent category) as long as this is not defined in the sqale model by default.
        if(!(Category.USABILITY.equals(issue.getCategory()) || Category.USABILITY.equals(issue.getCategory().getParent()))) {
          errorMessageOfMissingSqale.add(getErrorMessage(rule, issue));
        }
      }
    }
    Collections.sort(errorMessageOfMissingSqale);
    for (String key : errorMessageOfMissingSqale) {
      System.out.println(key);
    }
    assertThat(errorMessageOfMissingSqale).isEmpty();
  }

  private String getErrorMessage(RulesDefinition.Rule rule, Issue issue) {
    return StringUtils.rightPad("" + issue.getPriority(), 4)
        + StringUtils.rightPad(issue.getCategory().getFullName(), 22)
        + StringUtils.rightPad(SQALE_BY_LINT_CATEGORY.get(issue.getCategory()), 30)
        + StringUtils.rightPad(rule.key(), 30)
        + issue.getDescription(Issue.OutputFormat.TEXT);
  }

}
