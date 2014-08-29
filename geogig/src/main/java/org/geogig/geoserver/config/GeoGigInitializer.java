package org.geogig.geoserver.config;

import static java.lang.String.format;
import static com.google.common.base.Preconditions.checkArgument;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.REPOSITORY;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.RESOLVER_CLASS_NAME;

import java.io.Serializable;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.config.GeoServer;
import org.geoserver.config.GeoServerInitializer;
import org.geotools.util.logging.Logging;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;

import com.google.common.collect.ArrayListMultimap;
import com.google.common.collect.ListMultimap;
import com.google.common.collect.Multimap;

public class GeoGigInitializer implements GeoServerInitializer {

    private static final Logger LOGGER = Logging.getLogger(GeoGigInitializer.class);

    private ConfigStore store;

    public GeoGigInitializer(ConfigStore store) {
        this.store = store;
    }

    @Override
    public void initialize(GeoServer geoServer) throws Exception {
        // create RepositoryInfos for each datastore that doesn't have it to preserve backwards
        // compatibility
        Catalog catalog = geoServer.getCatalog();

        Map<String, RepositoryInfo> allByLocation = getAllByLocation();

        Multimap<String, DataStoreInfo> byRepo = storeByRepository(catalog);

        for (String repoDirectory : byRepo.keySet()) {

            final String repoResolverClasName = GeoServerStoreRepositoryResolver.class.getName();

            if (!allByLocation.containsKey(repoDirectory)) {

                final RepositoryInfo info = create(repoDirectory);

                for (DataStoreInfo store : byRepo.get(repoDirectory)) {
                    LOGGER.info(format(
                            "Upgrading config for GeoGig store %s to refer to GeoServer's RepositoryInfo %s",
                            store.getName(), info.getId()));
                    Map<String, Serializable> params = store.getConnectionParameters();
                    params.put(REPOSITORY.key, info.getId());
                    params.put(RESOLVER_CLASS_NAME.key, repoResolverClasName);
                    catalog.save(store);
                }
            }
        }
    }

    private Map<String, RepositoryInfo> getAllByLocation() {
        Map<String, RepositoryInfo> byLocation = new HashMap<>();
        for (RepositoryInfo info : store.getRepositories()) {
            byLocation.put(info.getLocation(), info);
        }
        return byLocation;
    }

    private RepositoryInfo create(String repoDirectory) {
        RepositoryInfo info = new RepositoryInfo();
        info.setLocation(repoDirectory);
        store.save(info);
        return info;
    }

    private Multimap<String, DataStoreInfo> storeByRepository(Catalog catalog) {
        List<DataStoreInfo> stores = RepositoryManager.findGeogigStores(catalog);
        ListMultimap<String, DataStoreInfo> multimap = ArrayListMultimap.create();
        for (DataStoreInfo ds : stores) {
            multimap.put(repo(ds), ds);
        }
        return multimap;
    }

    private String repo(DataStoreInfo ds) {
        Serializable value = ds.getConnectionParameters()
                .get(GeoGigDataStoreFactory.REPOSITORY.key);
        checkArgument(value != null, "%s not present in %s", GeoGigDataStoreFactory.REPOSITORY.key,
                ds);
        return String.valueOf(value);
    }

}
