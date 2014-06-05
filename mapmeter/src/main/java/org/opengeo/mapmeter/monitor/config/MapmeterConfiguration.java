package org.opengeo.mapmeter.monitor.config;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Date;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geoserver.platform.GeoServerExtensions;
import org.geoserver.platform.GeoServerResourceLoader;
import org.geoserver.security.GeoServerSecurityManager;
import org.geoserver.security.password.GeoServerPBEPasswordEncoder;
import org.geotools.util.logging.Logging;
import org.opengeo.mapmeter.monitor.saas.MapmeterSaasCredentials;

import com.google.common.base.Charsets;
import com.google.common.base.Function;
import com.google.common.base.Objects;
import com.google.common.base.Optional;
import com.google.common.io.Closer;
import com.google.common.io.Files;

public class MapmeterConfiguration {

    private static final String MAPMETER_APIKEY_OVERRIDE_PROPERTY_NAME = "MAPMETER_API_KEY";

    private final static Logger LOGGER = Logging.getLogger(MapmeterConfiguration.class);

    private final String mapmeterConfigRelPath;

    private final GeoServerResourceLoader loader;

    private final Optional<String> apiKeyOverride;

    private final String defaultBaseUrl;

    private final String storageSuffix;

    private final String checkSuffix;

    private final String systemUpdateSuffix;

    private final GeoServerPBEPasswordEncoder geoServerPBEPasswordEncoder;

    private final GeoServerSecurityManager geoServerSecurityManager;

    private Optional<String> apiKeyProperties;

    private Optional<String> baseUrl;

    private Optional<Boolean> isOnPremise;

    private Optional<MapmeterSaasCredentials> mapmeterSaasCredentials;

    public MapmeterConfiguration(String defaultBaseUrl, GeoServerResourceLoader loader,
            GeoServerPBEPasswordEncoder geoServerPBEPasswordEncoder,
            GeoServerSecurityManager geoServerSecurityManager) {

        this.defaultBaseUrl = defaultBaseUrl;
        this.storageSuffix = "/controller/v1/message/store";
        this.checkSuffix = "/controller/v1/message/check";
        this.systemUpdateSuffix = "/controller/v1/server";
        this.loader = loader;
        this.geoServerPBEPasswordEncoder = geoServerPBEPasswordEncoder;
        this.geoServerSecurityManager = geoServerSecurityManager;
        this.mapmeterConfigRelPath = "monitoring" + File.separatorChar + "mapmeter.properties";

        String apiKeyOverrideProperty = GeoServerExtensions.getProperty(MAPMETER_APIKEY_OVERRIDE_PROPERTY_NAME);
        apiKeyOverride = Optional.fromNullable(apiKeyOverrideProperty);

        migrateConfigIfNecessary("monitoring" + File.separatorChar + "controller.properties",
                mapmeterConfigRelPath);

        refreshConfig();
    }

    private void migrateConfigIfNecessary(String oldPath, String newPath) {
        try {
            File oldConfigFile = loader.find(oldPath);
            File newConfigFile = loader.find(newPath);
            if (oldConfigFile != null && oldConfigFile.isFile()) {
                if (newConfigFile != null && newConfigFile.isFile()) {
                    LOGGER.warning("Detected legacy mapmeter configuration: " + oldPath
                            + " - as well as new mapmeter configuration: " + newPath
                            + ". Not performing migration");
                    return;
                }
                LOGGER.warning("Detected configuration file controller.properties. Migrating to mapmeter.properties.");
                Closer closer = Closer.create();
                try {
                    BufferedReader fileReader = closer.register(Files.newReader(oldConfigFile,
                            Charsets.UTF_8));
                    Properties oldProperties = new Properties();
                    oldProperties.load(fileReader);

                    Properties newProperties = new Properties();

                    String apiKey = (String) oldProperties.get("apikey");
                    if (apiKey != null) {
                        newProperties.put("apikey", apiKey);
                    }
                    String url = (String) oldProperties.get("url");
                    if (url != null) {
                        try {
                            // this was the controller storage url
                            // parse the base url out of this
                            URI uri = new URI(url);
                            String baseUrl = uri.getScheme() + "://" + uri.getAuthority();
                            newProperties.put("baseurl", baseUrl);
                        } catch (URISyntaxException e) {
                            LOGGER.log(Level.SEVERE, "Could not parse url: " + url, e);
                        }
                    }

                    newConfigFile = loader.createFile(newPath);
                    BufferedWriter writer = closer.register(Files.newWriter(newConfigFile,
                            Charsets.UTF_8));
                    newProperties.store(writer, null);

                    if (!oldConfigFile.delete()) {
                        LOGGER.log(Level.SEVERE,
                                "Failure removing legacy mapmeter configuration file: " + oldPath);
                    }
                } finally {
                    closer.close();
                }
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE, "IO failure detecting/migrating mapmeter configuration", e);
        }
    }

