(function() {
  $(document).ready(function() {
    var mapmeterDataContainer = $('<div></div>')
      .attr('id', 'mapmeter-data-container');
    $('.page-pane').append(mapmeterDataContainer);
    mapmeter.fetchDataAndApply(mapmeterDataContainer);
  });
}());
