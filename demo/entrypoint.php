<?php

$actualPath = "$_SERVER[REQUEST_URI]";
$url  = "http://localhost:8080/api/v1/tag/get$actualPath";
$json =  file_get_contents($url); 
$data= json_decode($json);
//var_dump($data);
//echo $url;
$GLOBALS['date']  = "December 01, 2014";

//var_dump($data);
if (is_null($data)) {
	http_response_code(404);
	$GLOBALS['content'] = "Page not found";
	$GLOBALS['title'] = "Not found";
} else {
	$GLOBALS['content'] = $data->object->content;
	$GLOBALS['id'] = $data->object->id;
	$GLOBALS['teaser'] = $data->object->teaser;
	$GLOBALS['author'] = $data->object->author;
	$GLOBALS['title'] = $data->object->title;
}

require_once 'templates/post.php';
