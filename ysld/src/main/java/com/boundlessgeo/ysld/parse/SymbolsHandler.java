package com.boundlessgeo.ysld.parse;

import org.geotools.styling.ExternalGraphic;
import org.geotools.styling.Graphic;
import org.geotools.styling.Mark;
import org.yaml.snakeyaml.events.ScalarEvent;

import java.util.Deque;

public class SymbolsHandler extends YsldParseHandler {

    Graphic graphic;

    SymbolsHandler(Graphic graphic, Factory factory) {
        super(factory);
        this.graphic = graphic;
    }

    @Override
    public void scalar(ScalarEvent evt, Deque<YamlParseHandler> handlers) {
        String val = evt.getValue();
        if ("mark".equals(val)) {
            handlers.push(new MarkHandler(factory) {
                @Override
                protected void mark(Mark mark) {
                    graphic.graphicalSymbols().add(mark);
                }
            });
        }
        else if ("external".equals(val)) {
            handlers.push(new ExternalGraphicHandler(factory) {
                @Override
                protected void externalGraphic(ExternalGraphic externalGraphic) {
                    graphic.graphicalSymbols().add(externalGraphic);
                }
            });
        }

    }
}
