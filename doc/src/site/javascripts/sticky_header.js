function updateStickyHeader() {
    if ($(this).scrollTop() > 25) {
        $('header').addClass("sticky");
    } else {
        $('header').removeClass("sticky");
    }
}
$(window).scroll(function() {
    updateStickyHeader();
});
updateStickyHeader();
