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

import com.google.common.base.Charsets;
import org.apache.commons.io.IOUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;
import org.sonar.squidbridge.rules.SqaleXmlLoader;

import java.io.InputStream;
import java.io.InputStreamReader;

public class AndroidLintRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "android-lint";
  public static final String REPOSITORY_NAME = "Android Lint";
  public static final String RULES_XML_PATH = "/org/sonar/plugins/android/lint/rules.xml";

  private RulesDefinitionXmlLoader xmlLoader;

  public AndroidLintRulesDefinition(RulesDefinitionXmlLoader xmlLoader) {
    this.xmlLoader = xmlLoader;
  }

  @Override
  public void define(Context context) {
    define(context, RULES_XML_PATH);
  }

  void define(Context context, String rulesXmlPaths) {
    NewRepository repository = context.createRepository(REPOSITORY_KEY, "java").setName(REPOSITORY_NAME);
    InputStream inputStream = getClass().getResourceAsStream(rulesXmlPaths);
    InputStreamReader reader = new InputStreamReader(inputStream, Charsets.UTF_8);
    try {
      xmlLoader.load(repository, reader);
      SqaleXmlLoader.load(repository, "/org/sonar/plugins/android/lint/java-model.xml");
      repository.done();
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }
}
