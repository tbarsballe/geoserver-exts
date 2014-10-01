package org.geogig.geoserver.web.security;

import java.io.Serializable;

public final class WhitelistRule implements Serializable {
    private String name;
    private String pattern;
    private boolean requireSSL;

    public WhitelistRule(String name, String pattern, boolean requireSSL) {
        this.name = name;
        this.pattern = pattern;
        this.requireSSL = requireSSL;
    }

    public void setName(String name) {
        this.name = name;
    }

    public void setPattern(String pattern) {
        this.pattern = pattern;
    }

    public void setRequireSSL(boolean requireSSL) {
        this.requireSSL = requireSSL;
    }

    public String getName() {
        return name;
    }

    public String getPattern() {
        return pattern;
    }

    public boolean isRequireSSL() {
        return requireSSL;
    }
}
