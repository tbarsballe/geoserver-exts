package com.boundlessgeo.ysld.transform.ysld;

import com.boundlessgeo.ysld.parse.YamlParseHandler;
import org.xml.sax.ContentHandler;
import org.yaml.snakeyaml.events.*;

import java.util.Deque;

public class YsldParseHandler extends YamlParseHandler {

    protected ContentHandler xml;

    public YsldParseHandler(ContentHandler xml) {
        this.xml = xml;
    }
}
