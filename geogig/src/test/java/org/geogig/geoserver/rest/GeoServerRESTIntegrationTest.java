/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.rest;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.Serializable;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;

import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogFactory;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.data.test.SystemTestData;
import org.geoserver.test.GeoServerSystemTestSupport;
import org.geotools.data.DataAccess;
import org.junit.AfterClass;
import org.junit.Test;
import org.locationtech.geogig.api.Context;
import org.locationtech.geogig.api.GeoGIG;
import org.locationtech.geogig.api.GlobalContextBuilder;
import org.locationtech.geogig.api.ObjectId;
import org.locationtech.geogig.api.Ref;
import org.locationtech.geogig.api.RevObject;
import org.locationtech.geogig.api.TestPlatform;
import org.locationtech.geogig.api.plumbing.RefParse;
import org.locationtech.geogig.api.plumbing.ResolveGeogigDir;
import org.locationtech.geogig.api.plumbing.ResolveTreeish;
import org.locationtech.geogig.api.plumbing.RevObjectParse;
import org.locationtech.geogig.api.porcelain.CommitOp;
import org.locationtech.geogig.cli.test.functional.general.CLITestContextBuilder;
import org.locationtech.geogig.geotools.data.GeoGigDataStore;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.locationtech.geogig.storage.ObjectSerializingFactory;
import org.locationtech.geogig.storage.datastream.DataStreamSerializationFactoryV1;
import org.locationtech.geogig.test.integration.RepositoryTestCase;
import org.opengis.feature.Feature;
import org.opengis.feature.type.FeatureType;
import org.restlet.data.MediaType;

import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.AbstractIterator;
import com.google.common.collect.Iterators;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonPrimitive;
import com.mockrunner.mock.web.MockHttpServletResponse;

/**
 * Integration test for GeoServer cached layers using the GWC REST API
 * 
 */
public class GeoServerRESTIntegrationTest extends GeoServerSystemTestSupport {

    private static final String WORKSPACE = "geogigtest";

    private static final String STORE = "geogigstore";

    private static final String BASE_URL = "/geogig/" + WORKSPACE + ":" + STORE;

    private static RepositoryTestCase helper;

    @Override
    protected void onSetUp(SystemTestData testData) throws Exception {
        helper = new RepositoryTestCase() {

            @Override
            protected Context createInjector() {
                TestPlatform testPlatform = (TestPlatform) createPlatform();
                GlobalContextBuilder.builder = new CLITestContextBuilder(testPlatform);
                return GlobalContextBuilder.builder.build();
            }

            @Override
            protected void setUpInternal() throws Exception {
                configureGeoGigDataStore();
            }
        };
        helper.repositoryTempFolder.create();
        helper.setUp();
    }

    @AfterClass
    public static void oneTimeTearDown() throws Exception {
        if (helper != null) {
            helper.tearDown();
            helper.repositoryTempFolder.delete();
        }
    }

    private void configureGeoGigDataStore() throws Exception {
        helper.insertAndAdd(helper.lines1);
        helper.getGeogig().command(CommitOp.class).call();

        Catalog catalog = getCatalog();
        CatalogFactory factory = catalog.getFactory();
        NamespaceInfo ns = factory.createNamespace();
        ns.setPrefix(WORKSPACE);
        ns.setURI("http://geogig.org");
        catalog.add(ns);
        WorkspaceInfo ws = factory.createWorkspace();
        ws.setName(ns.getName());
        catalog.add(ws);

        DataStoreInfo ds = factory.createDataStore();
        ds.setEnabled(true);
        ds.setDescription("Test Geogig DataStore");
        ds.setName(STORE);
        ds.setType(GeoGigDataStoreFactory.DISPLAY_NAME);
        ds.setWorkspace(ws);
        Map<String, Serializable> connParams = ds.getConnectionParameters();

        Optional<URL> geogigDir = helper.getGeogig().command(ResolveGeogigDir.class).call();
        File repositoryUrl = new File(geogigDir.get().toURI()).getParentFile();
        assertTrue(repositoryUrl.exists() && repositoryUrl.isDirectory());

        connParams.put(GeoGigDataStoreFactory.REPOSITORY.key, repositoryUrl);
        connParams.put(GeoGigDataStoreFactory.DEFAULT_NAMESPACE.key, ns.getURI());
        catalog.add(ds);

        DataStoreInfo dsInfo = catalog.getDataStoreByName(WORKSPACE, STORE);
        assertNotNull(dsInfo);
        assertEquals(GeoGigDataStoreFactory.DISPLAY_NAME, dsInfo.getType());
        DataAccess<? extends FeatureType, ? extends Feature> dataStore = dsInfo.getDataStore(null);
        assertNotNull(dataStore);
        assertTrue(dataStore instanceof GeoGigDataStore);
    }

    /**
     * Test for resource {@code /rest/<repository>/repo/manifest}
     */
    @Test
    public void testGetManifest() throws Exception {
        final String url = BASE_URL + "/repo/manifest";
        MockHttpServletResponse sr = getAsServletResponse(url);
        assertEquals(200, sr.getStatusCode());

        String contentType = sr.getContentType();
        assertTrue(contentType, sr.getContentType().startsWith("text/plain"));

        String responseBody = sr.getOutputStreamContent();
        assertNotNull(responseBody);
        assertTrue(responseBody, responseBody.startsWith("HEAD refs/heads/master"));
    }

