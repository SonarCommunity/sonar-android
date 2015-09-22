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

import org.junit.Test;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.profiles.XMLProfileParser;
import org.sonar.api.rules.Rule;
import org.sonar.api.rules.RuleFinder;
import org.sonar.api.utils.ValidationMessages;

import static org.fest.assertions.Assertions.assertThat;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class AndroidLintSonarWayTest {

  @Test
  public void createSonarWayTest() {
    RuleFinder ruleFinder = mock(RuleFinder.class);
    when(ruleFinder.findByKey(eq(AndroidLintRulesDefinition.REPOSITORY_KEY), anyString()))
      .thenAnswer(new Answer<Rule>() {
        @Override
        public Rule answer(InvocationOnMock invocation) throws Throwable {
          return Rule.create(AndroidLintRulesDefinition.REPOSITORY_KEY, (String) invocation.getArguments()[1], (String) invocation.getArguments()[1]);
        }
      }
      );
    AndroidLintSonarWay sonarWay = new AndroidLintSonarWay(new XMLProfileParser(ruleFinder));

    RulesProfile profile = sonarWay.createProfile(ValidationMessages.create());

    assertThat(profile.getActiveRules().size()).isEqualTo(183);
  }
}
