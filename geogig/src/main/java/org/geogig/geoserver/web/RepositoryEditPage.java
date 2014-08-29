/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web;

import javax.annotation.Nullable;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.link.BookmarkablePageLink;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
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
        FeedbackPanel feedback = new FeedbackPanel("feedback");
        form.add(feedback);

        form.add(new BookmarkablePageLink<Void>("cancel", RepositoriesPage.class));

        form.add(new AjaxSubmitLink("save", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                super.onError(target, form);
                target.addComponent(form);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                try {
                    RepositoryInfo repoInfo = (RepositoryInfo) form.getModelObject();
                    onSave(repoInfo, target);
                } catch (IllegalArgumentException e) {
                    form.error(e.getMessage());
                    target.addComponent(form);
                }
            }
        });
    }

    private void onSave(RepositoryInfo repoInfo, AjaxRequestTarget target) {
        System.err.println("Createing repo " + repoInfo.getLocation());
        setResponsePage(RepositoriesPage.class);
    }
}
