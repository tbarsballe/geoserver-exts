/* Copyright (c) 2014 - 2015 Boundless http://boundlessgeo.com - all rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package com.boundlessgeo.suite.geoserver.cloudwatch.metrics;

import com.amazonaws.services.cloudwatch.model.MetricDatum;
import com.boundlessgeo.suite.geoserver.cloudwatch.aws.MetricDatumEncoder;
import java.lang.management.ManagementFactory;
import java.lang.management.MemoryMXBean;
import java.lang.management.MemoryUsage;
import java.lang.management.OperatingSystemMXBean;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;


/**
 *  stolen from the the MapMeter extension 
 */

public class JmxSystemMonitor implements SystemMonitor, MetricProvider{

    private final String JRE_HEAP_PCT = "geoserver-jre-pct-used";
 
    
    private final String JRE_TOTAL = "geoserver-jre-max-memory";
    private final String JRE_MAX = "geoserver-jre-total-memory";
    
    
    private OperatingSystemMXBean operatingSystemMXBean;

    private MemoryMXBean memoryMXBean;

    private MetricDatumEncoder encoder;
    
    public JmxSystemMonitor() {
        operatingSystemMXBean = ManagementFactory.getOperatingSystemMXBean();
        memoryMXBean = ManagementFactory.getMemoryMXBean();
    }

    @Override
    public SystemStatSnapshot pollSystemStatSnapshot() {
        double systemLoadAverage = operatingSystemMXBean.getSystemLoadAverage();

        MemoryUsage heapMemoryUsage = memoryMXBean.getHeapMemoryUsage();
        MemoryUsage nonHeapMemoryUsage = memoryMXBean.getNonHeapMemoryUsage();

        long heapUsed = heapMemoryUsage.getUsed();
        long nonHeapUsed = nonHeapMemoryUsage.getUsed();
        long totalUsed = heapUsed + nonHeapUsed;

        long heapMax = heapMemoryUsage.getMax();
        long nonHeapMax = nonHeapMemoryUsage.getMax();
        long totalMax = heapMax + nonHeapMax;

        SystemStatSnapshot systemStatSnapshot = new SystemStatSnapshot(totalUsed, totalMax,
                systemLoadAverage);
        return systemStatSnapshot;
    }

    @Override
    public Collection<MetricDatum> getMetrics() {
        
        
      SystemStatSnapshot snap = this.pollSystemStatSnapshot();
               
      List<MetricDatum> jreStats = new ArrayList<>();
      
      
      jreStats.add(encoder.encodeDatum(JRE_HEAP_PCT, snap.getSystemLoadAverage(), MetricDatumEncoder.UOM.Percent));
      jreStats.add(encoder.encodeDatum(JRE_MAX, new Double(snap.getTotalMemoryMax()), MetricDatumEncoder.UOM.Bytes));
      jreStats.add(encoder.encodeDatum(JRE_TOTAL, new Double(snap.getTotalMemoryUsage()), MetricDatumEncoder.UOM.Bytes));
        
        
        return Collections.unmodifiableCollection(jreStats);
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
