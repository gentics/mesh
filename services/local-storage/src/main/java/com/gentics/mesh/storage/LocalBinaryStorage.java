package com.gentics.mesh.storage;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.etc.config.MeshUploadOptions;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.core.streams.Pump;
import io.vertx.core.streams.ReadStream;
import rx.Completable;

@Singleton
public class LocalBinaryStorage extends AbstractBinaryStorage {

	private static final Logger log = LoggerFactory.getLogger(LocalBinaryStorage.class);

	@Inject
	public LocalBinaryStorage() {
	}

	@Override
	public Completable store(ReadStream<Buffer> stream, String sha512sum) {
		return Completable.create(sub -> {
			FileSystem fileSystem = Mesh.vertx().fileSystem();
			String path = getFilePath(sha512sum);
			log.debug("Saving data for field to path {" + path + "}");
			MeshUploadOptions uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
			File uploadFolder = new File(uploadOptions.getDirectory(), getSegmentedPath(sha512sum));

			if (!uploadFolder.exists()) {
				uploadFolder.mkdirs();

				// log.error("Failed to create target folder {" + uploadFolder.getAbsolutePath() + "}", error);
				// throw error(BAD_REQUEST, "node_error_upload_failed", error);

				if (log.isDebugEnabled()) {
					log.debug("Created folder {" + uploadFolder.getAbsolutePath() + "}");
				}
			}
			File targetFile = new File(uploadFolder, sha512sum + ".bin");

			fileSystem.open(targetFile.getAbsolutePath(), new OpenOptions(), result -> {
				if (result.succeeded()) {
					AsyncFile file = result.result();
					Pump pump = Pump.pump(stream, file);
					pump.start();
					stream.endHandler(eh -> {
						sub.onCompleted();
					});
				} else {

				}
			});
			// log.error("Failed to save file to {" + targetPath + "}", error);
			// throw error(INTERNAL_SERVER_ERROR, "node_error_upload_failed", error);
		});
	}

	public String getFilePath(String sha512sum) {
		File folder = new File(Mesh.mesh().getOptions().getUploadOptions().getDirectory(), getSegmentedPath(sha512sum));
		File binaryFile = new File(folder, sha512sum + ".bin");
		return binaryFile.getAbsolutePath();
	}

	@Override
	public boolean exists(BinaryGraphField field) {
		String sha512sum = field.getBinary().getSHA512Sum();
		return false;
	}

	@Override
	public ReadStream<Buffer> read(BinaryGraphField field) {
		String sha512sum = field.getBinary().getSHA512Sum();
		String path = getFilePath(sha512sum);
		return Mesh.vertx().fileSystem().openBlocking(path, new OpenOptions());
	}

	public static String getSegmentedPath(String binaryFieldUuid) {
		String[] parts = binaryFieldUuid.split("(?<=\\G.{4})");
		StringBuffer buffer = new StringBuffer();
		buffer.append(File.separator);
		for (String part : parts) {
			buffer.append(part + File.separator);
		}
		return buffer.toString();
	}

	/**
	 * Move the file upload from the temporary upload directory to the given target path.
	 * 
	 * @param fileUpload
	 * @param targetPath
	 */
	private void moveUploadIntoPlace(String fileUpload, String targetPath) {
		FileSystem fileSystem = Mesh.vertx().fileSystem();
		fileSystem.moveBlocking(fileUpload, targetPath);
		if (log.isDebugEnabled()) {
			log.debug("Moved upload file from {" + fileUpload + "} to {" + targetPath + "}");
		}
		// log.error("Failed to move upload file from {" + fileUpload.uploadedFileName() + "} to {" + targetPath + "}", error);
	}

}
