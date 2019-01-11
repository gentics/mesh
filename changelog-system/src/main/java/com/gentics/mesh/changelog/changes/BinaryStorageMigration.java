package com.gentics.mesh.changelog.changes;

import static com.tinkerpop.blueprints.Direction.IN;
import static com.tinkerpop.blueprints.Direction.OUT;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;

import com.gentics.mesh.Mesh;
import com.gentics.mesh.changelog.AbstractChange;
import com.google.common.io.Files;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

/**
 * Migrates the old binary format to the new format.
 */
public class BinaryStorageMigration extends AbstractChange {

	static final String FIELD_KEY_PROPERTY_KEY = "fieldkey";

	static final String EMPTY_HASH = "cf83e1357eefb8bdf1542850d66d8007d620e4050b5715dc83f4a921d36ce9ce47d0d13c5d85f2b0ff8318d2877eec2f63b931bd47417a81a538327af927da3e";

	static final String NEW_HASH_KEY = "sha512sum";

	static final String OLD_HASH_KEY = "binarySha512Sum";

	static final String BINARY_FILESIZE_PROPERTY_KEY = "binaryFileSize";

	static final String BINARY_FILENAME_PROPERTY_KEY = "binaryFilename";

	static final String BINARY_CONTENT_TYPE_PROPERTY_KEY = "binaryContentType";

	static final String BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY = "binaryImageDominantColor";

	static final String BINARY_IMAGE_WIDTH_PROPERTY_KEY = "binaryImageWidth";

	static final String BINARY_IMAGE_HEIGHT_PROPERTY_KEY = "binaryImageHeight";

	@Override
	public String getName() {
		return "Migration of the binary data";
	}

	@Override
	public String getDescription() {
		return "Creates new graph elements to structure binaries and migrates the local binary filesystem to utilize the new filesystem format.";
	}

	private static File dataSourceFolder() {
		String currentDataPath = Mesh.mesh().getOptions().getUploadOptions().getDirectory();
		return new File(new File(currentDataPath).getParentFile(), "binaryFilesMigrationBackup");
	}

	private static File dataTargetFolder() {
		String currentDataPath = Mesh.mesh().getOptions().getUploadOptions().getDirectory();
		return new File(currentDataPath);
	}

	@Override
	public void actualApply() {
		// Move the data directory away
		try {
			log.info("Moving current data directory away to {" + dataSourceFolder().getAbsolutePath() + "}");
			FileUtils.moveDirectory(dataTargetFolder(), dataSourceFolder());
		} catch (Exception e) {
			throw new RuntimeException("Could not move data folder to backup location {" + dataTargetFolder().getAbsolutePath()
					+ "}. Maybe the permissions not allowing this?");
		}

		// Create binary root
		Vertex meshRoot = getMeshRootVertex();
		Vertex binaryRoot = getGraph().addVertex("class:BinaryRootImpl");
		binaryRoot.setProperty("ferma_type", "BinaryRootImpl");
		binaryRoot.setProperty("uuid", randomUUID());
		meshRoot.addEdge("HAS_BINARY_ROOT", binaryRoot).setProperty("uuid", randomUUID());

		// Iterate over all binary fields and convert them to edges to binaries
		Iterable<Vertex> it = getGraph().getVertices("@class", "BinaryGraphFieldImpl");
		for (Vertex binaryField : it) {
			migrateField(binaryField, binaryRoot);
		}
	}

