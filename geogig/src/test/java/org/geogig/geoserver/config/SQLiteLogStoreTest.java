package org.geogig.geoserver.config;

import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

public class SQLiteLogStoreTest extends AbstractLogStoreTest {

    @Test
    public void testCreatesDefault() throws Exception {
        logStore.afterPropertiesSet();
        assertTrue(new File(tmpDir.newFolder("geogig", "config", "security"), "securitylogs.db")
                .exists());
    }
}
