package com.gentics.mesh.core.endpoint.admin.debuginfo;

import java.util.zip.ZipEntry;

import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.buffer.Buffer;

public class DebugInfoBufferEntry implements DebugInfoEntry {
	private final String fileName;
	private final Buffer data;

	private DebugInfoBufferEntry(String fileName, Buffer data) {
		this.fileName = fileName;
		this.data = data;
	}

	public static DebugInfoEntry fromBuffer(String filename, Buffer data) {
		return new DebugInfoBufferEntry(filename, data);
	}

	public static DebugInfoEntry fromString(String filename, String data) {
		return new DebugInfoBufferEntry(
			filename,
			Buffer.buffer(data)
		);
	}

	public static DebugInfoEntry asJson(String fileName, Object data) {
		return fromString(fileName, JsonUtil.toJson(data));
	}

	@Override
	public ZipEntry createZipEntry() {
		return new ZipEntry(fileName);
	}

	@Override
	public String getFileName() {
		return fileName;
	}

	@Override
	public Buffer getData() {
		return data;
	}
}
