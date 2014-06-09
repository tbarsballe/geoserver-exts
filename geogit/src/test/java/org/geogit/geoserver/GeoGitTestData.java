package org.geogit.geoserver;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Strings.isNullOrEmpty;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URISyntaxException;
import java.net.URL;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.annotation.Nullable;

import org.geogit.api.AbstractGeoGitOp;
import org.geogit.api.Context;
import org.geogit.api.FeatureBuilder;
import org.geogit.api.GeoGIT;
import org.geogit.api.GlobalContextBuilder;
import org.geogit.api.NodeRef;
import org.geogit.api.ObjectId;
import org.geogit.api.RevFeature;
import org.geogit.api.RevFeatureType;
import org.geogit.api.RevTree;
import org.geogit.api.TestPlatform;
import org.geogit.api.plumbing.FindTreeChild;
import org.geogit.api.plumbing.ResolveGeogitDir;
import org.geogit.api.plumbing.RevObjectParse;
import org.geogit.api.porcelain.AddOp;
import org.geogit.api.porcelain.BranchCreateOp;
import org.geogit.api.porcelain.CheckoutOp;
import org.geogit.api.porcelain.CommitOp;
import org.geogit.api.porcelain.ConfigOp;
import org.geogit.api.porcelain.ConfigOp.ConfigAction;
import org.geogit.api.porcelain.InitOp;
import org.geogit.cli.test.functional.general.CLITestContextBuilder;
import org.geogit.geotools.data.GeoGitDataStore;
import org.geogit.geotools.data.GeoGitDataStoreFactory;
import org.geogit.repository.WorkingTree;
import org.geoserver.catalog.Catalog;
import org.geoserver.catalog.DataStoreInfo;
import org.geoserver.catalog.FeatureTypeInfo;
import org.geoserver.catalog.LayerInfo;
import org.geoserver.catalog.NamespaceInfo;
import org.geoserver.catalog.ProjectionPolicy;
import org.geoserver.catalog.WorkspaceInfo;
import org.geotools.data.DataAccess;
import org.geotools.data.DataUtilities;
import org.geotools.feature.SchemaException;
import org.geotools.feature.simple.SimpleFeatureBuilder;
import org.geotools.geometry.jts.ReferencedEnvelope;
import org.geotools.referencing.CRS;
import org.geotools.referencing.crs.DefaultGeographicCRS;
import org.geotools.util.Converters;
import org.junit.Assert;
import org.junit.rules.ExternalResource;
import org.junit.rules.TemporaryFolder;
import org.opengis.feature.Feature;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.feature.simple.SimpleFeatureType;
import org.opengis.feature.type.AttributeDescriptor;
import org.opengis.feature.type.FeatureType;
import org.opengis.referencing.crs.CoordinateReferenceSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.base.Function;
import com.google.common.base.Optional;
import com.google.common.base.Throwables;
import com.google.common.collect.Lists;
import com.google.common.collect.Maps;

public class GeoGitTestData extends ExternalResource {

    private static final Logger LOGGER = LoggerFactory.getLogger(GeoGitTestData.class);

    private TemporaryFolder tmpFolder;

    private GeoGIT geogit;

    @Override
    protected void before() throws Throwable {
        tmpFolder = new TemporaryFolder();
        tmpFolder.create();
        this.geogit = createGeogit();
    }

    @Override
    protected void after() {
        try {
            if (geogit != null) {
                geogit.close();
                geogit = null;
            }
        } finally {
            tmpFolder.delete();
        }
    }

    protected GeoGIT createGeogit() throws IOException {

        File dataDirectory = tmpFolder.getRoot();
        File repoDir = new File(dataDirectory, "testrepo");
        Assert.assertTrue(repoDir.mkdir());

        TestPlatform testPlatform = new TestPlatform(dataDirectory);
        GlobalContextBuilder.builder = new CLITestContextBuilder(testPlatform);
        Context context = GlobalContextBuilder.builder.build();
        GeoGIT geogit = new GeoGIT(context);

        return geogit;
    }

    public GeoGIT getGeogit() {
        return geogit;
    }

    private Object lastCommandResult;

    @SuppressWarnings("unchecked")
    public <T> T get() {
        return (T) lastCommandResult;
    }

    public GeoGitTestData config(String key, String value) {
        run(geogit.command(ConfigOp.class).setAction(ConfigAction.CONFIG_SET).setName(key)
                .setValue(value));
        return this;
    }

    public GeoGitTestData init() {
        run(geogit.command(InitOp.class));
        return this;
    }

    public GeoGitTestData add(@Nullable String... paths) {
        AddOp command = geogit.command(AddOp.class);
        if (paths != null) {
            for (String path : paths) {
                command.addPattern(path);
            }
        }
        run(command);
        return this;
    }

