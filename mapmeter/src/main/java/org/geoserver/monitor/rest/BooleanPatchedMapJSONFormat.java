package org.geoserver.monitor.rest;

import org.geoserver.rest.format.MapJSONFormat;

public class BooleanPatchedMapJSONFormat extends MapJSONFormat {

    @Override
    public Object toJSONObject(Object obj) {
        if (obj instanceof Boolean) {
            return obj;
        }
        return super.toJSONObject(obj);
    }

}
