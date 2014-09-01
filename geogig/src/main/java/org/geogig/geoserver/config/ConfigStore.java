package org.geogig.geoserver.config;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.base.Predicates.notNull;
import static com.google.common.collect.Iterators.filter;
import static com.google.common.collect.Iterators.transform;
import static com.google.common.collect.Lists.newArrayList;

import java.io.File;
import java.io.FileNotFoundException;
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
import java.util.regex.Pattern;

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

/**
 * Handles storage for {@link RepositoryInfo}s inside the GeoServer data directory's
 * {@code geogigconfig/} subdirectory.
 * <p>
 * {@link RepositoryInfo} instances are created through its default constructor, which assings a
 * {@code null} id, meaning its a new instance and has not yet being saved.
 * <p>
 * Persistence is handled with {@link XStream} on a one file per {@code RepositoryInfo} bases under
 * {@code <data-dir>/geogigconfig/}, named {@code RepositoryInfo.getId()+".xml"}.
 * <p>
 * {@link #save(RepositoryInfo)} sets an id on new instances, which is the String representation of
 * a random {@link UUID}.
 * <p>
 * {@code RepositoryInfo} instances deserialized from XML have its id set by {@link XStream}, and
 * {@link #save(RepositoryInfo)} knows its an existing instance and replaces its file.
 * 
 *
 */
public class ConfigStore {

    private static final Logger LOGGER = Logging.getLogger(ConfigStore.class);

    /**
     * Regex pattern to assert the format of ids on {@link #save(RepositoryInfo)}
     */
    private static final Pattern UUID_PATTERN = Pattern
            .compile("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");

    private static final String CONFIG_DIR_NAME = "geogigconfig";

    private ResourceStore resourceLoader;

    public ConfigStore(ResourceStore resourceLoader) {
        checkNotNull(resourceLoader, "resourceLoader");
        this.resourceLoader = resourceLoader;
        if (null == Resources.directory(resourceLoader.get(CONFIG_DIR_NAME), true)) {
            throw new IllegalStateException("Unable to create config directory " + CONFIG_DIR_NAME);
        }
    }

    /**
     * Saves a {@link RepositoryInfo} to its {@code <data-dir>/geogigconfig/<id>.xml} file.
     * <p>
     * If {@code info} has no id set, one is assigned, meaning it didn't yet exist. Otherwise its
     * xml file is replaced meaning it has been modified.
     * 
     * @return {@code info}, possibly with its id set if it was {@code null}
     * 
     */
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
        } else {
            checkArgument(UUID_PATTERN.matcher(info.getId()).matches(),
                    "Id doesn't match UUID format: '%s'", info.getId());
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

    /**
     * Loads and returns all <b>valid</b> {@link RepositoryInfo}'s from
     * {@code <data-dir>/geogigconfig/}; any xml file that can't be parsed is ignored.
     */
    public List<RepositoryInfo> getRepositories() {
        List<Resource> list = getConfigRoot().list();
        Iterator<Resource> xmlfiles = filter(list.iterator(), FILENAMEFILTER);
        return newArrayList(filter(transform(xmlfiles, LOADER), notNull()));
    }

    /**
     * Loads a {@link RepositoryInfo} by {@link RepositoryInfo#getId() id} from its xml file under
     * {@code <data-dir>/geogigconfig/}
     */
    public RepositoryInfo load(final String id) throws IOException {
        checkNotNull(id, "provided a null id");
        Resource resource = resource(id);
        return load(resource);
    }

    private static RepositoryInfo load(Resource input) throws IOException {
        // make an explicit check here because FileSystemResource.file() creates an empty file
        File parent = input.parent().dir();
        File f = new File(parent, input.name());
        if (!(parent.exists() && f.exists())) {
            throw new FileNotFoundException("File not found: " + f.getAbsolutePath());
        }
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
