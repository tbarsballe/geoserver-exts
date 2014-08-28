/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web;

import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.web.repository.RepositoryEditPanel;
import org.geoserver.web.GeoServerSecuredPage;

import com.google.common.base.Preconditions;

/**
 * @see RepositoryEditPanel
 */
public class RepositoryEditPage extends GeoServerSecuredPage {

    public RepositoryEditPage() {
        this(new Model<RepositoryInfo>(new RepositoryInfo()));
    }

    public RepositoryEditPage(IModel<RepositoryInfo> repoInfo) {
        super();
        Preconditions.checkNotNull(repoInfo);
        Form<RepositoryInfo> form = new Form<RepositoryInfo>("repoForm", repoInfo);
        form.add(new RepositoryEditPanel("repo", repoInfo));
        add(form);
    }
}
