package org.geoserver.monitor.rest;

import java.io.BufferedWriter;
import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.Properties;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.geoserver.data.test.MockData;
import org.geoserver.test.GeoServerTestSupport;
import org.junit.Test;
import org.opengeo.mapmeter.monitor.config.MapmeterConfiguration;

import com.google.common.base.Charsets;
import com.google.common.io.CharStreams;
import com.google.common.io.Closer;
import com.google.common.io.Files;
import com.mockrunner.mock.web.MockHttpServletResponse;

public class MapmeterConfigurationResourceTest extends GeoServerTestSupport {

    private static final String mapmeterConfigurationEndpointUrl = "/rest/mapmeter/configuration.json";

    private MapmeterConfiguration mapmeterConfiguration;

    @Override
    protected void setUpInternal() throws Exception {
        mapmeterConfiguration = applicationContext.getBean(MapmeterConfiguration.class);
    }

    @Test
    public void testGetRequest() throws Exception {
        mapmeterConfiguration.clearConfig();

        JSON json = getAsJSON(mapmeterConfigurationEndpointUrl);
        JSONObject jsonObject = (JSONObject) json;
        Object apiKeyObj = jsonObject.get("apikey");
        Object onPremiseObj = jsonObject.get("onpremise");

        JSONObject apiKey = (JSONObject) apiKeyObj;
        assertTrue(apiKey.isNullObject());
        assertEquals(Boolean.FALSE, onPremiseObj);
    }

    @Test
    public void testGetRequestWithConfigurationSet() throws Exception {
        mapmeterConfiguration.clearConfig();
        mapmeterConfiguration.setApiKey("apikey-foo");
        mapmeterConfiguration.setBaseUrl("http://example.com");
        mapmeterConfiguration.setIsOnPremise(true);

        JSON json = getAsJSON(mapmeterConfigurationEndpointUrl);
        JSONObject jsonObject = (JSONObject) json;
        Object apiKeyObj = jsonObject.get("apikey");
        Object onPremiseObj = jsonObject.get("onpremise");
        Object baseUrlObj = jsonObject.get("baseurl");

        assertEquals("apikey-foo", apiKeyObj);
        assertEquals("http://example.com", baseUrlObj);
        assertEquals(Boolean.TRUE, onPremiseObj);
    }

    @Test
    public void testPutRequest() throws Exception {
        mapmeterConfiguration.clearConfig();

        InputStream result = put(mapmeterConfigurationEndpointUrl,
                "{\"apikey\": \"foo-key\", \"onpremise\": true}", "application/json");
        Closer closer = Closer.create();
        String json;
        try {
            InputStreamReader inputStreamReader = closer.register(new InputStreamReader(result,
                    Charsets.UTF_8));
            json = CharStreams.toString(inputStreamReader);
        } finally {
            closer.close();
        }
        JSONObject jsonObject = JSONObject.fromObject(json);
        Object apiKeyObj = jsonObject.get("apikey");
        Object onPremiseObj = jsonObject.get("onpremise");

        assertEquals("foo-key", apiKeyObj);
        assertEquals(Boolean.TRUE, onPremiseObj);

        assertEquals("foo-key", mapmeterConfiguration.getApiKey().get());
        assertTrue(mapmeterConfiguration.getIsOnPremise());
    }

    @Test
    public void testDeleteRequest() throws Exception {
        mapmeterConfiguration.clearConfig();
        mapmeterConfiguration.setApiKey("foo-apikey");

        MockHttpServletResponse deleteAsServletResponse = deleteAsServletResponse(mapmeterConfigurationEndpointUrl);
        assertEquals(200, deleteAsServletResponse.getStatusCode());

        assertFalse(mapmeterConfiguration.getApiKey().isPresent());
    }

    @Test
    public void testPostRequest() throws Exception {
        mapmeterConfiguration.clearConfig();

        MockData mockData = getTestData();
        File dataDirectoryRoot = mockData.getDataDirectoryRoot();
        File monitoring = new File(dataDirectoryRoot, "monitoring");
        File mapmeterProperties = new File(monitoring, "mapmeter.properties");

        Properties properties = new Properties();
        properties.put("apikey", "post-reload-apikey");

        Closer closer = Closer.create();
        try {
            BufferedWriter writer = closer.register(Files.newWriter(mapmeterProperties,
                    Charsets.UTF_8));
            properties.store(writer, null);
        } finally {
            closer.close();
        }

        closer = Closer.create();
        try {
            InputStream result = post(mapmeterConfigurationEndpointUrl, "", "application/json");
            InputStreamReader reader = closer.register(new InputStreamReader(result, Charsets.UTF_8));
            CharStreams.toString(reader);
        } finally {
            closer.close();
        }

        assertEquals("post-reload-apikey", mapmeterConfiguration.getApiKey().get());
    }

}
