/* Copyright (c) 2014 - 2015 Boundless http://boundlessgeo.com - all rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package com.boundlessgeo.suite.geoserver.cloudwatch.metrics;

import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.boundlessgeo.suite.geoserver.cloudwatch.aws.MetricDatumEncoder;
import java.util.ArrayList;
import java.util.Collection;
//import org.slf4j.Logger;
//import org.slf4j.LoggerFactory;

/**
 *
 * @author tingold@boundlessgeo.com
 */
public class MonitorMetricProvider implements MetricProvider{

    private MetricDatumEncoder encoder;
    
    //private static final Logger logger = LoggerFactory.getLogger(MonitorMetricProvider.class);

    
    @Override
    public Collection<MetricDatum> getMetrics() {
        

        return new ArrayList<MetricDatum>();
    }

    /**
     * @return the encoder
     */
    public MetricDatumEncoder getEncoder() {
        return encoder;
    }

    /**
     * @param encoder the encoder to set
     */
    public void setEncoder(MetricDatumEncoder encoder) {
        this.encoder = encoder;
    }

    
}
