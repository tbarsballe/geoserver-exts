/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web;

import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.util.List;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.geogig.geoserver.config.ConfigStore;
import org.geogig.geoserver.config.WhitelistRule;
import org.geogig.geoserver.web.security.SecurityLogsPanel;
import org.geogig.geoserver.web.security.WhitelistRulePanel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerSecuredPage;

/**
 */
public class RemotesPage extends GeoServerSecuredPage {
    private List<WhitelistRule> rules;

    private ModalWindow window;

    public RemotesPage() {
        add(new SecurityLogsPanel("table"));
        ConfigStore configStore = (ConfigStore) GeoServerExtensions.bean("geogigConfigStore");
        try {
            rules = configStore.getWhitelist();
        } catch (IOException e) {
            rules = newArrayList();
        }
        Form<?> form = new Form<Void>("form") {

            private static final long serialVersionUID = 3964569868924250688L;

            @Override
            protected void onSubmit() {
                final ConfigStore configStore = (ConfigStore) GeoServerExtensions
                        .bean("geogigConfigStore");
                configStore.saveWhitelist(rules);
            }
        };
        window = new ModalWindow("popup");
        add(window);
        add(form);
        final WhitelistRulePanel whitelistRulePanel = new WhitelistRulePanel("whitelist.rules",
                rules, window);
        whitelistRulePanel.setOutputMarkupId(true);
        form.add(whitelistRulePanel);
        form.add(new AjaxLink<Void>("whitelist.add") {

            private static final long serialVersionUID = 5869313981483016964L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                rules.add(new WhitelistRule("", "", false));
                target.addComponent(whitelistRulePanel);
            };
        });
        form.add(new Button("submit"));
    }
}