    /**
     * Test for resource {@code /rest/<repository>/repo/exists?oid=...}
     */
    @Test
    public void testRevObjectExists() throws Exception {
        final String resource = BASE_URL + "/repo/exists?oid=";

        GeoGIG geogig = helper.getGeogig();
        Ref head = geogig.command(RefParse.class).setName(Ref.HEAD).call().get();
        ObjectId commitId = head.getObjectId();

        String url;
        url = resource + commitId.toString();
        assertResponse(url, "1");

        ObjectId treeId = geogig.command(ResolveTreeish.class).setTreeish(commitId).call().get();
        url = resource + treeId.toString();
        assertResponse(url, "1");

        url = resource + ObjectId.forString("fake");
        assertResponse(url, "0");
    }

    /**
     * Test for resource {@code /rest/<repository>/repo/objects/<oid>}
     */
    @Test
    public void testGetObject() throws Exception {
        GeoGIG geogig = helper.getGeogig();
        Ref head = geogig.command(RefParse.class).setName(Ref.HEAD).call().get();
        ObjectId commitId = head.getObjectId();
        ObjectId treeId = geogig.command(ResolveTreeish.class).setTreeish(commitId).call().get();

        testGetRemoteObject(commitId);
        testGetRemoteObject(treeId);
    }

    private void testGetRemoteObject(ObjectId oid) throws Exception {
        GeoGIG geogig = helper.getGeogig();

        final String resource = BASE_URL + "/repo/objects/";
        final String url = resource + oid.toString();

        MockHttpServletResponse servletResponse;
        InputStream responseStream;

        servletResponse = getAsServletResponse(url);
        assertEquals(200, servletResponse.getStatusCode());

        String contentType = MediaType.APPLICATION_OCTET_STREAM.toString();
        assertEquals(contentType, servletResponse.getContentType());

        responseStream = getBinaryInputStream(servletResponse);

        ObjectSerializingFactory factory = DataStreamSerializationFactoryV1.INSTANCE;

        RevObject actual = factory.createObjectReader().read(oid, responseStream);
        RevObject expected = geogig.command(RevObjectParse.class).setObjectId(oid).call().get();
        assertEquals(expected, actual);
    }

    /**
     * Test for resource {@code /rest/<repository>/repo/batchobjects}
     */
    @Test
    public void testGetBatchedObjects() throws Exception {
        GeoGIG geogig = helper.getGeogig();
        Ref head = geogig.command(RefParse.class).setName(Ref.HEAD).call().get();
        ObjectId commitId = head.getObjectId();

        testGetBatchedRemoteObjects(commitId);
    }

    private void testGetBatchedRemoteObjects(ObjectId oid) throws Exception {
        GeoGIG geogig = helper.getGeogig();

        final String resource = BASE_URL + "/repo/batchobjects";
        final String url = resource;

        RevObject expected = geogig.command(RevObjectParse.class).setObjectId(oid).call().get();

        JsonObject requestBody = new JsonObject();
        JsonArray wantList = new JsonArray();
        wantList.add(new JsonPrimitive(oid.toString()));
        requestBody.add("want", wantList);

        MockHttpServletResponse servletResponse;
        InputStream responseStream;

        servletResponse = postAsServletResponse(url, requestBody.toString(), "application/json");
        assertEquals(200, servletResponse.getStatusCode());

        String contentType = MediaType.APPLICATION_OCTET_STREAM.toString();
        assertEquals(contentType, servletResponse.getContentType());

        responseStream = getBinaryInputStream(servletResponse);

        ObjectSerializingFactory factory = DataStreamSerializationFactoryV1.INSTANCE;

        Iterator<RevObject> objects = new ObjectStreamIterator(responseStream, factory);
        RevObject actual = Iterators.getLast(objects);
        assertEquals(expected, actual);
    }

    private MockHttpServletResponse assertResponse(String url, String expectedContent)
            throws Exception {

        MockHttpServletResponse sr = getAsServletResponse(url);
        assertEquals(sr.getOutputStreamContent(), 200, sr.getStatusCode());

        String responseBody = sr.getOutputStreamContent();

        assertNotNull(responseBody);
        assertEquals(expectedContent, responseBody);
        return sr;
    }

    private class ObjectStreamIterator extends AbstractIterator<RevObject> {
        private final InputStream bytes;

        private final ObjectSerializingFactory formats;

        public ObjectStreamIterator(InputStream input, ObjectSerializingFactory formats) {
            this.bytes = input;
            this.formats = formats;
        }

        @Override
        protected RevObject computeNext() {
            try {
                byte[] id = new byte[20];
                int len = bytes.read(id, 0, 20);
                if (len < 0)
                    return endOfData();
                if (len != 20)
                    throw new IllegalStateException("We need a 'readFully' operation!");
                System.out.println(bytes);
                return formats.createObjectReader().read(new ObjectId(id), bytes);
            } catch (EOFException e) {
                return endOfData();
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
        }
    }

}
