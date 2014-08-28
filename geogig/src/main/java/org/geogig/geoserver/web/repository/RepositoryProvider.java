/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;

import org.apache.wicket.extensions.markup.html.repeater.util.SortParam;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.LoadableDetachableModel;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.web.wicket.GeoServerDataProvider;

public class RepositoryProvider extends GeoServerDataProvider<RepositoryInfo> {

    private static final long serialVersionUID = 4883560661021761394L;

    static final Property<RepositoryInfo> NAME = new BeanProperty<RepositoryInfo>("name", "name");

    static final Property<RepositoryInfo> LOCATION = new BeanProperty<RepositoryInfo>("location",
            "location");

    static final Property<RepositoryInfo> REMOVELINK = new AbstractProperty<RepositoryInfo>(
            "remove") {
        private static final long serialVersionUID = 1L;

        @Override
        public Boolean getPropertyValue(RepositoryInfo item) {
            return Boolean.TRUE;
        }

        @Override
        public boolean isSearchable() {
            return false;
        }
    };

    final List<Property<RepositoryInfo>> PROPERTIES = Arrays.asList(NAME, LOCATION, REMOVELINK);

    public RepositoryProvider() {
    }

    @Override
    protected List<RepositoryInfo> getItems() {
        return mockItems();
    }

    @Override
    protected List<Property<RepositoryInfo>> getProperties() {
        return PROPERTIES;
    }

    @Override
    protected Comparator<RepositoryInfo> getComparator(SortParam sort) {
        return super.getComparator(sort);
    }

    @Override
    public IModel<RepositoryInfo> newModel(Object object) {
        return new RepositoryInfoDetachableModel((RepositoryInfo) object);
    }

    /**
     * A RepositoryInfo detachable model that holds the store id to retrieve it on demand from the
     * catalog
     */
    static class RepositoryInfoDetachableModel extends LoadableDetachableModel<RepositoryInfo> {

        private static final long serialVersionUID = -6829878983583733186L;

        String id;

        public RepositoryInfoDetachableModel(RepositoryInfo repoInfo) {
            super(repoInfo);
            this.id = repoInfo.getLocation();
        }

        @Override
        protected RepositoryInfo load() {
            List<RepositoryInfo> mockItems = mockItems();
            for (RepositoryInfo i : mockItems) {
                if (this.id.equals(i.getLocation())) {
                    return i;
                }
            }
            return null;
        }
    }

    private static List<RepositoryInfo> mockItems() {
        List<RepositoryInfo> repos = RepositoryManager.get().getAll();
        return repos;
    }

}
