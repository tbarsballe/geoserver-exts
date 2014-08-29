package org.geogig.geoserver.config;

import java.io.File;
import java.io.Serializable;

public class RepositoryInfo implements Serializable {

    private static final long serialVersionUID = -5946705936987075713L;

    private String parentDirectory;

    private String name;

    public String getLocation() {
        if (parentDirectory == null || name == null) {
            return null;
        }
        return new File(parentDirectory, name).getAbsolutePath();
    }

    public void setLocation(String location) {
        File repoDir = new File(location);
        setName(repoDir.getName());
        setParentDirectory(repoDir.getParent());
    }

    public void setParentDirectory(String parent) {
        this.parentDirectory = parent;
    }

    public String getParentDirectory() {
        return parentDirectory;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }
}
