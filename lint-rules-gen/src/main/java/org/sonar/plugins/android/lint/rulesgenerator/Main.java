package org.sonar.plugins.android.lint.rulesgenerator;

public class Main {
    public static void main(String[] args) {
        SonarRulesGenerator generator = new SonarRulesGenerator();
        generator.generateRules();
    }
}
