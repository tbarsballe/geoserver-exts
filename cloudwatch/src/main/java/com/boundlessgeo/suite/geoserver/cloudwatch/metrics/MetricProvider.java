/* Copyright (c) 2014 - 2015 Boundless http://boundlessgeo.com - all rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package com.boundlessgeo.suite.geoserver.cloudwatch.metrics;

import com.amazonaws.services.cloudwatch.model.MetricDatum;
import java.util.Collection;

/**
 *
 * @author tingold@boundlessgeo.com
 * @author tbattle@boundlessgeo.com
 *
 * Simple interface for classes which wish to provide metrics to Cloudwatch
 */
public interface MetricProvider
{

    public Collection<MetricDatum> getMetrics();
    public Boolean getEnabled();
}
