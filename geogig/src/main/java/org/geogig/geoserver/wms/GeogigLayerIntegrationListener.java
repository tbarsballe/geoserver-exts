package org.geogig.geoserver.wms;

import static org.geoserver.catalog.Predicates.and;
import static org.geoserver.catalog.Predicates.equal;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

import org.geoserver.catalog.AuthorityURLInfo;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.CatalogException;
import org.geoserver.catalog.CatalogInfo;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerIdentifierInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.ResourceInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.WorkspaceInfo;
import org.geoserver.catalog.event.CatalogAddEvent;
import org.geoserver.catalog.event.CatalogListener;
import org.geoserver.catalog.event.CatalogModifyEvent;
import org.geoserver.catalog.event.CatalogPostModifyEvent;
import org.geoserver.catalog.event.CatalogRemoveEvent;
import org.geoserver.catalog.impl.AuthorityURL;
import org.geoserver.catalog.impl.LayerIdentifier;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.config.GeoServer;
import org.geoserver.wms.WMSInfo;
import org.geotools.util.logging.Logging;
import org.locationtech.geogig.geotools.data.GeoGigDataStoreFactory;
import org.opengis.filter.Filter;

/**
 * Ensures a global WMS {@link AuthorityURL} exists with name {@code GEOGIG_ENTRY_POINT} and URL
 * {@code http://geogig.org}, and that each {@link LayerInfo layer} from a geogig datastore gets a
 * {@link LayerIdentifierInfo} with authority {@code GEOGIG_ENTRY_POINT} and the identifier composed
 * of {@code <workspace name>:<store name>:<nativeName>[:<branch/head>]}
 * <p>
 * The identifier is made of the following parts:
 * <ul>
 * <li> {@code <workspace name>}: the name of the {@link WorkspaceInfo workspace} the layer's
 * resource belongs to
 * <li> {@code <store name>}: the name of the {@link DataStoreInfo data store} the layer's resource
 * belongs to
 * <li> {@code <nativeName>}: the layer's resource {@link ResourceInfo#getNativeName() native name}
 * <li> {@code <branch/head>}: the geogig datastore's configured
 * {@link GeoGigDataStoreFactory#BRANCH branch} or {@link GeoGigDataStoreFactory#HEAD}, whichever is
 * present, or absent if no branch or head is configured (and hence the datastore operates on
 * whatever the current HEAD is)
 * </ul>
 * <p>
 * Handles the following events:
 * <ul>
 * <li> {@link WorkspaceInfo} renamed: all geogig layers of stores in that workspace get their
 * authority URL identifiers updated
 * <li> {@link DataStoreInfo} renamed: all geogig layers of stores in that workspace get their
 * authority URL identifiers updated
 * <li> {@link DataStoreInfo} workspace changed: all geogig layers of stores in that workspace get
 * their authority URL identifiers updated
 * <li> {@link LayerInfo} added: if its a geogig layer, creates its geogig authority URL identifier
 * and saves the layer info
 * </ul>
 */
public class GeogigLayerIntegrationListener implements CatalogListener {

    private static final Logger LOGGER = Logging.getLogger(GeogigLayerIntegrationListener.class);

    public static final String AUTHORITY_URL_NAME = "GEOGIG_ENTRY_POINT";

    public static final String AUTHORITY_URL = "http://geogig.org";

    private GeoServer geoserver;

    /**
     */
    public GeogigLayerIntegrationListener(GeoServer geoserver) {
        LOGGER.fine("Initialized " + getClass().getName());
        this.geoserver = geoserver;
        this.geoserver.getCatalog().addListener(this);
    }

    @Override
    public void handleAddEvent(CatalogAddEvent event) throws CatalogException {
        if (!(event.getSource() instanceof LayerInfo)) {
            return;
        }
        LayerInfo layer = (LayerInfo) event.getSource();
        if (!isGeogigLayer(layer)) {
            return;
        }
        if (!forceServiceRootLayerHaveGeogigAuthURL()) {
            return;
        }

        setIdentifier(layer);
    }

    private static final ThreadLocal<CatalogInfo> PRE_MOFIFY_EVENT = new ThreadLocal<CatalogInfo>();

    @Override
    public void handleModifyEvent(CatalogModifyEvent event) throws CatalogException {
        if (PRE_MOFIFY_EVENT.get() != null) {
            LOGGER.fine("pre-modify event exists, ignoring handleModifyEvent ("
                    + PRE_MOFIFY_EVENT.get() + ")");
            return;
        }

        final CatalogInfo source = event.getSource();
        final boolean isGeogigStore = isGeogigStore(source);

        boolean tryPostUpdate = (source instanceof WorkspaceInfo) || isGeogigStore;
        final List<String> propertyNames = event.getPropertyNames();
        tryPostUpdate &= propertyNames.contains("name")
                || propertyNames.contains("connectionParameters");

        if (tryPostUpdate) {
            LOGGER.fine("Storing event for post-handling on " + source);
            PRE_MOFIFY_EVENT.set(source);
        }
    }

    @Override
    public void handlePostModifyEvent(CatalogPostModifyEvent event) throws CatalogException {
        final CatalogInfo preModifySource = PRE_MOFIFY_EVENT.get();
        if (preModifySource == null) {
            return;
        }
        if (!event.getSource().getId().equals(preModifySource.getId())) {
            return;
        }
        PRE_MOFIFY_EVENT.remove();
        LOGGER.fine("handing post-modify event for " + preModifySource);

        CatalogInfo source = event.getSource();

        if (source instanceof WorkspaceInfo) {
            handlePostWorkspaceChange((WorkspaceInfo) source);
        } else if (source instanceof DataStoreInfo) {
            handlePostGeogigStoreChange((DataStoreInfo) source);
        }
    }

