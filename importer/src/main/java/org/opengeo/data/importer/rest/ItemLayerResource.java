package org.opengeo.data.importer.rest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.logging.Logger;

import net.sf.json.JSONObject;

import org.geoserver.catalog.CatalogBuilder;
import org.geoserver.catalog.CoverageInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.impl.LayerInfoImpl;
import org.geoserver.rest.RestletException;
import org.geoserver.rest.format.DataFormat;
import org.geoserver.rest.format.StreamDataFormat;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.util.logging.Logging;
import org.opengeo.data.importer.ImportItem;
import org.opengeo.data.importer.Importer;
import org.opengis.referencing.FactoryException;
import org.opengis.referencing.NoSuchAuthorityCodeException;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;

public class ItemLayerResource extends BaseResource {

    static Logger LOGGER = Logging.getLogger(ItemLayerResource.class);

    public ItemLayerResource(Importer importer) {
        super(importer);
    }

    @Override
    protected List<DataFormat> createSupportedFormats(Request request, Response response) {
        return (List) Arrays.asList(new ItemLayerJSONFormat());
    }

    @Override
    public void handleGet() {
        ImportItem item = item();
        getResponse().setEntity(getFormatGet().toRepresentation(item.getLayer()));
    }

    @Override
    public boolean allowPut() {
        return true;
    }

    @Override
    public void handlePut() {
        ImportItem item = item();

        LayerInfo layer = (LayerInfo) getFormatPostOrPut().toObject(getRequest().getEntity());
        
        updateLayer(item, layer, importer);
        importer.changed(item);
    }
    
    static void updateLayer(ImportItem orig, LayerInfo l, Importer importer) {
        //update the original layer and resource from the new

        ResourceInfo r = l.getResource();
        
        //TODO: this is not thread safe, clone the object before overwriting it
        //save the existing resource, which will be overwritten below,  
        ResourceInfo resource = orig.getLayer().getResource();
        
        CatalogBuilder cb = new CatalogBuilder(importer.getCatalog());
        if (l != null) {
            l.setResource(resource);
            // @hack workaround OWSUtils bug - trying to copy null collections
            // why these are null in the first place is a different question
            LayerInfoImpl impl = (LayerInfoImpl) orig.getLayer();
            if (impl.getAuthorityURLs() == null) {
                impl.setAuthorityURLs(new ArrayList(1));
            }
            if (impl.getIdentifiers() == null) {
                impl.setIdentifiers(new ArrayList(1));
            }
            // @endhack
            cb.updateLayer(orig.getLayer(), l);
        }
        
        // validate SRS - an invalid one will destroy capabilities doc and make
        // the layer totally broken in UI
        CoordinateReferenceSystem newRefSystem = null;
        if (r instanceof FeatureTypeInfo) {
            String srs = ((FeatureTypeInfo) r).getSRS();
            if (srs != null) {
                try {
                    newRefSystem = CRS.decode(srs);
                } catch (NoSuchAuthorityCodeException ex) {
                    String msg = "Invalid SRS " + srs;
                    LOGGER.warning(msg + " in PUT request");
                    throw ImportJSONIO.badRequest(msg);
                } catch (FactoryException ex) {
                    throw new RestletException("Error with referencing",Status.SERVER_ERROR_INTERNAL,ex);
                }
                // make this the specified native if none exists
                // useful for csv or other files
                if (resource.getNativeCRS() == null) {
                    resource.setNativeCRS(newRefSystem);
                }
            }
        }

        //update the resource
        if (r != null) {
            if (r instanceof FeatureTypeInfo) {
                cb.updateFeatureType((FeatureTypeInfo) resource, (FeatureTypeInfo) r);
            }
            else if (r instanceof CoverageInfo) {
                cb.updateCoverage((CoverageInfo) resource, (CoverageInfo) r);
            }
        }
        
        // have to do this after updating the original
        //JD: not actually sure this should be here... it doesn't work in the indirect case since
        // we don't have the physical feature type yet... i think it might be better to just to 
        // null and let the importer recalculate on the fly
        if (newRefSystem != null && orig.getTask().isDirect()) {
            try {
                ReferencedEnvelope nativeBounds = cb.getNativeBounds(resource);
                resource.setLatLonBoundingBox(cb.getLatLonBounds(nativeBounds, newRefSystem));
            } catch (IOException ex) {
                throw new RestletException("Error with bounds computation",Status.SERVER_ERROR_INTERNAL,ex);
            }
        }
        
    }

    class ItemLayerJSONFormat extends StreamDataFormat {

        public ItemLayerJSONFormat() {
            super(MediaType.APPLICATION_JSON);
        }
    
        @Override
        protected Object read(InputStream in) throws IOException {
            return new ImportJSONIO(importer).layer(in);
        }
    
        @Override
        protected void write(Object object, OutputStream out) throws IOException {
            new ImportJSONIO(importer).layer(item(), getPageInfo(), out);
        }
    }
}
