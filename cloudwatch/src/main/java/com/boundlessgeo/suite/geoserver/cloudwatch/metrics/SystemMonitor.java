/* Copyright (c) 2014 - 2015 Boundless http://boundlessgeo.com - all rights reserved.
 * This code is licensed under the GPL 2.0 license, available at the root
 * application directory.
 */

package com.boundlessgeo.suite.geoserver.cloudwatch.metrics;
/**
 * stolen from MapMeter extension
 */
public interface SystemMonitor {

    SystemStatSnapshot pollSystemStatSnapshot();

}
