package org.geogig.geoserver.web.repository;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.PasswordTextField;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.panel.FeedbackPanel;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.validator.PatternValidator;
import org.geogig.geoserver.config.RepositoryManager;
import org.geoserver.web.wicket.GeoServerAjaxFormLink;
import org.locationtech.geogig.api.Ref;
import org.locationtech.geogig.api.SymRef;

public class RemoteEditPanel extends Panel {

    private static final long serialVersionUID = 1510021788966720096L;

    Form<RemoteInfo> form;

    TextField<String> name;

    TextField<String> url;

    TextField<String> user;

    PasswordTextField password;

    private boolean isNew;

    public RemoteEditPanel(String id, IModel<RemoteInfo> model, final ModalWindow parentWindow,
            final RemotesListPanel table, final boolean isNew) {
        super(id, model);
        this.isNew = isNew;

        form = new Form<RemoteInfo>("form", model);
        add(form);

        final FeedbackPanel feedback = new FeedbackPanel("feedback");
        feedback.setOutputMarkupId(true);
        form.add(feedback);

        name = new TextField<String>("name", new PropertyModel<String>(model, "name"));
        name.setRequired(true);
        name.add(new PatternValidator("[^\\s]+"));
        name.add(new IValidator<String>() {
            private static final long serialVersionUID = 2927770353770055054L;

            final String previousName = isNew ? null : form.getModelObject().getName();

            @Override
            public void validate(IValidatable<String> validatable) {
                String name = validatable.getValue();
                for (RemoteInfo ri : table.getRemotes()) {
                    String newName = ri.getName();
                    if (newName != null && !newName.equals(previousName) && newName.equals(name)) {
                        form.error(String.format("A remote named %s already exists", name));
                    }
                }
            }
        });

        url = new TextField<String>("url", new PropertyModel<String>(model, "URL"));
        url.setRequired(true);

        user = new TextField<String>("user", new PropertyModel<String>(model, "userName"));

        password = new PasswordTextField("password", new PropertyModel<String>(model, "password"));
        password.setRequired(false);
        password.setResetPassword(false);

        form.add(name);
        form.add(url);
        form.add(user);
        form.add(password);

        GeoServerAjaxFormLink remotePingLink = new RemotePingLink("validate", form);
        form.add(remotePingLink);

        form.add(new AjaxSubmitLink("submit", form) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                target.addComponent(feedback);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                if (RemoteEditPanel.this.isNew) {
                    RemoteInfo newRemote = (RemoteInfo) form.getModelObject();
                    table.add(newRemote);
                }
                parentWindow.close(target);
                target.addComponent(table);
            }
        });

        form.add(new AjaxSubmitLink("cancel") {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target, Form<?> form) {
                onSubmit(target, form);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form<?> form) {
                parentWindow.close(target);
                target.addComponent(table);
            }
        });
    }

    private final class RemotePingLink extends GeoServerAjaxFormLink {
        private static final long serialVersionUID = 1L;

        private RemotePingLink(String id, Form form) {
            super(id, form);
        }

        @Override
        protected void onClick(AjaxRequestTarget target, Form form) {
            url.processInput();
            user.processInput();
            password.processInput();
            RemoteInfo remoteInfo = (RemoteInfo) form.getModelObject();
            String location = remoteInfo.getURL();
            String username = remoteInfo.getUserName();
            String pwd = remoteInfo.getPassword();
            try {
                Ref head = RepositoryManager.pingRemote(location, username, pwd);
                String headTarget;
                if (head instanceof SymRef) {
                    headTarget = ((SymRef) head).getTarget();
                } else {
                    headTarget = head.getObjectId().toString();
                }
                form.info("Connection suceeded. HEAD is at " + headTarget);
            } catch (Exception e) {
                form.error(e.getMessage());
            }
            target.addComponent(form);
        }
    }

}
