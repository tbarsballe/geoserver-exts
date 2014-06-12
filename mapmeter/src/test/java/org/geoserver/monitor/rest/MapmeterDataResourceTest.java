package org.geoserver.monitor.rest;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Map;

import net.sf.json.JSON;
import net.sf.json.JSONObject;

import org.geoserver.test.GeoServerTestSupport;
import org.junit.Test;
import org.opengeo.mapmeter.monitor.saas.MapmeterSaasException;
import org.opengeo.mapmeter.monitor.saas.MapmeterService;
import org.opengeo.mapmeter.monitor.saas.MissingMapmeterApiKeyException;
import org.opengeo.mapmeter.monitor.saas.MissingMapmeterSaasCredentialsException;

import com.google.common.collect.Maps;

public class MapmeterDataResourceTest extends GeoServerTestSupport {

    private static final String mapmeterDataEndpointUrl = "/rest/mapmeter/data.json";

    private MapmeterService mapmeterService;

    @Override
    protected void setUpInternal() throws Exception {
        mapmeterService = mock(MapmeterService.class);
        MapmeterDataResource mapmeterDataResource = applicationContext.getBean(MapmeterDataResource.class);
        mapmeterDataResource.setMapmeterService(mapmeterService);
    }

    @Test
    public void testGetSuccessful() throws Exception {
        Map<String, Object> mockResult = Maps.newHashMap();
        mockResult.put("data", Collections.emptyMap());
        when(mapmeterService.fetchMapmeterData()).thenReturn(mockResult);

        JSON json = getAsJSON(mapmeterDataEndpointUrl);
        JSONObject jsonObject = (JSONObject) json;
        Object dataObj = jsonObject.get("data");
        assertTrue(dataObj instanceof Map);

        verify(mapmeterService).fetchMapmeterData();
    }

    @Test
    public void testGetIOException() throws Exception {
        when(mapmeterService.fetchMapmeterData()).thenThrow(new IOException("boom"));

        JSONObject jsonObject = (JSONObject) getAsJSON(mapmeterDataEndpointUrl);
        String error = (String) jsonObject.get("error");
        assertEquals("boom", error);

        verify(mapmeterService).fetchMapmeterData();
    }

    @Test
    public void testGetMissingApiException() throws Exception {
        when(mapmeterService.fetchMapmeterData()).thenThrow(
                new MissingMapmeterApiKeyException("boom"));

        JSONObject jsonObject = (JSONObject) getAsJSON(mapmeterDataEndpointUrl);
        String error = (String) jsonObject.get("error");
        assertTrue(error.contains("api"));

        verify(mapmeterService).fetchMapmeterData();
    }

    @Test
    public void testGetMissingSaasCredentialsException() throws Exception {
        when(mapmeterService.fetchMapmeterData()).thenThrow(
                new MissingMapmeterSaasCredentialsException("boom"));

        JSONObject jsonObject = (JSONObject) getAsJSON(mapmeterDataEndpointUrl);
        String error = (String) jsonObject.get("error");
        assertTrue(error.contains("credentials"));

        verify(mapmeterService).fetchMapmeterData();
    }

    @Test
    public void testGetMapmeterSaasUnauthorizedException() throws Exception {
        when(mapmeterService.fetchMapmeterData()).thenThrow(
                new MapmeterSaasException(403, Collections.<String, Object> singletonMap("message",
                        "boom"), "boom"));

        JSONObject jsonObject = (JSONObject) getAsJSON(mapmeterDataEndpointUrl);
        String error = (String) jsonObject.get("error");
        String reason = (String) jsonObject.get("reason");
        assertEquals("boom", error);
        assertEquals("serverExpired", reason);

        verify(mapmeterService).fetchMapmeterData();
    }

    @Test
    public void testGetMapmeterSaasOtherException() throws Exception {
        when(mapmeterService.fetchMapmeterData()).thenThrow(
                new MapmeterSaasException(400, Collections.<String, Object> singletonMap("message",
                        "boom"), "boom"));

        JSONObject jsonObject = (JSONObject) getAsJSON(mapmeterDataEndpointUrl);
        String error = (String) jsonObject.get("error");
        assertEquals("boom", error);

        verify(mapmeterService).fetchMapmeterData();
    }

}
