package com.gentics.mesh.core.endpoint.admin.debuginfo;

import java.util.zip.ZipEntry;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

public interface DebugInfoEntry {
	ZipEntry createZipEntry();
	String getFileName();
	Flowable<Buffer> getData();
}
