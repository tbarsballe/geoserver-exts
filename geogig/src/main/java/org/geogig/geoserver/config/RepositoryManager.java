package org.geogig.geoserver.config;

import static org.geoserver.catalog.Predicates.and;
import static org.geoserver.catalog.Predicates.equal;

import java.io.File;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.platform.GeoServerExtensions;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.opengis.filter.Filter;

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
            infos.put(ri.getLocation(), ri);
        }

        return new ArrayList<RepositoryInfo>(infos.values());
    }

    public List<DataStoreInfo> findGeogigStores() {
        List<DataStoreInfo> geogigStores;
        org.opengis.filter.Filter filter = equal("type", GeoGigDataStoreFactory.DISPLAY_NAME);
        CloseableIterator<DataStoreInfo> stores = catalog.list(DataStoreInfo.class, filter);
        try {
            geogigStores = Lists.newArrayList(stores);
        } finally {
            stores.close();
        }

        return geogigStores;
    }

    public List<DataStoreInfo> findDataStoes(final String repoLocation) {
        Filter filter = equal("type", GeoGigDataStoreFactory.DISPLAY_NAME);

        String locationKey = "connectionParameters." + GeoGigDataStoreFactory.REPOSITORY.key;
        filter = and(filter, equal(locationKey, repoLocation));
        List<DataStoreInfo> dependent;
        try (CloseableIterator<DataStoreInfo> stores = catalog.list(DataStoreInfo.class, filter)) {
            dependent = Lists.newArrayList(stores);
        }
        return dependent;
    }

    public List<? extends CatalogInfo> findDependentCatalogObjects(final String repoLocation) {
        Filter filter = equal("type", GeoGigDataStoreFactory.DISPLAY_NAME);

        String locationKey = "connectionParameters." + GeoGigDataStoreFactory.REPOSITORY.key;
        filter = and(filter, equal(locationKey, repoLocation));
        List<DataStoreInfo> stores = findDataStoes(repoLocation);
        List<CatalogInfo> dependent = new ArrayList<CatalogInfo>(stores);
        for (DataStoreInfo store : stores) {
            List<FeatureTypeInfo> ftypes = catalog.getFeatureTypesByDataStore(store);
            dependent.addAll(ftypes);
            for (FeatureTypeInfo ftype : ftypes) {
                dependent.addAll(catalog.getLayers(ftype));
            }
        }

        return dependent;
    }

    public List<LayerInfo> findLayers(DataStoreInfo store) {
        Filter filter = equal("resource.store.id", store.getId());
        try (CloseableIterator<LayerInfo> it = catalog.list(LayerInfo.class, filter)) {
            return Lists.newArrayList(it);
        }
    }

    public List<FeatureTypeInfo> findFeatureTypes(DataStoreInfo store) {
        Filter filter = equal("store.id", store.getId());
        try (CloseableIterator<FeatureTypeInfo> it = catalog.list(FeatureTypeInfo.class, filter)) {
            return Lists.newArrayList(it);
        }
    }

    public static boolean isGeogigDirectory(final File file) {
        if (file == null) {
            return false;
        }
        final File geogigDir = new File(file, ".geogig");
        final boolean isGeogigDirectory = geogigDir.exists() && geogigDir.isDirectory();
        return isGeogigDirectory;
    }
}
