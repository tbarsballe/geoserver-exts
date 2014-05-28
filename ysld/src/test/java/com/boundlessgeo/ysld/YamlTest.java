package com.boundlessgeo.ysld;

import org.junit.Test;
import org.yaml.snakeyaml.DumperOptions;
import org.yaml.snakeyaml.Yaml;
import org.yaml.snakeyaml.emitter.Emitter;
import org.yaml.snakeyaml.events.*;
import org.yaml.snakeyaml.nodes.ScalarNode;
import org.yaml.snakeyaml.nodes.Tag;
import org.yaml.snakeyaml.resolver.Resolver;
import org.yaml.snakeyaml.serializer.Serializer;

import java.io.*;
import java.util.*;

import static org.junit.Assert.assertEquals;

public class YamlTest {

    @Test
    public void testParse() {
        Yaml yaml = new Yaml();
        for (Event evt : yaml.parse(yaml())) {
            System.out.println(evt);
        }
    }

    @Test
    public void testEncode() throws Exception {
        Writer w = new OutputStreamWriter(System.out);
        DumperOptions dumpOpts = new DumperOptions();
        dumpOpts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
        Emitter e = new Emitter(w, dumpOpts);
        e.emit(new StreamStartEvent(null, null));
        e.emit(new DocumentStartEvent(null, null, false, null, null));
        e.emit(new MappingStartEvent(null, null, true, null, null, false));
        e.emit(new ScalarEvent(null, null, new ImplicitTuple(true, false), "hi", null, null, null ));
        e.emit(new ScalarEvent(null, null, new ImplicitTuple(true, false), "there", null, null, null ));
        e.emit(new ScalarEvent(null, null, new ImplicitTuple(true, false), "foo", null, null, null ));
        e.emit(new SequenceStartEvent(null, null, true, null, null, false));
        e.emit(new SequenceEndEvent(null, null));
        e.emit(new MappingEndEvent(null, null));
        e.emit(new DocumentEndEvent(null, null, false));
        e.emit(new StreamEndEvent(null, null));
//        List l = new ArrayList();
//        l.add(1);
//        l.add(2);
//
//        Map map = new LinkedHashMap();
//        map.put("name", "foo");
//
//        Writer w = new OutputStreamWriter(System.out);
//        DumperOptions dumpOpts = new DumperOptions();
//        dumpOpts.setDefaultFlowStyle(DumperOptions.FlowStyle.BLOCK);
//
//        Yaml yaml = new Yaml(dumpOpts);
//        yaml.dumpAll(Arrays.asList(map).iterator(), w);

    }

    Reader yaml() {
        return new InputStreamReader(YsldTests.class.getResourceAsStream("point.yml"));
    }

}
