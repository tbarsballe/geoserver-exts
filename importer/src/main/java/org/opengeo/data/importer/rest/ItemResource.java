package org.opengeo.data.importer.rest;

import org.opengeo.data.importer.ValidationException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.rest.AbstractResource;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.json.JSONException;
import org.json.JSONObject;
import org.opengeo.data.importer.ImportContext;
import org.opengeo.data.importer.ImportItem;
import org.opengeo.data.importer.ImportTask;
import org.opengeo.data.importer.Importer;
import org.opengeo.data.importer.transform.TransformChain;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.ext.json.JsonRepresentation;
import org.restlet.resource.Representation;

/**
 * REST resource for /imports/<import>/tasks/<task>/items[/<id>]
 * 
 * @author Justin Deoliveira, OpenGeo
 *
 */
public class ItemResource extends BaseResource {

    public ItemResource(Importer importer) {
        super(importer);
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        return (List) Arrays.asList(new ImportItemJSONFormat());
    }

    @Override
    public void handleGet() {
        if (getRequest().getResourceRef().getLastSegment().equals("progress")) {
            getResponse().setEntity(createProgressRepresentation());
        } else {
            getResponse().setEntity(getFormatGet().toRepresentation(lookupItem(true)));
        }
    }
    
    private Representation createProgressRepresentation() {
        JSONObject progress = new JSONObject();
        long imprt = Long.parseLong(getAttribute("import"));
        ImportItem inProgress = importer.getCurrentlyProcessingItem(imprt);
        try {
            if (inProgress != null) {
                progress.put("progress", inProgress.getNumberProcessed());
                progress.put("total", inProgress.getTotalToProcess());
                progress.put("state", inProgress.getState().toString());
            } else {
                ImportItem item = (ImportItem) lookupItem(false);
                progress.put("state", item.getState().toString());
                if (item.getState() == ImportItem.State.ERROR) {
                    if (item.getError() != null) {
                        progress.put("message", item.getError().getMessage());
                    }
                }
            }
        } catch (JSONException jex) {
            throw new RestletException("Internal Error", Status.SERVER_ERROR_INTERNAL, jex);
        }
        return new JsonRepresentation(progress);
    }

    @Override
    public boolean allowPut() {
        return getAttribute("item") != null;
    }

    @Override
    public void handlePut() {
        ImportItem orig = (ImportItem) lookupItem(false);
        ImportItem item;
        try {
            item = (ImportItem) getFormatPostOrPut().toObject(getRequest().getEntity());
        } catch (ValidationException ve) {
            getLogger().log(Level.WARNING, null, ve);
            throw ImportJSONIO.badRequest(ve.getMessage());
        }

        //now handled by ItemLayerResource, but handle here for backwards compatability
        ItemLayerResource.updateLayer(orig, item.getLayer(), importer);

        if (item.getUpdateMode() != null) {
            orig.setUpdateMode(item.getUpdateMode());
        }

        TransformChain chain = item.getTransform();
        if (chain != null) {
            orig.setTransform(chain);
        }

        //notify the importer that the item has changed
        importer.changed(orig);
        
        getResponse().setStatus(Status.SUCCESS_ACCEPTED);
    }

    public boolean allowDelete() {
        return getAttribute("item") != null;
    }

    public void handleDelete() {
        ImportItem item = (ImportItem) lookupItem(false);
        ImportTask task = item.getTask();
        task.removeItem(item);

        importer.changed(task);
    }

    Object lookupItem(boolean allowAll) {
        ImportContext context = context();
        Long imprt = context.getId();

        ImportItem item = item(true);
        if (item == null) {
            if (allowAll) {
                return task().getItems();
            }
            throw new RestletException("No item specified", Status.CLIENT_ERROR_BAD_REQUEST);
        }
        
        return item;
    }

    class ImportItemJSONFormat extends StreamDataFormat {

        ImportItemJSONFormat() {
            super(MediaType.APPLICATION_JSON);
        }

        @Override
        protected Object read(InputStream in) throws IOException {
            return new ImportJSONIO(importer).item(in);
        }

        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            ImportJSONIO json = new ImportJSONIO(importer);

            if (object instanceof ImportItem) {
                ImportItem item = (ImportItem) object;
                json.item(item, getPageInfo(), out);
            }
            else {
                json.items((List<ImportItem>)object, getPageInfo(), out);
            }
        }

    }
}
