package com.boundlessgeo.ysld.parse;

import org.geotools.styling.StyledLayerDescriptor;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.events.*;
import org.yaml.snakeyaml.reader.UnicodeReader;

import java.io.IOException;
import java.io.InputStream;
import java.io.Reader;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.Iterator;

/**
 * Parses a Yaml/Ysld stream into GeoTools style objects.
 */
public class YsldParser extends YamlParser {

    public YsldParser(InputStream ysld) throws IOException {
        super(ysld);
    }

    public YsldParser(Reader reader) throws IOException {
        super(reader);
    }

    public StyledLayerDescriptor parse() throws IOException {
        RootHandler root = new RootHandler();
        doParse(root);
        return root.sld();
    }
}
