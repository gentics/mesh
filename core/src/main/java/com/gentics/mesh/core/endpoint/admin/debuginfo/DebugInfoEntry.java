package com.gentics.mesh.core.endpoint.admin.debuginfo;

import java.util.zip.ZipEntry;

import io.reactivex.Flowable;
import io.vertx.core.buffer.Buffer;

/**
 * Entry for the debug info zip file.
 */
public interface DebugInfoEntry {

	/**
	 * Zip entry.
	 * 
	 * @return
	 */
	ZipEntry createZipEntry();

	/**
	 * Filename within the zip.
	 * 
	 * @return
	 */
	String getFileName();

	/**
	 * Data which should be added to the zip.
	 * 
	 * @return
	 */
	Flowable<Buffer> getData();
}
