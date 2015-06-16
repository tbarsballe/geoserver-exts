/* Copyright (c) 2014 - 2015 Boundless http://boundlessgeo.com - all rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package com.boundlessgeo.suite.geoserver.cloudwatch.metrics;

import java.util.Date;
/**
 *  Stolen from the MapMeter extension
 */
public class SystemStatSnapshot {

    private final long totalMemoryUsage;

    private final long totalMemoryMax;

    private final double systemLoadAverage;

    private final long timeStamp;

    public SystemStatSnapshot(long totalMemoryUsage, long totalMemoryMax,
            double systemLoadAverage) {
        this(totalMemoryUsage, totalMemoryMax, systemLoadAverage, new Date().getTime() / 1000);
    }

    public SystemStatSnapshot(long totalMemoryUsage, long totalMemoryMax,
            double systemLoadAverage, long secondsSinceEpoch) {
        this.totalMemoryUsage = totalMemoryUsage;
        this.totalMemoryMax = totalMemoryMax;
        this.systemLoadAverage = systemLoadAverage;
        this.timeStamp = secondsSinceEpoch;
    }

    public long getTotalMemoryUsage() {
        return totalMemoryUsage;
    }

    public long getTotalMemoryMax() {
        return totalMemoryMax;
    }

    public double getSystemLoadAverage() {
        return systemLoadAverage;
    }

    public long getTimeStamp() {
        return timeStamp;
    }

}
