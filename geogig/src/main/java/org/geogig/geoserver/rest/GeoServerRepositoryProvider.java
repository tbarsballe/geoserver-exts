/* Copyright (c) 2014 OpenPlans. All rights reserved.
 * This code is licensed under the GNU GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import static org.locationtech.geogig.rest.repository.RESTUtils.getStringAttribute;

import java.io.IOException;
import java.util.List;
import java.util.NoSuchElementException;

import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.rest.RestletException;
import org.locationtech.geogig.api.GeoGIG;
import org.locationtech.geogig.rest.repository.RepositoryProvider;
import org.restlet.data.Request;
import org.restlet.data.Status;

import com.google.common.base.Optional;

/**
 * {@link RepositoryProvider} that looks up the coresponding {@link GeoGIG} instance to a given
 * {@link Request} by asking the geoserver's {@link RepositoryManager}
 */
public class GeoServerRepositoryProvider implements RepositoryProvider {

    private Optional<String> getRepositoryId(Request request) {
        final String repo = getStringAttribute(request, "repository");
        return Optional.fromNullable(repo);
    }

    public List<RepositoryInfo> findRepositories() {
        return RepositoryManager.get().getAll();
    }

    @Override
    public Optional<GeoGIG> getGeogig(Request request) {
        Optional<String> repositoryName = getRepositoryId(request);
        if (!repositoryName.isPresent()) {
            return Optional.absent();
        }
        GeoGIG geogig = findRepository(request, repositoryName.get());
        return Optional.of(geogig);
    }

    private GeoGIG findRepository(Request request, String repositoryId) {

        RepositoryManager manager = RepositoryManager.get();
        try {
            manager.get(repositoryId);
            return manager.getRepository(repositoryId);
        } catch (NoSuchElementException e) {
            throw new RestletException("No such repository: " + repositoryId,
                    Status.CLIENT_ERROR_NOT_FOUND);
        } catch (IOException e) {
            throw new RestletException("Error accessing datastore " + repositoryId,
                    Status.SERVER_ERROR_INTERNAL, e);
        }
    }

}