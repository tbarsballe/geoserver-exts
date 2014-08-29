package org.geogig.geoserver.config;

import java.io.File;

import org.geotools.data.DataAccessFactory.Param;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;

public class GeoServerGeoGigDataStoreFactory extends GeoGigDataStoreFactory {

    public static final Param REPOSITORY_ID = new Param("repository_info_id", String.class,
            "Identifier for the RepositoryInfo", true);

}
