/* Copyright (c) 2014 - 2015 Boundless http://boundlessgeo.com - all rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package com.boundlessgeo.suite.geoserver.intruments;

import com.boundlessgeo.suite.geoserver.cloudwatch.aws.MetricDatumEncoder;
import com.boundlessgeo.suite.geoserver.cloudwatch.metrics.MetricProvider;
import com.codahale.metrics.MetricRegistry;
import org.aopalliance.intercept.MethodInterceptor;

/**
 *
 * @author tingold@boundlessgeo.com
 */
public abstract class Instrument implements MethodInterceptor, MetricProvider{
    

    protected MetricDatumEncoder encoder;
    
    
    protected MetricRegistry metricRegistry;
            
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

    /**
     * @return the metricRegistry
     */
    public MetricRegistry getMetricRegistry() {
        return metricRegistry;
    }

    /**
     * @param metricRegistry the metricRegistry to set
     */
    public void setMetricRegistry(MetricRegistry metricRegistry) {
        this.metricRegistry = metricRegistry;
    }
    
}