    public GeoGitTestData commit(@Nullable String message) {
        run(geogit.command(CommitOp.class).setMessage(message).setAllowEmpty(message == null));
        return this;
    }

    private Object run(AbstractGeoGitOp<?> cmd) {
        Object result = cmd.call();
        LOGGER.debug("ran cmd '{}', returned '{}'", cmd.getClass().getSimpleName(), result);
        lastCommandResult = result;
        return result;
    }

    public GeoGitTestData createTypeTree(String typeName, String typeSpec) {
        FeatureType type;
        try {
            type = DataUtilities.createType(typeName, typeSpec);
        } catch (SchemaException e) {
            throw Throwables.propagate(e);
        }
        return createTypeTree(typeName, type);
    }

    public GeoGitTestData createTypeTree(String treePath, FeatureType type) {
        geogit.getRepository().workingTree().createTypeTree(treePath, type);
        return this;
    }

    public GeoGitTestData branch(String branchName) {
        run(geogit.command(BranchCreateOp.class).setName(branchName));
        return this;
    }

    public GeoGitTestData checkout(String branchOrCommit) {
        run(geogit.command(CheckoutOp.class).setSource(branchOrCommit));
        return this;
    }

    /**
     * Inserts features in the working tree under the given parent tree path.
     * <p>
     * The parent tree must exist. The {@code featureSpecs} are of the form
     * {@code  featureSpec := <id>=<attname>:<value>[;<attname>:<value>]+} . The parsing routine is
     * as naive as it can be so do not use any '=', ':', or ';' in the values.
     * <p>
     * An empty value string is assumed to mean {@code null}. Any {@code <attname>} not provided
     * (wrt. its type) will be left as {@code null} in the built feature.
     * <p>
     * An {@code <attname>} that doesn't exist in the feature type throws an unchecked exception.
     */
    public GeoGitTestData insert(String parentTreePath, String... featureSpecs) {
        SimpleFeatureType type = getType(parentTreePath);
        SimpleFeatureBuilder fb = new SimpleFeatureBuilder(type);
        Map<String, Map<String, String>> specs = parseFeatureSpecs(featureSpecs);

        List<Feature> features = Lists.newArrayList();

        for (Map.Entry<String, Map<String, String>> spec : specs.entrySet()) {
            String fid = spec.getKey();
            Map<String, String> attributes = spec.getValue();
            fb.reset();
            for (Map.Entry<String, String> e : attributes.entrySet()) {
                String att = e.getKey();
                String sval = e.getValue();
                AttributeDescriptor descriptor = type.getDescriptor(att);
                Class<?> binding = descriptor.getType().getBinding();
                Object value = Converters.convert(sval, binding);
                checkArgument(sval == null || value != null, "Unable to convert value '%s' to %s",
                        sval, binding.getName());
                fb.set(att, value);
            }
            SimpleFeature feature = fb.buildFeature(fid);
            features.add(feature);
        }

        return insert(parentTreePath, features.toArray(new Feature[features.size()]));
    }

    public GeoGitTestData insert(String parentTreePath, Feature... features) {
        WorkingTree workingTree = geogit.getContext().workingTree();
        for (Feature feature : features) {
            workingTree.insert(parentTreePath, feature);
        }
        return this;
    }

    private Map<String, Map<String, String>> parseFeatureSpecs(String[] featureSpecs) {
        final String format = "<id>=<attname>:<value>[;<attname>:<value>]+";

        Map<String, Map<String, String>> specs = Maps.newHashMap();
        for (String spec : featureSpecs) {
            String[] split = spec.split("=");
            checkArgument(split.length == 2, "invalid feature spec. Expected '%s', got '%s'",
                    format, spec);
            String fid = split[0];
            checkArgument(!isNullOrEmpty(fid), "invalid feature fid. Expected '%s', got '%s'",
                    format, spec);
            checkArgument(!specs.containsKey(fid), "Duplicate fid '%s' in feature spec '%s'", fid,
                    Arrays.asList(featureSpecs));
            String atts = split[1];
            String[] attSpecs = atts.split(";");
            Map<String, String> attributes = Maps.newHashMap();
            for (String attSpec : attSpecs) {
                String[] attval = attSpec.split(":");
                checkArgument(attval.length == 2,
                        "invalid attribute spec '%s'. Expected '%s', got '%s'", attSpec, format,
                        spec);
                String attName = attval[0];
                checkArgument(!isNullOrEmpty(attName),
                        "empty attribute name in attribute spec '%s'. Expected '%s', got '%s'",
                        attSpec, format, spec);
                String attValue = attval[1];
                if (isNullOrEmpty(attValue)) {
                    attValue = null;
                }
                attributes.put(attName, attValue);
            }
            specs.put(fid, attributes);
        }
        return specs;
    }

