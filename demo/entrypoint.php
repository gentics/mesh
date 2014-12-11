<?php

$actualPath = "$_SERVER[REQUEST_URI]";
$json =  file_get_contents("http://localhost:8000/tag/get$actualPath"); 
$data= json_decode($json);
$GLOBALS['data'] = $data;
//var_dump($data);
if(is_null($data)) {
  http_response_code(404);
} else {
  require_once 'templates/post.php';
}
