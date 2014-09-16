/* Copyright (c) 2001 - 2013 OpenPlans - www.openplans.org. All rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */
package org.geogig.geoserver.web;

import org.geogig.geoserver.web.security.SecurityLogsPanel;
import org.geoserver.web.GeoServerSecuredPage;

/**
 */
public class RemotesPage extends GeoServerSecuredPage {

    public RemotesPage() {
        add(new SecurityLogsPanel("table"));
    }
}
