package org.opengeo.mapmeter.monitor.config;

import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.util.Properties;

import org.geoserver.data.test.MockData;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.password.GeoServerPBEPasswordEncoder;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Test;

import com.google.common.base.Charsets;
import com.google.common.io.Closer;
import com.google.common.io.Files;

public class MapmeterConfigurationTest extends GeoServerTestSupport {

    private File monitoring;

    private GeoServerPBEPasswordEncoder passwordEncoder;

    private GeoServerSecurityManager securityManager;

    private String defaultBaseUrl;

    @Override
    protected void setUpInternal() throws Exception {
        MockData mockData = getTestData();
        File dataDirectoryRoot = mockData.getDataDirectoryRoot();
        monitoring = new File(dataDirectoryRoot, "monitoring");
        if (!monitoring.isDirectory()) {
            assertTrue(monitoring.mkdir());
        }
        passwordEncoder = applicationContext.getBean("pbePasswordEncoder",
                GeoServerPBEPasswordEncoder.class);
        securityManager = applicationContext.getBean(GeoServerSecurityManager.class);
        defaultBaseUrl = "http://example.com";
    }

    private void setMapmeterProperties(Properties properties) throws IOException {
        File legacyProperties = new File(monitoring, "controller.properties");
        legacyProperties.delete();
        File mapmeterProperties = new File(monitoring, "mapmeter.properties");
        Closer closer = Closer.create();
        try {
            BufferedWriter writer = closer.register(Files.newWriter(mapmeterProperties,
                    Charsets.UTF_8));
            properties.store(writer, null);
        } finally {
            closer.close();
        }
    }

    private void setMapmeterLegacyProperties(Properties properties) throws IOException {
        File legacyProperties = new File(monitoring, "controller.properties");
        File mapmeterProperties = new File(monitoring, "mapmeter.properties");
        mapmeterProperties.delete();
        Closer closer = Closer.create();
        try {
            BufferedWriter writer = closer.register(Files.newWriter(legacyProperties,
                    Charsets.UTF_8));
            properties.store(writer, null);
        } finally {
            closer.close();
        }
    }

    private MapmeterConfiguration createMapmeterConfiguration() {
        MapmeterConfiguration mapmeterConfiguration = new MapmeterConfiguration(defaultBaseUrl,
                getResourceLoader(), passwordEncoder, securityManager);
        return mapmeterConfiguration;
    }

    @Test
    public void testDefaultConfiguration() throws IOException {
        MapmeterConfiguration mapmeterConfiguration = createMapmeterConfiguration();
        assertEquals(defaultBaseUrl, mapmeterConfiguration.getBaseUrl());
        assertFalse(mapmeterConfiguration.getApiKey().isPresent());
        assertFalse(mapmeterConfiguration.getIsOnPremise());
    }

    @Test
    public void testApiKeyConfigured() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("apikey", "foo-api-key");
        setMapmeterProperties(properties);

        MapmeterConfiguration mapmeterConfiguration = createMapmeterConfiguration();

        assertEquals(defaultBaseUrl, mapmeterConfiguration.getBaseUrl());
        assertEquals("foo-api-key", mapmeterConfiguration.getApiKey().get());
    }

    @Test
    public void testBaseUrlConfigured() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("apikey", "foo-api-key");
        properties.setProperty("baseurl", "http://localhost:8080");
        setMapmeterProperties(properties);

        MapmeterConfiguration mapmeterConfiguration = createMapmeterConfiguration();

        assertEquals("http://localhost:8080", mapmeterConfiguration.getBaseUrl());
    }

    @Test
    public void testOnPremiseConfiguredOn() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("onpremise", "true");
        setMapmeterProperties(properties);

        MapmeterConfiguration mapmeterConfiguration = createMapmeterConfiguration();

        assertTrue(mapmeterConfiguration.getIsOnPremise());
    }

    @Test
    public void testOnPremiseConfiguredOff() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("onpremise", "false");
        // set a different base url, which can implicitly influence the on premise state
        properties.setProperty("baseurl", "http://localhost:8080");
        setMapmeterProperties(properties);

        MapmeterConfiguration mapmeterConfiguration = createMapmeterConfiguration();

        assertFalse(mapmeterConfiguration.getIsOnPremise());
    }

    @Test
    public void testOnPremiseSetOnDifferentBaseUrl() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("baseurl", "http://localhost:8080");
        setMapmeterProperties(properties);

        MapmeterConfiguration mapmeterConfiguration = createMapmeterConfiguration();

        assertTrue(mapmeterConfiguration.getIsOnPremise());
    }

    @Test
    public void testLegacyConfigMigrated() throws IOException {
        Properties properties = new Properties();
        properties.setProperty("url", "http://localhost:8080/controller/v1/message/check");
        properties.setProperty("apikey", "my-legacy-api-key");
        setMapmeterLegacyProperties(properties);

        MapmeterConfiguration mapmeterConfiguration = createMapmeterConfiguration();

        assertEquals("http://localhost:8080", mapmeterConfiguration.getBaseUrl());
        assertEquals("my-legacy-api-key", mapmeterConfiguration.getApiKey().get());
        assertTrue(mapmeterConfiguration.getIsOnPremise());

        File legacyConfigFile = new File(monitoring, "controller.properties");
        File currentConfigFile = new File(monitoring, "mapmeter.properties");
        assertFalse(legacyConfigFile.isFile());
        assertTrue(currentConfigFile.isFile());
    }

}
