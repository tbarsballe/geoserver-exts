/* Copyright (c) 2014 - 2015 Boundless http://boundlessgeo.com - all rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package com.boundlessgeo.suite.geoserver.cloudwatch.callbacks;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
//import java.util.Map;
import java.util.UUID;
//import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

//import javax.annotation.PostConstruct;

import org.geoserver.ows.DispatcherCallback;
import org.geoserver.ows.Request;
import org.geoserver.ows.Response;
import org.geoserver.platform.Operation;
import org.geoserver.platform.Service;
import org.geoserver.platform.ServiceException;

import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.boundlessgeo.suite.geoserver.cloudwatch.aws.MetricDatumEncoder;
import com.boundlessgeo.suite.geoserver.cloudwatch.metrics.MetricProvider;
import com.codahale.metrics.Meter;
import com.codahale.metrics.MetricRegistry;
import com.codahale.metrics.Timer;
import com.codahale.metrics.Timer.Context;
import com.google.common.cache.Cache;
import com.google.common.cache.CacheBuilder;
//import com.google.common.cache.CacheLoader;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author tingold@boundlessgeo.com
 * @author tbattle@boundlessgeo.com
 */
public class OWSServiceDispatcherCallbacks implements DispatcherCallback, MetricProvider {
	   private MetricDatumEncoder encoder;
	   //private Timer serviceTimer;
	   private MetricRegistry metricRegistry;
	   private Boolean enabled;
	   private List<String> allowedMetrics;
	   //private final Map<String, Context> timercontextstore = new ConcurrentHashMap();
	   private final Cache<String, Context> timercontextstore = CacheBuilder.newBuilder()
			    .concurrencyLevel(4)
			    .expireAfterWrite(24, TimeUnit.HOURS)
			    .build();
	   private Timer wfsTimer;
	   private Timer wmsTimer;
	   private Timer wcs100Timer;
	   private Timer wcs111Timer;
	   private Timer wcs20Timer;
	   private Timer wpsTimer;
	   private Timer cswTimer;
	   private Timer owsTimer;
	   private Meter wmsRequestMeter;
	   private Meter wfsRequestMeter;
	   private Meter wcs100RequestMeter;
	   private Meter wcs111RequestMeter;
	   private Meter wcs20RequestMeter;
	   private Meter wpsRequestMeter;
	   private Meter cswRequestMeter;
	   private Meter owsRequestMeter;

	   private static final Logger logger = LoggerFactory.getLogger(OWSServiceDispatcherCallbacks.class);



	   public void init() {
		   wfsTimer = metricRegistry.timer("WFS-REQUEST-TIMER");
		   wmsTimer = metricRegistry.timer("WMS-REQUEST-TIMER");
		   wcs100Timer = metricRegistry.timer("WCS100-REQUEST-TIMER");
		   wcs111Timer = metricRegistry.timer("WCS111-REQUEST-TIMER");
		   wcs20Timer = metricRegistry.timer("WCS20-REQUEST-TIMER");
		   wpsTimer = metricRegistry.timer("WPS-REQUEST-TIMER");
		   cswTimer = metricRegistry.timer("CWS-REQUEST-TIMER");
		   owsTimer = metricRegistry.timer("OWS-REQUEST-TIMER");

		   wmsRequestMeter = metricRegistry.meter("geoserver.ows.wms.requests");
		   wfsRequestMeter = metricRegistry.meter("geoserver.ows.wfs.requests");
		   wcs100RequestMeter = metricRegistry.meter("geoserver.ows.wcs100.requests");
		   wcs111RequestMeter = metricRegistry.meter("geoserver.ows.wcs111.requests");
		   wcs20RequestMeter = metricRegistry.meter("geoserver.ows.wcs20.requests");
		   wpsRequestMeter = metricRegistry.meter("geoserver.ows.wps.requests");
		   cswRequestMeter = metricRegistry.meter("geoserver.ows.csw.requests");
		   owsRequestMeter = metricRegistry.meter("geoserver.ows.requests");
	   }

	public void setAllowedMetrics(List<String> allowedMetrics) {
		this.allowedMetrics = allowedMetrics;
	}

	public void setMetricRegistry(MetricRegistry metricRegistry) {
		this.metricRegistry = metricRegistry;
	}

	public void setEncoder(MetricDatumEncoder encoder) {
		this.encoder = encoder;
	}

	@Override
	public Request init(Request request) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Service serviceDispatched(Request request, Service service)
			throws ServiceException {
		Timer.Context context = null;
		String uid = UUID.randomUUID().toString();
		request.getHttpRequest().setAttribute("uid", uid);
		System.out.println("started" + service.getId() + "; "+service.getNamespace()+"; "+ request.getHttpRequest().getAttribute("uid"));
		//serviceTimer = metricRegistry.timer("geoserver.ows."+"dummytimer"+".processing");

		if(allowedMetrics.contains(service.getId())){
			switch(service.getId()){
				case "wms":
					context = wmsTimer.time();
					timercontextstore.put(uid, context);
					wmsRequestMeter.mark();
					break;
				case "wfs":
					context = wfsTimer.time();
					timercontextstore.put(uid, context);
					wfsRequestMeter.mark();
					break;
				case "wcs100":
					context = wcs100Timer.time();
					timercontextstore.put(uid, context);
					wcs100RequestMeter.mark();
					break;
				case "wcs111":
					context = wcs111Timer.time();
					timercontextstore.put(uid, context);
					wcs111RequestMeter.mark();
					break;
				case "wcs20":
					context = wcs20Timer.time();
					timercontextstore.put(uid, context);
					wcs20RequestMeter.mark();
					break;
				case "wps":
					context = wpsTimer.time();
					timercontextstore.put(uid, context);
					wpsRequestMeter.mark();
					break;
				case "csw":
					context = cswTimer.time();
					timercontextstore.put(uid, context);
					cswRequestMeter.mark();
					break;
				case "ows":
					context = owsTimer.time();
					timercontextstore.put(uid, context);
					owsRequestMeter.mark();
					break;
			}
		}
		return service;
	}