    public void refreshConfig() {
        Optional<String> apiKey = Optional.absent();
        Optional<String> baseUrl = Optional.absent();
        Optional<Boolean> isOnPremise = Optional.absent();
        Optional<MapmeterSaasCredentials> mapmeterSaasCredentials = Optional.absent();

        try {
            Closer closer = Closer.create();
            try {
                Optional<File> propFile = findMapmeterConfigurationFile();

                if (propFile.isPresent()) {
                    Properties properties = new Properties();
                    BufferedReader fileReader = closer.register(Files.newReader(propFile.get(),
                            Charsets.UTF_8));
                    properties.load(fileReader);

                    String apiKeyString = (String) properties.get("apikey");
                    String baseUrlString = (String) properties.get("baseurl");
                    String isOnPremiseString = (String) properties.get("onpremise");
                    String usernameString = (String) properties.get("username");
                    String encryptedPasswordString = (String) properties.get("password");

                    if (apiKeyString != null) {
                        apiKey = Optional.of(apiKeyString.trim());
                    }
                    if (baseUrlString != null) {
                        String prefix = baseUrlString.trim();
                        if (prefix.endsWith("/")) {
                            prefix = prefix.substring(0, prefix.length() - 1);
                        }
                        baseUrl = Optional.of(prefix);
                    }
                    if (isOnPremiseString != null) {
                        isOnPremiseString = isOnPremiseString.trim().toLowerCase();
                        if ("true".equals(isOnPremiseString) || "1".equals(isOnPremiseString)
                                || "yes".equals(isOnPremiseString)) {
                            isOnPremise = Optional.of(true);
                        } else {
                            isOnPremise = Optional.of(false);
                        }
                    }
                    if (usernameString != null && encryptedPasswordString != null) {
                        String username = usernameString.trim();
                        String password = decrypt(encryptedPasswordString.trim());
                        mapmeterSaasCredentials = Optional.of(new MapmeterSaasCredentials(username,
                                password));
                    }
                }
            } catch (Throwable e) {
                throw closer.rethrow(e);
            } finally {
                closer.close();
            }
        } catch (IOException e) {
            LOGGER.log(Level.SEVERE,
                    "Failure reading: " + mapmeterConfigRelPath + " from data dir", e);
        }

        this.apiKeyProperties = apiKey;
        this.baseUrl = baseUrl;
        this.isOnPremise = isOnPremise;
        this.mapmeterSaasCredentials = mapmeterSaasCredentials;
    }

