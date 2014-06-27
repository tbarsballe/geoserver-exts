/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.gdb;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.geoserver.importer.ImporterTestUtils.unpack;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import org.geotools.data.DataUtilities;
import org.geotools.data.FileDataStore;
import org.junit.Before;
import org.junit.Test;
import org.geoserver.importer.csv.parse.CSVAttributesOnlyStrategy;
import org.geoserver.importer.csv.parse.CSVLatLonStrategy;
import org.geoserver.importer.csv.parse.CSVSpecifiedLatLngStrategy;
import org.geoserver.importer.csv.parse.CSVSpecifiedWKTStrategy;
import org.geoserver.importer.csv.parse.CSVStrategy;

public class GDBDataStoreFactoryTest {

    private GDBDataStoreFactory factory;

    private File file;

    private URL locationsResource;

    @Before
    public void setUp() throws Exception {
        factory = new GDBDataStoreFactory();
        locationsResource = DataUtilities.fileToURL(new File(unpack("csv/locations.zip"), "locations.csv"));
        //locationsResource = CSVDataStoreFactory.class.getResource("locations.csv");
        assert locationsResource != null : "Could not find locations.csv resource";
        file = DataUtilities.urlToFile(locationsResource);
    }

    @Test
    public void testBasicGetters() throws MalformedURLException {
        assertEquals("CSV", factory.getDisplayName());
        assertEquals("Comma delimited text file", factory.getDescription());
        assertTrue(factory.canProcess(locationsResource));
        assertTrue(factory.getImplementationHints().isEmpty());
        assertArrayEquals(new String[] { ".csv" }, factory.getFileExtensions());
        assertNotNull("Invalid Parameter Info", factory.getParametersInfo());
    }

    @Test
    public void testCreateDataStoreFileParams() throws Exception {
        Map<String, Serializable> fileParams = new HashMap<String, Serializable>(1);
        fileParams.put("file", file);
        FileDataStore dataStore = factory.createDataStore(fileParams);
        assertNotNull("Could not create datastore from file params", dataStore);
    }

    @Test
    public void testCreateDataStoreURLParams() throws Exception {
        Map<String, Serializable> urlParams = new HashMap<String, Serializable>(1);
        urlParams.put("url", locationsResource);
        FileDataStore dataStore = factory.createDataStore(urlParams);
        assertNotNull("Could not create datastore from url params", dataStore);
    }

    @Test
    public void testCreateDataStoreURL() throws MalformedURLException, IOException {
        FileDataStore dataStore = factory.createDataStore(locationsResource);
        assertNotNull("Failure creating data store", dataStore);
    }

    @Test
    public void testGetTypeName() throws IOException {
        FileDataStore dataStore = factory.createDataStore(locationsResource);
        String[] typeNames = dataStore.getTypeNames();
        assertEquals("Invalid number of type names", 1, typeNames.length);
        assertEquals("Invalid type name", "locations", typeNames[0]);
    }

    @Test
    public void testCanProcessFileParams() {
        Map<String, Serializable> fileParams = new HashMap<String, Serializable>(1);
        fileParams.put("file", file);
        assertTrue("Did not process file params", factory.canProcess(fileParams));
    }

    @Test
    public void testCanProcessURLParams() {
        Map<String, Serializable> urlParams = new HashMap<String, Serializable>(1);
        urlParams.put("url", locationsResource);
        assertTrue("Did not process url params", factory.canProcess(urlParams));
    }

    @Test
    public void testInvalidParamsCreation() throws Exception {
        Map<String, Serializable> params = Collections.emptyMap();
        try {
            factory.createDataStore(params);
        } catch (IllegalArgumentException e) {
            assertTrue(true);
            return;
        } catch (Exception e) {
        }
        fail("Did not throw illegal argument exception for null file");
    }

    @Test
    public void testFileDoesNotExist() throws Exception {
        try {
            factory.createDataStoreFromFile(new File("/tmp/does-not-exist.csv"));
        } catch (IllegalArgumentException e) {
            assertTrue(true);
            return;
        } catch (Exception e) {
        }
        assertTrue("Did not throw illegal argument exception for non-existent file", false);
    }

    @Test
    public void testCSVStrategyDefault() throws Exception {
        CSVDataStore datastore = (CSVDataStore) factory.createDataStoreFromFile(file);
        CSVStrategy csvStrategy = datastore.getCSVStrategy();
        assertEquals("Unexpected default csv strategy", CSVAttributesOnlyStrategy.class,
                csvStrategy.getClass());
    }

    @Test
    public void testCSVStrategyGuess() throws Exception {
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("strategy", "guess");
        params.put("file", file);
        CSVDataStore datastore = (CSVDataStore) factory.createDataStore(params);
        CSVStrategy csvStrategy = datastore.getCSVStrategy();
        assertEquals("Unexpected strategy", CSVLatLonStrategy.class, csvStrategy.getClass());
    }

    @Test
    public void testCSVStrategySpecifiedBadParams() throws Exception {
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("strategy", "specify");
        params.put("file", file);
        try {
            factory.createDataStore(params);
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("Expected illegal argument exception for missing latField and lngField");
    }

    @Test
    public void testCSVStrategySpecified() throws Exception {
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("strategy", "specify");
        params.put("file", file);
        params.put("latField", "foo");
        params.put("lngField", "bar");
        CSVDataStore datastore = (CSVDataStore) factory.createDataStore(params);
        CSVStrategy csvStrategy = datastore.getCSVStrategy();
        assertEquals("Unexpected strategy", CSVSpecifiedLatLngStrategy.class,
                csvStrategy.getClass());
    }

    @Test
    public void testCSVStrategyWKTMissingWktField() throws IOException {
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("strategy", "wkt");
        params.put("file", file);
        try {
            factory.createDataStore(params);
        } catch (IllegalArgumentException e) {
            return;
        }
        fail("Expected illegal argument exception for missing wktField");
    }

    @Test
    public void testCSVStrategyWKT() throws IOException {
        Map<String, Serializable> params = new HashMap<String, Serializable>();
        params.put("strategy", "wkt");
        params.put("wktField", "whatever");
        params.put("file", file);
        CSVDataStore datastore = (CSVDataStore) factory.createDataStore(params);
        CSVStrategy csvStrategy = datastore.getCSVStrategy();
        assertEquals("Unexpected strategy", CSVSpecifiedWKTStrategy.class, csvStrategy.getClass());
    }
}
