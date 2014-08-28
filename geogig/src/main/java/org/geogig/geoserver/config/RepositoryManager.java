package org.geogig.geoserver.config;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.platform.GeoServerExtensions;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;

import com.google.common.base.Preconditions;
import com.google.common.collect.Lists;

public class RepositoryManager {

    private Catalog catalog;

    public static RepositoryManager get() {
        RepositoryManager repoManager = GeoServerExtensions.bean(RepositoryManager.class);
        Preconditions.checkState(repoManager != null);
        return repoManager;
    }

    public RepositoryManager(Catalog catalog) {
        this.catalog = catalog;
    }

    public List<RepositoryInfo> getAll() {
        List<DataStoreInfo> geoeogigStores = findGeogigStores();

        Map<String, RepositoryInfo> infos = new HashMap<String, RepositoryInfo>();

        for (DataStoreInfo info : geoeogigStores) {
            RepositoryInfo ri = new RepositoryInfo();
            Serializable location = info.getConnectionParameters().get(
                    GeoGigDataStoreFactory.REPOSITORY.key);
            String locationStr = String.valueOf(location);
            ri.setLocation(locationStr);
            ri.setName(new File(locationStr).getName());
            infos.put(ri.getLocation(), ri);
        }

        return new ArrayList<RepositoryInfo>(infos.values());
    }

    public List<DataStoreInfo> findGeogigStores() {
        List<DataStoreInfo> geogigStores;
        org.opengis.filter.Filter filter = Predicates.equal("type",
                GeoGigDataStoreFactory.DISPLAY_NAME);
        CloseableIterator<DataStoreInfo> stores = catalog.list(DataStoreInfo.class, filter);
        try {
            geogigStores = Lists.newArrayList(stores);
        } finally {
            stores.close();
        }

        return geogigStores;
    }

}
