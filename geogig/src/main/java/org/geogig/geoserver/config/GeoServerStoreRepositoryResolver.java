package org.geogig.geoserver.config;

import java.io.File;
import java.io.IOException;

import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;

import com.google.common.base.Throwables;

public class GeoServerStoreRepositoryResolver implements GeoGigDataStoreFactory.RepositoryLookup {

    @Override
    public File resolve(final String repository) {
        RepositoryManager repositoryManager = RepositoryManager.get();
        try {
            RepositoryInfo info = repositoryManager.get(repository);
            return new File(info.getLocation());
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }

}
