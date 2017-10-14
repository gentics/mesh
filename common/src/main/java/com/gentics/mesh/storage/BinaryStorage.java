package com.gentics.mesh.storage;

import io.vertx.core.buffer.Buffer;

public interface BinaryStorage {

	void store(Buffer buffer, String sha512sum, String uuid);

}
