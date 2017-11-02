package com.gentics.mesh.storage;

import java.io.File;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.core.data.node.field.BinaryGraphField;
import com.gentics.mesh.etc.config.MeshUploadOptions;

import io.vertx.core.buffer.Buffer;
import io.vertx.core.file.AsyncFile;
import io.vertx.core.file.FileSystem;
import io.vertx.core.file.OpenOptions;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.ext.web.FileUpload;
import rx.Single;

public class LocalBinaryStorage implements BinaryStorage {

	private static final Logger log = LoggerFactory.getLogger(LocalBinaryStorage.class);

	@Override
	public void store(Buffer buffer, String sha512sum, String uuid) {

		File targetFile = getFile(field);
		String targetPath = targetFile.getAbsolutePath();
		checkUploadFolderExists(uploadFolder);
		deletePotentialUpload(targetPath);

		moveUploadIntoPlace(fileUpload, targetPath);
		FileSystem fileSystem = Mesh.vertx().fileSystem();
		fileSystem.writeFileBlocking(targetPath, buffer);
		// log.error("Failed to save file to {" + targetPath + "}", error);
		// throw error(INTERNAL_SERVER_ERROR, "node_error_upload_failed", error);
	}

	public String getFilePath(String binaryFieldUuid) {
		String path = getSegmentedPath(binaryFieldUuid);
		File folder = new File(Mesh.mesh().getOptions().getUploadOptions().getDirectory(), path);
		File binaryFile = new File(folder, binaryFieldUuid + ".bin");
		return binaryFile.getAbsolutePath();
	}

	@Override
	public boolean exists(BinaryGraphField field) {
		File file = getFile(field);
		return file.exists();
	}

	private File getFile(BinaryGraphField field) {
		String segmentedPath = getSegmentedPath(field.getUuid());
		MeshUploadOptions uploadOptions = Mesh.mesh().getOptions().getUploadOptions();
		File uploadFolder = new File(uploadOptions.getDirectory(), segmentedPath);
		File targetFile = new File(uploadFolder, field.getUuid() + ".bin");
		return targetFile;
	}

	// /**
	// * Return a single that holds an AsyncFile for the binary.
	// *
	// * @return
	// */
	// Single<AsyncFile> getFileStream();
	//
	// /**
	// * Return the file that points to the binary file within the binary file storage.
	// *
	// * @return Found file or null when no binary file could be found
	// */
	// File getFile();

	/**
	 * Returns the segmented path that points to the binary file within the binary file location. The segmented path is build using the uuid of the binary field
	 * vertex.
	 * 
	 * @return
	 */
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
	 * Return the async file for the field.
	 * 
	 * @param field
	 * @return
	 */
	public Single<AsyncFile> getFile(BinaryGraphField field) {
		return Single.create(new io.vertx.rx.java.SingleOnSubscribeAdapter<AsyncFile>(fut -> {
			Mesh.vertx().fileSystem().open(getFile(field), new OpenOptions().setRead(true), fut);
		}));
	}

	/**
	 * Delete potential existing file uploads from the given path.
	 * 
	 * @param targetPath
	 */
	private void deletePotentialUpload(String targetPath) {
		FileSystem fileSystem = Mesh.vertx().fileSystem();
		if (fileSystem.existsBlocking(targetPath)) {
			// Deleting of existing binary file
			fileSystem.deleteBlocking(targetPath);
		}
	}

	/**
	 * Move the file upload from the temporary upload directory to the given target path.
	 * 
	 * @param fileUpload
	 * @param targetPath
	 */
	private void moveUploadIntoPlace(FileUpload fileUpload, String targetPath) {
		FileSystem fileSystem = Mesh.vertx().fileSystem();
		fileSystem.moveBlocking(fileUpload.uploadedFileName(), targetPath);
		if (log.isDebugEnabled()) {
			log.debug("Moved upload file from {" + fileUpload.uploadedFileName() + "} to {" + targetPath + "}");
		}
	}

	/**
	 * Check the target upload folder and create it if needed.
	 * 
	 * @param uploadFolder
	 */
	private void checkUploadFolderExists(File uploadFolder) {
		boolean folderExists = uploadFolder.exists();
		if (!folderExists) {
			uploadFolder.mkdirs();
			if (log.isDebugEnabled()) {
				log.debug("Created folder {" + uploadFolder.getAbsolutePath() + "}");
			}
		}
	}

}
