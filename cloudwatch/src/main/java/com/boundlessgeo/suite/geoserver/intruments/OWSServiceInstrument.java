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
 * @author tbattle@boundlessgeo.com
 */

public class OWSServiceInstrument extends Instrument {

    private static final Logger logger = LoggerFactory.getLogger(OWSServiceInstrument.class);
    private String serviceName;

    private String serviceRequestMeterName;
    private String serviceErrorMeterName;
    private String timerName;

    private final String owsReqName = "geoserver-ows-requests";
    private final String owsErrorName = "geoserver-ows-errors";

    //counts this particular service
    private Meter serviceRequestMeter;
    //counts all OWS services agggragated
    private Meter owsRequestMeter;
    //counts this particular serices errors
    private Meter serviceErrorMeter;
    //counts all ows service errors
    private Meter owsErrorMeter;
    //times service executions
    private Timer serviceTimer;


    public void afterPropertiesSet() {
        logger.debug("{} enabled: {}", this.serviceName, this.enabled);
        serviceRequestMeterName = "geoserver-ows-"+this.serviceName+"-requests";
        serviceErrorMeterName = "geoserver-ows-"+this.serviceName+"-errors";
        timerName = "geoserver-ows-"+this.serviceName+"-timer";

        if (this.enabled) {
            serviceRequestMeter = metricRegistry.meter("geoserver.ows."+this.serviceName+".requests");
            owsRequestMeter = metricRegistry.meter("geoserver.ows.requests");
            owsErrorMeter = metricRegistry.meter("geoserver.ows.errors");
            serviceErrorMeter = metricRegistry.meter("geoserver.ows."+this.serviceName+".errors");
            serviceTimer = metricRegistry.timer("geoserver.ows."+this.serviceName+".processing");
        }
    }

    @Override
    public Object invoke(MethodInvocation mi) throws Throwable {
        if (!this.enabled) {
            try {
                return mi.proceed();
            }
            catch(Exception ex) {
                logger.debug("Error on disabled OWS Instrument");
                throw ex;
            }
        }

        logger.trace("Counting request");
        serviceRequestMeter.mark();
        owsRequestMeter.mark();
        Timer.Context context = serviceTimer.time();
        try {
            return mi.proceed();
        }
        catch(Exception ex) {
            logger.trace("Counting OWS error");
            owsErrorMeter.mark();
            serviceErrorMeter.mark();
            throw ex;
        }
        finally {
            context.stop();
        }
    }

    @Override
    public Collection<MetricDatum> getMetrics() {
        List<MetricDatum> metrics = new ArrayList<>();
        if (this.enabled) {
            metrics.add(encoder.encodeDatum(this.serviceRequestMeterName, serviceRequestMeter.getOneMinuteRate(), MetricDatumEncoder.UOM.Count_Second));
            metrics.add(encoder.encodeDatum(this.owsReqName, owsRequestMeter.getOneMinuteRate(), MetricDatumEncoder.UOM.Count_Second));
            metrics.add(encoder.encodeDatum(this.serviceErrorMeterName, serviceErrorMeter.getOneMinuteRate(), MetricDatumEncoder.UOM.Count_Second));
            metrics.add(encoder.encodeDatum(this.owsErrorName, owsErrorMeter.getOneMinuteRate(), MetricDatumEncoder.UOM.Count_Second));
            metrics.add(encoder.encodeDatum(this.timerName, serviceTimer.getSnapshot().getMedian(), MetricDatumEncoder.UOM.Microseconds));
        }
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
