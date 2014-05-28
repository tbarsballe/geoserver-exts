package com.boundlessgeo.ysld.parse;

import org.geotools.factory.CommonFactoryFinder;
import org.geotools.styling.StyleFactory;
import org.opengis.filter.FilterFactory;

public class Factory {
    StyleFactory style;
    FilterFactory filter;

    public Factory() {
        this(CommonFactoryFinder.getStyleFactory(), CommonFactoryFinder.getFilterFactory());
    }

    public Factory(StyleFactory style, FilterFactory filter) {
        this.style = style;
        this.filter = filter;
    }
}
