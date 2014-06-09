package org.opengeo.mapmeter.monitor.saas;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Matchers.any;
import static org.mockito.Matchers.anyString;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.util.Collections;
import java.util.Date;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Matchers;
import org.opengeo.mapmeter.monitor.config.MapmeterConfiguration;

import com.google.common.base.Optional;
import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class MapmeterServiceTest {

    private MapmeterSaasService mapmeterSaasService;

    private MapmeterConfiguration mapmeterConfiguration;

    private int daysToFetch;

    private MapmeterService mapmeterService;

    private String baseUrl;

    private String apiKey;

    private MapmeterSaasCredentials mapmeterSaasCredentials;

    private MapmeterSaasCredentials newCredentials;

    @Before
    public void setUp() {
        mapmeterSaasService = mock(MapmeterSaasService.class);
        mapmeterConfiguration = mock(MapmeterConfiguration.class);
        daysToFetch = 7;
        baseUrl = "http://example.com";
        apiKey = "apikey";
        mapmeterSaasCredentials = new MapmeterSaasCredentials("user@example.com", "hunter2");
        newCredentials = new MapmeterSaasCredentials("user2@example.com", "hunter3");
        mapmeterService = new MapmeterService(mapmeterSaasService, mapmeterConfiguration,
                daysToFetch);
    }

    private Map<String, Object> createStubFreeTrialResult() {
        Map<String, Object> userResult = Maps.newHashMap();
        userResult.put("id", "saasid");
        userResult.put("email", "saasusername@example.com");
        userResult.put("password", "hunter2");

        Map<String, Object> orgResult = Maps.newHashMap();
        orgResult.put("name", "orgname");

        Map<String, Object> serverResult = Maps.newHashMap();
        serverResult.put("apiKey", "apikey");

        Map<String, Object> result = Maps.newHashMap();
        result.put("user", userResult);
        result.put("organization", orgResult);
        result.put("server", serverResult);

        return result;
    }

    @Test
    public void testStartFreeTrialSuccessful() throws IOException, MapmeterSaasException {
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.<String> absent());
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);

        Map<String, Object> result = createStubFreeTrialResult();
        MapmeterSaasResponse mapmeterSaasResponse = new MapmeterSaasResponse(200, result);

        when(mapmeterSaasService.createAnonymousTrial(anyString())).thenReturn(mapmeterSaasResponse);
        MapmeterEnableResult freeTrialResult = mapmeterService.startFreeTrial();

        assertEquals("apikey", freeTrialResult.getServerApiKey());
        assertEquals("saasusername@example.com", freeTrialResult.getUsername());
        assertEquals("hunter2", freeTrialResult.getPassword());
        assertEquals("saasid", freeTrialResult.getExternalUserId());
        assertEquals("orgname", freeTrialResult.getOrgName());

        verify(mapmeterConfiguration).setApiKey("apikey");
        verify(mapmeterConfiguration).setMapmeterSaasCredentials(
                new MapmeterSaasCredentials("saasusername@example.com", "hunter2"));
        verify(mapmeterConfiguration).save();
        verify(mapmeterSaasService).createAnonymousTrial(anyString());
    }

    @Test
    public void testStartFreeTrialUnexpectedResponse() throws IOException, MapmeterSaasException {
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.<String> absent());
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);

        Map<String, Object> result = createStubFreeTrialResult();
        // simulate unexpected response by removing the user data
        result.remove("user");

        MapmeterSaasResponse mapmeterSaasResponse = new MapmeterSaasResponse(200, result);
        when(mapmeterSaasService.createAnonymousTrial(anyString())).thenReturn(mapmeterSaasResponse);

        try {
            mapmeterService.startFreeTrial();
            fail("Expected an unexpected response");
        } catch (MapmeterSaasException e) {
            assertEquals("Unexpected mapmeter saas response", e.getMessage());
            assertEquals("Unexpected json response from mapmeter saas", e.getErrorMessage().get());
        }

        verify(mapmeterSaasService).createAnonymousTrial(anyString());
    }

    @Test
    public void testStartFreeTrialIOException() throws IOException, MapmeterSaasException {
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.<String> absent());
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);

        when(mapmeterSaasService.createAnonymousTrial(anyString())).thenThrow(
                new IOException("boom"));

        try {
            mapmeterService.startFreeTrial();
            fail("Expected an IO exception to be thrown");
        } catch (IOException e) {
            assertEquals("boom", e.getMessage());
        }

        verify(mapmeterSaasService).createAnonymousTrial(anyString());
    }

    @Test
    public void testStartFreeTrialSaasException() throws IOException {
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.<String> absent());
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);

        Map<String, Object> result = createStubFreeTrialResult();
        MapmeterSaasResponse errorSaasResponse = new MapmeterSaasResponse(400, result);
        when(mapmeterSaasService.createAnonymousTrial(anyString())).thenReturn(errorSaasResponse);

        try {
            mapmeterService.startFreeTrial();
            fail("Expected mapmeter saas exception to be thrown");
        } catch (MapmeterSaasException e) {
            assertEquals(400, e.getStatusCode());
        }
        verify(mapmeterSaasService).createAnonymousTrial(anyString());
    }

    @Test
    public void testStartFreeTrialIllegalState() throws MapmeterSaasException, IOException {
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.of(apiKey));

        try {
            mapmeterService.startFreeTrial();
            fail("Expected illegal state exception to be thrown");
        } catch (IllegalStateException e) {
        }
        verify(mapmeterSaasService, times(0)).createAnonymousTrial(anyString());
    }

    private MapmeterSaasResponse createStubFetchDataResult(int statusCode) {
        MapmeterSaasResponse mapmeterSaasResponse = new MapmeterSaasResponse(statusCode,
                Collections.<String, Object> singletonMap("data", "data"));
        return mapmeterSaasResponse;
    }

    private MapmeterSaasResponse createStubFetchDataResult() {
        return createStubFetchDataResult(200);
    }

    @Test
    public void testFetchMapmeterDataSuccessful() throws IOException,
            MissingMapmeterApiKeyException, MissingMapmeterSaasCredentialsException,
            MapmeterSaasException {
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.of(apiKey));
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getMapmeterSaasCredentials()).thenReturn(
                Optional.of(mapmeterSaasCredentials));
        when(mapmeterConfiguration.getIsOnPremise()).thenReturn(false);

        when(
                mapmeterSaasService.fetchData(anyString(),
                        Matchers.<Optional<MapmeterSaasCredentials>> any(), anyString(),
                        any(Date.class), any(Date.class))).thenReturn(createStubFetchDataResult());

        Map<String, Object> result = mapmeterService.fetchMapmeterData();
        assertEquals("data", result.get("data"));
        verify(mapmeterSaasService).fetchData(eq(baseUrl),
                eq(Optional.of(mapmeterSaasCredentials)), eq(apiKey), any(Date.class),
                any(Date.class));
    }

    @Test
    public void testFetchMapmeterDataMissingApiKey() throws IOException,
            MissingMapmeterSaasCredentialsException, MapmeterSaasException {
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.<String> absent());

        try {
            mapmeterService.fetchMapmeterData();
            fail("Expected missing mapmeter api exception to be thrown");
        } catch (MissingMapmeterApiKeyException e) {
        }
        verify(mapmeterConfiguration).getApiKey();
    }

    @Test
    public void testFetchMapmeterDataMissingCredentials() throws IOException,
            MissingMapmeterApiKeyException, MapmeterSaasException {
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.of(apiKey));
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getMapmeterSaasCredentials()).thenReturn(
                Optional.<MapmeterSaasCredentials> absent());
        when(mapmeterConfiguration.getIsOnPremise()).thenReturn(false);

        try {
            mapmeterService.fetchMapmeterData();
            fail("Expected missing credentials exception to be thrown");
        } catch (MissingMapmeterSaasCredentialsException e) {
        }
    }

    @Test
    public void testFetchMapmeterDataIOException() throws IOException,
            MissingMapmeterApiKeyException, MissingMapmeterSaasCredentialsException,
            MapmeterSaasException {
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.of(apiKey));
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getMapmeterSaasCredentials()).thenReturn(
                Optional.of(mapmeterSaasCredentials));
        when(mapmeterConfiguration.getIsOnPremise()).thenReturn(false);

        when(
                mapmeterSaasService.fetchData(anyString(),
                        Matchers.<Optional<MapmeterSaasCredentials>> any(), anyString(),
                        any(Date.class), any(Date.class))).thenThrow(new IOException("boom"));

        try {
            mapmeterService.fetchMapmeterData();
            fail("Expected IO exception to be thrown");
        } catch (IOException e) {
            assertEquals("boom", e.getMessage());
        }
    }

    @Test
    public void testFetchMapmeterDataSaasException() throws IOException,
            MissingMapmeterApiKeyException, MissingMapmeterSaasCredentialsException {
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.of(apiKey));
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getMapmeterSaasCredentials()).thenReturn(
                Optional.of(mapmeterSaasCredentials));
        when(mapmeterConfiguration.getIsOnPremise()).thenReturn(false);

        MapmeterSaasResponse fetchResult = createStubFetchDataResult(400);

        when(
                mapmeterSaasService.fetchData(anyString(),
                        Matchers.<Optional<MapmeterSaasCredentials>> any(), anyString(),
                        any(Date.class), any(Date.class))).thenReturn(fetchResult);

        try {
            mapmeterService.fetchMapmeterData();
            fail("Expected mapmeter saas exception to be thrown");
        } catch (MapmeterSaasException e) {
            assertEquals(400, e.getStatusCode());
        }
    }

    @Test
    public void testFetchMapmeterDataSaasSuccessfulOnPremise() throws IOException,
            MissingMapmeterApiKeyException, MissingMapmeterSaasCredentialsException,
            MapmeterSaasException {
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.of(apiKey));
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getMapmeterSaasCredentials()).thenReturn(
                Optional.<MapmeterSaasCredentials> absent());
        when(mapmeterConfiguration.getIsOnPremise()).thenReturn(true);

        MapmeterSaasResponse fetchResult = createStubFetchDataResult();

        when(
                mapmeterSaasService.fetchData(anyString(),
                        Matchers.<Optional<MapmeterSaasCredentials>> any(), anyString(),
                        any(Date.class), any(Date.class))).thenReturn(fetchResult);

        Map<String, Object> result = mapmeterService.fetchMapmeterData();
        assertEquals("data", result.get("data"));
        verify(mapmeterSaasService).fetchData(eq(baseUrl),
                eq(Optional.<MapmeterSaasCredentials> absent()), eq(apiKey), any(Date.class),
                any(Date.class));
    }

    private MapmeterSaasResponse createLookupUserResponse(int statusCode) {
        Map<String, Object> result = Collections.<String, Object> singletonMap("id", "1234");
        return new MapmeterSaasResponse(statusCode, result);
    }

    private MapmeterSaasResponse createLookupUserResponse() {
        return createLookupUserResponse(200);
    }

    private MapmeterSaasResponse createConvertCredentialsResponse(int statusCode) {
        return new MapmeterSaasResponse(statusCode, Collections.<String, Object> emptyMap());
    }

    private MapmeterSaasResponse createConvertCredentialsResponse() {
        return createConvertCredentialsResponse(200);
    }

    @Test
    public void testConvertCredentialsSuccessful() throws IOException, MapmeterSaasException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getMapmeterSaasCredentials()).thenReturn(
                Optional.of(mapmeterSaasCredentials));

        MapmeterSaasResponse lookupUserResponse = createLookupUserResponse();
        MapmeterSaasResponse convertCredentialsResponse = createConvertCredentialsResponse();

        when(mapmeterSaasService.lookupUser(anyString(), any(MapmeterSaasCredentials.class))).thenReturn(
                lookupUserResponse);
        when(
                mapmeterSaasService.convertCredentials(anyString(), anyString(),
                        any(MapmeterSaasCredentials.class), any(MapmeterSaasCredentials.class))).thenReturn(
                convertCredentialsResponse);

        mapmeterService.convertMapmeterCredentials(newCredentials);

        verify(mapmeterSaasService).lookupUser(baseUrl, mapmeterSaasCredentials);
        verify(mapmeterSaasService).convertCredentials(baseUrl, "1234", mapmeterSaasCredentials,
                newCredentials);
        verify(mapmeterConfiguration).setMapmeterSaasCredentials(newCredentials);
        verify(mapmeterConfiguration).save();
    }

    @Test
    public void testConvertCredentialsNoExistingCredentials() throws IOException,
            MapmeterSaasException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getMapmeterSaasCredentials()).thenReturn(
                Optional.<MapmeterSaasCredentials> absent());

        try {
            mapmeterService.convertMapmeterCredentials(newCredentials);
            fail("Expected illegal state exception to be thrown");
        } catch (IllegalStateException e) {
        }
        verify(mapmeterConfiguration).getBaseUrl();
        verify(mapmeterConfiguration).getMapmeterSaasCredentials();
    }

    @Test
    public void testConvertCredentialsLookupFailure() throws IOException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getMapmeterSaasCredentials()).thenReturn(
                Optional.of(mapmeterSaasCredentials));

        MapmeterSaasResponse lookupUserResponse = createLookupUserResponse(400);
        when(mapmeterSaasService.lookupUser(baseUrl, mapmeterSaasCredentials)).thenReturn(
                lookupUserResponse);

        try {
            mapmeterService.convertMapmeterCredentials(newCredentials);
            fail("Expected saas exception to be thrown");
        } catch (MapmeterSaasException e) {
            assertEquals(400, e.getStatusCode());
        }
    }

    @Test
    public void testConvertCredentialsConvertFailure() throws IOException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getMapmeterSaasCredentials()).thenReturn(
                Optional.of(mapmeterSaasCredentials));

        MapmeterSaasResponse lookupUserResponse = createLookupUserResponse();
        MapmeterSaasResponse convertCredentialsResponse = createConvertCredentialsResponse(400);
        when(mapmeterSaasService.lookupUser(baseUrl, mapmeterSaasCredentials)).thenReturn(
                lookupUserResponse);
        when(
                mapmeterSaasService.convertCredentials(baseUrl, "1234", mapmeterSaasCredentials,
                        newCredentials)).thenReturn(convertCredentialsResponse);

        try {
            mapmeterService.convertMapmeterCredentials(newCredentials);
            fail("Expected saas exception to be thrown");
        } catch (MapmeterSaasException e) {
            assertEquals(400, e.getStatusCode());
        }
        verify(mapmeterSaasService).lookupUser(baseUrl, mapmeterSaasCredentials);
    }

    @Test
    public void testConvertCredentialsIOException() throws MapmeterSaasException, IOException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getMapmeterSaasCredentials()).thenReturn(
                Optional.of(mapmeterSaasCredentials));
        when(mapmeterSaasService.lookupUser(baseUrl, mapmeterSaasCredentials)).thenThrow(
                new IOException("boom"));

        try {
            mapmeterService.convertMapmeterCredentials(newCredentials);
            fail("Expected IO exception to be thrown");
        } catch (IOException e) {
            assertEquals("boom", e.getMessage());
        }
    }

    private MapmeterSaasResponse createFindUserStateResponse(int statusCode,
            boolean isAnonymousSignup, boolean isConverted) {
        Map<String, Object> result = Maps.newHashMap();
        if (isAnonymousSignup) {
            result.put("isAnonymousSignup", isAnonymousSignup);
        }
        if (isConverted) {
            result.put("isAnonymousCredentialsConverted", isConverted);
        }
        return new MapmeterSaasResponse(statusCode, result);
    }

    private MapmeterSaasResponse createFindUserStateResponse(boolean isAnonymousSignup,
            boolean isConverted) {
        return createFindUserStateResponse(200, isAnonymousSignup, isConverted);
    }

    @Test
    public void testFindUserStateSuccessfulAnonymous() throws IOException, MapmeterSaasException,
            MissingMapmeterSaasCredentialsException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getMapmeterSaasCredentials()).thenReturn(
                Optional.of(mapmeterSaasCredentials));
        MapmeterSaasResponse userStateResponse = createFindUserStateResponse(true, false);
        when(mapmeterSaasService.lookupUser(anyString(), any(MapmeterSaasCredentials.class))).thenReturn(
                userStateResponse);

        MapmeterSaasUserState userState = mapmeterService.findUserState();

        assertEquals(MapmeterSaasUserState.ANONYMOUS, userState);
        verify(mapmeterConfiguration).getBaseUrl();
        verify(mapmeterConfiguration).getMapmeterSaasCredentials();
        verify(mapmeterSaasService).lookupUser(baseUrl, mapmeterSaasCredentials);
    }

    @Test
    public void testFindUserStateSuccessfulConverted() throws IOException, MapmeterSaasException,
            MissingMapmeterSaasCredentialsException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getMapmeterSaasCredentials()).thenReturn(
                Optional.of(mapmeterSaasCredentials));
        MapmeterSaasResponse userStateResponse = createFindUserStateResponse(true, true);
        when(mapmeterSaasService.lookupUser(anyString(), any(MapmeterSaasCredentials.class))).thenReturn(
                userStateResponse);

        MapmeterSaasUserState userState = mapmeterService.findUserState();

        assertEquals(MapmeterSaasUserState.CONVERTED, userState);
        verify(mapmeterConfiguration).getBaseUrl();
        verify(mapmeterConfiguration).getMapmeterSaasCredentials();
        verify(mapmeterSaasService).lookupUser(baseUrl, mapmeterSaasCredentials);
    }

    @Test
    public void testFindUserStateSuccessfulNormal() throws IOException, MapmeterSaasException,
            MissingMapmeterSaasCredentialsException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getMapmeterSaasCredentials()).thenReturn(
                Optional.of(mapmeterSaasCredentials));
        MapmeterSaasResponse userStateResponse = createFindUserStateResponse(false, false);
        when(mapmeterSaasService.lookupUser(anyString(), any(MapmeterSaasCredentials.class))).thenReturn(
                userStateResponse);

        MapmeterSaasUserState userState = mapmeterService.findUserState();

        assertEquals(MapmeterSaasUserState.NORMAL, userState);
        verify(mapmeterConfiguration).getBaseUrl();
        verify(mapmeterConfiguration).getMapmeterSaasCredentials();
        verify(mapmeterSaasService).lookupUser(baseUrl, mapmeterSaasCredentials);
    }

    @Test
    public void testFindUserStateSaasError() throws IOException, MapmeterSaasException,
            MissingMapmeterSaasCredentialsException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getMapmeterSaasCredentials()).thenReturn(
                Optional.of(mapmeterSaasCredentials));
        MapmeterSaasResponse userStateResponse = createFindUserStateResponse(400, false, false);
        when(mapmeterSaasService.lookupUser(anyString(), any(MapmeterSaasCredentials.class))).thenReturn(
                userStateResponse);

        try {
            mapmeterService.findUserState();
            fail("Expected mapmeter saas exception to be thrown");
        } catch (MapmeterSaasException e) {
            assertEquals(400, e.getStatusCode());
        }

        verify(mapmeterConfiguration).getBaseUrl();
        verify(mapmeterConfiguration).getMapmeterSaasCredentials();
        verify(mapmeterSaasService).lookupUser(baseUrl, mapmeterSaasCredentials);
    }

    @Test
    public void testFindUserStateMissingCredentials() throws IOException, MapmeterSaasException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getMapmeterSaasCredentials()).thenReturn(
                Optional.<MapmeterSaasCredentials> absent());

        try {
            mapmeterService.findUserState();
            fail("Expected missing saas credentials to be thrown");
        } catch (MissingMapmeterSaasCredentialsException e) {
        }
        verify(mapmeterConfiguration).getBaseUrl();
        verify(mapmeterConfiguration).getMapmeterSaasCredentials();
        verify(mapmeterSaasService, times(0)).lookupUser(anyString(),
                any(MapmeterSaasCredentials.class));
    }

    @Test
    public void testFindUserStateIOException() throws IOException, MapmeterSaasException,
            MissingMapmeterSaasCredentialsException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getMapmeterSaasCredentials()).thenReturn(
                Optional.of(mapmeterSaasCredentials));
        when(mapmeterSaasService.lookupUser(anyString(), any(MapmeterSaasCredentials.class))).thenThrow(
                new IOException("boom"));

        try {
            mapmeterService.findUserState();
            fail("Expected IO exception to be thrown");
        } catch (IOException e) {
        }

        verify(mapmeterConfiguration).getBaseUrl();
        verify(mapmeterConfiguration).getMapmeterSaasCredentials();
        verify(mapmeterSaasService).lookupUser(baseUrl, mapmeterSaasCredentials);
    }

    @Test
    public void testMapmeterStorageCheckSuccessful() throws IOException, MapmeterSaasException,
            MissingMapmeterApiKeyException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.of(apiKey));

        MapmeterSaasResponse saasResponse = new MapmeterSaasResponse(200,
                Collections.<String, Object> singletonMap("status", "OK"));
        when(mapmeterSaasService.checkMapmeterMessageStorage(baseUrl, apiKey)).thenReturn(
                saasResponse);

        MapmeterMessageStorageResult storageResult = mapmeterService.checkMapmeterMessageStorage();

        assertTrue(storageResult.isValidApiKey());
        assertFalse(storageResult.isError());
        verify(mapmeterConfiguration).getBaseUrl();
        verify(mapmeterConfiguration).getApiKey();
        verify(mapmeterSaasService).checkMapmeterMessageStorage(baseUrl, apiKey);
    }

    @Test
    public void testMapmeterStorageCheckInvalid() throws IOException, MapmeterSaasException,
            MissingMapmeterApiKeyException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.of(apiKey));

        Map<String, Object> response = ImmutableMap.<String, Object> of("status", "ERROR",
                "api_key_status", "INVALID");
        MapmeterSaasResponse saasResponse = new MapmeterSaasResponse(200, response);

        when(mapmeterSaasService.checkMapmeterMessageStorage(baseUrl, apiKey)).thenReturn(
                saasResponse);

        MapmeterMessageStorageResult storageResult = mapmeterService.checkMapmeterMessageStorage();

        assertFalse(storageResult.isValidApiKey());
        assertFalse(storageResult.isError());
        verify(mapmeterConfiguration).getBaseUrl();
        verify(mapmeterConfiguration).getApiKey();
        verify(mapmeterSaasService).checkMapmeterMessageStorage(baseUrl, apiKey);
    }

    @Test
    public void testMapmeterStorageCheckSaasError() throws IOException, MapmeterSaasException,
            MissingMapmeterApiKeyException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.of(apiKey));

        Map<String, Object> response = ImmutableMap.<String, Object> of("status", "ERROR", "error",
                "boom");
        MapmeterSaasResponse saasResponse = new MapmeterSaasResponse(200, response);

        when(mapmeterSaasService.checkMapmeterMessageStorage(baseUrl, apiKey)).thenReturn(
                saasResponse);

        MapmeterMessageStorageResult storageResult = mapmeterService.checkMapmeterMessageStorage();

        assertFalse(storageResult.isValidApiKey());
        assertTrue(storageResult.isError());
        assertEquals("boom", storageResult.getError());
        verify(mapmeterConfiguration).getBaseUrl();
        verify(mapmeterConfiguration).getApiKey();
        verify(mapmeterSaasService).checkMapmeterMessageStorage(baseUrl, apiKey);
    }

    @Test
    public void testMapmeterStorageCheckSaasException() throws IOException, MapmeterSaasException,
            MissingMapmeterApiKeyException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.of(apiKey));

        Map<String, Object> response = ImmutableMap.<String, Object> of("status", "ERROR", "error",
                "boom");
        MapmeterSaasResponse saasResponse = new MapmeterSaasResponse(500, response);

        when(mapmeterSaasService.checkMapmeterMessageStorage(baseUrl, apiKey)).thenReturn(
                saasResponse);

        try {
            mapmeterService.checkMapmeterMessageStorage();
            fail("Expected mapmeter saas exception to be thrown");
        } catch (MapmeterSaasException e) {
            assertEquals(500, e.getStatusCode());
        }

        verify(mapmeterConfiguration).getBaseUrl();
        verify(mapmeterConfiguration).getApiKey();
        verify(mapmeterSaasService).checkMapmeterMessageStorage(baseUrl, apiKey);
    }

    @Test
    public void testMapmeterStorageCheckUnknownSaasError() throws IOException,
            MapmeterSaasException, MissingMapmeterApiKeyException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.of(apiKey));

        Map<String, Object> response = ImmutableMap.<String, Object> of("status",
                "unknown-status-type");
        MapmeterSaasResponse saasResponse = new MapmeterSaasResponse(200, response);

        when(mapmeterSaasService.checkMapmeterMessageStorage(baseUrl, apiKey)).thenReturn(
                saasResponse);

        try {
            mapmeterService.checkMapmeterMessageStorage();
            fail("Expected mapmeter saas exception to be thrown");
        } catch (MapmeterSaasException e) {
            assertEquals(200, e.getStatusCode());
        }

        verify(mapmeterConfiguration).getBaseUrl();
        verify(mapmeterConfiguration).getApiKey();
        verify(mapmeterSaasService).checkMapmeterMessageStorage(baseUrl, apiKey);
    }

    @Test
    public void testMapmeterStorageCheckUnknownSaasApiKeyStatus() throws IOException,
            MapmeterSaasException, MissingMapmeterApiKeyException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.of(apiKey));

        Map<String, Object> response = ImmutableMap.<String, Object> of("status", "ERROR",
                "api_key_status", "unknown-api-key-status");
        MapmeterSaasResponse saasResponse = new MapmeterSaasResponse(200, response);

        when(mapmeterSaasService.checkMapmeterMessageStorage(baseUrl, apiKey)).thenReturn(
                saasResponse);

        try {
            mapmeterService.checkMapmeterMessageStorage();
            fail("Expected mapmeter saas exception to be thrown");
        } catch (MapmeterSaasException e) {
            assertEquals(200, e.getStatusCode());
        }

        verify(mapmeterConfiguration).getBaseUrl();
        verify(mapmeterConfiguration).getApiKey();
        verify(mapmeterSaasService).checkMapmeterMessageStorage(baseUrl, apiKey);
    }

    @Test
    public void testMapmeterStorageCheckInvalidResponse() throws IOException,
            MapmeterSaasException, MissingMapmeterApiKeyException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.of(apiKey));

        Map<String, Object> response = ImmutableMap.<String, Object> of("status", 7,
                "api_key_status", 14);
        MapmeterSaasResponse saasResponse = new MapmeterSaasResponse(200, response);

        when(mapmeterSaasService.checkMapmeterMessageStorage(baseUrl, apiKey)).thenReturn(
                saasResponse);

        try {
            mapmeterService.checkMapmeterMessageStorage();
            fail("Expected mapmeter saas exception to be thrown");
        } catch (MapmeterSaasException e) {
            assertEquals(200, e.getStatusCode());
        }

        verify(mapmeterConfiguration).getBaseUrl();
        verify(mapmeterConfiguration).getApiKey();
        verify(mapmeterSaasService).checkMapmeterMessageStorage(baseUrl, apiKey);
    }

    @Test
    public void testMapmeterStorageCheckIOException() throws IOException, MapmeterSaasException,
            MissingMapmeterApiKeyException {
        when(mapmeterConfiguration.getBaseUrl()).thenReturn(baseUrl);
        when(mapmeterConfiguration.getApiKey()).thenReturn(Optional.of(apiKey));

        when(mapmeterSaasService.checkMapmeterMessageStorage(baseUrl, apiKey)).thenThrow(
                new IOException("boom"));

        try {
            mapmeterService.checkMapmeterMessageStorage();
            fail("Expected mapmeter saas exception to be thrown");
        } catch (IOException e) {
            assertEquals("boom", e.getMessage());
        }

        verify(mapmeterConfiguration).getBaseUrl();
        verify(mapmeterConfiguration).getApiKey();
        verify(mapmeterSaasService).checkMapmeterMessageStorage(baseUrl, apiKey);
    }

}
