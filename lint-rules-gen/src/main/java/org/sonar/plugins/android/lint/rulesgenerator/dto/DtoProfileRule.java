/*
 * SonarQube Android Plugin
 * Copyright (C) 2015 SonarSource and Jordan Hansen
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
package org.sonar.plugins.android.lint.rulesgenerator.dto;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.Root;

@Root(name = "rule")
public class DtoProfileRule {
  @Element
  private String repositoryKey;
  @Element
  private String key;

  public String getRepositoryKey() {
    return repositoryKey;
  }

  public void setRepositoryKey(String repositoryKey) {
    this.repositoryKey = repositoryKey;
  }

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }
}
