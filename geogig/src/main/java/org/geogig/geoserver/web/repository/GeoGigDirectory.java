/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import java.io.File;

import org.apache.wicket.Component;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;
import org.apache.wicket.extensions.ajax.markup.html.modal.ModalWindow;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponentPanel;
import org.apache.wicket.markup.html.form.TextField;
import org.apache.wicket.markup.html.form.validation.FormComponentFeedbackBorder;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.Model;
import org.apache.wicket.model.ResourceModel;
import org.apache.wicket.validation.IValidatable;
import org.apache.wicket.validation.IValidator;
import org.apache.wicket.validation.ValidationError;
import org.geoserver.web.data.store.panel.FileParamPanel;

import com.google.common.base.Strings;

/**
 * A panel to browse the filesystem for geogig repositories.
 * <p>
 * Adapted from {@link FileParamPanel}
 */
class GeoGigDirectory extends FormComponentPanel<String> {

    private static final long serialVersionUID = -7456670856888745195L;

    private final TextField<String> directory;

    private final ModalWindow dialog;

    /**
     * 
     * @param validators any extra validator that should be added to the input field, or
     *        {@code null}
     */
    public GeoGigDirectory(final String id, final IModel<String> valueModel) {
        // make the value of the text field the model of this panel, for easy value retrieval
        super(id, valueModel);

        // add the dialog for the file chooser
        add(dialog = new ModalWindow("dialog"));

        // the text field, with a decorator for validations
        directory = new TextField<String>("value", new GeoGigDirectoryModel(valueModel));
        directory.setRequired(true);
        directory.setOutputMarkupId(true);
        directory.add(GEOGIG_DIR_VALIDATOR);

        FormComponentFeedbackBorder feedback = new FormComponentFeedbackBorder("wrapper");
        feedback.add(directory);
        feedback.add(chooserButton());
        add(feedback);
    }

    @Override
    protected void convertInput() {
        String uri = directory.getConvertedInput();
        setConvertedInput(uri);
    }

    protected Component chooserButton() {
        AjaxSubmitLink link = new AjaxSubmitLink("chooser") {

            private static final long serialVersionUID = 1242472443848716943L;

            @Override
            public boolean getDefaultFormProcessing() {
                return false;
            }

            @Override
            public void onSubmit(AjaxRequestTarget target, Form<?> form) {
                File file = null;
                directory.processInput();
                String input = directory.getConvertedInput();
                if (input != null && !input.equals("")) {
                    file = new File(input);
                }

                final boolean makeRepositoriesSelectable = false;
                DirectoryChooser chooser = new DirectoryChooser(dialog.getContentId(),
                        new Model<File>(file), makeRepositoriesSelectable) {

                    private static final long serialVersionUID = 1L;

                    @Override
                    protected void geogigDirectoryClicked(final File file, AjaxRequestTarget target) {
                        // clear the raw input of the field won't show the new model value
                        directory.clearInput();
                        directory.setModelObject(file.getAbsolutePath());

                        target.addComponent(directory);
                        dialog.close(target);
                    };
                    
                    @Override
                    protected void directorySelected(File file, AjaxRequestTarget target) {
                       directory.setModelObject(file.getAbsolutePath());
                       target.addComponent(directory);
                       dialog.close(target);
                   }
                };
                chooser.setFileTableHeight(null);
                dialog.setContent(chooser);
                dialog.setTitle(new ResourceModel("GeoGigDirectoryPanel.chooser.title"));
                dialog.show(target);
            }

        };
        return link;
    }

    private static final IValidator<String> GEOGIG_DIR_VALIDATOR = new IValidator<String>() {

        private static final long serialVersionUID = -3359171335804837536L;

        @Override
        public void validate(IValidatable<String> validatable) {
            final String uri = validatable.getValue();
            ValidationError error = new ValidationError();
            if (Strings.isNullOrEmpty(uri)) {
                error.addMessageKey("GeoGigDirectoryPanel.error.empty");
            } else {
                File repo = new File(uri, ".geogig");
                if (!(repo.exists() && repo.isDirectory())) {
                    error.addMessageKey("GeoGigDirectoryPanel.error.notAGeogigDir");
                } else if (!repo.canRead()) {
                    error.addMessageKey("GeoGigDirectoryPanel.error.cantRead");
                }
            }
            if (!error.getKeys().isEmpty()) {
                validatable.error(error);
            }
        }
    };

}
