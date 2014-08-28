/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import java.io.File;
import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.Nullable;

import org.apache.wicket.model.IModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.platform.resource.Files;
import org.geoserver.web.data.store.panel.FileModel;
import org.geotools.util.logging.Logging;

/**
 * Model for the geogig repository directory.
 * <p>
 * Adapted from {@link FileModel}
 *
 */
public class GeoGigDirectoryModel implements IModel<String> {

    private static final long serialVersionUID = -3471257853993308269L;

    static final Logger LOGGER = Logging.getLogger(GeoGigDirectoryModel.class);

    IModel<String> geogigDirectory;

    private File rootDir;

    public GeoGigDirectoryModel(IModel<String> geogigDirectory) {
        this(geogigDirectory, GeoServerExtensions.bean(GeoServerResourceLoader.class)
                .getBaseDirectory());
    }

    public GeoGigDirectoryModel(IModel<String> geogigDirectory, File rootDir) {
        this.geogigDirectory = geogigDirectory;
        this.rootDir = rootDir;
    }

    private boolean isSubfile(File root, File selection) {
        if (selection == null || "".equals(selection.getPath()))
            return false;
        if (selection.equals(root))
            return true;

        return isSubfile(root, selection.getParentFile());
    }

    @Override
    public String getObject() {
        return geogigDirectory.getObject();
    }

    @Override
    public void setObject(@Nullable String location) {
        if (location != null) {
            File dataDirectory = canonicalize(rootDir);
            File file = canonicalize(new File(location));
            if (isSubfile(dataDirectory, file)) {
                File curr = file;
                String path = null;
                // paranoid check to avoid infinite loops
                while (curr != null && !curr.equals(dataDirectory)) {
                    if (path == null) {
                        path = curr.getName();
                    } else {
                        path = curr.getName() + "/" + path;
                    }
                    curr = curr.getParentFile();
                }
                location = "file:" + path;
            } else {
                File dataFile = Files.url(rootDir, location);
                if (dataFile != null && !dataFile.equals(file)) {
                    // relative to the data directory, does not need fixing
                } else {
                    location = "file://" + file.getAbsolutePath();
                }
            }
        }
        geogigDirectory.setObject(location);
    }

    /**
     * Turns a file in canonical form if possible
     * 
     * @param file
     * @return
     */
    File canonicalize(File file) {
        try {
            return file.getCanonicalFile();
        } catch (IOException e) {
            LOGGER.log(Level.INFO, "Could not convert " + file + " into canonical form", e);
            return file;
        }
    }

    @Override
    public void detach() {
        // nothing to do
    }
}
