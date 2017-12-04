package com.gentics.mesh.storage;

import java.io.File;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.etc.config.MeshUploadOptions;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.rxjava.core.file.AsyncFile;
import io.vertx.rxjava.core.file.FileSystem;
import io.vertx.rxjava.core.streams.Pump;
import io.vertx.rxjava.core.streams.WriteStream;
import rx.Completable;
import rx.Observable;
import rx.Single;

@Singleton
public class LocalBinaryStorage extends AbstractBinaryStorage {

	private static final Logger log = LoggerFactory.getLogger(LocalBinaryStorage.class);

	@Inject
	public LocalBinaryStorage() {
	}

	@Override
	public Completable store(Observable<Buffer> stream, String sha512sum) {
		return Completable.defer(() -> {
			FileSystem fileSystem = FileSystem.newInstance(Mesh.vertx().fileSystem());
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

			return fileSystem.rxOpen(targetFile.getAbsolutePath(), new OpenOptions()).map(file -> {
				Pump pump = Pump.pump(stream, (WriteStream) file);
				pump.start();
				return stream;
			}).toCompletable();
			// log.error("Failed to save file to {" + targetPath + "}", error);
			// throw error(INTERNAL_SERVER_ERROR, "node_error_upload_failed", error);
		});
	}

	/**
	 * Return the absolute path to the binary data for the given hashsum.
	 * 
	 * @param sha512sum
	 * @return
	 */
	public String getFilePath(String sha512sum) {
		File folder = new File(Mesh.mesh().getOptions().getUploadOptions().getDirectory(), getSegmentedPath(sha512sum));
		File binaryFile = new File(folder, sha512sum + ".bin");
		return binaryFile.getAbsolutePath();
	}

	@Override
	public boolean exists(BinaryGraphField field) {
		String sha512sum = field.getBinary().getSHA512Sum();
		return new File(getFilePath(sha512sum)).exists();
	}

	@Override
	public Observable<Buffer> read(String hashsum) {
		String path = getFilePath(hashsum);
		Observable<Buffer> obs = FileSystem.newInstance(Mesh.vertx().fileSystem()).rxOpen(path, new OpenOptions()).toObservable().flatMap(
				AsyncFile::toObservable).map(buf -> buf.getDelegate());
		return obs;
	}

	public static String getSegmentedPath(String binaryFieldUuid) {
		String[] parts = binaryFieldUuid.split("(?<=\\G.{12})");
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
		FileSystem fileSystem = new FileSystem(Mesh.vertx().fileSystem());
		fileSystem.moveBlocking(fileUpload, targetPath);
		if (log.isDebugEnabled()) {
			log.debug("Moved upload file from {" + fileUpload + "} to {" + targetPath + "}");
		}
		// log.error("Failed to move upload file from {" + fileUpload.uploadedFileName() + "} to {" + targetPath + "}", error);
	}

}