    @SuppressWarnings("unchecked")
    private SimpleFeatureType getType(String parentTreePath) {
        Context context = geogit.getContext();

        List<NodeRef> featureTypeTrees = context.workingTree().getFeatureTypeTrees();
        List<String> treeNames = Lists.transform(featureTypeTrees, new Function<NodeRef, String>() {
            @Override
            public String apply(NodeRef input) {
                return input.path();
            }
        });
        for (int i = 0; i < treeNames.size(); i++) {
            String treeName = treeNames.get(i);
            if (treeName.equals(parentTreePath)) {
                ObjectId metadataId = featureTypeTrees.get(i).getMetadataId();
                RevFeatureType revType = ((Optional<RevFeatureType>) run(geogit.command(
                        RevObjectParse.class).setObjectId(metadataId))).get();
                SimpleFeatureType featureType = (SimpleFeatureType) revType.type();
                return featureType;
            }
        }

        throw new IllegalArgumentException(String.format("No tree path named '%s' exists: %s",
                parentTreePath, treeNames));
    }

    public GeoGitTestData update(String featurePath, String attributeName, @Nullable Object value) {
        SimpleFeature feature = getFeature(featurePath);

        SimpleFeatureType featureType = feature.getFeatureType();
        AttributeDescriptor descriptor = featureType.getDescriptor(attributeName);
        Class<?> binding = descriptor.getType().getBinding();
        Object actualValue = Converters.convert(value, binding);
        checkArgument(value == null || actualValue != null, "Unable to convert value '%s' to %s",
                value, binding.getName());

        feature.setAttribute(attributeName, actualValue);
        String parentPath = NodeRef.parentPath(featurePath);
        Context context = geogit.getContext();
        WorkingTree workingTree = context.workingTree();
        workingTree.insert(parentPath, feature);
        return this;
    }

    public SimpleFeature getFeature(String featurePath) {
        Context context = geogit.getContext();
        WorkingTree workingTree = context.workingTree();
        RevTree rootWorkingTree = workingTree.getTree();

        @SuppressWarnings("unchecked")
        Optional<NodeRef> ref = (Optional<NodeRef>) run(context.command(FindTreeChild.class)
                .setParent(rootWorkingTree).setChildPath(featurePath));
        checkArgument(ref.isPresent(), "No feature ref found: '%s'", featurePath);

        NodeRef featureRef = ref.get();

        SimpleFeatureType type = getType(featureRef.getParentPath());

        @SuppressWarnings("unchecked")
        Optional<RevFeature> revFeature = (Optional<RevFeature>) run(context.command(
                RevObjectParse.class).setObjectId(featureRef.objectId()));

        String id = featureRef.name();
        Feature feature = new FeatureBuilder(type).build(id, revFeature.get());
        return (SimpleFeature) feature;
    }

    public CatalogBuilder newCatalogBuilder(Catalog catalog) {
        return new CatalogBuilder(catalog);
    }

    public class CatalogBuilder {
        public static final String NAMESPACE = "http://geogit.org";

        public static final String WORKSPACE = "geogittest";

        public static final String STORE = "geogitstore";

        private String workspace = WORKSPACE;

        private String nsUri = NAMESPACE;

        private String storeName = STORE;

        private Set<String> layerNames = new TreeSet<String>();

        private Catalog catalog;

        private CatalogBuilder(Catalog catalog) {
            this.catalog = catalog;
        }

        public String workspaceName() {
            return workspace;
        }

        public String namespaceUri() {
            return nsUri;
        }

        public String storeName() {
            return storeName;
        }

        public CatalogBuilder workspace(String workspace) {
            this.workspace = workspace;
            return this;
        }

        public CatalogBuilder namespace(String nsUri) {
            this.nsUri = nsUri;
            return this;
        }

        public CatalogBuilder store(String storeName) {
            this.storeName = storeName;
            return this;
        }

        public CatalogBuilder layer(String treeName) {
            this.layerNames.add(treeName);
            return this;
        }

        public CatalogBuilder addAllRepoLayers() {
            List<NodeRef> featureTypeTrees = geogit.getContext().workingTree()
                    .getFeatureTypeTrees();
            for (NodeRef ref : featureTypeTrees) {
                layer(ref.name());
            }
            return this;
        }

        public Catalog build() {
            NamespaceInfo ns = setUpNamespace(workspace, nsUri);
            WorkspaceInfo ws = setUpWorkspace(workspace);
            DataStoreInfo ds = setUpDataStore(ns, ws, storeName);
            for (String layerName : layerNames) {
                setUpLayer(ds, layerName);
            }
            return catalog;
        }

