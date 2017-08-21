package com.gentics.mesh.core.rest.common;

import java.nio.charset.StandardCharsets;

import com.gentics.mesh.doc.GenerateDocumentation;
import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.shareddata.Shareable;
import io.vertx.core.shareddata.impl.ClusterSerializable;

/**
 * Marker interface for all rest models.
 */
@GenerateDocumentation
public interface RestModel extends ClusterSerializable, Shareable {

	default void writeToBuffer(Buffer buffer) {
		String encoded = JsonUtil.toJson(this);
		byte[] bytes = encoded.getBytes(StandardCharsets.UTF_8);
		buffer.appendInt(bytes.length);
		buffer.appendBytes(bytes);
	}

	default int readFromBuffer(int pos, Buffer buffer) {
		int length = buffer.getInt(pos);
		int start = pos + 4;
		String encoded = buffer.getString(start, start + length);
		JsonUtil.readValue(encoded, getClass());
		return pos + length + 4;
	}

}
