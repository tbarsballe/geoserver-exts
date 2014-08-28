package org.geogig.geoserver.web.data.store.geogig;

import java.io.Serializable;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;
import org.apache.wicket.behavior.SimpleAttributeModifier;
import org.apache.wicket.markup.html.basic.Label;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.markup.html.form.FormComponent;
import org.apache.wicket.model.IModel;
import org.apache.wicket.model.PropertyModel;
import org.apache.wicket.model.ResourceModel;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.panel.CheckBoxParamPanel;
import org.geoserver.web.data.store.panel.DropDownChoiceParamPanel;
import org.geoserver.web.data.store.panel.PasswordParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.geoserver.web.util.MapModel;
import org.geotools.data.DataAccessFactory.Param;
import org.geotools.util.logging.Logging;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.REPOSITORY;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.BRANCH;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.HEAD;
import static org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory.CREATE;

public final class GeoGigDataStoreEditPanel extends StoreEditPanel {
    private static final Logger LOGGER = Logging.getLogger("org.geogig.geoserver.web.data.store.geogig");
    private static final String RESOURCE_KEY_PREFIX = GeoGigDataStoreEditPanel.class.getSimpleName();
    final FormComponent repository;
    final FormComponent branch;
    final FormComponent create;

    public GeoGigDataStoreEditPanel(final String componentId, final Form storeEditForm) {
        super(componentId, storeEditForm);
        final IModel model = storeEditForm.getModel();
        setDefaultModel(model);
        final DataStoreInfo storeInfo = (DataStoreInfo) storeEditForm.getModelObject();
        {
            Map<String, String> existingParameters = parseConnectionParameters(storeInfo);
            Map<String, Serializable> connectionParameters = storeInfo.getConnectionParameters();
            connectionParameters.putAll(existingParameters);
        }
        final IModel paramsModel = new PropertyModel(model, "connectionParameters");

        repository = addTextPanel(paramsModel, REPOSITORY);
        branch = addBranchNameComponent(paramsModel);
        create = addCheckboxPanel(paramsModel, CREATE);
    }

    private Map<String, String> parseConnectionParameters(final DataStoreInfo storeInfo) {
        Map<String, String> params = new HashMap<String, String>();
        Map<String, Serializable> storeParams = storeInfo.getConnectionParameters();
        Object repository = storeParams.get(REPOSITORY.key);
        Object branch = storeParams.get(BRANCH.key);
        Object create = storeParams.get(CREATE.key);
        params.put(REPOSITORY.key, repository == null ? null : storeParams.get(REPOSITORY.key).toString());
        params.put(BRANCH.key, branch == null ? null : storeParams.get(BRANCH.key).toString());
        params.put(CREATE.key, create == null ? null : storeParams.get(CREATE.key).toString());
        return params;
    }

    private FormComponent addTextPanel(final IModel paramsModel, final Param param) {
        final String paramName = param.key;
        final String resourceKey = getClass().getSimpleName() + "." + paramName;
        final boolean required = param.required;
        final TextParamPanel textParamPanel = new TextParamPanel(paramName, new MapModel(paramsModel, paramName), new ResourceModel(resourceKey, paramName), required);
        textParamPanel.getFormComponent().setType(param.type);
        String defaultTitle = String.valueOf(param.title);
        ResourceModel titleModel = new ResourceModel(resourceKey + ".title", defaultTitle);
        String title = String.valueOf(titleModel.getObject());
        textParamPanel.add(new SimpleAttributeModifier("title", title));
        add(textParamPanel);
        return textParamPanel.getFormComponent();
    }

    private FormComponent addCheckboxPanel(final IModel paramsModel, final Param param) {
        final String paramName = param.key;
        final String resourceKey = getClass().getSimpleName() + "." + paramName;
        final boolean required = param.required;
        final CheckBoxParamPanel checkboxParamPanel = new CheckBoxParamPanel(paramName, new MapModel(paramsModel, paramName), new ResourceModel(resourceKey, paramName));
        checkboxParamPanel.getFormComponent().setType(param.type);
        String defaultTitle = String.valueOf(param.title);
        ResourceModel titleModel = new ResourceModel(resourceKey + ".title", defaultTitle);
        String title = String.valueOf(titleModel.getObject());
        checkboxParamPanel.add(new SimpleAttributeModifier("title", title));
        add(checkboxParamPanel);
        return checkboxParamPanel.getFormComponent();
    }

    private FormComponent addBranchNameComponent(final IModel paramsModel) {
        final FormComponent branchNameComponent;
        final String panelId = "branch";
        BranchSelectionPanel selectionPanel;
        selectionPanel = new BranchSelectionPanel(panelId, paramsModel, storeEditForm, repository);
        add(selectionPanel);
        return selectionPanel.getFormComponent();
    }
}
