/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web.data.store.geogig;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import org.apache.wicket.Page;
import org.apache.wicket.feedback.FeedbackMessage;
import org.apache.wicket.markup.html.form.DropDownChoice;
import org.apache.wicket.markup.html.form.Form;
import org.apache.wicket.model.Model;
import org.apache.wicket.util.tester.FormTester;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.web.GeoServerApplication;
import org.geoserver.web.GeoServerWicketTestSupport;
import org.geoserver.web.data.store.DataAccessEditPage;
import org.geoserver.web.data.store.DataAccessNewPage;
import org.geoserver.web.data.store.StoreEditPanel;
import org.geoserver.web.data.store.StoreExtensionPoints;
import org.geoserver.web.data.store.panel.CheckBoxParamPanel;
import org.geoserver.web.data.store.panel.TextParamPanel;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.junit.Test;
import java.io.IOException;
import java.io.Serializable;
import java.util.Arrays;
import java.util.List;

public class GeoGigDataStoreEditPanelTest extends GeoServerWicketTestSupport {
    private Page page;
    private DataStoreInfo storeInfo;
    private Form editForm;

    private GeoGigDataStoreEditPanel startPanelForNewStore() {
        login();
        page = new DataAccessNewPage(new GeoGigDataStoreFactory().getDisplayName());
        tester.startPage(page);

        editForm = (Form) tester.getComponentFromLastRenderedPage("dataStoreForm");

        GeoGigDataStoreEditPanel panel = (GeoGigDataStoreEditPanel) tester
                .getComponentFromLastRenderedPage("dataStoreForm:parametersPanel");

        return panel;
    }

    private GeoGigDataStoreEditPanel startPanelToEditStore() {
        final Catalog catalog = getCatalog();
        storeInfo = catalog.getFactory().createDataStore();
        storeInfo.setDescription("dummy geogig store");
        storeInfo.setEnabled(true);
        storeInfo.setName("dummy_geogig");
        storeInfo.setType((new GeoGigDataStoreFactory()).getDisplayName());
        storeInfo.setWorkspace(catalog.getDefaultWorkspace());
        storeInfo.getConnectionParameters().put(GeoGigDataStoreFactory.BRANCH.key, "alpha");
        storeInfo.getConnectionParameters().put(GeoGigDataStoreFactory.REPOSITORY.key, "/dummy/repo/");
        catalog.save(storeInfo);
        final String storeId = storeInfo.getId();
        login();
        page = new DataAccessEditPage(storeId);
        tester.startPage(page);
        editForm = (Form) tester.getComponentFromLastRenderedPage("dataStoreForm");
        GeoGigDataStoreEditPanel panel = (GeoGigDataStoreEditPanel) tester
                .getComponentFromLastRenderedPage("dataStoreForm:parametersPanel");
        return panel;
    }

    @Test
    public void testExtensionPoint() {
        storeInfo = getCatalog().getFactory().createDataStore();
        storeInfo.setType(new GeoGigDataStoreFactory().getDisplayName());
        editForm = new Form("formid");
        editForm.setModel(new Model(storeInfo));
        GeoServerApplication app = getGeoServerApplication();
        StoreEditPanel storeEditPanel = StoreExtensionPoints.getStoreEditPanel("id", editForm, storeInfo, app);
        assertNotNull(storeEditPanel);
        assertTrue(storeEditPanel instanceof GeoGigDataStoreEditPanel);
    }

    @Test
    public void testStartupForNew() {
        startPanelForNewStore();

        final String base = "dataStoreForm:parametersPanel:";
        tester.assertComponent(base + "geogig_repository", TextParamPanel.class);
        tester.assertComponent(base + "create", CheckBoxParamPanel.class);
        tester.assertComponent(base + "branch", BranchSelectionPanel.class);
    }

    @Test
    public void testStartupForEdit() {
        startPanelToEditStore();

        final String base = "dataStoreForm:parametersPanel:";
        tester.assertComponent(base + "geogig_repository", TextParamPanel.class);
        tester.assertComponent(base + "create", CheckBoxParamPanel.class);
        tester.assertComponent(base + "branch", BranchSelectionPanel.class);
        tester.assertModelValue(base + "branch:branchDropDown", "alpha");
    }

    @Test
    public void testRefreshBranchListWithBadConnectionParams() {
        startPanelForNewStore();
        final FormTester formTester = tester.newFormTester("dataStoreForm");
        final String base = "dataStoreForm:parametersPanel:";
        BranchSelectionPanel branchPanel = (BranchSelectionPanel) tester.getComponentFromLastRenderedPage(base + "branch");
        branchPanel.setGeoGigConnector(new GeoGigConnector() {
            @Override
            public List<String> listBranches(Serializable repository) throws IOException {
                throw new IOException("Could not connect");
            }
        });
        String submitLink = base + "branch:refresh";
        tester.executeAjaxEvent(submitLink, "onclick");
        FeedbackMessage feedbackMessage = formTester.getForm().getFeedbackMessage();
        assertNotNull(feedbackMessage);
        Serializable message = feedbackMessage.getMessage();
        assertNotNull(message);
        String expectedMessage = "Could not connect";
        assertEquals(expectedMessage, message.toString());
    }

    @Test
    public void testRefreshBranchList() {
        startPanelForNewStore();
        final FormTester formTester = tester.newFormTester("dataStoreForm");
        final String base = "dataStoreForm:parametersPanel:";
        BranchSelectionPanel branchPanel = (BranchSelectionPanel) tester.getComponentFromLastRenderedPage(base + "branch");
        final List<String> branches = Arrays.asList("master", "alpha", "sandbox");
        branchPanel.setGeoGigConnector(new GeoGigConnector() {
            public List<String> listBranches(Serializable repository) {
                return branches;
            }
        });
        String dropDownPath = base + "branch:branchDropDown";
        final DropDownChoice choice = (DropDownChoice) tester.getComponentFromLastRenderedPage(dropDownPath);

        assertTrue(choice.getChoices().isEmpty());
        String submitLink = base + "branch:refresh";
        tester.executeAjaxEvent(submitLink, "onclick");
        FeedbackMessage feedbackMessage = formTester.getForm().getFeedbackMessage();
        assertNull(feedbackMessage);
        assertEquals(branches, choice.getChoices());
    }
}
