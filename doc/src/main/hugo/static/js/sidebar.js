$(document).ready(function() {
    var $toc = $("#toc ul.sectlevel1");
    // Check whether a toc exists
    if (!$toc.length) {
        return;
    }
    var $sidebar = $("#sidebar");
    $sidebar.append($toc);
    $("#toc").remove();

    // The sidebar sticks to the top of the viewport via CSS (position: sticky)

    // add .nav class to all lists in the sidebar (necessary for scrollspy)
    $sidebar.find("ul").addClass("nav");

    // enable scrollspy
    $("body").scrollspy({ target: "#sidebar" });
});
