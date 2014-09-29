/* Copyright (c) 2013 OpenPlans. All rights reserved.
 * This code is licensed under the GNU GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import static org.locationtech.geogig.rest.repository.RESTUtils.repositoryProvider;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.geogig.geoserver.config.RepositoryInfo;
import org.geoserver.rest.MapResource;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.FreemarkerFormat;
import org.geoserver.rest.format.MapJSONFormat;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class RepositoryListResource extends MapResource {

    public RepositoryListResource() {
        super();
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        List<DataFormat> formats = Lists.newArrayListWithCapacity(3);

        formats.add(new FreemarkerFormat(RepositoryListResource.class.getSimpleName() + ".ftl",
                getClass(), MediaType.TEXT_HTML));

        formats.add(new MapJSONFormat());

        return formats;
    }

    @Override
    public Map<String, Object> getMap() throws Exception {
        List<RepositoryInfo> repositories = getRepositories();

        Map<String, Object> map = Maps.newHashMap();
        map.put("repositories", repositories);
        map.put("page", getPageInfo());
        return map;
    }

    private List<RepositoryInfo> getRepositories() {
        Request request = getRequest();
        GeoServerRepositoryProvider repoFinder = (GeoServerRepositoryProvider) repositoryProvider(request);

        List<RepositoryInfo> repos = new ArrayList<>(repoFinder.findRepositories());

        return repos;
    }
}
