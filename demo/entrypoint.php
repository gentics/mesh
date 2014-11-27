<?php

$actualPath = "$_SERVER[REQUEST_URI]";
//echo $actualPath;
$json =  file_get_contents("http://localhost:8000/tag/get$actualPath"); 
$data= json_decode($json);
$GLOBALS['data'] = $data;
if(is_null($data)) {
  http_response_code(404);
} else {
//var_dump($data);
require_once 'templates/post.php';
}
