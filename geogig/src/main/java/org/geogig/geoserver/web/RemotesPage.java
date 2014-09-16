/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import static com.google.common.collect.Lists.newArrayList;
import org.apache.wicket.AttributeModifier;
import org.apache.wicket.Component;
import org.apache.wicket.behavior.AttributeAppender;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Button;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.markup.repeater.RepeatingView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.web.GeoServerSecuredPage;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geogig.geoserver.config.ConfigStore;
import org.geogig.geoserver.web.security.WhitelistRule;
import org.geogig.geoserver.web.security.WhitelistRulePanel;
import org.geowebcache.diskquota.storage.Quota;
import org.geowebcache.layer.TileLayer;
import org.geowebcache.mime.MimeType;

/**
 */
public class RemotesPage extends GeoServerSecuredPage {
    private List<WhitelistRule> rules;
    private ModalWindow window;

    public RemotesPage() {
        ConfigStore configStore = (ConfigStore) GeoServerExtensions.bean("geogigConfigStore");
        try {
            rules = configStore.getWhitelist();
        } catch (IOException e) {
            rules = newArrayList();
        }
        Form form = new Form("form") {
            @Override
            protected void onSubmit() {
                final ConfigStore configStore = (ConfigStore) GeoServerExtensions.bean("geogigConfigStore");
                configStore.saveWhitelist(rules);
            }
        };
        window = new ModalWindow("popup");
        add(window);
        add(form);
        final WhitelistRulePanel whitelistRulePanel = new WhitelistRulePanel("whitelist.rules", rules, window);
        whitelistRulePanel.setOutputMarkupId(true);
        form.add(whitelistRulePanel);
        form.add(new AjaxLink("whitelist.add") {
            @Override
            public void onClick(AjaxRequestTarget target) {
                rules.add(new WhitelistRule("", "", false));
                target.addComponent(whitelistRulePanel);
            };
        });
        form.add(new Button("submit"));
    }
}
