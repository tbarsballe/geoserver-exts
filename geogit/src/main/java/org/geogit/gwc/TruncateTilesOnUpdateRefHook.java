/* Copyright (c) 2014 OpenPlans. All rights reserved.
 * This code is licensed under the GNU GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogit.gwc;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkState;
import static org.geoserver.catalog.Predicates.and;
import static org.geoserver.catalog.Predicates.equal;

import java.io.Serializable;
import java.util.List;
import java.util.Map;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Context;
import org.geogit.api.Ref;
import org.geogit.api.SymRef;
import org.geogit.api.hooks.CannotRunGeogitOperationException;
import org.geogit.api.hooks.CommandHook;
import org.geogit.api.hooks.Scripting;
import org.geogit.api.plumbing.RefParse;
import org.geogit.api.plumbing.UpdateRef;
import org.geogit.geotools.data.GeoGitDataStore;
import org.geogit.geotools.data.GeoGitDataStoreFactory;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.StoreInfo;
import org.geoserver.catalog.util.CloseableIterator;
import org.geoserver.gwc.GWC;
import org.geoserver.gwc.layer.GeoServerTileLayer;
import org.geowebcache.GeoWebCacheExtensions;
import org.geowebcache.seed.TileBreeder;
import org.opengis.filter.Filter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Optional;
import com.google.common.base.Stopwatch;
import com.google.common.collect.LinkedListMultimap;
import com.google.common.collect.ListMultimap;

/**
 * A "classpath" command hook that hooks onto the {@link UpdateRef} command and truncates GWC tiles
 * for any {@link GeoGitDataStore} configured in geoserver that's affected by the ref update.
 * <p>
 * Ref updates may come from remote repositories pushing changes to the geogit web api as exposed by
 * the {@code /geogit/<workspace>:<datastore>} repository entry points.
 * <p>
 * When such geogit command is caught, this hook looks for GWC tile layers configured so that the
 * change may affect them, figures out the "minimal bounds" geometry of the changeset, and issues
 * GWC truncate tasks appropriately.
 */
public class TruncateTilesOnUpdateRefHook implements CommandHook {

    private static final Logger LOGGER = LoggerFactory
            .getLogger(TruncateTilesOnUpdateRefHook.class);

    /**
     * {@link Catalog} filter to retrieve enabled layer infos backed by a geogit datastore
     */
    private static final Filter GEOGIT_LAYERINFO_FILTER = and(equal("enabled", Boolean.TRUE),
            equal("resource.store.type", GeoGitDataStoreFactory.DISPLAY_NAME));

    @Override
    public boolean appliesTo(@SuppressWarnings("rawtypes") Class<? extends AbstractGeoGitOp> clazz) {
        return UpdateRef.class.equals(clazz);
    }

