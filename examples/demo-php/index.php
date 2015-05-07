<?php

namespace Gentics\MeshDemo;

ini_set ( 'display_errors', 'on' );

require_once __DIR__ . '/mesh-client.php';
require_once __DIR__ . '/vendor/autoload.php';

$mesh = new MeshPHPDemo ( "joe1", "test123", "dummy");
// $mesh->handleRequest ();

var_dump($mesh->webroot("/2014"));

die();

$app = new \Silex\Application ();

$app->register ( new \Silex\Provider\TwigServiceProvider (), array (
		'twig.path' => __DIR__ . '/templates' 
) );

$app->get ( '/{path}', function ($path) use($app) {
// 	$mesh->webroot($path);
	$page = array();
	$page['content'] = "hallo";
	$page['title'] = "du";
	$page['name'] = "the name";
	$page['author'] = "author";
	$page['id']    = 20;
	$page['teaser'] = "moped" . $path;
	
	return $app ['twig']->render ('post.twig', array (
			'page' => $page 
	) );
} )->assert("path", ".*");

$app->run ();

