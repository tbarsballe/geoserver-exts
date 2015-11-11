package org.geogig.geoserver.config;

import java.io.IOException;
import java.net.URI;

import org.apache.commons.io.FilenameUtils;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;

import com.google.common.base.Throwables;

public class GeoServerStoreRepositoryResolver implements GeoGigDataStoreFactory.RepositoryLookup {

    @Override
    public URI resolve(final String repository) {
        RepositoryManager repositoryManager = RepositoryManager.get();
        try {
            RepositoryInfo info = repositoryManager.get(repository);
            //enforce unix file seperators
            String unixsep = FilenameUtils.separatorsToUnix(info.getLocation());
            //check if refers to file, if it does add the file prefix
            if(FilenameUtils.getPrefix(unixsep)!=null&&!unixsep.startsWith("file:/"))
            	unixsep = "file:/"+unixsep;
            return URI.create(unixsep);
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
    }


}
