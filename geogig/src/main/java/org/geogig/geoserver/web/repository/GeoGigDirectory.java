/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.repository;

import java.io.File;
import java.io.FileFilter;
import java.io.Serializable;

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
import org.geoserver.web.wicket.browser.GeoServerFileChooser;

import com.google.common.base.Strings;

/**
 * A panel to browse the filesystem for geogig repositories.
 * <p>
 * Adapted from {@link FileParamPanel}
 */
public class GeoGigDirectory extends FormComponentPanel<String> {

    private static final long serialVersionUID = -7456670856888745195L;

    private final TextField<String> value;

    private final ModalWindow dialog;

    private final IModel<DirectoryFilter> fileFilter = new Model<DirectoryFilter>(
            new DirectoryFilter());

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
        value = new TextField<String>("value", new GeoGigDirectoryModel(valueModel));
        value.setRequired(true);
        value.setOutputMarkupId(true);
        value.add(GEOGIG_DIR_VALIDATOR);

        FormComponentFeedbackBorder feedback = new FormComponentFeedbackBorder("wrapper");
        feedback.add(value);
        feedback.add(chooserButton());
        add(feedback);
    }
    
    @Override
    protected void convertInput(){
        String uri = value.getConvertedInput();
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
                value.processInput();
                String input = value.getConvertedInput();
                if (input != null && !input.equals("")) {
                    file = new File(input);
                }

                GeoServerFileChooser chooser = new GeoServerFileChooser(dialog.getContentId(),
                        new Model<File>(file)) {

                    private static final long serialVersionUID = 1L;

                    protected void fileClicked(File file, AjaxRequestTarget target) {
                        // clear the raw input of the field won't show the new model value
                        value.clearInput();
                        value.setModelObject(file.getAbsolutePath());

                        target.addComponent(value);
                        dialog.close(target);
                    };
                };
                chooser.setFileTableHeight(null);
                chooser.setFilter(fileFilter);
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

    private static final class DirectoryFilter implements FileFilter, Serializable {
        private static final long serialVersionUID = -2280505390702552L;

        @Override
        public boolean accept(File file) {
            return file.isDirectory();
        }
    }
}
