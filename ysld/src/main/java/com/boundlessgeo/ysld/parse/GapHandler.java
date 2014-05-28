package com.boundlessgeo.ysld.parse;

import com.boundlessgeo.ysld.Tuple;
import org.opengis.filter.expression.Expression;
import org.yaml.snakeyaml.events.Event;

public abstract class GapHandler extends ValueHandler  {

    protected GapHandler(Factory factory) {
        super(factory);
    }

    @Override
    protected void value(String value, Event event) {
        Tuple t = Tuple.parse(value, event,
            "Bad gap: "+value+", must be of form: (<gap>;[<initial>])");

        Expression gap = t.first != null ? ExpressionHandler.parse(t.first, event) : null;
        Expression init = t.second != null ? ExpressionHandler.parse(t.second, event) : null;
              
        gap(gap, init);
    }

    protected abstract void gap(Expression gap, Expression init);
}
