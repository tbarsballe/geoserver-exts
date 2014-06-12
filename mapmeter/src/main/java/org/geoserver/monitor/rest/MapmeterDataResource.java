package org.geoserver.monitor.rest;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.format.DataFormat;
import org.geotools.util.logging.Logging;
import org.opengeo.mapmeter.monitor.saas.MapmeterSaasException;
import org.opengeo.mapmeter.monitor.saas.MapmeterService;
import org.opengeo.mapmeter.monitor.saas.MissingMapmeterApiKeyException;
import org.opengeo.mapmeter.monitor.saas.MissingMapmeterSaasCredentialsException;
import org.restlet.data.Request;
import org.restlet.data.Response;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.Maps;

public class MapmeterDataResource extends AbstractResource {

    private static final Logger LOGGER = Logging.getLogger(MapmeterDataResource.class);

    private MapmeterService mapmeterService;

    public MapmeterDataResource(MapmeterService mapmeterService) {
        this.mapmeterService = mapmeterService;
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        return Collections.<DataFormat> singletonList(new BooleanPatchedMapJSONFormat());
    }

    @Override
    public boolean allowGet() {
        return true;
    }

    @Override
    public void handleGet() {
        DataFormat format = getFormatGet();
        getResponse().setEntity(format.toRepresentation(fetchResponse()));
    }

    public Map<String, Object> fetchResponse() {
        try {
            Map<String, Object> result = mapmeterService.fetchMapmeterData();
            return result;
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, e.getMessage(), e);
            return Collections.<String, Object> singletonMap("error", e.getLocalizedMessage());
        } catch (MissingMapmeterApiKeyException e) {
            String errMsg = "No mapmeter api key configured, cannot fetch mapmeter data";
            LOGGER.log(Level.INFO, errMsg);
            return ImmutableMap.<String, Object> of("error", errMsg, "reason", "missingApiKey");
        } catch (MissingMapmeterSaasCredentialsException e) {
            String errMsg = "No mapmeter saas credentials configured, cannot fetch mapmeter data";
            LOGGER.log(Level.INFO, errMsg);
            return ImmutableMap.<String, Object> of("error", errMsg, "reason", "missingCredentials");
        } catch (MapmeterSaasException e) {
            LOGGER.log(Level.SEVERE, "Failure fetching mapmeter data", e);
            Map<String, Object> result = Maps.newHashMap();
            result.put("error", e.getMessage());

            Map<String, Object> response = e.getResponse();
            int statusCode = e.getStatusCode();

            // if we have a reason from mapmeter, use that
            Object reasonObj = response.get("reason");
            if (reasonObj instanceof String) {
                String reason = (String) reasonObj;
                result.put("reason", reason);
                return result;
            }

            // default reasons based on status code
            if (statusCode == 403) {
                result.put("reason", "serverExpired");
            } else if (statusCode == 401) {
                result.put("reason", "missingCredentials");
            } else if (statusCode == 400) {
                result.put("reason", "invalidApiKey");
            }

            return result;
        }
    }

    void setMapmeterService(MapmeterService mapmeterService) {
        // for tests
        this.mapmeterService = mapmeterService;
    }

}
