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
package org.sonar.plugins.android;

import com.google.common.collect.ImmutableList;
import org.sonar.api.Properties;
import org.sonar.api.Property;
import org.sonar.api.SonarPlugin;
import org.sonar.plugins.android.emma.AndroidEmmaSensor;
import org.sonar.plugins.android.lint.AndroidLintRulesDefinition;
import org.sonar.plugins.android.lint.AndroidLintSensor;
import org.sonar.plugins.android.lint.AndroidLintSonarWay;

import java.util.List;

@Properties({
    @Property(
        key = AndroidPlugin.EMMA_REPORT_DIR_PROPERTY,
        name = "Emma Report file",
        description = "Path (absolute or relative) of directory where the .ec and the .em Emma files are generated.",
        module = true,
        project = true,
        global = false
    ),
    @Property(
            key = AndroidPlugin.LINT_REPORT_PROPERTY,
            name = "Lint Report file",
            description = "Path (absolute or relative) to the lint-results.xml file.",
            defaultValue = AndroidPlugin.LINT_REPORT_PROPERTY_DEFAULT,
            module = true,
            project = true,
            global = false
    )
})
public class AndroidPlugin extends SonarPlugin {

  public static final String EMMA_REPORT_DIR_PROPERTY = "sonar.android.emma.report";
  public static final String LINT_REPORT_PROPERTY = "sonar.android.lint.report";
  public static final String LINT_REPORT_PROPERTY_DEFAULT = "build/outputs/lint-results.xml";

  @Override
  public List getExtensions() {
    return ImmutableList.of(
        AndroidLintSensor.class,
        AndroidLintRulesDefinition.class,
        AndroidLintSonarWay.class,
        AndroidEmmaSensor.class
    );
  }
}
