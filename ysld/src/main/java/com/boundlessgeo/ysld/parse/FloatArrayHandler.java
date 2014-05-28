package com.boundlessgeo.ysld.parse;

import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.events.SequenceEndEvent;
import org.yaml.snakeyaml.events.SequenceStartEvent;

import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

public abstract class FloatArrayHandler extends YsldParseHandler {

    List<Float> list;

    public FloatArrayHandler(Factory factory) {
        super(factory);
    }

    @Override
    public void sequence(SequenceStartEvent evt, Deque<YamlParseHandler> handlers) {
        super.sequence(evt, handlers);
        list = new ArrayList<Float>();
    }

    @Override
    public void scalar(ScalarEvent evt, Deque<YamlParseHandler> handlers) {
        list.add(Float.parseFloat(evt.getValue()));
    }

    @Override
    public void endSequence(SequenceEndEvent evt, Deque<YamlParseHandler> handlers) {
        super.endSequence(evt, handlers);
        float[] array = new float[list.size()];
        for (int i = 0; i < list.size(); i++) {
            array[i] = list.get(i).floatValue();
        }
        array(array);
        handlers.pop();
    }

    protected abstract void array(float[] array);
}