    @Override
    public <C extends AbstractGeoGitOp<?>> C pre(C command)
            throws CannotRunGeogitOperationException {

        /*
         * Store the ref name and its old value in the command's user properties to be used in the
         * post-hook if the operation was successful
         */
        Map<String, Object> params = Scripting.getParamMap(command);
        String refName = (String) params.get("name");
        command.getClientData().put("name", refName);
        if (Ref.WORK_HEAD.equals(refName) || Ref.STAGE_HEAD.equals(refName)) {
            command.getClientData().put("ignore", Boolean.TRUE);
            // ignore updates to work/stage heads, we only care of updates to branches
            return command;
        }

        Optional<Ref> currentValue = command.command(RefParse.class).setName(refName).call();
        command.getClientData().put("oldValue", currentValue);

        LOGGER.debug("GWC geogit truncate pre-hook engaged for ref '{}'", refName);
        return command;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> T post(AbstractGeoGitOp<T> command, Object retVal, boolean success) throws Exception {
        checkArgument(command instanceof UpdateRef);
        final UpdateRef cmd = (UpdateRef) command;
        final String refName = (String) cmd.getClientData().get("name");
        checkState(refName != null, "refName not captured in pre-hook");
        if (Boolean.TRUE.equals(cmd.getClientData().get("ignore"))) {
            LOGGER.debug("GWC geogit truncate post-hook returning, ref '{}' is ignored.", refName);
            return (T) retVal;
        }

        if (!success) {
            LOGGER.info(
                    "GWC geogit truncate post-hook returning, UpdateRef operation failed on ref '{}'.",
                    refName);
            return (T) retVal;
        }

        final GWC mediator = GWC.get();
        if (mediator == null) {
            LOGGER.debug("GWC geogit truncate post-hook returning, GWC mediator not installed?.");
            return (T) retVal;
        }

        final Optional<Ref> oldValue = (Optional<Ref>) cmd.getClientData().get("oldValue");
        final Optional<Ref> newValue = (Optional<Ref>) retVal;// == oldValue if the ref was deleted

        checkState(oldValue != null, "oldValue not captured in pre-hook");

        if (oldValue.equals(newValue)) {
            LOGGER.debug("GWC geogit truncate post-hook returning, ref '{}' didn't change ({}).",
                    refName, oldValue);
            return (T) retVal;
        }

        List<LayerInfo> affectedLayers;
        final String newRefName = newValue.get().getName();
        Stopwatch sw = Stopwatch.createStarted();
        affectedLayers = findAffectedLayers(mediator, command.context(), newRefName);
        LOGGER.debug("GWC geogit truncate post-hook found {} affected layers on branch {} in {}.",
                affectedLayers.size(), refName, sw.stop());

        for (LayerInfo layer : affectedLayers) {
            truncate(mediator, command.context(), layer, oldValue, newValue);
        }
        return (T) retVal;
    }

    private void truncate(GWC mediator, Context geogitContext, LayerInfo layer,
            Optional<Ref> oldValue, Optional<Ref> newValue) {

        GeoServerTileLayer tileLayer = mediator.getTileLayer(layer);
        if (tileLayer == null) {
            return;
        }

        TileBreeder breeder = GeoWebCacheExtensions.bean(TileBreeder.class);
        checkState(breeder != null);// if GWC wasn't installed it should have returned earlier
        TruncateHelper.issueTruncateTasks(geogitContext, oldValue, newValue, tileLayer, breeder);

    }

    private List<LayerInfo> findAffectedLayers(GWC mediator, Context context, String newRefName) {

        final Catalog catalog = mediator.getCatalog();

        ListMultimap<StoreInfo, LayerInfo> affectedLayers = LinkedListMultimap.create();

        CloseableIterator<LayerInfo> geogitLayers;
        geogitLayers = catalog.list(LayerInfo.class, GEOGIT_LAYERINFO_FILTER);
        try {
            while (geogitLayers.hasNext()) {
                LayerInfo layerInfo = geogitLayers.next();
                // re check for the cascaded enabled() property
                if (!layerInfo.enabled()) {
                    continue;
                }
                // now we're sure the layer info is enabled and so is its store
                // ignore if there's no tile layer for it
                if (!mediator.hasTileLayer(layerInfo)) {
                    continue;
                }
                final DataStoreInfo store = (DataStoreInfo) layerInfo.getResource().getStore();
                if (affectedLayers.containsKey(store)) {
                    affectedLayers.put(store, layerInfo);
                } else {
                    @Nullable
                    final String dataStoreHead = findDataStoreHeadRefName(store, context);
                    if (newRefName.equals(dataStoreHead)) {
                        affectedLayers.put(store, layerInfo);
                    }
                }

            }
        } finally {
            geogitLayers.close();
        }
        return (List<LayerInfo>) affectedLayers.values();
    }

    private String findDataStoreHeadRefName(DataStoreInfo store, Context context) {
        String dataStoreHead;
        Map<String, Serializable> storeParams = store.getConnectionParameters();

        final String branch = (String) storeParams.get(GeoGitDataStoreFactory.BRANCH.key);
        final String head = (String) storeParams.get(GeoGitDataStoreFactory.HEAD.key);
        dataStoreHead = (head == null) ? branch : head;
        if (dataStoreHead == null) {
            Optional<Ref> currHead = context.command(RefParse.class).setName(Ref.HEAD).call();
            if (!currHead.isPresent() || !(currHead.get() instanceof SymRef)) {
                // can't figure out the current branch, ignore?
                return null;
            }
            dataStoreHead = ((SymRef) currHead.get()).getTarget();
        } else {
            Optional<Ref> storeRef = context.command(RefParse.class).setName(dataStoreHead).call();
            if (storeRef.isPresent()) {
                Ref ref = storeRef.get();
                if (ref instanceof SymRef) {
                    dataStoreHead = ((SymRef) ref).getTarget();
                } else {
                    dataStoreHead = ref.getName();
                }
            } else {
                LOGGER.info("HEAD '{}' configured for store '{}' does not resolve to a ref",
                        dataStoreHead, store.getName());
                dataStoreHead = null;
            }
        }
        return dataStoreHead;
    }
}
