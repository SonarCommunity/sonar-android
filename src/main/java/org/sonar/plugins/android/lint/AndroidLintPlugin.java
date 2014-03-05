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

import com.google.common.collect.ImmutableList;
import org.sonar.api.CoreProperties;
import org.sonar.api.SonarPlugin;
import org.sonar.api.config.PropertyDefinition;
import org.sonar.api.resources.Qualifiers;

import java.util.List;

public class AndroidLintPlugin extends SonarPlugin {

  private static final String SETTINGS_CATEGORY_NAME = "Android Lint";

  @Override
  public List<?> getExtensions() {
    return ImmutableList.of(
        PropertyDefinition.builder(AndroidLintConstants.PROJECT_PATH_PROPERTY)
            .category(SETTINGS_CATEGORY_NAME)
            .subCategory(SETTINGS_CATEGORY_NAME)
            .name("Android project path")
            .description("Path (relative to the base path) where the Android project resides (i.e. where the AndroidManifest.xml is located).")
            .onlyOnQualifiers(Qualifiers.PROJECT)
            .build(),
        AndroidLintSensor.class,
        AndroidLintRuleRepository.class,
        AndroidLintSonarWay.class,
        AndroidLintExecutor.class);
  }
}
