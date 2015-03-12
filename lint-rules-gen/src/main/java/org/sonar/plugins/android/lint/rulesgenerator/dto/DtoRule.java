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
