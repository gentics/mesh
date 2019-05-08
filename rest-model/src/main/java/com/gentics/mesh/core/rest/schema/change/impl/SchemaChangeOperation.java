package com.gentics.mesh.core.rest.schema.change.impl;

import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.INTERNAL_SERVER_ERROR;

import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.util.Map;

import org.apache.commons.io.IOUtils;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Name of schema update operations which can be handled.
 */
public enum SchemaChangeOperation {

	ADDFIELD, REMOVEFIELD, CHANGEFIELDTYPE, UPDATEFIELD, UPDATESCHEMA, UPDATEMICROSCHEMA, EMPTY;

	private static final Logger log = LoggerFactory.getLogger(SchemaChangeOperation.class);



}
