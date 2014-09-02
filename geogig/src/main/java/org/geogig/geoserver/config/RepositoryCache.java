package org.geogig.geoserver.config;

import java.io.File;
import java.io.IOException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.locationtech.geogig.api.GeoGIG;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Throwables;
import com.google.common.cache.CacheBuilder;
import com.google.common.cache.CacheLoader;
import com.google.common.cache.LoadingCache;
import com.google.common.cache.RemovalListener;
import com.google.common.cache.RemovalNotification;

class RepositoryCache {

    private static final Logger LOGGER = LoggerFactory.getLogger(RepositoryCache.class);

    private LoadingCache<String, GeoGIG> repoCache;

    public RepositoryCache(final RepositoryManager repoManager) {

        RemovalListener<String, GeoGIG> listener = new RemovalListener<String, GeoGIG>() {
            @Override
            public void onRemoval(RemovalNotification<String, GeoGIG> notification) {
                String repoId = notification.getKey();
                GeoGIG geogig = notification.getValue();
                if (geogig != null) {
                    try {
                        geogig.close();
                    } catch (RuntimeException e) {
                        LOGGER.warn("Error disposing GeoGig repository instance for id {}", repoId,
                                e);
                    }
                }
            }
        };

        final CacheLoader<String, GeoGIG> loader = new CacheLoader<String, GeoGIG>() {
            private RepositoryManager manager = repoManager;

            @Override
            public GeoGIG load(final String repoId) throws Exception {
                try {
                    RepositoryInfo repoInfo = manager.get(repoId);
                    String repoLocation = repoInfo.getLocation();
                    File repoDir = new File(repoLocation);
                    GeoGIG geogig = new GeoGIG(repoDir);
                    geogig.getRepository();
                    return geogig;
                } catch (Exception e) {
                    LOGGER.warn("Error loading GeoGig repository instance for id {}", repoId, e);
                    throw e;
                }
            }
        };

        repoCache = CacheBuilder.newBuilder()//
                .softValues()//
                .expireAfterAccess(5, TimeUnit.MINUTES)//
                .removalListener(listener)//
                .build(loader);
    }

    public GeoGIG get(String repositoryId) throws IOException {
        try {
            return repoCache.get(repositoryId);
        } catch (ExecutionException e) {
            Throwables.propagateIfInstanceOf(e.getCause(), IOException.class);
            throw new IOException(
                    "Error obtaining cached geogig instance for repo " + repositoryId, e.getCause());
        }
    }

    public void invalidate(final String repoId) {
        repoCache.invalidate(repoId);
    }

    public void invalidateAll() {
        repoCache.invalidateAll();
    }
}
