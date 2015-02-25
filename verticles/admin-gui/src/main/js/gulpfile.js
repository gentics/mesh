/* global require */
var gulp    = require('gulp');
var ts      = require('gulp-typescript');
var concat  = require('gulp-concat');
var jshint  = require('gulp-jshint');
var serve   = require('gulp-serve');

var project = ts.createProject({
	declarationFiles: false,
	noExternalResolve: true
});

gulp.task('scripts', function () {
	var compiled = gulp
		.src('src/ts/*.ts')
		.pipe(ts(project))
		.pipe(jshint())
		.pipe(jshint.reporter('jshint-stylish'))
		.pipe(concat('app.js'))
		.pipe(gulp.dest('build'));
	return compiled;
});

gulp.task('serve', serve(__dirname));

gulp.task('watch', ['serve', 'scripts'], function () {
	gulp.watch('src/ts/*.ts', ['scripts']);
});
