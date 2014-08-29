/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import java.io.File;
import java.io.FileFilter;
import java.text.DateFormat;
import java.util.Date;
import java.util.Locale;

import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxFallbackLink;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.extensions.ajax.markup.html.IndicatingAjaxFallbackLink;
import org.apache.wicket.extensions.markup.html.repeater.util.SortableDataProvider;
import org.apache.wicket.markup.ComponentTag;
import org.apache.wicket.markup.html.WebMarkupContainer;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.markup.repeater.Item;
import org.apache.wicket.markup.repeater.data.DataView;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.convert.IConverter;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.browser.FileDataView;
import org.geoserver.web.wicket.browser.FileProvider;

/**
 * A data view listing directories, subject to a file filter
 * <p>
 * Adapted from {@link FileDataView}
 */
abstract class DirectoryDataView extends Panel {

    private static final ResourceReference folder = new ResourceReference(GeoServerBasePage.class,
            "img/icons/silk/folder.png");

    private static final ResourceReference geogigFolder = new ResourceReference(
            DirectoryDataView.class, "../geogig_16x16_babyblue.png");

    private static final long serialVersionUID = -2932107412054607607L;

    private final boolean allowSelectingRepositories;

    private final IConverter FILE_NAME_CONVERTER = new StringConverter() {

        private static final long serialVersionUID = 2050812486536366790L;

        @Override
        public String convertToString(Object value, Locale locale) {
            File file = (File) value;
            if (file.isDirectory()) {
                if (RepositoryManager.isGeogigDirectory(file)) {
                    return file.getName();
                }
                return file.getName() + "/";
            } else {
                return file.getName();
            }
        }

    };

    private static final IConverter FILE_LASTMODIFIED_CONVERTER = new StringConverter() {

        private static final long serialVersionUID = 7862772890388011374L;

        @Override
        public String convertToString(Object value, Locale locale) {
            File file = (File) value;
            long lastModified = file.lastModified();
            if (lastModified == 0L)
                return null;
            else {
                return DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT).format(
                        new Date(file.lastModified()));
            }
        }

    };

    private SortableDataProvider<File> provider;

    private WebMarkupContainer fileContent;

    private String tableHeight = "25em";

    @SuppressWarnings("unchecked")
    public DirectoryDataView(final String id, final FileProvider fileProvider,
            final boolean allowSelectingRepositories) {
        super(id);
        this.provider = fileProvider;
        this.allowSelectingRepositories = allowSelectingRepositories;

        final WebMarkupContainer table = new WebMarkupContainer("fileTable");
        table.setOutputMarkupId(true);
        add(table);

        DataView<File> fileTable = new DataView<File>("files", fileProvider) {
            private static final long serialVersionUID = 1345694542339080271L;

            @Override
            protected void populateItem(final Item<File> item) {
                // odd/even alternate style
                item.add(new SimpleAttributeModifier("class", item.getIndex() % 2 == 0 ? "even"
                        : "odd"));

                final File file = item.getModelObject();
                final boolean isGeogigDirectory = RepositoryManager.isGeogigDirectory(file);
                ResourceReference icon = isGeogigDirectory ? geogigFolder : folder;
                item.add(new Image("icon", icon));

                // navigation/selection links
                AjaxFallbackLink<File> link = new IndicatingAjaxFallbackLink<File>("nameLink") {
                    private static final long serialVersionUID = -644973941443812893L;

                    @Override
                    public void onClick(AjaxRequestTarget target) {
                        linkNameClicked((File) item.getModelObject(), target);
                    }

                };
                Label nameLabel = new Label("name", item.getModel()) {

                    private static final long serialVersionUID = -4028081066393114129L;

                    @Override
                    public IConverter getConverter(Class<?> type) {
                        return FILE_NAME_CONVERTER;
                    }
                };
                link.add(nameLabel);
                link.setEnabled(isGeogigDirectory ? DirectoryDataView.this.allowSelectingRepositories
                        : true);
                item.add(link);

                // last modified and size labels
                item.add(new Label("lastModified", item.getModel()) {

                    private static final long serialVersionUID = -4706544449170830483L;

                    @Override
                    public IConverter getConverter(Class<?> type) {
                        return FILE_LASTMODIFIED_CONVERTER;
                    }
                });
            }

        };

        fileContent = new WebMarkupContainer("fileContent") {

            private static final long serialVersionUID = -4197754944388542068L;

            @Override
            protected void onComponentTag(ComponentTag tag) {
                if (tableHeight != null) {
                    tag.getAttributes().put("style", "overflow:auto; height:" + tableHeight);
                }
            }
        };

        fileContent.add(fileTable);

        table.add(fileContent);
    }

    protected abstract void linkNameClicked(File file, AjaxRequestTarget target);

    private static abstract class StringConverter implements IConverter {

        private static final long serialVersionUID = -6464669942374870999L;

        public Object convertToObject(String value, Locale locale) {
            throw new UnsupportedOperationException("This converter works only for strings");
        }
    }

    public SortableDataProvider<File> getProvider() {
        return provider;
    }

    public void setTableHeight(String tableHeight) {
        this.tableHeight = tableHeight;
    }

    public void setFileFilter(IModel<? extends FileFilter> fileFilter) {
        ((FileProvider) provider).setFileFilter(fileFilter);
    }

    public void setDirectory(Model<File> model) {
        ((FileProvider) provider).setDirectory(model);
    }
}
