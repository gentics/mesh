package com.gentics.mesh.core.rest.schema.change.impl;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Name of schema update operations which can be handled.
 */
public enum SchemaChangeOperation {

	ADDFIELD, REMOVEFIELD, CHANGEFIELDTYPE, UPDATEFIELD, UPDATESCHEMA, UPDATEMICROSCHEMA, EMPTY;

	private static final Logger log = LoggerFactory.getLogger(SchemaChangeOperation.class);



}
