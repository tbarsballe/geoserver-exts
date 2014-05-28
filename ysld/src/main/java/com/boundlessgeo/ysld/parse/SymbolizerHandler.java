package com.boundlessgeo.ysld.parse;

import org.geotools.styling.Rule;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.events.SequenceEndEvent;

import java.util.Deque;

public class SymbolizerHandler extends YsldParseHandler {

    Rule rule;

    public SymbolizerHandler(Rule rule, Factory factory) {
        super(factory);
        this.rule = rule;
    }

    @Override
    public void scalar(ScalarEvent evt, Deque<YamlParseHandler> handlers) {
        String val = evt.getValue();
        if ("point".equals(val)) {
            handlers.push(new PointHandler(rule, factory));
        }
        else if ("line".equals(val)) {

        }
        else if ("polygon".equals(val)) {

        }
        else if ("text".equals(val)) {

        }
        else if ("raster".equals(val)) {

        }
    }

    @Override
    public void endSequence(SequenceEndEvent evt, Deque<YamlParseHandler> handlers) {
        handlers.pop();
    }
}
