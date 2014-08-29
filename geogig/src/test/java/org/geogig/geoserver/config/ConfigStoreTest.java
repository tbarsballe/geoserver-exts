package org.geogig.geoserver.config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import jline.internal.InputStreamReader;

import org.custommonkey.xmlunit.XMLAssert;
import org.custommonkey.xmlunit.XMLUnit;
import org.geoserver.platform.resource.FileSystemResourceStore;
import org.geoserver.platform.resource.Resource;
import org.geoserver.platform.resource.ResourceStore;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.ExpectedException;
import org.junit.rules.TemporaryFolder;

import com.google.common.base.Charsets;
import com.google.common.collect.Sets;
import com.google.common.io.Files;

public class ConfigStoreTest {

    @Rule
    public TemporaryFolder tempFolder = new TemporaryFolder();

    @Rule
    public ExpectedException thrown = ExpectedException.none();

    private ResourceStore dataDir;

    private ConfigStore store;

    @Before
    public void before() {
        File root = tempFolder.getRoot();
        dataDir = new FileSystemResourceStore(root);
        store = new ConfigStore(dataDir);
        XMLUnit.setIgnoreWhitespace(true);
    }

    @Test
    public void saveNull() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("null RepositoryInfo");
        store.save(null);
    }

    @Test
    public void saveNoId() throws Exception {
        RepositoryInfo info = new RepositoryInfo();
        info.setName("name");
        info.setParentDirectory("/path");
        info.setId(null);
        assertNull(info.getId());
        store.save(info);
        assertNotNull(info.getId());
    }

    @Test
    public void save() throws Exception {
        RepositoryInfo info = new RepositoryInfo("uuid");
        info.setName("repo");
        info.setParentDirectory("/home/test");
        store.save(info);

        String path = ConfigStore.path(info.getId());
        Resource resource = dataDir.get(path);
        assertTrue(resource.file().exists());
        // Files.copy(resource.file(), System.err);
        String expected = "<RepositoryInfo>"//
                + "<id>uuid</id>"//
                + "<parentDirectory>/home/test</parentDirectory>"//
                + "<name>repo</name>"//
                + "</RepositoryInfo>";

        XMLAssert.assertXMLEqual(new StringReader(expected), new InputStreamReader(resource.in(),
                Charsets.UTF_8));
    }

    @Test
    public void loadNull() throws Exception {
        thrown.expect(NullPointerException.class);
        thrown.expectMessage("null id");
        store.load(null);
    }

    @Test
    public void load() throws Exception {
        String expected = "<RepositoryInfo>"//
                + "<id>uuid</id>"//
                + "<parentDirectory>/home/test</parentDirectory>"//
                + "<name>repo</name>"//
                + "</RepositoryInfo>";

        String path = ConfigStore.path("uuid");
        Resource resource = dataDir.get(path);
        Files.write(expected, resource.file(), Charsets.UTF_8);

        RepositoryInfo info = store.load("uuid");
        assertNotNull(info);
        assertEquals("uuid", info.getId());
        assertEquals("repo", info.getName());
        assertEquals("/home/test", info.getParentDirectory());
    }

    @Test
    public void loadMalformed() throws Exception {
        // this xml has a missing > character at the end
        String expected = "<RepositoryInfo>"//
                + "<id>uuid</id>"//
                + "<parentDirectory>/home/test</parentDirectory>"//
                + "<name>repo</name>"//
                + "</RepositoryInfo";

        String path = ConfigStore.path("uuid");
        Resource resource = dataDir.get(path);
        Files.write(expected, resource.file(), Charsets.UTF_8);
        thrown.expect(IOException.class);
        thrown.expectMessage("Unable to load");
        store.load("uuid");
    }

    @Test
    public void getRepositoriesEmpty() {
        List<RepositoryInfo> all = store.getRepositories();
        assertNotNull(all);
        assertTrue(all.isEmpty());
    }

    @Test
    public void getRepositories() {
        store.save(dummy(1));
        store.save(dummy(2));
        store.save(dummy(3));
        store.save(dummy(4));
        List<RepositoryInfo> all = store.getRepositories();
        assertNotNull(all);
        assertEquals(4, all.size());
        Set<RepositoryInfo> expected = Sets.newHashSet(dummy(1), dummy(2), dummy(3), dummy(4));
        assertEquals(expected, new HashSet<RepositoryInfo>(all));
    }

    @Test
    public void getRepositoriesIgnoresMalformed() throws Exception {
        store.save(dummy(1));
        store.save(dummy(2));
        store.save(dummy(3));
        RepositoryInfo dummy = dummy(4);
        store.save(dummy);
        Resource breakIt = dataDir.get(ConfigStore.path(dummy.getId()));
        byte[] bytes = Files.toByteArray(breakIt.file());
        byte[] from = new byte[bytes.length - 5];
        System.arraycopy(bytes, 0, from, 0, from.length);
        Files.write(from, breakIt.file());

        List<RepositoryInfo> all = store.getRepositories();
        assertNotNull(all);
        assertEquals(3, all.size());
        Set<RepositoryInfo> expected = Sets.newHashSet(dummy(1), dummy(2), dummy(3));
        assertEquals(expected, new HashSet<RepositoryInfo>(all));
    }

    private RepositoryInfo dummy(int i) {
        RepositoryInfo info = new RepositoryInfo();
        info.setId(String.valueOf(i));
        info.setName("name-" + i);
        info.setParentDirectory("parent/directory/" + i);
        return info;
    }
}
