package com.boundlessgeo.ysld.parse;

import org.geotools.styling.AnchorPoint;
import org.geotools.styling.Displacement;
import org.geotools.styling.Graphic;
import org.opengis.filter.expression.Expression;
import org.yaml.snakeyaml.events.MappingEndEvent;
import org.yaml.snakeyaml.events.ScalarEvent;

import java.util.Deque;

public class GraphicHandler extends YsldParseHandler {

    Graphic g;

    GraphicHandler(Factory factory) {
        this(factory, factory.style.createDefaultGraphic());
    }

    GraphicHandler(Factory factory, Graphic g) {
        super(factory);
        this.g = g;
    }

    @Override
    public void scalar(ScalarEvent evt, Deque<YamlParseHandler> handlers) {
        String val = evt.getValue();

        if ("anchor".equals(val)) {
            handlers.push(new AnchorHandler(factory) {
                protected void anchor(AnchorPoint anchor) {
                    g.setAnchorPoint(anchor);
                }
            });
        }
        if ("opacity".equals(val)) {
            handlers.push(new ExpressionHandler(factory) {
                protected void expression(Expression expr) {
                    g.setOpacity(expr);
                }
            });
        }
        if ("size".equals(val)) {
            handlers.push(new ExpressionHandler(factory) {
                protected void expression(Expression expr) {

                }
            });
        }
        if ("displace".equals(val)) {
            handlers.push(new DisplaceHandler(factory) {
                @Override
                protected void displace(Displacement displacement) {
                    g.setDisplacement(displacement);
                }
            });
        }
        if ("rotate".equals(val)) {
            handlers.push(new ExpressionHandler(factory) {
                protected void expression(Expression expr) {
                    g.setRotation(expr);
                }
            });
        }
        if ("gap".equals(val)) {
            handlers.push(new GapHandler(factory) {
                @Override
                protected void gap(Expression gap, Expression init) {
                    if (gap != null) {
                        g.setGap(gap);
                    }
                    if (init != null) {
                        g.setInitialGap(init);
                    }
                }
            });
        }
        if ("symbols".equals(val)) {
            handlers.push(new SymbolsHandler(g, factory));
        }
    }

    @Override
    public void endMapping(MappingEndEvent evt, Deque<YamlParseHandler> handlers) {
        super.endMapping(evt, handlers);
        graphic(g);
        handlers.pop();
    }

    protected void graphic(Graphic graphic) {
    }
}
