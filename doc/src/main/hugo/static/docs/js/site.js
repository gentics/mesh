/* global $ */
(function($, window) {
    "use strict";
    // test for native emoji support
    var emojiSupported = (function() {
        var node = document.createElement('canvas');
        if (!node.getContext || !node.getContext('2d') || typeof node.getContext('2d').fillText !== 'function')
            return false;
        var ctx = node.getContext('2d');
        ctx.textBaseline = 'top';
        ctx.font = '32px Arial';
        ctx.fillText('\ud83d\ude03', 0, 0);
        return ctx.getImageData(16, 16, 1, 1).data[0] !== 0;
    })();

    if (!emojiSupported) {
        // emojis
        // Set the size of the rendered Emojis
        // This can be set to 16x16, 36x36, or 72x72
        twemoji.size = '36x36';
        // Parse the document body and
        // insert <img> tags in place of Unicode Emojis
        twemoji.parse(document.body);
    }
})($, window);
