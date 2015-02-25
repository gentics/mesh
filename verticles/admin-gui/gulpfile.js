/* global require */
var gulp    = require('gulp');
var ts      = require('gulp-typescript');
var concat  = require('gulp-concat');
var jshint  = require('gulp-jshint');
var serve   = require('gulp-serve');

var TS_FILES = 'src/main/ts/*.ts';

var project = ts.createProject({
	declarationFiles: false,
	noExternalResolve: true
});

gulp.task('scripts', function () {
	var compiled = gulp
		.src(TS_FILES)
		.pipe(ts(project))
		.pipe(jshint())
		.pipe(jshint.reporter('jshint-stylish'))
		.pipe(concat('app.js'))
		.pipe(gulp.dest('target/js'));
	return compiled;
});

gulp.task('serve', serve(__dirname));

gulp.task('watch', ['serve', 'scripts'], function () {
	gulp.watch(TS_FILES, ['scripts']);
});
