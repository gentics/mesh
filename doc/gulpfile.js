var bower = require("gulp-bower");
var compress = require("compression");
var connect = require("connect");
var decompress = require("gulp-decompress");
var del = require("del");
var flatten = require("gulp-flatten");
var fs = require("fs");
var gulp = require("gulp");
var gutil = require("gulp-util");
var iconfilter = require("./src/main/filters/iconfilter.js");
var path = require("path");
var prettyHrtime = require("pretty-hrtime");
var rename = require("gulp-rename");
var replace = require("gulp-replace");
var request = require("request");
var serveStatic = require("serve-static");
var source = require("vinyl-source-stream");
var streamify = require("gulp-streamify");
var swig = require("swig");

var Metalsmith = require("metalsmith");
var autoprefixer = require("metalsmith-autoprefixer");
var cleanCSS = require("metalsmith-clean-css");
var define = require("metalsmith-define");
var htmlMinifier = require("metalsmith-html-minifier");
var ignore = require("metalsmith-ignore");
var less = require("metalsmith-less");
var templates = require("metalsmith-templates");

// path to website on the server in production mode (i.e. when running
// `gulp build`). MUST BE ABSOLUTE AND MUST END WITH A SLASH!
var contextPath = "/";

// path to website on the server in development mode (i.e. when running
// `gulp watch`). MUST BE ABSOLUTE AND MUST END WITH A SLASH!
var contextPathDev = "/";

// port to listen to in development mode (i.e. when running `gulp watch`)
var devPort = 4000;

// website url in production mode (must be absolute; protocol and host may be omitted)
var siteUrl = "" + contextPath;

// website url in development mode (must be absolute; protocol and host may be omitted)
var siteUrlDev = "http://localhost:" + devPort + contextPathDev;

// paths to source files
var paths = {
  bootstrap_js: "bower_components/bootstrap/dist/js/bootstrap.min.js",
  docs_generated: "target/docs-generated",
  entypo: "Entypo+",
  less_includes: [
    "src/site/stylesheets",
    "src/main/less",
    "bower_components/bootstrap/less"
  ],
  src: "src/site",
  site: "target/site",
  target_asciidoctor_bs_themes: "target/asciidoctor-bs-themes",
  target_docs: "target/site/docs",
  target_icons: "target/site/assets/icons",
  target_scripts: "target/site/javascripts",
  target_stylesheets: "target/site/stylesheets",
  templates: "src/main/templates"
};

// build site
function build(done, dev) {
  var useTemplateCache = dev ? false : undefined;
  if (!useTemplateCache) {
    swig.setDefaults({ cache: false });
  }

  swig.setTag("icon", iconfilter.parse, iconfilter.compile, iconfilter.ends, iconfilter.block);

  var site_url = siteUrl;
  if (dev) {
    site_url = siteUrlDev;
  }

  Metalsmith(__dirname)
    .source(paths.src)
    .destination(paths.site)

    // do not remove files already in the target directory
    .clean(false)

    // compile LESS sources to CSS
    .use(less({
      render: {
        paths: paths.less_includes
      }
    }))

    // remove LESS sources
    .use(ignore([
      "**/*.less"
    ]))

    // compress css
    .use(cleanCSS())

    // autoprefix css
    .use(autoprefixer())

    // define global variables for templates
    .use(define({
      "site_url": site_url
    }))

    // apply template engine in-place
    .use(templates({
      engine: "swig",
      cache: useTemplateCache,
      pattern: "**/*.html",
      inPlace: true
    }))

    // apply templates (e.g. header, footer)
    .use(templates({
      engine: "swig",
      cache: useTemplateCache,
      pattern: "**/*.html",
      directory: paths.templates
    }))

    // minify HTML
    .use(htmlMinifier())

    // build site
    .build(done);
}

// build docs
function buildDocs(done, dev) {
  var site_url = siteUrl;
  if (dev) {
    site_url = siteUrlDev;
  }

  Metalsmith(__dirname)
    .source(paths.docs_generated)
    .destination(paths.target_docs)

    // do not remove files already in the target directory
    .clean(false)

    // define global variables for templates
    .use(define({
      "site_url": site_url
    }))

    // apply templates
    .use(templates({
      engine: "swig",
      directory: paths.templates,
      pattern: "**/*.html"
    }))

    // build site
    .build(done);
}

// install bower dependencies
gulp.task("bower", function() {
  return bower();
});

// download bootstrap themes for AsciiDoc
gulp.task("install-asciidoc-bs-themes", function(done) {
  if (fs.existsSync(paths.target_asciidoctor_bs_themes)) {
    done();
    return;
  }
  return request("https://github.com/nerk/asciidoctor-bs-themes/archive/92dd167181d3c4ea48061ad4cba40ebc99d7151a.tar.gz")
    .pipe(source("asciidoctor-bs-themes.tar.gz"))
    .pipe(streamify(decompress({ strip: 1 })))
    .pipe(gulp.dest(paths.target_asciidoctor_bs_themes));
});

// copy svg icons
gulp.task("icons", function() {
  return gulp.src(path.join(paths.entypo, "**/*.svg"))
    .pipe(flatten())
    .pipe(rename(function(path) {
      // rename 'resize-100%.svg' because the maven-scm-publish-plugin has
      // problems with the percent sign which it considers an escape character
      if (path.basename === "resize-100%") {
        path.basename = "resize-100"
      }
    }))
    .pipe(replace(/id="[^"]+"/, 'id="icon"')) // set id of all icons to a fixed value so we can reference it easier
    .pipe(gulp.dest(path.join(paths.target_icons, "entypo")));
});

// copy required javascripts
gulp.task("scripts", ["bower"], function() {
  return gulp.src(paths.bootstrap_js)
    .pipe(gulp.dest(paths.target_scripts));
});

// build docs
gulp.task("site-docs", function(done) {
  buildDocs(done);
})

// build site
gulp.task("site", ["icons", "scripts", "site-docs", "install-asciidoc-bs-themes"], function(done) {
  build(done);
});

// build site in development mode
gulp.task("site-dev", ["site"], function(done) {
  buildDocs(function() {
    build(done, true);
  }, true);
});

// start a web server, watch source directory and rebuild if necessary
gulp.task("watch", ["site-dev"], function() {
    // start web server
    var app = connect();
    app.use(compress());
    app.use(contextPathDev, serveStatic(paths.site, {
        "index": ["index.html"]
    }));
    app.listen(devPort, function() {
        gutil.log("Listening on port", gutil.colors.cyan("4000"), "...");
    });

    return gulp.watch([paths.src + "/**/*", paths.templates + "/**/*"], {}, function() {
        gutil.log("Rebuilding ...");
        var start = process.hrtime();
        build(function() {
            gutil.log("Finished", "'" + gutil.colors.cyan("rebuilding") + "'",
                "after", gutil.colors.magenta(prettyHrtime(process.hrtime(start))));
        }, true);
    });
});

// clean target directory
gulp.task("clean", function(cb) {
  del([paths.site], cb);
});

// default task
gulp.task("default", ["site"]);
