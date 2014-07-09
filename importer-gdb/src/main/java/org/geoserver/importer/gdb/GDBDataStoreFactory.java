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
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.geotools.data.DataStore;
import org.geotools.data.DataStoreFactorySpi;
import org.geotools.data.DataUtilities;
import org.geotools.data.ogr.OGR;
import org.geotools.data.ogr.OGRDataStore;
import org.geotools.data.ogr.jni.JniOGR;
import org.geotools.data.ogr.jni.JniOGRDataStoreFactory;
import org.geotools.util.KVP;
import org.geotools.util.logging.Logging;

public class GDBDataStoreFactory implements DataStoreFactorySpi {
    private static final Logger LOGGING =  Logging.getLogger("org.geoserver.importer.gdb");
    
    /** Choice of OpenFileGDB or FileGDB (which requires SDK) */
    private static final String DRIVER = "FileGDB";
    
    private static final String FILE_TYPE = "gdb";
    public static final String[] EXTENSIONS = new String[] { FILE_TYPE };
    public static final Param FILE_PARAM = new Param("file", File.class, FILE_TYPE + " file", false);
    public static final Param URL_PARAM = new Param("url", URL.class, FILE_TYPE + " file", false);
    public static final Param NAMESPACEP = new Param("namespace", URI.class,
            "uri to the namespace", false, null, new KVP(Param.LEVEL, "advanced"));
    public static final Param[] parametersInfo = new Param[] { FILE_PARAM };

    private boolean singleWarning = false;

    /** Constructor for Service Provider Interface */
    public GDBDataStoreFactory(){
    }
    
    @Override
    public String getDisplayName() {
        return DRIVER;
    }

    @Override
    public String getDescription() {
        return DRIVER + "Import";
    }

    @Override
    public Param[] getParametersInfo() {
        return parametersInfo;
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
                return file.getName().equalsIgnoreCase( FILE_TYPE ); 
            }
        } catch (IOException e) {
        }
        return false;
    }

    @Override
    public boolean isAvailable() {
        try {
            JniOGRDataStoreFactory factory = new JniOGRDataStoreFactory();
            boolean available = false;
            Throwable loadException = null;
            try {
                available = factory.isAvailable(false);
            }
            catch (Throwable t){
                loadException = t;
            }
            
            boolean checkDriver = false;
            
            Set<String> supported = Collections.emptySet();
            if( available ){
                supported = factory.getAvailableDrivers();
                for( String driver : supported ){
                    if( DRIVER.equals( driver )){
                        checkDriver = true;
                        break;
                    }
                }
            }
            // check environment
            String GDAL_DATA =  System.getenv("GDAL_DATA");
            
            // optional environment
            String DYLD_LIBRARY_PATH = System.getenv("DYLD_LIBRARY_PATH");
            String LD_LIBRARY_PATH = System.getenv("LD_LIBRARY_PATH");
            String GDAL_DRIVER_PATH = System.getenv("GDAL_DRIVER_PATH");
            String CPL_LOG = System.getenv("CPL_LOG");
            String CPL_LOGS_ERRORS = System.getenv("CPL_LOG_ERRORS");
            
            boolean checkEnv = true;
            
            if( GDAL_DATA != null && !GDAL_DATA.isEmpty() ){
                File gdalData = new File(GDAL_DATA);
                if( !gdalData.exists() ){
                    LOGGING.warning("GDAL_DATA directory does not exist");
                    checkEnv = false;
                }
                else if( !gdalData.isDirectory() ){
                    LOGGING.warning("GDAL_DATA is not a directory");
                    checkEnv = false;
                }
            }
                
            if(!singleWarning){
                singleWarning = true;
                Level level = (available && checkDriver && checkEnv) ? Level.FINE : Level.INFO;
                
                if( available ){
                    LOGGING.log(level, "OGR Envrionment: "+DRIVER+" supported");
                    if( checkDriver ){
                        LOGGING.log(level, "OGR Envrionment: "+DRIVER+" supported");
                    }
                    else {
                        SortedSet<String> sorted = new TreeSet<String>( supported );
                        LOGGING.log(Level.WARNING, "OGR Envrionment: "+DRIVER+" not supported: "+sorted);
                    }
                }
                else {
                    LOGGING.log(level, "OGR JniOGRDataStoreFactory load 'gdaljni' failed.",loadException);
                }
                LOGGING.log(level, "Environment variable GDAL_DATA="+GDAL_DATA);
                LOGGING.log(level, "Environment variable DYLD_LIBRARY_PATH="+DYLD_LIBRARY_PATH);
                LOGGING.log(level, "Environment variable LD_LIBRARY_PATH="+LD_LIBRARY_PATH);
                LOGGING.log(level, "Environment variable GDAL_DRIVER_PATH="+GDAL_DRIVER_PATH);
                LOGGING.log(level, "Environment variable CPL_LOG="+CPL_LOG);
                LOGGING.log(level, "Environment variable CPL_LOGS_ERRORS="+CPL_LOGS_ERRORS);
            }
            return available && checkDriver && checkEnv;
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
                    "Could not find file from params to access GDB");
        }
        URI namespace = (URI) NAMESPACEP.lookUp(params);
        return createDataStoreFromFile(file, namespace, params);
    }

    private DataStore createDataStoreFromFile(File file, URI namespace,
            Map<String, Serializable> params) throws IOException {
        OGR ogr = new JniOGR();
        if( file.getName().equalsIgnoreCase(FILE_TYPE)){
            file = file.getParentFile();
        }
    	return new OGRDataStore(file.getAbsolutePath(), DRIVER, namespace, ogr);
    }

    @Override
    public DataStore createNewDataStore(Map<String, Serializable> params) throws IOException {
        return createDataStore(params);
    }

}
