/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geoserver.importer.gdb;

import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.Serializable;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.commons.io.IOUtils;
import org.geotools.data.DataStore;
import org.geotools.data.DataUtilities;
import org.geotools.util.KVP;
import org.junit.Before;
import org.junit.Test;

public class GDBDataStoreFactoryTest {

    private GDBDataStoreFactory factory;

    private URL locationsResource;

    private File file;

    @Before
    public void setUp() throws Exception {
        factory = new GDBDataStoreFactory();
        File archive = testData("locations.zip");
        
        File unpack = File.createTempFile("importer", "data", new File("target"));
        unpack.delete();
        unpack.mkdirs();
        unpack( archive, unpack);
        locationsResource = DataUtilities.fileToURL(unpack);

        assert locationsResource != null : "Could not find locations.gdb resource";
        file = DataUtilities.urlToFile(locationsResource);
    }
    @Test
    public void testIsAvailable() throws Exception {
        boolean available = factory.isAvailable();
        assertTrue("avaialble", available);
    }
    
    @SuppressWarnings("unchecked")
    @Test
    public void testCreateDataStoreURL() throws MalformedURLException, IOException {
        Map<String, Serializable> params = (Map) new KVP( GDBDataStoreFactory.URL_PARAM.key, locationsResource );
        DataStore dataStore = factory.createDataStore(params);
        assertNotNull("Failure creating data store", dataStore);
    }
    
    private static File testData(String path ) throws IOException {
        URL url = GDBDataStoreFactory.class.getResource("test-data/"+path);
        if( url == null ) {
            throw new FileNotFoundException("Could not find locations.zip");
        }
        File file = new File(URLDecoder.decode(url.getPath(), "UTF-8"));
        if (!file.exists()) {
            throw new FileNotFoundException("Can not locate test-data for \"" + path + '"');
        }
        return file;
    }
    //
    // Utility Methods
    //
    private static void unpackFile(ZipInputStream in, File outdir, String name) throws IOException {
        File file = new File(outdir, name);
        FileOutputStream out = new FileOutputStream(file);
        try {
            IOUtils.copy(in, out);
        } finally {
            out.close();
        }
    }

    private static boolean mkdirs(File outdir, String path) {
        if( path == null ) return false;
        File directory = new File(outdir, path);
        if (!directory.exists()) {
            return directory.mkdirs();
        }
        return false;
    }

    private static String directoryName(String name) {
        int s = name.lastIndexOf(File.separatorChar);
        return s == -1 ? null : name.substring(0, s);
    }

    /***
     * Unpack archive to directory (maintaining directory structure).
     * @param archive
     * @param directory
     */
    public static void unpack(File archive, File directory) throws IOException {
        // see http://stackoverflow.com/questions/10633595/java-zip-how-to-unzip-folder
        ZipInputStream zip = new ZipInputStream(new FileInputStream(archive));
        try {
            for( ZipEntry entry; (entry = zip.getNextEntry()) != null; ) {
                String name = entry.getName();
                if (entry.isDirectory()) {
                    mkdirs(directory, name);
                    continue;
                }
                /*
                 * this part is necessary because file entry can come before directory entry where is file located i.e.: /foo/foo.txt /foo/
                 */
                String dir = directoryName(name);
                if (dir != null){
                    mkdirs(directory, dir);
                }
                unpackFile(zip, directory, name);
            }
            zip.close();
        }
        finally {
            zip.close();
        }
    }
}
