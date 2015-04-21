package org.geogig.geoserver.config;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.apache.commons.io.FileUtils;
import org.junit.Test;

public class SQLiteLogStoreTest extends AbstractLogStoreTest {

    @Test
    public void testCreatesDefault() throws Exception {
        File file = FileUtils.getFile(tmpDir.getRoot(), "geogig", "config", "security", "securitylogs.db");
        logStore.afterPropertiesSet();
        assertTrue(file.exists());
    }
}
