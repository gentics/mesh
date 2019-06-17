package com.gentics.mesh.handler;

import io.vertx.ext.web.RoutingContext;

/**
 * Handler which will take care of a byte range request and return the 
 * requested chunked data of the given binary field data. 
 */
public interface RangeRequestHandler {

	/**
	 * Default of whether vary header should be sent.
	 */
	boolean DEFAULT_SEND_VARY_HEADER = true;

	/**
	 * The default max cache size
	 */
	int DEFAULT_MAX_CACHE_SIZE = 10000;

	/**
	 * Default max age for cache headers
	 */
	long DEFAULT_MAX_AGE_SECONDS = 86400; // One day

	/**
	 * Default value of whether files are read -only and never will be updated
	 */
	boolean DEFAULT_FILES_READ_ONLY = true;

	/**
	 * Process the request for the requested binary file.
	 * 
	 * @param rc
	 * @param localPath
	 * @param contentType
	 */
	void handle(RoutingContext rc, String localPath, String contentType);

}
