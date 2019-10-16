package com.gentics.mesh.core.endpoint.admin.debuginfo;

import java.util.zip.ZipEntry;

import io.vertx.core.buffer.Buffer;

public class DebugInfoEntry {
	private final String fileName;
	private final Buffer data;

	private DebugInfoEntry(String fileName, Buffer data) {
		this.fileName = fileName;
		this.data = data;
	}

	public static DebugInfoEntry fromString(String filename, String data) {
		return new DebugInfoEntry(
			filename,
			Buffer.buffer(data)
		);
	}

	public ZipEntry createZipEntry() {
		return new ZipEntry(fileName);
	}

	public String getFileName() {
		return fileName;
	}

	public Buffer getData() {
		return data;
	}
}
