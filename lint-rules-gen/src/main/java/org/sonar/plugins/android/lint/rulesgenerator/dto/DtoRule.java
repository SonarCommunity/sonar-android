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
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;
import org.sonar.plugins.android.lint.rulesgenerator.SonarSeverity;

import java.util.ArrayList;
import java.util.List;

@Root(name = "rule")
public class DtoRule {
  @Element
  private String key;
  @Element
  private String name;
  @Element(data = true)
  private String description;
  @Element
  private String severity;
  @ElementList(name="tag", inline = true)
  private List<String> tags = new ArrayList<String>();

  public String getKey() {
    return key;
  }

  public void setKey(String key) {
    this.key = key;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public String getSeverity() {
    return severity;
  }

  public void setSeverity(String severity) {
    this.severity = severity;
  }

  public void setSeverity(SonarSeverity severity) {
    this.severity = severity.name();
  }

  public List<String> getTags() {
    return tags;
  }

  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  public void addTag(String tag) {
    this.tags.add(tag);
  }
}
