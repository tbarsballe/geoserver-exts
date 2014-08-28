package org.geogig.geoserver.web.data.store.geogig;

import java.io.IOException;
import java.io.Serializable;
import java.util.List;

public interface GeoGigConnector extends Serializable {
    List<String> listBranches(Serializable repository) throws IOException;
}
