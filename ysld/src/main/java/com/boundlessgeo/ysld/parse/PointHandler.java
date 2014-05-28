package com.boundlessgeo.ysld.parse;

import org.geotools.styling.*;
import org.yaml.snakeyaml.events.MappingEndEvent;
import org.yaml.snakeyaml.events.MappingStartEvent;

import java.util.Deque;

public class PointHandler extends YsldParseHandler {

    PointSymbolizer sym;

    public PointHandler(Rule rule, Factory factory) {
        super(factory);
        rule.symbolizers().add(sym = factory.style.createPointSymbolizer());
    }

    @Override
    public void mapping(MappingStartEvent evt, Deque<YamlParseHandler> handlers) {
        super.mapping(evt, handlers);
        handlers.push(new GraphicHandler(factory, sym.getGraphic()));
    }

    @Override
    public void endMapping(MappingEndEvent evt, Deque<YamlParseHandler> handlers) {
        super.endMapping(evt, handlers);
        handlers.pop();
    }
}
