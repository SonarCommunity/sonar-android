package org.sonar.plugins.android.lint.rulesgenerator;

import com.android.tools.lint.checks.BuiltinIssueRegistry;
import com.android.tools.lint.client.api.IssueRegistry;
import com.android.tools.lint.detector.api.Category;
import com.android.tools.lint.detector.api.Issue;
import com.android.tools.lint.detector.api.TextFormat;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.sonar.plugins.android.lint.AndroidLintRulesDefinition;
import org.sonar.plugins.android.lint.AndroidLintSonarWay;
import org.sonar.plugins.android.lint.rulesgenerator.dto.DtoProfile;
import org.sonar.plugins.android.lint.rulesgenerator.dto.DtoRule;
import org.sonar.plugins.android.lint.rulesgenerator.dto.DtoRules;
import org.sonar.plugins.java.Java;

import javax.annotation.Nullable;
import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

public class SonarRulesGenerator {

  private static final Logger LOGGER = LoggerFactory.getLogger(SonarRulesGenerator.class);

  private static final int ERROR_CRITICAL_PRIORITY = 7;
  private static final int WARNING_MAJOR_PRIORITY = 7;
  private static final String PROFILE_NAME = "Android Lint";
  private static final File BASE_OUTPUT_DIR = new File("out");
  private static final File RULES_FILE = new File(BASE_OUTPUT_DIR, AndroidLintRulesDefinition.RULES_XML_PATH);
  private static final File PROFILE_FILE = new File(BASE_OUTPUT_DIR, AndroidLintSonarWay.PROFILE_XML_PATH);

  private DtoProfile dtoProfile = new DtoProfile();
  private DtoRules dtoRules = new DtoRules();

  @SuppressWarnings("ResultOfMethodCallIgnored")
  public void generateRules() {
    dtoProfile.setName(PROFILE_NAME);
    dtoProfile.setLanguage(Java.KEY);

    for (Issue issue : getIssues()) {
      processIssue(issue);
    }

    // Output Files
    RULES_FILE.getParentFile().mkdirs();
    PROFILE_FILE.getParentFile().mkdirs();

    try {
      Serializer serializer = new Persister();
      serializer.write(dtoRules, RULES_FILE);
      serializer.write(dtoProfile, PROFILE_FILE);
    } catch (Exception e) {
      LOGGER.error("Failed to write files", e);
    }
  }

  private void processIssue(Issue issue) {
    DtoRule dtoRule = new DtoRule();
    dtoRule.setKey(issue.getId());
    dtoRule.setName(issue.getBriefDescription(TextFormat.TEXT));
    dtoRule.setDescription(issue.getExplanation(TextFormat.HTML));
    dtoRule.setSeverity(getSeverity(issue));
    setTags(issue.getCategory(), dtoRule);
    dtoRules.addRule(dtoRule);

    if (issue.isEnabledByDefault()) {
      dtoProfile.addRule(AndroidLintRulesDefinition.REPOSITORY_KEY, dtoRule.getKey());
    }
  }

  private void setTags(@Nullable Category category, DtoRule dtoRule) {
    if (category != null) {
      setTags(category.getParent(), dtoRule);
      dtoRule.addTag(category.getName());
    }
  }

  private SonarSeverity getSeverity(Issue issue) {
    switch (issue.getDefaultSeverity()) {
      case FATAL:
        return SonarSeverity.BLOCKER;
      case ERROR:
        if (issue.getPriority() >= ERROR_CRITICAL_PRIORITY) {
          return SonarSeverity.CRITICAL;
        }
        return SonarSeverity.MAJOR;
      case WARNING:
        if (issue.getPriority() >= WARNING_MAJOR_PRIORITY) {
          return SonarSeverity.MAJOR;
        }
        return SonarSeverity.MINOR;
      case INFORMATIONAL:
      case IGNORE:
      default:
        return SonarSeverity.INFO;
    }
  }

  private List<Issue> getIssues() {
    IssueRegistry registry = new BuiltinIssueRegistry();
    List<Issue> sorted = new ArrayList<Issue>(registry.getIssues());
    Collections.sort(sorted, new Comparator<Issue>() {
      @Override
      public int compare(Issue issue1, Issue issue2) {
        int d = issue1.getCategory().compareTo(issue2.getCategory());
        if (d != 0) {
          return d;
        }
        d = issue2.getPriority() - issue1.getPriority();
        if (d != 0) {
          return d;
        }

        return issue1.getId().compareTo(issue2.getId());
      }
    });
    return sorted;
  }
}
