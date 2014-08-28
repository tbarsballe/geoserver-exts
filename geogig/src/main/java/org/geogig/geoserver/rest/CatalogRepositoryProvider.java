/* Copyright (c) 2014 OpenPlans. All rights reserved.
 * This code is licensed under the GNU GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import static org.locationtech.geogig.rest.repository.RESTUtils.getStringAttribute;

import java.io.IOException;
import java.util.List;

import javax.annotation.Nullable;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.Predicates;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.rest.RestletException;
import org.geotools.data.DataAccess;
import org.locationtech.geogig.api.GeoGIG;
import org.locationtech.geogig.geotools.data.GeoGigDataStore;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.locationtech.geogig.rest.repository.RepositoryProvider;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.restlet.data.Request;
import org.restlet.data.Status;

import com.google.common.base.Optional;
import com.google.common.base.Predicate;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterators;

/**
 * {@link RepositoryProvider} that looks up the coresponding {@link GeoGIG} instance to a given
 * {@link Request} by searching the geoserver catalog for the geogig datastore that matches the
 * request's <code>{repository}</code> path step.
 */
public class CatalogRepositoryProvider implements RepositoryProvider {

    private Catalog catalog;

    public CatalogRepositoryProvider(Catalog catalog) {
        this.catalog = catalog;
    }

    private Catalog getCatalog() {
        return catalog;
    }

    private Optional<String> getRepositoryName(Request request) {
        final String repo = getStringAttribute(request, "repository");
        if (repo != null && !repo.contains(":")) {
            throw new IllegalArgumentException(
                    "Repository name should be of the form <workspace>:<datastore>: " + repo);
        }
        return Optional.fromNullable(repo);
    }

    public List<DataStoreInfo> findGeogigStores() {
        List<DataStoreInfo> geogigStores;

        Catalog catalog = getCatalog();
        org.opengis.filter.Filter filter = Predicates.equal("type",
                GeoGigDataStoreFactory.DISPLAY_NAME);
        CloseableIterator<DataStoreInfo> stores = catalog.list(DataStoreInfo.class, filter);
        try {
            Predicate<DataStoreInfo> enabled = new Predicate<DataStoreInfo>() {
                @Override
                public boolean apply(@Nullable DataStoreInfo input) {
                    return input.isEnabled();
                }
            };
            geogigStores = ImmutableList.copyOf(Iterators.filter(stores, enabled));
        } finally {
            stores.close();
        }

        return geogigStores;
    }

    @Override
    public Optional<GeoGIG> getGeogig(Request request) {
        Optional<String> repositoryName = getRepositoryName(request);
        if (!repositoryName.isPresent()) {
            return Optional.absent();
        }
        Optional<GeoGigDataStore> dataStore = findDataStore(request, repositoryName.get());
        if (!dataStore.isPresent()) {
            return Optional.absent();
        }
        return Optional.of(dataStore.get().getGeogig());
    }

    private Optional<GeoGigDataStore> findDataStore(Request request, String repositoryName) {
        String[] wsds = repositoryName.split(":");
        String workspace = wsds[0];
        String datastore = wsds[1];

        Catalog catalog = getCatalog();
        DataStoreInfo geogigStoreInfo = catalog.getDataStoreByName(workspace, datastore);
        if (null == geogigStoreInfo) {
            throw new RestletException("No such repository: " + repositoryName,
                    Status.CLIENT_ERROR_NOT_FOUND);
        }
        if (!geogigStoreInfo.isEnabled()) {
            throw new RestletException("Repository is not enabled: " + repositoryName,
                    Status.CLIENT_ERROR_BAD_REQUEST);
        }
        DataAccess<? extends FeatureType, ? extends Feature> dataStore;
        try {
            dataStore = geogigStoreInfo.getDataStore(null);
        } catch (IOException e) {
            throw new RestletException("Error accessing datastore " + repositoryName,
                    Status.SERVER_ERROR_INTERNAL, e);
        }
        if (!(dataStore instanceof GeoGigDataStore)) {
            throw new RestletException(repositoryName + " is not a Geogig DataStore: "
                    + geogigStoreInfo.getType(), Status.CLIENT_ERROR_BAD_REQUEST);
        }
        GeoGigDataStore geogigDataStore = (GeoGigDataStore) dataStore;
        return Optional.of(geogigDataStore);
    }

}