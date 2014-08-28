package org.geogig.geoserver.web.data.store.geogig;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.ajax.markup.html.form.AjaxSubmitLink;                                                                                                                       
import org.apache.wicket.markup.html.form.DropDownChoice;                                                                                                                            
import org.apache.wicket.markup.html.form.Form;                                                                                                                            
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;
import org.geoserver.web.util.MapModel;
import org.locationtech.geogig.api.Ref;
import org.locationtech.geogig.api.porcelain.BranchListOp;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.locationtech.geogig.geotools.data.GeoGigDataStore;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.BRANCH;

public class BranchSelectionPanel extends Panel {
    private static final long serialVersionUID = 1L; 

    private final DropDownChoice choice;
    private final FormComponent repositoryComponent;

    public BranchSelectionPanel(String id, IModel paramsModel, Form storeEditForm, FormComponent repositoryComponent) {
        super(id);
        final MapModel branchNameModel = new MapModel(paramsModel, BRANCH.key);
        final List<String> choices = new ArrayList<String>();
        this.repositoryComponent = repositoryComponent;
        choice = new DropDownChoice("branchDropDown", branchNameModel, choices);
        choice.setOutputMarkupId(true);
        choice.setNullValid(true);
        choice.setRequired(false);
        add(choice);
        updateChoices();

        final AjaxSubmitLink refreshLink = new AjaxSubmitLink("refresh", storeEditForm) {
            private static final long serialVersionUID = 1L;

            @Override
            protected void onError(AjaxRequestTarget target, Form form) {
                onSubmit(target, form);
            }

            @Override
            protected void onSubmit(AjaxRequestTarget target, Form form) {
                updateChoices();
                target.addComponent(choice);
            }
        };
        add(refreshLink);
    }

    private List<String> getBranchNames(Serializable repository) throws IOException {
        GeoGigDataStoreFactory factory = new GeoGigDataStoreFactory();
        Map<String, Serializable> parameters = new HashMap<String, Serializable>();
        parameters.put(GeoGigDataStoreFactory.REPOSITORY.key, repository);
        GeoGigDataStore store = factory.createDataStore(parameters);
        List<Ref> refs = store.getGeogig().command(BranchListOp.class).call();
        List<String> names = new ArrayList<String>();
        for (Ref r : refs) {
            names.add(r.localName());
        }
        return names;
    }

    public FormComponent getFormComponent() {
        return choice;
    }

    private void updateChoices() {
        final Serializable repository = (Serializable) repositoryComponent.getModelObject();
        List<String> branchNames;
        try {
            branchNames = getBranchNames(repository);
        } catch (IOException e) {
            branchNames = new ArrayList<String>();
        } catch (RuntimeException e) {
            branchNames = new ArrayList<String>();
        }
        String current = (String) choice.getModelObject();
        if (current != null && !branchNames.contains(current)) {
            branchNames.add(0, current);
        }
        choice.setChoices(branchNames);
    }
}
