package org.sonar.plugins.android.lint.rulesgenerator.dto;

import org.simpleframework.xml.Element;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Path;
import org.simpleframework.xml.Root;

import java.util.ArrayList;
import java.util.List;

@Root(name = "Profile")
public class DtoProfile {
    @Element
    private String name;
    @Element
    private String language;
    @Path("rules")
    @ElementList(inline=true)
    private List<DtoProfileRule> rules;

    public DtoProfile() {
        rules = new ArrayList<DtoProfileRule>();
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getLanguage() {
        return language;
    }

    public void setLanguage(String language) {
        this.language = language;
    }

    public List<DtoProfileRule> getRules() {
        return rules;
    }

    public void setRules(List<DtoProfileRule> rules) {
        this.rules = rules;
    }

    public void addRule(String repositoryKey, String key) {
        DtoProfileRule rule = new DtoProfileRule();
        rule.setRepositoryKey(repositoryKey);
        rule.setKey(key);
        this.rules.add(rule);
    }
}
