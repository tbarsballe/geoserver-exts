(function() {
  $(document).ready(function() {
    var chartWrapperDomElt = $('#mapmeter-chart-container').get(0);
    var chartDomElt = $('#mapmeter-chart').get(0);
    mapmeter.fetchDataAndDrawChart(chartDomElt, chartWrapperDomElt);
  });
}());