    private void handlePostGeogigStoreChange(DataStoreInfo source) {
        Catalog catalog = geoserver.getCatalog();

        final String storeId = source.getId();
        Filter filter = equal("resource.store.id", storeId);

        CloseableIterator<LayerInfo> affectedLayers = catalog.list(LayerInfo.class, filter);
        updateLayers(affectedLayers);
    }

    private void handlePostWorkspaceChange(WorkspaceInfo source) {
        Catalog catalog = geoserver.getCatalog();
        final String wsId = source.getId();
        final String storeType = GeoGigDataStoreFactory.DISPLAY_NAME;

        Filter filter = and(equal("resource.store.workspace.id", wsId),
                equal("resource.store.type", storeType));

        CloseableIterator<LayerInfo> affectedLayers = catalog.list(LayerInfo.class, filter);
        updateLayers(affectedLayers);
    }

    private void updateLayers(CloseableIterator<LayerInfo> affectedLayers) {
        try {
            while (affectedLayers.hasNext()) {
                LayerInfo geogigLayer = affectedLayers.next();
                setIdentifier(geogigLayer);
            }
        } finally {
            affectedLayers.close();
        }
    }

    private boolean forceServiceRootLayerHaveGeogigAuthURL() {
        LOGGER.fine("Checking for root layer geogig auth URL");

        WMSInfo serviceInfo = geoserver.getService(WMSInfo.class);
        if (serviceInfo == null) {
            LOGGER.info("No WMSInfo available in GeoServer. This is strange but can happen");
            return false;
        }

        GeoServer geoserver = this.geoserver;
        List<AuthorityURLInfo> authorityURLs = serviceInfo.getAuthorityURLs();
        for (AuthorityURLInfo urlInfo : authorityURLs) {
            if (AUTHORITY_URL_NAME.equals(urlInfo.getName())) {
                LOGGER.fine("geogig root layer auth URL already exists");
                return true;
            }
        }

        AuthorityURL geogigAuthURL = new AuthorityURL();
        geogigAuthURL.setName(AUTHORITY_URL_NAME);
        geogigAuthURL.setHref(AUTHORITY_URL);
        serviceInfo.getAuthorityURLs().add(geogigAuthURL);

        LOGGER.fine("Saving geogig root layer auth URL");
        geoserver.save(serviceInfo);
        LOGGER.info("geogig root layer auth URL saved");
        return true;
    }

    /**
     * Does nothing
     */
    @Override
    public void handleRemoveEvent(CatalogRemoveEvent event) throws CatalogException {
        // do nothing
    }

    /**
     * Does nothing
     */
    @Override
    public void reloaded() {
        // do nothing
    }

    private void setIdentifier(LayerInfo layer) {
        LOGGER.fine("Updating geogig auth identifier for layer " + layer.prefixedName());
        final String layerIdentifier = buildLayerIdentifier(layer);
        updateIdentifier(layer, layerIdentifier);
    }

    private void updateIdentifier(LayerInfo geogigLayer, final String newIdentifier) {

        List<LayerIdentifierInfo> layerIdentifiers = geogigLayer.getIdentifiers();
        {
            LayerIdentifierInfo id = null;
            for (LayerIdentifierInfo identifier : layerIdentifiers) {
                if (AUTHORITY_URL_NAME.equals(identifier.getAuthority())) {
                    id = identifier;
                    break;
                }
            }
            if (id != null) {
                if (newIdentifier.equals(id.getIdentifier())) {
                    return;
                }
                layerIdentifiers.remove(id);
            }
        }

        LayerIdentifier newId = new LayerIdentifier();
        newId.setAuthority(AUTHORITY_URL_NAME);
        newId.setIdentifier(newIdentifier);
        layerIdentifiers.add(newId);
        Catalog catalog = geoserver.getCatalog();
        catalog.save(geogigLayer);
        LOGGER.info("Updated geogig auth identifier for layer " + geogigLayer.prefixedName()
                + " as " + newIdentifier);
    }

    private String buildLayerIdentifier(LayerInfo geogigLayer) {

        ResourceInfo resource = geogigLayer.getResource();
        StoreInfo store = resource.getStore();
        WorkspaceInfo workspace = store.getWorkspace();

        Map<String, Serializable> connectionParameters = store.getConnectionParameters();

        Serializable refSpec = connectionParameters.get(GeoGigDataStoreFactory.BRANCH.key);
        if (refSpec == null) {
            refSpec = connectionParameters.get(GeoGigDataStoreFactory.HEAD.key);
        }

        String identifier = workspace.getName() + ":" + store.getName() + ":"
                + geogigLayer.getResource().getNativeName();
        if (refSpec != null) {
            identifier = identifier + ":" + refSpec;
        }

        return identifier;
    }

    private boolean isGeogigLayer(LayerInfo layer) {
        ResourceInfo resource = layer.getResource();
        StoreInfo store = resource.getStore();
        return isGeogigStore(store);
    }

    private boolean isGeogigStore(CatalogInfo store) {
        if (!(store instanceof DataStoreInfo)) {
            return false;
        }
        final String storeType = ((DataStoreInfo) store).getType();
        boolean isGeogigLayer = GeoGigDataStoreFactory.DISPLAY_NAME.equals(storeType);
        return isGeogigLayer;
    }
}
