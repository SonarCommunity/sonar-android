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

import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.api.batch.fs.FileSystem;
import org.sonar.api.batch.fs.InputFile;
import org.sonar.api.component.ResourcePerspectives;
import org.sonar.api.issue.Issuable;
import org.sonar.api.issue.Issue;
import org.sonar.api.profiles.RulesProfile;
import org.sonar.api.rules.ActiveRule;
import org.sonar.plugins.android.lint.dto.DtoIssue;
import org.sonar.plugins.android.lint.dto.DtoIssues;
import org.sonar.plugins.android.lint.dto.DtoLocation;

import java.io.File;

public class AndroidLintProcessor {

  private static final Logger LOGGER = LoggerFactory.getLogger(AndroidLintProcessor.class);
  private final RulesProfile profile;
  private final ResourcePerspectives perspectives;
  private final FileSystem fs;

  public AndroidLintProcessor(RulesProfile profile, ResourcePerspectives perspectives, FileSystem fs) {
    this.profile = profile;
    this.perspectives = perspectives;
    this.fs = fs;
  }

  public void process(File lintXml) {
    Serializer serializer = new Persister();
    try {
      DtoIssues dtoIssues = serializer.read(DtoIssues.class, lintXml);
      for (DtoIssue dtoIssue : dtoIssues.getIssues()) {
        processIssue(dtoIssue);
      }
    } catch (Exception e) {
      LOGGER.error("Exception reading " + lintXml.toString(), e);
    }
  }

  private void processIssue(DtoIssue dtoIssue) {
    ActiveRule rule = profile.getActiveRule(AndroidLintRulesDefinition.REPOSITORY_KEY, dtoIssue.getId());
    if (rule != null) {
      LOGGER.debug("Processing Issue: {}", dtoIssue.getId());
      for (DtoLocation dtoLocation : dtoIssue.getLocations()) {
        processIssueForLocation(rule, dtoIssue, dtoLocation);
      }
    } else {
      LOGGER.warn("Unable to find rule for {}", dtoIssue.getId());
    }
  }

  private void processIssueForLocation(ActiveRule rule, DtoIssue dtoIssue, DtoLocation dtoLocation) {
    InputFile inputFile = fs.inputFile(fs.predicates().hasAbsolutePath(dtoLocation.getFile()));
    if (inputFile != null) {
      LOGGER.debug("Processing File {} for Issue {}", dtoLocation.getFile(), dtoIssue.getId());
      Issuable issuable = perspectives.as(Issuable.class, inputFile);
      if (issuable != null) {
        Issue issue = issuable.newIssueBuilder()
            .ruleKey(rule.getRule().ruleKey())
            .message(dtoIssue.getMessage())
            .line(dtoLocation.getLine())
            .build();
        issuable.addIssue(issue);
        return;
      }
    }
    LOGGER.warn("Unable to process file {}", dtoLocation.getFile());
  }
}