	@Override
	public Operation operationDispatched(Request request, Operation operation) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Object operationExecuted(Request request, Operation operation,
			Object result) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public Response responseDispatched(Request request, Operation operation,
			Object result, Response response) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void finished(Request request) {
		String uid = (String) request.getHttpRequest().getAttribute("uid");
		System.out.println("finished " + uid);
		Timer.Context context = timercontextstore.asMap().get(uid);
		context.stop();
		timercontextstore.asMap().remove(uid);

	}

	@Override
	public Collection<MetricDatum> getMetrics() {
		 List<MetricDatum> callbackStats = new ArrayList<>();

		 logger.debug("start");
		 for(String s: allowedMetrics) {
		 	logger.debug(s);
		 }
		 logger.debug("done");

	     if(allowedMetrics.contains("wms")){
	    	 callbackStats.add(encoder.encodeDatum("geoserver-ows-wms-timer", wmsTimer.getOneMinuteRate(), MetricDatumEncoder.UOM.Milliseconds));
	    	 callbackStats.add(encoder.encodeDatum("geoserver-ows-wms-requests", wmsRequestMeter.getOneMinuteRate(), MetricDatumEncoder.UOM.Count_Second));
	     }
	     if(allowedMetrics.contains("wfs")){
	    	callbackStats.add(encoder.encodeDatum("geoserver-ows-wfs-timer", wfsTimer.getOneMinuteRate(), MetricDatumEncoder.UOM.Milliseconds));
	     	callbackStats.add(encoder.encodeDatum("geoserver-ows-wfs-requests", wfsRequestMeter.getOneMinuteRate(), MetricDatumEncoder.UOM.Count_Second));
	     }
	     if(allowedMetrics.contains("wcs100")){
			 callbackStats.add(encoder.encodeDatum("geoserver-ows-wcs100-timer", wcs100Timer.getOneMinuteRate(), MetricDatumEncoder.UOM.Milliseconds));
		     callbackStats.add(encoder.encodeDatum("geoserver-ows-wcs100-requests", wcs100RequestMeter.getOneMinuteRate(), MetricDatumEncoder.UOM.Count_Second));
	     }
	     if(allowedMetrics.contains("wcs111")){
			 callbackStats.add(encoder.encodeDatum("geoserver-ows-wcs111-timer", wcs111Timer.getOneMinuteRate(), MetricDatumEncoder.UOM.Milliseconds));
		     callbackStats.add(encoder.encodeDatum("geoserver-ows-wcs111-requests", wcs111RequestMeter.getOneMinuteRate(), MetricDatumEncoder.UOM.Count_Second));
	     }
	     if(allowedMetrics.contains("wcs20")){
			 callbackStats.add(encoder.encodeDatum("geoserver-ows-wcs20-timer", wcs20Timer.getOneMinuteRate(), MetricDatumEncoder.UOM.Milliseconds));
		     callbackStats.add(encoder.encodeDatum("geoserver-ows-wcs20-requests", wcs20RequestMeter.getOneMinuteRate(), MetricDatumEncoder.UOM.Count_Second));
	     }
	     if(allowedMetrics.contains("wps")){
			 callbackStats.add(encoder.encodeDatum("geoserver-ows-wps-timer", wpsTimer.getOneMinuteRate(), MetricDatumEncoder.UOM.Milliseconds));
		     callbackStats.add(encoder.encodeDatum("geoserver-ows-wps-requests", wpsRequestMeter.getOneMinuteRate(), MetricDatumEncoder.UOM.Count_Second));
	     }
	     if(allowedMetrics.contains("csw")){
			 callbackStats.add(encoder.encodeDatum("geoserver-ows-csw-timer", cswTimer.getOneMinuteRate(), MetricDatumEncoder.UOM.Milliseconds));
		     callbackStats.add(encoder.encodeDatum("geoserver-ows-csw-requests", cswRequestMeter.getOneMinuteRate(), MetricDatumEncoder.UOM.Count_Second));
	     }
	     if(allowedMetrics.contains("ows")){
	    	 callbackStats.add(encoder.encodeDatum("geoserver-ows-ows-timer", owsTimer.getOneMinuteRate(), MetricDatumEncoder.UOM.Milliseconds));
	    	 callbackStats.add(encoder.encodeDatum("geoserver-ows-requests", owsRequestMeter.getOneMinuteRate(), MetricDatumEncoder.UOM.Count_Second));
	     }

	     return Collections.unmodifiableCollection(callbackStats);
	}

	/**
     * @return whether the service is enabled
     */
    public Boolean getEnabled() {
        return enabled;
    }

    /**
     * @param whether the service is enabled
     */
    public void setEnabled(Boolean enabled) {
        this.enabled = enabled;
    }
}
