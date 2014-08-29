/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import static org.geoserver.catalog.CascadeRemovalReporter.ModificationType.DELETE;
import static org.geoserver.catalog.CascadeRemovalReporter.ModificationType.GROUP_CHANGED;

import java.util.Iterator;
import java.util.List;

import org.apache.commons.beanutils.BeanUtils;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.catalog.CascadeRemovalReporter;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.LayerGroupInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.ParamResourceModel;

class ConfirmRepoRemovePanel extends Panel {

    private static final long serialVersionUID = 653769682579422516L;

    public ConfirmRepoRemovePanel(String id, IModel<RepositoryInfo> repo) {
        super(id);

        add(new Label("aboutRemoveMsg", new ParamResourceModel(
                "ConfirmRepoRemovePanel.aboutRemove", this, repo.getObject().getName())));

        final String repoLocation = repo.getObject().getLocation();
        final List<? extends CatalogInfo> stores;
        stores = RepositoryManager.get().findDataStoes(repoLocation);

        // collect the objects that will be removed (besides the roots)
        Catalog catalog = GeoServerApplication.get().getCatalog();

        CascadeRemovalReporter visitor = new CascadeRemovalReporter(catalog);

        for (CatalogInfo info : stores) {
            info.accept(visitor);
        }
        // visitor.removeAll(stores);

        // removed objects root (we show it if any removed object is on the list)
        WebMarkupContainer removed = new WebMarkupContainer("removedObjects");
        List<CatalogInfo> cascaded = visitor.getObjects(CatalogInfo.class, DELETE);
        // remove the resources, they are cascaded, but won't be show in the UI
        for (Iterator<CatalogInfo> it = cascaded.iterator(); it.hasNext();) {
            CatalogInfo catalogInfo = it.next();
            if (catalogInfo instanceof ResourceInfo) {
                it.remove();
            }
        }
        removed.setVisible(!cascaded.isEmpty());
        add(removed);

        // removed stores
        WebMarkupContainer str = new WebMarkupContainer("storesRemoved");
        removed.add(str);
        str.setVisible(!stores.isEmpty());
        str.add(new Label("stores", names(stores)));

        // removed layers
        WebMarkupContainer lar = new WebMarkupContainer("layersRemoved");
        removed.add(lar);
        List<LayerInfo> layers = visitor.getObjects(LayerInfo.class, DELETE);
        if (layers.size() == 0)
            lar.setVisible(false);
        lar.add(new Label("layers", names(layers)));

        // modified objects root (we show it if any modified object is on the list)
        WebMarkupContainer modified = new WebMarkupContainer("modifiedObjects");
        modified.setVisible(visitor.getObjects(null, GROUP_CHANGED).size() > 0);
        add(modified);

        // groups modified
        WebMarkupContainer grm = new WebMarkupContainer("groupsModified");
        modified.add(grm);
        List<LayerGroupInfo> groups = visitor.getObjects(LayerGroupInfo.class, GROUP_CHANGED);
        grm.setVisible(!groups.isEmpty());
        grm.add(new Label("groups", names(groups)));
    }

    String names(List<? extends CatalogInfo> objects) {
        StringBuffer sb = new StringBuffer();
        for (int i = 0; i < objects.size(); i++) {
            sb.append(name(objects.get(i)));
            if (i < (objects.size() - 1)) {
                sb.append(", ");
            }
        }
        return sb.toString();
    }

    String name(Object object) {
        try {
            return (String) BeanUtils.getProperty(object, "name");
        } catch (Exception e) {
            throw new RuntimeException("A catalog object that does not have "
                    + "a 'name' property has been used, this is unexpected", e);
        }
    }
}
