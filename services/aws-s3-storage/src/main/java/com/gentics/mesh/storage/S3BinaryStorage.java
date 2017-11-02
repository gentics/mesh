package com.gentics.mesh.storage;

import com.gentics.mesh.storage.BinaryStorage;

import io.vertx.core.buffer.Buffer;

/**
 * Storage implementation which is backed by AWS S3.
 */
public class S3BinaryStorage implements BinaryStorage {

	@Override
	public void store(Buffer buffer, String sha512sum, String uuid) {
		// TODO Auto-generated method stub
		
	}

}
