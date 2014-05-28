package com.boundlessgeo.ysld.parse;

import com.boundlessgeo.ysld.Tuple;
import org.geotools.styling.FeatureTypeStyle;
import org.geotools.styling.Rule;
import org.opengis.filter.Filter;
import org.yaml.snakeyaml.events.Event;
import org.yaml.snakeyaml.events.MappingStartEvent;
import org.yaml.snakeyaml.events.ScalarEvent;
import org.yaml.snakeyaml.events.SequenceEndEvent;

import java.util.Deque;

public class RuleHandler extends YsldParseHandler {

    FeatureTypeStyle featureStyle;
    Rule rule;

    public RuleHandler(FeatureTypeStyle featureStyle, Factory factory) {
        super(factory);
        this.featureStyle = featureStyle;
    }

    @Override
    public void mapping(MappingStartEvent evt, Deque<YamlParseHandler> handlers) {
        featureStyle.rules().add(rule = factory.style.createRule());
    }

    @Override
    public void scalar(ScalarEvent evt, Deque<YamlParseHandler> handlers) {
        String val = evt.getValue();
        if ("name".equals(val)) {
            handlers.push(new ValueHandler(factory) {
                @Override
                protected void value(String value, Event evt) {
                    rule.setName(value);
                }
            });
        }
        else if ("title".equals(val)) {
            handlers.push(new ValueHandler(factory) {
                @Override
                protected void value(String value, Event evt) {
                    rule.setTitle(value);
                }
            });
        }
        else if ("abstract".equals(val)) {
            handlers.push(new ValueHandler(factory) {
                @Override
                protected void value(String value, Event evt) {
                    rule.setAbstract(value);
                }
            });
        }
        else if ("filter".equals(val)) {
            handlers.push(new FilterHandler(factory) {
                @Override
                protected void filter(Filter filter) {
                    rule.setFilter(filter);
                }
            });
        }
        else if ("else".equals(val)) {
            handlers.push(new ValueHandler(factory) {
                @Override
                protected void value(String value, Event evt) {
                    rule.setElseFilter(Boolean.valueOf(value));
                }
            });
        }
        else if ("scale".equals(val)) {
            Tuple t = Tuple.parse(val, evt,
                String.format("Bad scale value: '%s', must be of form (<min>;<max>)", val));

            if (t.first != null) {
                rule.setMinScaleDenominator(Double.parseDouble(t.first));
            }
            if (t.second != null) {
                rule.setMaxScaleDenominator(Double.parseDouble(t.second));
            }
        }
        else if ("symbolizers".equals(val)) {
            handlers.push(new SymbolizerHandler(rule, factory));
        }
    }

    @Override
    public void endSequence(SequenceEndEvent evt, Deque<YamlParseHandler> handlers) {
        handlers.pop();
    }
}
