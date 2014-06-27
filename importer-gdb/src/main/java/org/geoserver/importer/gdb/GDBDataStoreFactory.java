/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.gdb;

import java.awt.RenderingHints.Key;
import java.io.File;
import java.io.IOException;
import java.io.Serializable;
import java.net.URI;
import java.net.URL;
import java.util.Collections;
import java.util.Map;

import org.apache.commons.io.FilenameUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataUtilities;
import org.geotools.data.ogr.OGR;
import org.geotools.data.ogr.OGRDataStore;
import org.geotools.data.ogr.bridj.BridjOGR;
import org.geotools.data.ogr.bridj.BridjOGRDataStoreFactory;
import org.geotools.util.KVP;

public class GDBDataStoreFactory implements DataStoreFactorySpi {

    private static final String FILE_TYPE = "gdb";
    public static final String[] EXTENSIONS = new String[] { "." + FILE_TYPE };
    public static final Param FILE_PARAM = new Param("file", File.class, FILE_TYPE + " file", false);
    public static final Param URL_PARAM = new Param("url", URL.class, FILE_TYPE + " file", false);
    public static final Param NAMESPACEP = new Param("namespace", URI.class,
            "uri to the namespace", false, null, new KVP(Param.LEVEL, "advanced"));
    public static final Param[] parametersInfo = new Param[] { FILE_PARAM };

    @Override
    public String getDisplayName() {
        return "FileGDB";
    }

    @Override
    public String getDescription() {
        return "FileGDB Import";
    }

    @Override
    public Param[] getParametersInfo() {
        return parametersInfo;
    }

    private boolean canProcessExtension(String filename) {
        String extension = FilenameUtils.getExtension(filename);
        return FILE_TYPE.equalsIgnoreCase(extension);
    }

    private File fileFromParams(Map<String, Serializable> params) throws IOException {
        File file = (File) FILE_PARAM.lookUp(params);
        if (file != null) {
            return file;
        }
        URL url = (URL) URL_PARAM.lookUp(params);
        if (url != null) {
            return DataUtilities.urlToFile(url);
        }
        return null;
    }

    @Override
    public boolean canProcess(Map<String, Serializable> params) {
        try {
            File file = fileFromParams(params);
            if (file != null) {
                return canProcessExtension(file.getPath());
            }
        } catch (IOException e) {
        }
        return false;
    }

    @Override
    public boolean isAvailable() {
        try {
            BridjOGRDataStoreFactory factory = new BridjOGRDataStoreFactory();
            return factory.isAvailable(true);
        } catch (Exception e) {
            return false;
        }
    }

    @Override
    public Map<Key, ?> getImplementationHints() {
        return Collections.emptyMap();
    }

    public DataStore createDataStoreFromFile(File file) throws IOException {
        return createDataStoreFromFile(file, null);
    }

    public DataStore createDataStoreFromFile(File file, URI namespace) throws IOException {
        if (file == null) {
            throw new IllegalArgumentException("Cannot create store from null file");
        } else if (!file.exists()) {
            throw new IllegalArgumentException("Cannot create store with file that does not exist");
        }
        Map<String, Serializable> noParams = Collections.emptyMap();
        return createDataStoreFromFile(file, namespace, noParams);
    }

    @Override
    public DataStore createDataStore(Map<String, Serializable> params) throws IOException {
        File file = fileFromParams(params);
        if (file == null) {
            throw new IllegalArgumentException(
                    "Could not find file from params to create csv data store");
        }
        URI namespace = (URI) NAMESPACEP.lookUp(params);
        return createDataStoreFromFile(file, namespace, params);
    }

    private DataStore createDataStoreFromFile(File file, URI namespace,
            Map<String, Serializable> params) throws IOException {
        OGR ogr = new BridjOGR();
    	return new OGRDataStore(file.getAbsolutePath(), "FileGDB", namespace, ogr);
    }

    @Override
    public DataStore createNewDataStore(Map<String, Serializable> params) throws IOException {
        return createDataStore(params);
    }

    public String[] getFileExtensions() {
        return EXTENSIONS;
    }

    public boolean canProcess(URL url) {
        return canProcessExtension(DataUtilities.urlToFile(url).toString());
    }

}
