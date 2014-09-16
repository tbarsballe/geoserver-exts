package org.geogig.geoserver.web.security;

import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import org.apache.wicket.Component;
import org.apache.wicket.ResourceReference;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.AjaxLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.image.Image;
import org.apache.wicket.markup.html.panel.Fragment;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.PropertyModel;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.wicket.GeoServerDataProvider.Property;
import org.geoserver.web.wicket.GeoServerDataProvider.PropertyPlaceholder;
import org.geoserver.web.wicket.ImageAjaxLink;
import org.geoserver.web.wicket.ReorderableTablePanel;

public class WhitelistRulePanel extends ReorderableTablePanel<WhitelistRule> {
    private static final Property<WhitelistRule> EDIT = new PropertyPlaceholder<WhitelistRule>("");
    private static final Property<WhitelistRule> REMOVE = new PropertyPlaceholder<WhitelistRule>("");
    private static final Property<WhitelistRule> NAME = new Property<WhitelistRule>() {
        @Override
        public boolean isSearchable() {
            return false;
        }

        @Override
        public boolean isVisible() {
            return true;
        }

        @Override
        public Comparator getComparator() {
            return null;
        }

        @Override
        public IModel getModel(IModel m) {
            return new PropertyModel(m, "name");
        }

        @Override
        public Object getPropertyValue(WhitelistRule rule) {
            return rule.getName();
        }

        @Override
        public String getName() {
            return "Name";
        }
    };

    private static final Property<WhitelistRule> PATTERN = new Property<WhitelistRule>() {
        @Override
        public boolean isSearchable() {
            return false;
        }

        @Override
        public boolean isVisible() {
            return true;
        }

        @Override
        public Comparator getComparator() {
            return null;
        }

        @Override
        public IModel getModel(IModel m) {
            return new PropertyModel(m, "pattern");
        }

        @Override
        public Object getPropertyValue(WhitelistRule rule) {
            return rule.getPattern();
        }

        @Override
        public String getName() {
            return "Pattern";
        }
    };
    private static final Property<WhitelistRule> REQUIRE_SSL = new Property<WhitelistRule>() {
        @Override
        public boolean isSearchable() {
            return false;
        }

        @Override
        public boolean isVisible() {
            return true;
        }

        @Override
        public Comparator getComparator() {
            return null;
        }

        @Override
        public IModel getModel(IModel m) {
            return new PropertyModel(m, "requireSSL");
        }

        @Override
        public Object getPropertyValue(WhitelistRule rule) {
            return rule.isRequireSSL();
        }

        @Override
        public String getName() {
            return "Require SSL";
        }
    };

    private static List<Property<WhitelistRule>> PROPERTIES = Arrays.asList(NAME, PATTERN, REQUIRE_SSL, EDIT, REMOVE);
    private final ModalWindow window;
    private final List<WhitelistRule> items;

    public WhitelistRulePanel(String id, List<WhitelistRule> items, ModalWindow window) {
        super(id, items, PROPERTIES);
        this.window = window;
        this.items = items;
    }

    @Override
    public Component getComponentForProperty(String id, final IModel model, Property<WhitelistRule> property) {
        if (property == NAME) {
            return new Label(id, property.getModel(model));
        } else if (property == PATTERN) {
            return new Label(id, property.getModel(model));
        } else if (property == REQUIRE_SSL) {
            final WhitelistRule rule = (WhitelistRule) model.getObject();
            Fragment fragment = new Fragment(id, "image.cell");
            if (rule.isRequireSSL()) {
                fragment.add(new Image("display", new ResourceReference(getClass(), "../lock.png")));
            } else {
                fragment.add(new Image("display", new ResourceReference(getClass(), "../lock_open.png")));
            }
            return fragment;
        } else if (property == EDIT) {
            ImageAjaxLink link = new ImageAjaxLink(id, new ResourceReference(GeoServerApplication.class, "img/icons/silk/pencil.png")) {
                @Override 
                public void onClick(AjaxRequestTarget target) {
                    window.setInitialHeight(300);
                    window.setInitialWidth(300);
                    window.setTitle(new Model("Edit whitelist rule"));
                    window.setContent(new WhitelistRuleEditor(window.getContentId(), model, window, WhitelistRulePanel.this));
                    window.show(target);
                }
            };
            return link;
        } else if (property == REMOVE) {
            final WhitelistRule rule = (WhitelistRule) model.getObject();
            ImageAjaxLink link = new ImageAjaxLink(id, new ResourceReference(GeoServerApplication.class, "img/icons/silk/delete.png")) {
                @Override
                    protected void onClick(AjaxRequestTarget target) {
                        items.remove(rule);
                        target.addComponent(WhitelistRulePanel.this);
                    }
            };
            // link.getImage().add(new AttributeModifier("alt", true, new ParamResourceModel("AbstractLayerGroupPage.th.remove", link)));
            return link;
        } else {
            throw new IllegalArgumentException("Property " + property + " is not associated with this component.");
        }
    }
}
