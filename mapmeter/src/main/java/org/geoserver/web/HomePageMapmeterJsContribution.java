package org.geoserver.web;

import java.util.Set;

import org.apache.wicket.markup.html.WebPage;
import org.geoserver.security.GeoServerSecurityManager;

public class HomePageMapmeterJsContribution extends ClassLimitingHeaderContribution {

    private final GeoServerSecurityManager geoServerSecurityManager;

    public HomePageMapmeterJsContribution(GeoServerSecurityManager geoServerSecurityManager,
            Set<Class<?>> pagesToApplyTo) {
        super(pagesToApplyTo);
        this.geoServerSecurityManager = geoServerSecurityManager;
    }

    @Override
    public boolean appliesTo(WebPage page) {
        // we only want to apply this to the home page if we have the admin role
        return super.appliesTo(page) && geoServerSecurityManager.checkAuthenticationForAdminRole();
    }

}
