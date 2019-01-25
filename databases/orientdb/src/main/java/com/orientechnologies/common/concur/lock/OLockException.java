package com.orientechnologies.common.concur.lock;

import com.orientechnologies.common.exception.OSystemException;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class OLockException extends OSystemException {
	private static final long serialVersionUID = 2215169397325875189L;

	private static final Logger log = LoggerFactory.getLogger(OLockException.class);

	public OLockException(OLockException exception) {
		super(exception);
		if (log.isTraceEnabled()) {
			log.trace("Lock error", exception);
			log.trace("Lock error", this);
		}
	}

	public OLockException(String iMessage) {
		super(iMessage);
		if (log.isTraceEnabled()) {
			log.trace("Lock error:" + iMessage, this);
		}
	}
}
