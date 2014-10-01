package org.geogig.geoserver.web.security;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.CheckBox;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;

public class WhitelistRuleEditor extends Panel {
    public WhitelistRuleEditor(String id, IModel<?> model, final ModalWindow window,
            final WhitelistRulePanel table) {
        super(id, model);
        Form form = new Form("form", model);
        add(form);
        form.add(new TextField("name", new PropertyModel(model, "name")));
        form.add(new TextField("pattern", new PropertyModel(model, "pattern")));
        form.add(new CheckBox("requireSSL", new PropertyModel(model, "requireSSL")));
        form.add(new AjaxSubmitLink("submit") {
            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                onSubmit(target, form);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                window.close(target);
                target.addComponent(table);
            }
        });
    }
}
