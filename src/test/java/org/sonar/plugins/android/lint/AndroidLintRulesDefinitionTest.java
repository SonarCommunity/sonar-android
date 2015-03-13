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

import org.junit.Test;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;

import java.util.List;

import static org.fest.assertions.Assertions.assertThat;

/**
 * This class tests the AndroidLintRuleRepository class
 *
 * @author Florian Roncari
 */
public class AndroidLintRulesDefinitionTest {

  @Test
  public void createRulesTest() {
    RulesDefinition rulesDefinition = new AndroidLintRulesDefinition(new RulesDefinitionXmlLoader());
    RulesDefinition.Context context = new RulesDefinition.Context();
    rulesDefinition.define(context);
    RulesDefinition.Repository repository = context.repository(AndroidLintRulesDefinition.REPOSITORY_KEY);
    List<RulesDefinition.Rule> rules = repository.rules();
    assertThat(rules.size()).isEqualTo(208);
  }
}