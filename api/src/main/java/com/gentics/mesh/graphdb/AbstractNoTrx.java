package com.gentics.mesh.graphdb;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public abstract class AbstractNoTrx extends AbstractTrxBase implements NoTrx {

	private static final Logger log = LoggerFactory.getLogger(AbstractNoTrx.class);

}
