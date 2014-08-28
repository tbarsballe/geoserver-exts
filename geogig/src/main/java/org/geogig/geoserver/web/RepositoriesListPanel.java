package org.geogig.geoserver.web;

import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.model.IModel;
import org.geogig.geoserver.config.RepositoryInfo;
import org.geoserver.web.GeoServerBasePage;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDialog;
import org.geoserver.web.wicket.GeoServerTablePanel;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ParamResourceModel;
import org.geoserver.web.wicket.SimpleBookmarkableLink;

public class RepositoriesListPanel extends GeoServerTablePanel<RepositoryInfo> {

    private static final long serialVersionUID = 5957961031378924960L;

    private ModalWindow popupWindow;

    private GeoServerDialog dialog;

    public RepositoriesListPanel(final String id, RepositoryProvider provider, boolean selectable) {
        super(id, provider, selectable);

        // the popup window for messages
        popupWindow = new ModalWindow("popupWindow");
        add(popupWindow);

        add(dialog = new GeoServerDialog("dialog"));
    }

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

    @SuppressWarnings({ "unchecked", "rawtypes" })
    private Component nameLink(String id, IModel itemModel) {
        IModel<String> nameModel = RepositoryProvider.NAME.getModel(itemModel);
        return new SimpleBookmarkableLink(id, RepositoryEditPage.class, nameModel, "name",
                (String) nameModel.getObject());
    }

    @SuppressWarnings("rawtypes")
    protected Component removeLink(final String id, final IModel itemModel) {
        RepositoryInfo info = (RepositoryInfo) itemModel.getObject();

        final ParamResourceModel confirmRemove = new ParamResourceModel(
                "RepositoriesListPanel.confirmRemoval", this, info.getName());

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

                    @Override
                    protected Component getContents(String id) {
                        return new Label(id, confirmRemove).setEscapeModelStrings(false);
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
