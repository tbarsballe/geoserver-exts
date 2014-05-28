package com.boundlessgeo.ysld.parse;

import org.geotools.styling.ExternalGraphic;

public abstract class ExternalGraphicHandler extends YsldParseHandler {
    public ExternalGraphicHandler(Factory factory) {
        super(factory);
    }

    protected abstract void externalGraphic(ExternalGraphic externalGraphic);
}
