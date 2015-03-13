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

import com.google.common.collect.Lists;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleRepository;
import org.sonar.api.rules.XMLRuleParser;

import java.util.List;

/**
 * Repository for Android Lint rules (using {@link AndroidLintRuleParser}
 *
 * @author Stephane Nicolas
 * @author Thomas Bores
 */
public final class AndroidLintRuleRepository extends RuleRepository {
  private final XMLRuleParser xmlRuleParser;
  public static final String REPOSITORY_KEY = "android-lint";
  public static final String REPOSITORY_NAME = "Android Lint";
  public static final String RULES_XML_PATH = "/org/sonar/plugins/android/lint/rules.xml";

  public AndroidLintRuleRepository(XMLRuleParser xmlRuleParser) {
    super(REPOSITORY_KEY, "java");
    setName(REPOSITORY_NAME);
    this.xmlRuleParser = xmlRuleParser;
  }

  @Override
  public List<Rule> createRules() {
    List<Rule> rules = Lists.newArrayList();
    rules.addAll(xmlRuleParser.parse(getClass().getResourceAsStream(RULES_XML_PATH)));
    return rules;
  }
}
