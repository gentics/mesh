/**
 * Generates an SVG tag that refers to an icon from the entypo collection
 *
 * @example
 * // Generate an SVG tag that refers to `{{ site_url }}assets/icons/entypo/install.svg`
 * {% icon "install" %}
 * // => <svg class="icon"><use xlink:href="/assets/icons/entypo/install.svg#icon"></use></svg>
 */
exports.parse = function(str, line, parser, types, options) {
  parser.on(types.STRING, function(token) {
    var name = token.match.replace(/^("|')|("|')$/g, "");
    this.out.push(name);
  });
  return true;
};

exports.compile = function(compiler, args, content, parents, options, blockName) {
  return '_output += "<svg class=\\"icon\\"><use xlink:href=\\"' +
    '" + _ctx.site_url + "assets/icons/entypo/' + args[0] + '.svg#icon\\"></use></svg>";';
};

exports.ends = false;
exports.block = false;
