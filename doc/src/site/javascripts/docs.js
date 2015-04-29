$(document).ready(function() {
    // make sidebar sticky when it hits the top of the viewport
    var $sidebar = $("#sidebar");
    $sidebar.affix({
        offset: {
            top: $sidebar.offset().top - 20,
            bottom: $("footer").outerHeight(true) + 20
        }
    });

    $(window).resize(function() {
        $sidebar.affix("checkPosition");
    });

    // add .nav class to all lists in the sidebar (necessary for scrollspy)
    $sidebar.find("ul").addClass("nav");

    // enable scrollspy
    $("body").scrollspy({ target: "#sidebar" });

    // *************** START BUGFIX
    // Fix position of the sidebar in Safari. See https://github.com/twbs/bootstrap/issues/12126
    // Safari does not position the sidebar correctly because we use col-md-pull-x on it. We
    // need to set the position here. There's no clean and direct CSS solution yet.
    var explicitlySetAffixPosition = function() {
        $sidebar.css("left", $sidebar.offset().left + "px");
    };
    var resetLeftPosition = function() {
        $sidebar.css("left", "auto");
    };

    // Before the element becomes affixed, add left CSS that is equal to the
    // distance of the element from the left of the screen
    $sidebar.on("affix.bs.affix", function() {
        explicitlySetAffixPosition();
    });
    // Before the element becomes affix-bottom, reset the CSS left property to as it was before
    $sidebar.on("affix-bottom.bs.affix", function() {
        resetLeftPosition();
    });
    // Do the same for affix-top
    $sidebar.on("affix-top.bs.affix", function() {
        resetLeftPosition();
    });

    // On resize of window, un-affix affixed widget to measure where it
    // should be located, set the left CSS accordingly, re-affix it
    $(window).resize(function() {
        if ($sidebar.hasClass("affix")) {
            $sidebar.removeClass("affix");
            explicitlySetAffixPosition();
            $sidebar.addClass("affix");
        } else if (stickywidget.hasClass("affix-bottom")) {
            resetLeftPosition();
        }
    });
    // *************** END BUGFIX
});