    private String decrypt(String encryptedPassword) {
        try {
            geoServerPBEPasswordEncoder.initialize(geoServerSecurityManager);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String password = geoServerPBEPasswordEncoder.decode(encryptedPassword);
        return password;
    }

    private String encrypt(String rawPassword) {
        try {
            geoServerPBEPasswordEncoder.initialize(geoServerSecurityManager);
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        String encryptedPassword = geoServerPBEPasswordEncoder.encodePassword(rawPassword,
                getSalt());
        return encryptedPassword;
    }

    private Object getSalt() {
        return new Date();
    }

    public Optional<File> findMapmeterConfigurationFile() throws IOException {
        File propFile = loader.find(mapmeterConfigRelPath);
        if (propFile == null) {
            LOGGER.info("No mapmeter properties file in data dir. Expected data dir location: "
                    + mapmeterConfigRelPath);
            return Optional.absent();
        } else {
            return Optional.of(propFile);
        }
    }

    private Optional<String> maybeUrlPrefix(final String suffix) {
        return baseUrl.transform(new Function<String, String>() {
            @Override
            public String apply(String base) {
                return base + suffix;
            }
        });
    }

    private String defaultUrlPrefix(String suffix) {
        return defaultBaseUrl + suffix;
    }

    public String getBaseUrl() {
        return baseUrl.or(defaultBaseUrl);
    }

    public String getStorageUrl() {
        return maybeUrlPrefix(storageSuffix).or(defaultUrlPrefix(storageSuffix));
    }

    public String getCheckUrl() {
        return maybeUrlPrefix(checkSuffix).or(defaultUrlPrefix(checkSuffix));
    }

    public String getSystemUpdateUrl() {
        return maybeUrlPrefix(systemUpdateSuffix).or(defaultUrlPrefix(systemUpdateSuffix));
    }

    public Optional<String> getApiKey() {
        return apiKeyOverride.or(apiKeyProperties);
    }

    public void setApiKey(String apiKey) {
        this.apiKeyProperties = Optional.of(apiKey);
    }

    public void save() throws IOException {
        Properties properties = new Properties();
        // it's possible that the api key is coming from an environment variable or servlet context (web.xml)
        // only set the api key if it's meant to be set in the properties file
        if (apiKeyProperties.isPresent()) {
            properties.setProperty("apikey", apiKeyProperties.get());
        }
        if (baseUrl.isPresent()) {
            properties.setProperty("baseurl", baseUrl.get());
        }
        if (isOnPremise.isPresent()) {
            properties.setProperty("onpremise", "" + isOnPremise.get());
        }
        if (mapmeterSaasCredentials.isPresent()) {
            MapmeterSaasCredentials creds = mapmeterSaasCredentials.get();
            String encryptedPassword = encrypt(creds.getPassword());
            properties.setProperty("username", creds.getUsername());
            properties.setProperty("password", encryptedPassword);
        }

        File propFile = null;
        Optional<File> maybePropFile = findMapmeterConfigurationFile();
        if (maybePropFile.isPresent()) {
            propFile = maybePropFile.get();
        } else {
            LOGGER.info("Creating new mapmeter properties config: " + mapmeterConfigRelPath);
            propFile = loader.createFile(mapmeterConfigRelPath);
        }

        Closer closer = Closer.create();
        try {
            BufferedWriter out = closer.register(Files.newWriter(propFile, Charsets.UTF_8));
            properties.store(out, null);
        } catch (Throwable e) {
            throw closer.rethrow(e);
        } finally {
            closer.close();
        }
    }

    public boolean isApiKeyOverridden() {
        return apiKeyOverride.isPresent();
    }

    public void setBaseUrl(String baseUrl) {
        this.baseUrl = Optional.of(baseUrl);
    }

    public boolean getIsOnPremise() {
        return isOnPremise.or(!defaultBaseUrl.equals(getBaseUrl()));
    }

    public void setIsOnPremise(boolean isOnPremise) {
        this.isOnPremise = Optional.of(isOnPremise);
    }

    public Optional<MapmeterSaasCredentials> getMapmeterSaasCredentials() {
        return mapmeterSaasCredentials;
    }

    public void setMapmeterSaasCredentials(MapmeterSaasCredentials mapmeterSaasCredentials) {
        this.mapmeterSaasCredentials = Optional.of(mapmeterSaasCredentials);
    }

    @Override
    public String toString() {
        return Objects.toStringHelper(MapmeterConfiguration.class)
                .add("apikeyOverride", apiKeyOverride.orNull())
                .add("apikey", apiKeyProperties.orNull())
                .add("baseurl", getBaseUrl())
                .add("onpremise", getIsOnPremise())
                .add("credentials",
                        mapmeterSaasCredentials.isPresent() ? mapmeterSaasCredentials.get()
                                .toString() : null)
                .toString();
    }

    // clear all configuration settings
    public void clearConfig() {
        apiKeyProperties = Optional.absent();
        baseUrl = Optional.absent();
        isOnPremise = Optional.absent();
        mapmeterSaasCredentials = Optional.absent();
    }

}
