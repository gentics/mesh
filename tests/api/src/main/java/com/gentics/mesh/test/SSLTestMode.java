package com.gentics.mesh.test;

public enum SSLTestMode {

	/**
	 * Run mesh with SSL server disabled.
	 */
	OFF,

	/**
	 * Normal SSL server which does not use client certs.
	 */
	NORMAL,

	/**
	 * Configure the server so that it can accept a client cert.
	 */
	CLIENT_CERT_REQUEST,

	/**
	 * Configure the server so that it will require a client cert.
	 */
	CLIENT_CERT_REQUIRED;
}
