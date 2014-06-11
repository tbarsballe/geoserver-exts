(function() {
  $(document).ready(function() {
    var chartWrapper = $('<div></div>')
      .attr('id', 'mapmeter-chart-container');
    var chartHeading = $('<h2></h2>').text('Geoserver Request Data');
    var chartElt = $('<div></div>').attr('id', 'mapmeter-chart');
    chartWrapper.append(chartHeading);
    chartWrapper.append(chartElt);
    $('.page-pane').append(chartWrapper);
    var chartDomElt = chartElt.get(0);
    var chartWrapperElt = chartWrapper.get(0);
    mapmeter.fetchDataAndDrawChart(chartDomElt, chartWrapperElt);
  });
}());