	/**
	 * Migrate the given binary field.
	 * 
	 * @param oldBinaryField
	 * @param binaryRoot
	 */
	private void migrateField(Vertex oldBinaryField, Vertex binaryRoot) {
		String uuid = oldBinaryField.getProperty("uuid");
		String fileName = oldBinaryField.getProperty(BINARY_FILENAME_PROPERTY_KEY);
		if (fileName == null) {
			log.info("Removing stale binary vertex.");
			for (String key : oldBinaryField.getPropertyKeys()) {
				System.out.println("key: " + key + " : " + oldBinaryField.getProperty(key));
			}
			oldBinaryField.remove();
			return;
		}

		File file = getOldFile(uuid);

		log.info("Migrating binary field {" + uuid + "}");

		String hash = oldBinaryField.getProperty(OLD_HASH_KEY);
		if (hash == null && !file.exists()) {
			try {
				for (Edge fieldEdge : oldBinaryField.getEdges(Direction.IN, "HAS_FIELD")) {
					Vertex container = fieldEdge.getVertex(OUT);
					String fieldKey = fieldEdge.getProperty(FIELD_KEY_PROPERTY_KEY);
					for (Vertex node : container.getVertices(IN, "HAS_FIELD_CONTAINER")) {
						log.warn("Binary field {" + fieldKey + "} in node {" + node.getProperty("uuid")
								+ "} has no binary data file. Touching the file.");
					}
				}

				// Create an empty file and use the hash of an empty file
				file.getParentFile().mkdirs();
				new FileOutputStream(file).close();
				hash = EMPTY_HASH;

			} catch (IOException e) {
				log.error("Encountered field with missing hash and data. Could not touch file {" + file + "}");
				throw new RuntimeException(e);
			}
		} else if (hash == null) {
			if (file.exists()) {
				log.info("Found file {" + file.getAbsolutePath() + "} but no hash. Generating hash now.");
				hash = com.gentics.mesh.changelog.utils.FileUtils.hash(file.getAbsolutePath());
			}
		}
		Integer width = oldBinaryField.getProperty(BINARY_IMAGE_WIDTH_PROPERTY_KEY);
		Integer height = oldBinaryField.getProperty(BINARY_IMAGE_HEIGHT_PROPERTY_KEY);
		Long size = oldBinaryField.getProperty(BINARY_FILESIZE_PROPERTY_KEY);

		// Try to fetch the filesize if it is missing
		if (size == null) {
			if (file.exists()) {
				size = file.length();
			}
		}
		Vertex binary = findBinary(hash, binaryRoot);
		if (binary == null) {
			binary = createBinary(hash, size, height, width, binaryRoot);
		}
		migrateBinaryData(uuid, binary.getProperty("uuid"));

		String contentType = oldBinaryField.getProperty(BINARY_CONTENT_TYPE_PROPERTY_KEY);
		String dominantColor = oldBinaryField.getProperty(BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY);
		Set<Edge> oldEdges = new HashSet<>();

		// Iterate over all field edges and create a new edge which will replace it.
		for (Edge fieldEdge : oldBinaryField.getEdges(Direction.IN, "HAS_FIELD")) {
			oldEdges.add(fieldEdge);
			Vertex container = fieldEdge.getVertex(OUT);
			String fieldKey = fieldEdge.getProperty(FIELD_KEY_PROPERTY_KEY);

			// Correct the missing fieldkey info
			if (fieldKey == null) {
				log.info("Found field edge without fieldkey. Correcting this..");
				Vertex field = fieldEdge.getVertex(IN);
				fieldKey = field.getProperty(FIELD_KEY_PROPERTY_KEY);
				fieldEdge.setProperty(FIELD_KEY_PROPERTY_KEY, fieldKey);
			}

			log.info("Creating new edge for binary field between container {" + container + "} and binary with hash {" + hash + "}");
			createNewFieldEdge(container, binary, fileName, contentType, dominantColor, fieldKey);
		}
		// Finally remove the old edges and the old field vertex
		log.info("Removing legacy fields and edges");
		for (Edge edge : oldEdges) {
			edge.remove();
		}
		oldBinaryField.remove();
	}

