/* Copyright (c) 2014 - 2015 Boundless http://boundlessgeo.com - all rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package com.boundlessgeo.suite.geoserver.cloudwatch.aws;

import com.amazonaws.AmazonClientException;
import com.amazonaws.services.cloudwatch.AmazonCloudWatchAsyncClient;
import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.amazonaws.services.cloudwatch.model.PutMetricDataRequest;
import com.boundlessgeo.suite.geoserver.cloudwatch.metrics.MetricProvider;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.util.concurrent.Futures;
import com.google.common.util.concurrent.ListenableFuture;
import com.google.common.util.concurrent.JdkFutureAdapters;
import com.google.common.util.concurrent.FutureCallback;

/**
 *
 * @author tingold@boundlessgeo.com
 * @author tbattle@boundlessgeo.com
 */

public class CloudwatchSender {

    protected AmazonCloudWatchAsyncClient cloudwatch;

    protected boolean enabled = false;

    private static final Logger logger = LoggerFactory.getLogger(CloudwatchSender.class);

    private List<MetricProvider> providers;

    public CloudwatchSender()
    {
        try
        {
            cloudwatch = new AmazonCloudWatchAsyncClient();
            enabled = true;
            logger.info("Initialized AWS Client");
        }
        catch(Exception ex)
        {
            logger.error("Error initializing AWS Client!");
            logger.error(ex.getMessage());
        }
    }

    /**
     * Invoked by spring on a timer to get and send from all metric providers
     */
    public void sendAllMetrics()
    {
        if(!enabled)
        {
            logger.debug("Metrics are disabled...returning");
            return;
        }
        logger.debug("Sending all metrics");
        for(MetricProvider mp: providers)
        {
            if(!mp.getEnabled())
                continue;
            for(final MetricDatum md: mp.getMetrics())
            {
                try
                {
                    PutMetricDataRequest request = new PutMetricDataRequest()
                        .withNamespace("geoserver")
                        .withMetricData(md);
                    logger.trace("Sending statistic {}", md.getMetricName());
                    ListenableFuture<java.lang.Void> f = JdkFutureAdapters.listenInPoolThread(
                                                            cloudwatch.putMetricDataAsync(request));
                    Futures.addCallback(f, new FutureCallback<java.lang.Void>()
                    {
                        public void onSuccess(java.lang.Void ignored)
                        {
                            logger.trace("Sent statistic {}", md.getMetricName());
                        }
                        public void onFailure(Throwable ex) {
                            logger.error("Error sending metric: {}", md.getMetricName(), ex);
                        }
                    });
                }
                catch(AmazonClientException ex)
                {
                    logger.warn("Error sending AWS metric {}", md.getMetricName());
                }
            }
        }
    }
    /**
     * Invoked by spring on a timer to get and send from all metric providers
     */
    public void sendAllMetricsToConsole()
    {
        if(!enabled)
        {
            logger.debug("Metrics are disabled...returning");
            return;
        }
        logger.debug("Sending all metrics");
        for(MetricProvider mp: providers)
        {
            for(MetricDatum md: mp.getMetrics())
            {
                logger.debug("Sending statistic {}", md.getMetricName());
                //PutMetricDataRequest request = new PutMetricDataRequest().withNamespace("geoserver").withMetricData(md);
                //cloudwatch.putMetricDataAsync(request);
                logger.debug("Sent statistic {}", md.getMetricName());
                System.out.println(md.getMetricName() + " value: "+md.getValue());
            }
        }
    }

    protected void sendMetric(MetricDatum metric)
    {
        PutMetricDataRequest request = new PutMetricDataRequest()
            .withNamespace("AWS/EC2")
            .withMetricData(metric);

        cloudwatch.putMetricData(request);
    }

    /**
     * @return the providers
     */
    public List<MetricProvider> getProviders()
    {
        return providers;
    }

    /**
     * @param providers the providers to set
     */
    public void setProviders(List<MetricProvider> providers)
    {
        this.providers = providers;
    }
}
