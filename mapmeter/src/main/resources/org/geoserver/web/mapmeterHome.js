(function() {
  $(document).ready(function() {

    // to prevent a basic auth dialog box from appearing,
    // conditionally try to fetch mapmeter data only when user is logged in
    // going forward, it would probably be better to handle this server side

    // check for the login link to see if user is logged in or not
    // this is a brittle way to handle this
    if ($('#header .button-login').length === 0) {
      var mapmeterDataContainer = $('<div></div>')
        .attr('id', 'mapmeter-data-container');
      $('.page-pane').append(mapmeterDataContainer);
      mapmeter.fetchDataAndApply(mapmeterDataContainer);
    }

  });
}());