	/**
	 * Migrates the binary data from the filesystem to the new folder / file structure.
	 * 
	 * @param fieldUuid
	 *            Old field uuid
	 * @param binaryUuid
	 *            Uuid of the new binary field
	 */
	private void migrateBinaryData(String fieldUuid, String binaryUuid) {
		File oldFile = getOldFile(fieldUuid);
		File newFile = getNewFile(binaryUuid);

		// Already migrated
		if (newFile.exists()) {
			return;
		}
		log.info("Migrating file {" + oldFile.getAbsolutePath() + "} to {" + newFile.getAbsolutePath() + "}");
		if (oldFile.exists()) {
			try {
				// Create the folders
				newFile.getParentFile().mkdirs();
				Files.copy(oldFile, newFile);
			} catch (IOException e) {
				log.error("Error while coping file from {" + oldFile + " to " + newFile + "}");
			}
		} else {
			log.error("The binary data for field {" + fieldUuid + "} and hash {" + binaryUuid + "} could not be found.");
		}

	}

	public static String getNewSegmentedPath(String binaryUuid) {
		String partA = binaryUuid.substring(0, 2);
		String partB = binaryUuid.substring(2, 4);
		StringBuffer buffer = new StringBuffer();
		buffer.append(File.separator);
		buffer.append(partA);
		buffer.append(File.separator);
		buffer.append(partB);
		buffer.append(File.separator);
		return buffer.toString();
	}

	public static File getNewFile(String binaryUuid) {
		File folder = new File(dataTargetFolder(), getNewSegmentedPath(binaryUuid));
		File binaryFile = new File(folder, binaryUuid + ".bin");
		return binaryFile;
	}

	public static String getOldSegmentedPath(String uuid) {
		String[] parts = uuid.split("(?<=\\G.{4})");
		StringBuffer buffer = new StringBuffer();
		buffer.append(File.separator);
		for (String part : parts) {
			buffer.append(part + File.separator);
		}
		return buffer.toString();
	}

	public static File getOldFile(String uuid) {
		File folder = new File(dataSourceFolder(), getOldSegmentedPath(uuid));
		File binaryFile = new File(folder, uuid + ".bin");
		return binaryFile;
	}

	private Edge createNewFieldEdge(Vertex container, Vertex binary, String fileName, String contentType, String dominantColor, String fieldKey) {
		Edge edge = getGraph().addEdge("class:BinaryGraphFieldImpl", container, binary, "HAS_FIELD");
		edge.setProperty("ferma_type", "BinaryGraphFieldImpl");
		edge.setProperty("uuid", randomUUID());
		edge.setProperty(BINARY_FILENAME_PROPERTY_KEY, fileName);
		edge.setProperty(BINARY_CONTENT_TYPE_PROPERTY_KEY, contentType);
		if (dominantColor != null) {
			edge.setProperty(BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY, dominantColor);
		}
		edge.setProperty(FIELD_KEY_PROPERTY_KEY, fieldKey);
		return null;
	}

	/**
	 * Create new binary vertex and link it to the binary root.
	 * 
	 * @param hash
	 * @param binaryRoot
	 * @return
	 */
	private Vertex createBinary(String hash, Long size, Integer height, Integer width, Vertex binaryRoot) {
		Vertex binary = getGraph().addVertex("class:BinaryImpl");
		binary.setProperty("ferma_type", "BinaryImpl");
		binary.setProperty("uuid", randomUUID());

		if (height != null) {
			binary.setProperty(BINARY_IMAGE_HEIGHT_PROPERTY_KEY, height);
		}
		if (width != null) {
			binary.setProperty(BINARY_IMAGE_WIDTH_PROPERTY_KEY, width);
		}
		binary.setProperty(BINARY_FILESIZE_PROPERTY_KEY, size);
		binary.setProperty(NEW_HASH_KEY, hash);

		binaryRoot.addEdge("HAS_BINARY", binary).setProperty("uuid", randomUUID());

		return binary;
	}

	private Vertex findBinary(String hash, Vertex binaryRoot) {
		for (Vertex binary : binaryRoot.getVertices(OUT, "HAS_BINARY")) {
			String foundHash = binary.getProperty(NEW_HASH_KEY);
			if (foundHash.equals(hash)) {
				return binary;
			}
		}
		return null;
	}

	@Override
	public String getUuid() {
		return "4277C9B6BF724621B7C9B6BF7246217A";
	}

	@Override
	public boolean requiresReindex() {
		return true;
	}

}
