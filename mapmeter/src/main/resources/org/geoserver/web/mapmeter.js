(function() {

  var mapmeter = window.mapmeter = window.mapmeter || {};

  function log(msg) {
    if (console && console.log) {
      console.log(msg);
    }
  }

  // driver function that both main page and mapmeter page call
  // the main page creates the dom element and then calls
  // whereas the mapmeter page calls it on an existing empty element
  mapmeter.fetchDataAndApply = function(containerJqueryElt) {

    // create the dom elements
    var mapmeterElts = mapmeter.createDomElements(containerJqueryElt);
    mapmeterElts.container = containerJqueryElt;

    // fetch the mapmeter data
    mapmeter.fetchData(function(mapmeterData) {
      // apply to dom based on response
      if (!mapmeterData) {
        log('No mapmeter data returned');
      } else if (mapmeterData.error) {
        var reason = mapmeterData.reason;
        if (reason) {
          var msgElt = mapmeterElts.msg[reason];
          if (msgElt) {
            mapmeterElts.msg.container.show();
            msgElt.show();
          } else if (reason === 'missingApiKey') {
            // missing api key is ok
          } else {
            mapmeterElts.msg.container.show();
            mapmeterElts.msg.unknown.text(mapmeterData.error);
            mapmeterElts.msg.unknown.show();
          }
        } else {
          log('Error fetching mapmeter data: ' + mapmeterData.error);
        }
      } else if (mapmeterData.data) {
        mapmeterElts.chart.container.show();
        mapmeter.drawChart(mapmeterElts.chart.element, mapmeterData);
      } else {
        log('Unknown response when fetching mapmeter data');
      }
    });
  };

  mapmeter.createDomElements = function(containerJqueryElt) {
    var chart = $('<div></div>').attr('id', 'mapmeter-chart');
    var chartContainer = $('<div></div>')
      .attr('id', 'mapmeter-chart-container')
      .append(
        $('<h2></h2>').text('GeoServer Request Data'))
      .append(chart);

    var serverExpiredHtml = 'You no longer have access to view server data. ' +
        'Please contact: <a href="mailto:accounts@mapmeter.com">' +
        'accounts@mapmeter.com</a> to resume access.';
    var serverExpired = $('<span></span>')
      .attr('class', 'error access-denied')
      .html(serverExpiredHtml);
    var unauthorized = $('<span></span>')
      .attr('class', 'error unauthorized')
      .html('User is unauthorized to view API key.');
    var missingCredentials = $('<span></span>')
      .attr('class', 'error missing-credentials')
      .html('Missing Mapmeter credentials.');
    var invalidApiKey = $('<span></span>')
      .attr('class', 'error invalid-apikey')
      .html('Invalid API key.');
    var unknown = $('<span></span>')
      .attr('class', 'error unknown')
      .html('Mapmeter error');

    var msgContainer = $('<div></div>')
      .attr('id', 'mapmeter-msg-container')
      .append(
        $('<p></p>')
          .attr('class', 'warning-link')
          .text('Mapmeter Error: ')
          .append(serverExpired)
          .append(unauthorized)
          .append(missingCredentials)
          .append(invalidApiKey)
          .append(unknown));

    containerJqueryElt
      .append(chartContainer)
      .append(msgContainer);
    return {
      chart: {
        container: chartContainer,
        element: chart
      },
      msg: {
        container: msgContainer,
        serverExpired: serverExpired,
        unauthorized: unauthorized,
        missingCredentials: missingCredentials,
        invalidApiKey: invalidApiKey,
        unknown: unknown
      }
    };
  };

  mapmeter.fetchData = function(cb) {
    $.getJSON('../rest/mapmeter/data.json', cb);
  };

  mapmeter.drawChart = function(jqueryDomElt, mapmeterData) {
    var stats = mapmeterData;

    var domElt = jqueryDomElt.get(0);
    var d3Container = d3.select(domElt);

    // 1. set up the elements for the chart
    // 2. inject the data from the json response

    var width = jqueryDomElt.width();
    var height = jqueryDomElt.height();

    // set up all the elements needed for d3
    var xScale = d3.time.scale().range([0, width]),
        yScale = d3.scale.linear().range([height, 0]),
        xAxis = d3.svg.axis().scale(xScale).ticks(7),
        yAxis = d3.svg.axis().scale(yScale).ticks(5).orient('left');

    var yFormat = d3.format('s');
    yAxis.tickFormat(yFormat);

    var area = d3.svg.area()
      .x(function(d) { return xScale(d.date); })
      .y0(height)
      .y1(function(d) { return yScale(d.value); });

    var line = d3.svg.line()
      .x(function(d) { return xScale(d.date); })
      .y(function(d) { return yScale(d.value); });

    var margin = {top: 5, right: 0, bottom: 45, left: 100};
    var svg = d3Container.append('svg')
      .attr('width', width + margin.right + margin.left)
      .attr('height', height + margin.top + margin.bottom);
    var g = svg.append('g')
             .attr('transform', 'translate(' +
        margin.left + ',' + margin.top + ')');

    // y axis label
    svg.append('text')
      .attr('x', 0 - height / 2)
      .attr('y', (margin.left / 2) - 5)
      .attr('transform', 'rotate(-90)')
      .style('text-anchor', 'middle')
      .text('Requests');

    // x axis label
    svg.append('text')
      .attr('x', margin.left + (width / 2))
      .attr('y', height + margin.bottom + margin.top - 5)
      .style('text-anchor', 'middle')
      .text('Day');

    var areaEl = g.append('path')
      .attr('class', 'area');

    var xAxisEl = g.append('g')
      .attr('class', 'x axis')
      .attr('transform', 'translate(0,' + height + ')');

    var yAxisEl = g.append('g')
      .attr('class', 'y axis');

    var pathEl = g.append('path')
      .attr('class', 'line');

    // populate the data
    var values = [],
        times = stats.data.time,
        len = times.length,
        startDate = new Date(1000 * times[0]),
        endDate = new Date(1000 * times[len - 1]);

    // extract arrays of metrics
    var metrics = {};
    $.each(stats.data, function(key, values) {
      // no $.isArray in this version of jquery
      //if ($.isArray(values)) {
      if (Object.prototype.toString.call(values) === '[object Array]') {
        metrics[key] = values;
      }
    });

    // build values per date by parsing the given expression
    var maxValue = 0,
        sum = 0,
        i, context, key, value;
    for (i = 0; i < len; ++i) {
      context = {};
      for (key in metrics) {
        context[key] = metrics[key][i];
      }
      // value = $parse(attrs.expression)(scope, context) || 0;
      value = context.request_count;
      values[i] = {
        value: value,
        date: new Date(1000 * times[i])
      };
      maxValue = Math.max(value, maxValue);
      sum += value;
    }

    xScale.domain([startDate, endDate]);
    yScale.domain([0, maxValue]).nice();

    /* Redraw chart with updated values */
    areaEl.attr('d', area(values));
    xAxisEl.call(xAxis);
    yAxisEl.call(yAxis);
    pathEl.attr('d', line(values));
  };
}());
