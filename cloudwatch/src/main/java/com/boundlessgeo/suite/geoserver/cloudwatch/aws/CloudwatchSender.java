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

/**
 *
 * @author tingold@boundlessgeo.com
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

            for(MetricDatum md: mp.getMetrics())
            {
                try{
                    logger.debug("Sending statistic {}", md.getMetricName());
                    PutMetricDataRequest request = new PutMetricDataRequest().withNamespace("geoserver").withMetricData(md);
                    cloudwatch.putMetricDataAsync(request);
                    logger.debug("Sent statistic {}", md.getMetricName());
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
    protected void sendMetric(MetricDatum metric){

        PutMetricDataRequest request = new PutMetricDataRequest()
            .withNamespace("AWS/EC2").withMetricData(metric);


        cloudwatch.putMetricData(request);
    }

    /**
     * @return the providers
     */
    public List<MetricProvider> getProviders() {
        return providers;
    }

    /**
     * @param providers the providers to set
     */
    public void setProviders(List<MetricProvider> providers) {
        this.providers = providers;
    }


}
