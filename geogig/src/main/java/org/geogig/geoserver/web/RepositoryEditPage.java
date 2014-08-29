/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web;

import javax.annotation.Nullable;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.web.repository.RepositoryEditPanel;
import org.geoserver.web.GeoServerSecuredPage;

/**
 * @see RepositoryEditPanel
 */
public class RepositoryEditPage extends GeoServerSecuredPage {

    public RepositoryEditPage() {
        this(null);
    }

    public RepositoryEditPage(@Nullable IModel<RepositoryInfo> repoInfo) {
        super();
        final boolean isNew = repoInfo == null;
        if (isNew) {
            repoInfo = new Model<RepositoryInfo>(new RepositoryInfo());
        }
        Form<RepositoryInfo> form = new Form<RepositoryInfo>("repoForm", repoInfo);
        form.add(new RepositoryEditPanel("repo", repoInfo, isNew));
        add(form);
    }
}
