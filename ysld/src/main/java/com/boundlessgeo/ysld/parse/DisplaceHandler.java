package com.boundlessgeo.ysld.parse;

import com.boundlessgeo.ysld.Tuple;
import org.geotools.styling.Displacement;
import org.opengis.filter.expression.Expression;
import org.yaml.snakeyaml.events.Event;

public abstract class DisplaceHandler extends ValueHandler {

    protected DisplaceHandler(Factory factory) {
        super(factory);
    }

    @Override
    protected void value(String value, Event event) {
        Tuple t = Tuple.parse(value, event,
            String.format("Bad displace: '%s', must be of form (<x>;<y>)"));

        Expression x = t.first != null ? ExpressionHandler.parse(value, event) :
                factory.filter.literal(0);
        Expression y = t.first != null ? ExpressionHandler.parse(value, event) :
                factory.filter.literal(0);
        displace(factory.style.createDisplacement(x, y));
    }

    protected abstract void displace(Displacement displacement);
}
