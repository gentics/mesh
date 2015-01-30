<?php
/**
 * Gentics Aloha Editor AJAX Gateway
 * Copyright (c) 2010 Gentics Software GmbH
 * Licensed unter the terms of http://www.aloha-editor.com/license.html
 * aloha-sales@gentics.com
 * Author Haymo Meran     h.meran@gentics.com
 * Author Johannes Schüth j.schuet@gentics.com
 * Author Tobias Steiner  t.steiner@gentics.com
 * Author Bernhard Kaszt  b.kaszt@gentics.com
 * 
 * Testing from the command line:
 * function getallheaders(){return array('X-Gentics' => 'X');};
 * https url example: https://google.com/adsense
 * 
 */

require_once "settings.conf.php";
require_once "http.inc.php";

$_SERVER['SERVER_PROTOCOL'] = 'HTTP/1.0';

$request = array(
    'method'   => $_SERVER['REQUEST_METHOD'],
    'protocol' => $_SERVER['SERVER_PROTOCOL'],
    'headers'  => getallheaders(),
    // multipart/form-data  should work when the directives
    // in the .htaccess are working to prevent PHP from parsing
    // our data. This is done by a hack
    'payload'  => file_get_contents('php://input'),
);

// Check if the header X-proxyphp-Content-Type was set by our mod-rewrite rule.
// If yes: restore the original Content-Type header.
if (isset($request['headers']['X-proxyphp-Content-Type'])) {
	$request['headers']['Content-Type'] = $request['headers']['X-proxyphp-Content-Type'];
	unset($request['headers']['X-proxyphp-Content-Type']);
}

$url = $_SERVER['REQUEST_URI'];

if (strpos($url, $PROXYNAME) === 0) {
   $url = substr($url, strlen($PROXYNAME));
}

// Make sure that the URL starts with a / ofr security reasons.
if ($url[0] !== '/') {
	$url = '/' . $url;
}

// Unset some headers which shouldn't get forwarded
if (isset($request['headers']['If-None-Match'])){
    unset($request['headers']['If-None-Match']);
}

if (isset($request['headers']['If-Modified-Since'])){
    unset($request['headers']['If-Modified-Since']);
}

if (isset($request['headers']['If-Modified-Since'])){
    unset($request['headers']['If-Modified-Since']);
}

// Add parameters to the query URL if specified
if (!empty($HTTP_URL_ADD_QUERY_PARAMETERS) && strpos($url, '?') === false){
   $url = $url . '?' . $HTTP_URL_ADD_QUERY_PARAMETERS;
}

// Remove slash at the end of $CMS_SERVERHOST if there is one
if (substr($url, -1) === '/') {
	$CMS_SERVERHOST = substr($CMS_SERVERHOST, 0, -1);
}

$request['url'] = $CMS_SERVERHOST . $url;

//echo $request['url'];
//echo "---";
//exit(0); 

$response = http_request($request);

// Note HEAD does not always work even if specified...
// We use HEAD for Linkchecking so we do a 2nd request.
if ($_SERVER['REQUEST_METHOD'] === 'HEAD' && (int)$response['status'] >= 400) {

	$request['method'] = 'GET';
	fpassthru($response['socket']);
	fclose($response['socket']);
	$response = http_request($request);
}
else {
	$n_redirects = 0;
	// Follow redirections until we got a page finally, but only max $HTTP_MAX_REDIRECTS times.
	while (in_array($response['status'], array(301, 302, 307))) {
		// We got a redirection request from the remote server,
		// let's follow the trail and see what happens...
		// We don't check if the URL is an external now, because the response
		// should be trustworthy.
		// We handle HTTP 301, 302 and 307
		// See: http://en.wikipedia.org/wiki/List_of_HTTP_status_codes#3xx_Redirection

		// Close the old socket
		fclose($response['socket']);

		if ($n_redirects++ == $HTTP_MAX_REDIRECTS) {
			myErrorHandler('Too many redirects (' . $n_redirects . '), exiting...');
		}

		if (empty($response['headers']['Location'])) {
			myErrorHandler("Got redirection request by the remote server, but the redirection URL is empty.");
		}

		$n_redirects++;

		$url = $response['headers']['Location'];

		// For some unknown reason we got the new URL with the proxyname
		// prepended back, so we have to remove that part of the URL.
		// parse_url: http://php.net/manual/de/function.parse-url.php
		$parsedURL = parse_url($url);
		$url_path = $parsedURL['path'];

		// Check if the redirection URL starts with our proxyname
		if (substr($url_path, 0, strlen($PROXYNAME)) === $PROXYNAME) {
			$url_path = substr($url_path, strlen($PROXYNAME));

			// Now build the new URL
			$url =
				$parsedURL['scheme'] . '://' .
				$parsedURL['host'] .
				$url_path .
				(empty($parsedURL['query']) ? '' : '?' . $parsedURL['query']);
		}

		$request['url'] = $url;
		$response = http_request($request);
	}
}

// Forward the response code to our client
// this sets the response code.
// we don't use http_response_code() as that only works for PHP >= 5.4
header('HTTP/1.0 ' . $response['status']);

// forward each returned header...
foreach ($response['headers'] as $key => $value) {

	if (strtolower($key) == 'content-length') {
		// There is no need to specify a content length since we don't do keep
		// alive, and this can cause problems for integration (e.g. gzip output,
		// which would change the content length)
		// Note: overriding with header('Content-length:') will set
		// the content-length to zero for some reason
		continue;
	}

	header($key . ': ' . $value);
}

header('Connection: close');

// output the contents if any
if (null !== $response['socket']) {
	fpassthru($response['socket']);
	fclose($response['socket']);
}

function myErrorHandler($msg)
{
	// 500 could be misleading... 
	// Should we return a special Error when a proxy error occurs?
	header("HTTP/1.0 500 Internal Error");
	die("Gentics Aloha Editor AJAX Gateway Error: $msg");
}