        private LayerInfo setUpLayer(DataStoreInfo ds, String layerName) {
            FeatureTypeInfo ft = setUpFeatureType(ds, layerName);
            LayerInfo li = catalog.getFactory().createLayer();
            li.setResource(ft);
            li.setEnabled(true);
            li.setAdvertised(true);
            String resourceName = ft.getName();
            li.setName(resourceName);
            catalog.add(li);
            return catalog.getLayerByName(li.prefixedName());
        }

        private FeatureTypeInfo setUpFeatureType(DataStoreInfo ds, String layerName) {
            FeatureTypeInfo ft = catalog.getFactory().createFeatureType();
            ft.setStore(ds);
            ft.setAdvertised(true);
            ft.setEnabled(true);
            ft.setName(layerName);
            ft.setNativeName(layerName);
            NamespaceInfo namespaceInfo = catalog.getNamespaceByPrefix(ds.getWorkspace().getName());
            ft.setNamespace(namespaceInfo);
            ft.setProjectionPolicy(ProjectionPolicy.FORCE_DECLARED);

            WorkingTree workingTree = geogit.getRepository().workingTree();
            Map<String, NodeRef> trees = Maps.uniqueIndex(workingTree.getFeatureTypeTrees(),
                    new Function<NodeRef, String>() {
                        @Override
                        public String apply(NodeRef treeRef) {
                            return treeRef.name();
                        }
                    });
            NodeRef treeRef = trees.get(layerName);
            FeatureType featureType = getType(treeRef.path());
            CoordinateReferenceSystem nativeCRS = featureType.getCoordinateReferenceSystem();
            ft.setNativeCRS(nativeCRS);
            String srs = CRS.toSRS(nativeCRS);
            ft.setSRS(srs);

            ReferencedEnvelope box = new ReferencedEnvelope(nativeCRS);
            treeRef.expand(box);
            ft.setNativeBoundingBox(box);
            ReferencedEnvelope latLonBounds;
            try {
                latLonBounds = box.transform(DefaultGeographicCRS.WGS84, true);
            } catch (Exception e) {
                throw Throwables.propagate(e);
            }
            ft.setLatLonBoundingBox(latLonBounds);
            catalog.add(ft);
            ft = catalog.getFeatureTypeByName(ft.prefixedName());
            checkNotNull(ft);
            return ft;
        }

        public DataStoreInfo setUpDataStore(NamespaceInfo ns, WorkspaceInfo ws, String storeName) {
            DataStoreInfo ds = catalog.getFactory().createDataStore();
            ds.setEnabled(true);
            ds.setDescription("Test Geogit DataStore");
            ds.setName(storeName);
            ds.setType(GeoGitDataStoreFactory.DISPLAY_NAME);
            ds.setWorkspace(ws);
            Map<String, Serializable> connParams = ds.getConnectionParameters();

            Optional<URL> geogitDir = geogit.command(ResolveGeogitDir.class).call();
            File repositoryUrl;
            try {
                repositoryUrl = new File(geogitDir.get().toURI()).getParentFile();
            } catch (URISyntaxException e) {
                throw Throwables.propagate(e);
            }
            assertTrue(repositoryUrl.exists() && repositoryUrl.isDirectory());

            connParams.put(GeoGitDataStoreFactory.REPOSITORY.key, repositoryUrl);
            connParams.put(GeoGitDataStoreFactory.DEFAULT_NAMESPACE.key, ns.getURI());
            catalog.add(ds);

            DataStoreInfo dsInfo = catalog.getDataStoreByName(WORKSPACE, STORE);
            assertNotNull(dsInfo);
            assertEquals(GeoGitDataStoreFactory.DISPLAY_NAME, dsInfo.getType());
            DataAccess<? extends FeatureType, ? extends Feature> dataStore;
            try {
                dataStore = dsInfo.getDataStore(null);
            } catch (IOException e) {
                throw Throwables.propagate(e);
            }
            assertNotNull(dataStore);
            assertTrue(dataStore instanceof GeoGitDataStore);

            ds = catalog.getDataStoreByName(ds.getWorkspace(), ds.getName());
            checkNotNull(ds);
            return ds;
        }

        public WorkspaceInfo setUpWorkspace(String workspace) {
            WorkspaceInfo ws = catalog.getFactory().createWorkspace();
            ws.setName(workspace);
            catalog.add(ws);
            ws = catalog.getWorkspaceByName(workspace);
            checkNotNull(ws);
            return ws;
        }

        public NamespaceInfo setUpNamespace(String workspace, String nsUri) {
            NamespaceInfo ns = catalog.getFactory().createNamespace();
            ns.setPrefix(workspace);
            ns.setURI(nsUri);
            catalog.add(ns);
            ns = catalog.getNamespaceByPrefix(workspace);
            checkNotNull(ns);
            return ns;
        }
    }
}
