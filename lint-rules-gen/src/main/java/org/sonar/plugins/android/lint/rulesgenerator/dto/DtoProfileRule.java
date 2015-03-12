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
