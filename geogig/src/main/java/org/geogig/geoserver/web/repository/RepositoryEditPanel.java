/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import org.apache.wicket.Component;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.geogig.geoserver.config.RepositoryInfo;

public class RepositoryEditPanel extends FormComponentPanel<RepositoryInfo> {

    private static final long serialVersionUID = -870873448379832051L;

    public RepositoryEditPanel(final String wicketId, IModel<RepositoryInfo> model,
            final boolean isNew) {
        super(wicketId, model);

        IModel<String> parentModel = new PropertyModel<String>(model, "parentDirectory");
        Component parent = new GeoGigDirectory("parentDirectory", parentModel);
        parent.setEnabled(isNew);
        add(parent);

        IModel<String> nameModel = new PropertyModel<String>(model, "name");
        TextField<String> name = new TextField<String>("repositoryName", nameModel);
        name.setEnabled(isNew);
        add(name);
    }

}
