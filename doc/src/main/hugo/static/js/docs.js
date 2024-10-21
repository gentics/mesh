$(document).ready(function() {
	$('table').addClass('table');
	$('table').addClass('table-hover');
	$('table').addClass('table-bordered');

	// Handle deep links for tabs
	var url = document.location.toString();
	if (url.match('#')) {
		var anchorId = url.split('#')[1];
		console.log(anchorId);
		$('#pluginTabs a[href="#' + anchorId + '"]').tab('show');
	} 

	// Change hash for page-reload
	$('#pluginTabs a').on('shown.bs.tab', function (e) {
		window.location.hash = e.target.hash;
	})
});