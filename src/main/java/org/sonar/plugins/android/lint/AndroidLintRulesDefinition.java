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

import org.apache.commons.io.IOUtils;
import org.sonar.api.server.rule.RulesDefinition;
import org.sonar.api.server.rule.RulesDefinitionI18nLoader;
import org.sonar.api.server.rule.RulesDefinitionXmlLoader;
import org.sonar.plugins.java.Java;

import java.io.InputStream;
import java.io.InputStreamReader;

public class AndroidLintRulesDefinition implements RulesDefinition {

  public static final String REPOSITORY_KEY = "android-lint";
  public static final String REPOSITORY_NAME = "Android Lint";
  public static final String RULES_XML_PATH = "/org/sonar/plugins/android/lint/rules.xml";


  private RulesDefinitionXmlLoader xmlLoader;
  private RulesDefinitionI18nLoader i18nLoader;

  public AndroidLintRulesDefinition(RulesDefinitionXmlLoader xmlLoader) {
    this.xmlLoader = xmlLoader;
  }

  @Override
  public void define(Context context) {
    NewRepository repository = context.createRepository(REPOSITORY_KEY, Java.KEY).setName(REPOSITORY_NAME);
    InputStream inputStream = getClass().getResourceAsStream(RULES_XML_PATH);
    InputStreamReader reader = new InputStreamReader(inputStream);
    try {
      xmlLoader.load(repository, reader);
      repository.done();
    } finally {
      IOUtils.closeQuietly(reader);
    }
  }
}
