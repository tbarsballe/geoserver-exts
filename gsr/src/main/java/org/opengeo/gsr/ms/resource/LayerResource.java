/* Copyright (c) 2013 - 2014 Boundless - http://boundlessgeo.com All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.opengeo.gsr.ms.resource;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.logging.Logger;
import net.sf.json.util.JSONBuilder;
import org.geoserver.catalog.Catalog;
import org.opengeo.gsr.core.exception.ServiceError;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.resource.OutputRepresentation;
import org.restlet.resource.Representation;
import org.restlet.resource.Resource;
import org.restlet.resource.Variant;

public class LayerResource extends Resource {
    public static final Variant JSON = new Variant(new MediaType("application/json"));
    private final Catalog catalog;
    private static final Logger LOG = org.geotools.util.logging.Logging.getLogger("org.geoserver.global");

    public LayerResource(Context context, Request request, Response response, Catalog catalog) {
        super(context, request, response);
        this.catalog = catalog;
        getVariants().add(JSON);
    }

    public Representation getRepresentation(Variant variant) {
        if (variant == JSON) {
            try {
                return buildJsonRepresentation();
            } catch (IllegalArgumentException e) {
                return buildJsonError(new ServiceError(400, "Invalid arguments from client", Arrays.asList(e.getMessage())));
            } catch (UnsupportedOperationException e) {
                return buildJsonError(new ServiceError(500, "Requested operation is not implemented", Arrays.asList(e.getMessage())));
            } catch (NoSuchElementException e) {
                return buildJsonError(new ServiceError(404, "Requested element not found", Arrays.asList(e.getMessage())));
            } catch (Exception e) {
                List<String> trace = new ArrayList<String>();
                for (StackTraceElement elem : e.getStackTrace()) {
                    trace.add(elem.toString());
                }
                return buildJsonError(new ServiceError(500, "Requested operation is not implemented", trace));
            }
        }
        return super.getRepresentation(variant);
    }

    private Representation buildJsonError(ServiceError error) {
        return null;
    }

    private Representation buildJsonRepresentation() throws IOException {
    	String format = getRequest().getResourceRef().getQueryAsForm().getFirstValue("f");
        if (!"json".equals(format)) throw new IllegalArgumentException("json is the only supported format");
        String workspace = (String) getRequest().getAttributes().get("workspace");
        String layerOrTableId = (String) getRequest().getAttributes().get("layerOrTable");
        Integer layerOrTableIndex = Integer.valueOf(layerOrTableId);

        LayerOrTable layerOrTable = LayersAndTables.find(catalog, workspace, layerOrTableIndex);

        return new JsonLayerRepresentation(layerOrTable);
    }

    private static class JsonLayerRepresentation extends OutputRepresentation {
        private final LayerOrTable layerOrTable;
        public JsonLayerRepresentation(LayerOrTable layerOrTable) {
            super(MediaType.APPLICATION_JAVASCRIPT);
            this.layerOrTable = layerOrTable;
        }
        @Override
        public void write(OutputStream outputStream) throws IOException {
            Writer writer = new OutputStreamWriter(outputStream, "UTF-8");
            JSONBuilder json = new JSONBuilder(writer);
            LayerListResource.encodeLayerOrTable(layerOrTable, json);
            writer.flush();
            writer.close();
        }
    }
}
