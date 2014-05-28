package com.boundlessgeo.ysld.parse;

import org.geotools.filter.text.cql2.CQLException;
import org.geotools.filter.text.ecql.ECQL;
import org.opengis.filter.expression.Expression;
import org.yaml.snakeyaml.events.Event;

public abstract class ExpressionHandler extends ValueHandler {

    static Expression parse(String value, Event evt) {
        try {
            return ECQL.toExpression(value);
        } catch (CQLException e) {
            throw new ParseException("Bad expression: "+value, evt, e);
        }
    }

    protected ExpressionHandler(Factory factory) {
        super(factory);
    }

    @Override
    protected void value(String value, Event evt) {
        expression(parse(value, evt));
    }

    protected abstract void expression(Expression expr);
}
