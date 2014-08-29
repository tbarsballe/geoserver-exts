package org.geogig.geoserver.web.repository;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geogig.geoserver.web.RepositoryEditPage;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleAjaxLink;

public class RepositoriesListPanel extends GeoServerTablePanel<RepositoryInfo> {

    private static final long serialVersionUID = 5957961031378924960L;

    private ModalWindow popupWindow;

    private GeoServerDialog dialog;

    public RepositoriesListPanel(final String id) {
        super(id, new RepositoryProvider(), false);

        // the popup window for messages
        popupWindow = new ModalWindow("popupWindow");
        add(popupWindow);

        add(dialog = new GeoServerDialog("dialog"));
    }

    @SuppressWarnings({ "unchecked", "rawtypes" })
    @Override
    protected Component getComponentForProperty(String id, IModel itemModel,
            Property<RepositoryInfo> property) {

        if (property == RepositoryProvider.NAME) {
            return nameLink(id, itemModel);
        } else if (property == RepositoryProvider.LOCATION) {
            String location = (String) RepositoryProvider.LOCATION.getModel(itemModel).getObject();
            return new Label(id, location);
        } else if (property == RepositoryProvider.REMOVELINK) {
            return removeLink(id, itemModel);
        }
        return null;
    }

    private Component nameLink(String id, IModel<RepositoryInfo> itemModel) {
        @SuppressWarnings("unchecked")
        IModel<String> nameModel = RepositoryProvider.NAME.getModel(itemModel);

        SimpleAjaxLink<RepositoryInfo> link = new SimpleAjaxLink<RepositoryInfo>(id, itemModel,
                nameModel) {
            private static final long serialVersionUID = -18292070541084372L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                IModel<RepositoryInfo> model = getModel();
                RepositoriesListPanel.this.setResponsePage(new RepositoryEditPage(model));
            }
        };
        return link;
    }

    protected Component removeLink(final String id, final IModel<RepositoryInfo> itemModel) {

        ResourceReference removeIcon = new ResourceReference(GeoServerBasePage.class,
                "img/icons/silk/delete.png");

        return new ImageAjaxLink(id, removeIcon) {

            private static final long serialVersionUID = -3061812114487970427L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                GeoServerDialog dialog = RepositoriesListPanel.this.dialog;
                dialog.setTitle(new ParamResourceModel(
                        "RepositoriesListPanel.confirmRemoval.title", this));

                dialog.showOkCancel(target, new GeoServerDialog.DialogDelegate() {
                    private static final long serialVersionUID = -450822090965263894L;

                    @Override
                    protected Component getContents(String id) {
                        return new ConfirmRepoRemovePanel(id, itemModel);
                    }

                    @Override
                    protected boolean onSubmit(AjaxRequestTarget target, Component contents) {
                        boolean proceed = true;
                        System.err.println("Stub method. Implement repo remove here!");
                        return proceed;
                    }

                    @Override
                    public void onClose(AjaxRequestTarget target) {
                        target.addComponent(RepositoriesListPanel.this);
                    }

                });
            }
        };

    }
}
