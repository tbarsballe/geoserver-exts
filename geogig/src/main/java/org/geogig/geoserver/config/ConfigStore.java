package org.geogig.geoserver.config;

import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.transform;
import static com.google.common.collect.Lists.newArrayList;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.Iterator;
import java.util.List;
import java.util.UUID;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.resource.Paths;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.geoserver.platform.resource.Resources;
import org.geotools.util.logging.Logging;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Predicate;
import com.google.common.base.Throwables;
import com.thoughtworks.xstream.XStream;

public class ConfigStore {

    private static final Logger LOGGER = Logging.getLogger(ConfigStore.class);

    private static final String CONFIG_DIR_NAME = "geogigconfig";

    private ResourceStore resourceLoader;

    public ConfigStore(ResourceStore dataDir) {
        this.resourceLoader = dataDir;
        if (null == Resources.directory(resourceLoader.get(CONFIG_DIR_NAME), true)) {
            throw new IllegalStateException("Unable to create config directory " + CONFIG_DIR_NAME);
        }
    }

    public RepositoryInfo save(RepositoryInfo info) {
        checkNotNull(info, "null RepositoryInfo");
        checkId(info);
        Resource resource = resource(info.getId());
        try (OutputStream out = resource.out()) {
            getConfigredXstream().toXML(info, new OutputStreamWriter(out, Charsets.UTF_8));
        } catch (IOException e) {
            throw Throwables.propagate(e);
        }
        return info;
    }

    private void checkId(RepositoryInfo info) {
        if (info.getId() == null) {
            info.setId(UUID.randomUUID().toString());
        }
    }

    private Resource resource(String id) {
        Resource resource = resourceLoader.get(path(id));
        return resource;
    }

    private Resource getConfigRoot() {
        return resourceLoader.get(CONFIG_DIR_NAME);
    }

    static String path(String infoId) {
        return Paths.path(CONFIG_DIR_NAME, infoId + ".xml");
    }

    public List<RepositoryInfo> getRepositories() {
        List<Resource> list = getConfigRoot().list();
        Iterator<Resource> xmlfiles = filter(list.iterator(), FILENAMEFILTER);
        return newArrayList(filter(transform(xmlfiles, LOADER), notNull()));
    }

    public RepositoryInfo load(String id) throws IOException {
        checkNotNull(id, "provided a null id");
        Resource resource = resource(id);
        return load(resource);
    }

    private static RepositoryInfo load(Resource input) throws IOException {
        try (Reader reader = new InputStreamReader(input.in(), Charsets.UTF_8)) {
            RepositoryInfo info = (RepositoryInfo) getConfigredXstream().fromXML(reader);
            return info;
        } catch (Exception e) {
            String msg = "Unable to load repo config " + input.name();
            LOGGER.log(Level.WARNING, msg, e);
            throw new IOException(msg, e);
        }
    }

    private static XStream getConfigredXstream() {
        XStream xStream = new XStream();
        xStream.alias("RepositoryInfo", RepositoryInfo.class);
        return xStream;
    }

    private static final Predicate<Resource> FILENAMEFILTER = new Predicate<Resource>() {
        @Override
        public boolean apply(Resource input) {
            return input.name().endsWith(".xml");
        }
    };

    private static final Function<Resource, RepositoryInfo> LOADER = new Function<Resource, RepositoryInfo>() {

        @Override
        public RepositoryInfo apply(Resource input) {
            try {
                return load(input);
            } catch (IOException e) {
                return null;
            }
        }

    };

}
