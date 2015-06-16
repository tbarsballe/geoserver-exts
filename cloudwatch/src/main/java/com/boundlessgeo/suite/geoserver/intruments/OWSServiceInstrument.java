/* Copyright (c) 2014 - 2015 Boundless http://boundlessgeo.com - all rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package com.boundlessgeo.suite.geoserver.intruments;

import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.boundlessgeo.suite.geoserver.cloudwatch.aws.MetricDatumEncoder;
import com.codahale.metrics.Meter;
import com.codahale.metrics.Timer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import org.aopalliance.intercept.MethodInvocation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author tingold@boundlessgeo.com
 */

public class OWSServiceInstrument extends Instrument{
      
    private static final Logger logger = LoggerFactory.getLogger(OWSServiceInstrument.class);
    private String serviceName;
    
    private String serviceRequestMeterName;
    private String serviceErrorMeterName;
    private String timerName;
    
    private final String owsReqName = "geoserver-ows-requests";
    private final String owsErrorName = "geoserver-ows-errors";
    
    //counts this particular service 
    private Meter serviceRequestMeter;
    //countrs all OWS services agggragated
    private Meter owsRequestMeter;
    //counts this particular serices errors
    private Meter serviceErrorMeter;
    //counts all ows service errors
    private Meter owsErrorMeter;
    //times service executions
    private Timer serviceTimer;
    
    
    public void afterPropertiesSet()    
    {
        serviceRequestMeterName = "geoserver-ows-"+this.serviceName+"-requests";
        serviceErrorMeterName = "geoserver-ows-"+this.serviceName+"-errors";
        timerName = "geoserver-ows-"+this.serviceName+"-timer";
                
        serviceRequestMeter = metricRegistry.meter("geoserver.ows."+this.serviceName+".requests");
        owsRequestMeter = metricRegistry.meter("geoserver.ows.requests");
        owsErrorMeter = metricRegistry.meter("geoserver.ows.errors");
        serviceErrorMeter = metricRegistry.meter("geoserver.ows."+this.serviceName+".errors");        
        serviceTimer = metricRegistry.timer("geoserver.ows."+this.serviceName+".processing");
        
    }
    
    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        logger.debug("Counting  request");
        serviceRequestMeter.mark();
        owsRequestMeter.mark();
        Timer.Context context = serviceTimer.time();
        try
        {
            return mi.proceed();            
        }
        catch(Exception ex)
        {
            logger.debug("Counting WMS error");
            owsErrorMeter.mark();
            serviceErrorMeter.mark();
            throw ex;
        }
        finally
        {
            context.stop();
        }
    }

    @Override
    public Collection<MetricDatum> getMetrics() {
         List<MetricDatum> metrics = new ArrayList<>();
        
        metrics.add(encoder.encodeDatum(this.serviceRequestMeterName, serviceRequestMeter.getOneMinuteRate(), MetricDatumEncoder.UOM.Count_Second));
        metrics.add(encoder.encodeDatum(this.owsReqName, owsRequestMeter.getOneMinuteRate(), MetricDatumEncoder.UOM.Count_Second));
        metrics.add(encoder.encodeDatum(this.serviceErrorMeterName, serviceErrorMeter.getOneMinuteRate(), MetricDatumEncoder.UOM.Count_Second));
        metrics.add(encoder.encodeDatum(this.owsErrorName, owsErrorMeter.getOneMinuteRate(), MetricDatumEncoder.UOM.Count_Second));
        metrics.add(encoder.encodeDatum(this.timerName, serviceTimer.getOneMinuteRate(), MetricDatumEncoder.UOM.Milliseconds));
        
        return Collections.unmodifiableCollection(metrics);
    }


    /**
     * @return the serviceName
     */
    public String getServiceName() {
        return serviceName;
    }

    /**
     * @param serviceName the serviceName to set
     */
    public void setServiceName(String serviceName) {
        this.serviceName = serviceName;
    }

    
}
