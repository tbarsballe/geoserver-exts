/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.Fragment;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDialog;

/**
 * Add/edit/remove repositories
 */
public class GeoGigRepositoriesPage extends GeoServerSecuredPage {

    private GeoServerDialog dialog;

    private RepositoriesListPanel table;

    private RepositoryProvider provider = new RepositoryProvider();

    public GeoGigRepositoriesPage() {

        table = new RepositoriesListPanel("table", provider, true);
        table.setOutputMarkupId(true);
        add(table);
        // the confirm dialog
        dialog = new GeoServerDialog("dialog");
        add(dialog);
        setHeaderPanel(headerPanel());
    }

    protected Component headerPanel() {
        Fragment header = new Fragment(HEADER_PANEL, "header", this);

        // the add button
        header.add(new BookmarkablePageLink<String>("addNew", RepositoryEditPage.class));

        return header;
    }

}
