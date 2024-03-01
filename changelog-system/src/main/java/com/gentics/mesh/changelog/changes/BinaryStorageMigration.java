package com.gentics.mesh.changelog.changes;


import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

import org.apache.commons.io.FileUtils;
import org.apache.tinkerpop.gremlin.process.traversal.dsl.graph.DefaultGraphTraversal;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.core.data.relationship.GraphRelationships;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.madl.frame.ElementFrame;
import com.gentics.mesh.util.StreamUtil;
import com.google.common.io.Files;


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

	private final MeshOptions options;

	public BinaryStorageMigration(MeshOptions options) {
		this.options = options;
	}

	@Override
	public String getName() {
		return "Migration of the binary data";
	}

	@Override
	public String getDescription() {
		return "Creates new graph elements to structure binaries and migrates the local binary filesystem to utilize the new filesystem format.";
	}

	private File dataSourceFolder() {
		String currentDataPath = options.getUploadOptions().getDirectory();
		return new File(new File(currentDataPath).getParentFile(), "binaryFilesMigrationBackup");
	}

	private File dataTargetFolder() {
		String currentDataPath = options.getUploadOptions().getDirectory();
		return new File(currentDataPath);
	}

	@Override
	public void applyInTx() {
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
		Vertex binaryRoot = getGraph().addVertex();
		binaryRoot.property("ferma_type", "BinaryRootImpl");
		binaryRoot.property("uuid", randomUUID());
		meshRoot.addEdge("HAS_BINARY_ROOT", binaryRoot).property("uuid", randomUUID());

		// Iterate over all binary fields and convert them to edges to binaries
		Iterable<Vertex> it = StreamUtil.toIterable(getGraph().traversal().V().has(ElementFrame.TYPE_RESOLUTION_KEY, "BinaryGraphFieldImpl"));
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
		String uuid = oldBinaryField.<String>property("uuid").orElse(null);
		String fileName = oldBinaryField.<String>property(BINARY_FILENAME_PROPERTY_KEY).orElse(null);
		if (fileName == null) {
			log.info("Removing stale binary vertex.");
			for (VertexProperty<String> p : StreamUtil.<VertexProperty<String>>toIterable(oldBinaryField.properties())) {
				log.info("key: " + p.key() + " : " + p.orElse(null));
			}
			oldBinaryField.remove();
			return;
		}

		File file = getOldFile(uuid);

		log.info("Migrating binary field {" + uuid + "}");

		String hash = oldBinaryField.<String>property(OLD_HASH_KEY).orElse(null);
		if (hash == null && !file.exists()) {
			try {
				for (Edge fieldEdge : StreamUtil.toIterable(oldBinaryField.edges(Direction.IN, "HAS_FIELD"))) {
					Vertex container = fieldEdge.outVertex();
					String fieldKey = fieldEdge.<String>property(FIELD_KEY_PROPERTY_KEY).orElse(null);
					for (Vertex node : StreamUtil.toIterable(container.vertices(Direction.IN, "HAS_FIELD_CONTAINER"))) {
						log.warn("Binary field {" + fieldKey + "} in node {" + node.<String>property("uuid")
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
		Integer width = oldBinaryField.<Integer>property(BINARY_IMAGE_WIDTH_PROPERTY_KEY).orElse(null);
		Integer height = oldBinaryField.<Integer>property(BINARY_IMAGE_HEIGHT_PROPERTY_KEY).orElse(null);
		Long size = oldBinaryField.<Long>property(BINARY_FILESIZE_PROPERTY_KEY).orElse(null);

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
		migrateBinaryData(uuid, binary.<String>property("uuid").orElse(null));

		String contentType = oldBinaryField.<String>property(BINARY_CONTENT_TYPE_PROPERTY_KEY).orElse(null);
		String dominantColor = oldBinaryField.<String>property(BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY).orElse(null);
		Set<Edge> oldEdges = new HashSet<>();

		// Iterate over all field edges and create a new edge which will replace it.
		for (Edge fieldEdge : StreamUtil.toIterable(oldBinaryField.edges(Direction.IN, "HAS_FIELD"))) {
			oldEdges.add(fieldEdge);
			Vertex container = fieldEdge.outVertex();
			String fieldKey = fieldEdge.<String>property(FIELD_KEY_PROPERTY_KEY).orElse(null);

			// Correct the missing fieldkey info
			if (fieldKey == null) {
				log.info("Found field edge without fieldkey. Correcting this..");
				Vertex field = fieldEdge.inVertex();
				fieldKey = field.<String>property(FIELD_KEY_PROPERTY_KEY).orElse(null);
				fieldEdge.property(FIELD_KEY_PROPERTY_KEY, fieldKey);
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

	private static String getNewSegmentedPath(String binaryUuid) {
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

	/**
	 * Return a file for the new segmentation format.
	 * 
	 * @param binaryUuid
	 * @return
	 */
	public File getNewFile(String binaryUuid) {
		File folder = new File(dataTargetFolder(), getNewSegmentedPath(binaryUuid));
		File binaryFile = new File(folder, binaryUuid + ".bin");
		return binaryFile;
	}

	/**
	 * Return the old binary path.
	 * 
	 * @param uuid
	 * @return
	 */
	public String getOldSegmentedPath(String uuid) {
		String[] parts = uuid.split("(?<=\\G.{4})");
		StringBuffer buffer = new StringBuffer();
		buffer.append(File.separator);
		for (String part : parts) {
			buffer.append(part + File.separator);
		}
		return buffer.toString();
	}

	/**
	 * Return the file for the old style format.
	 * 
	 * @param uuid
	 * @return
	 */
	public File getOldFile(String uuid) {
		File folder = new File(dataSourceFolder(), getOldSegmentedPath(uuid));
		File binaryFile = new File(folder, uuid + ".bin");
		return binaryFile;
	}

	private Edge createNewFieldEdge(Vertex container, Vertex binary, String fileName, String contentType, String dominantColor, String fieldKey) {
		Edge edge = container.addEdge(GraphRelationships.HAS_FIELD, binary);
		edge.property(ElementFrame.TYPE_RESOLUTION_KEY, "BinaryGraphFieldImpl");
		edge.property("uuid", randomUUID());
		edge.property(BINARY_FILENAME_PROPERTY_KEY, fileName);
		edge.property(BINARY_CONTENT_TYPE_PROPERTY_KEY, contentType);
		if (dominantColor != null) {
			edge.property(BINARY_IMAGE_DOMINANT_COLOR_PROPERTY_KEY, dominantColor);
		}
		edge.property(FIELD_KEY_PROPERTY_KEY, fieldKey);
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
		Vertex binary = getGraph().addVertex();
		binary.property(ElementFrame.TYPE_RESOLUTION_KEY, "BinaryImpl");
		binary.property("uuid", randomUUID());

		if (height != null) {
			binary.property(BINARY_IMAGE_HEIGHT_PROPERTY_KEY, height);
		}
		if (width != null) {
			binary.property(BINARY_IMAGE_WIDTH_PROPERTY_KEY, width);
		}
		binary.property(BINARY_FILESIZE_PROPERTY_KEY, size);
		binary.property(NEW_HASH_KEY, hash);

		binaryRoot.addEdge("HAS_BINARY", binary).property("uuid", randomUUID());

		return binary;
	}

	private Vertex findBinary(String hash, Vertex binaryRoot) {
		for (Vertex binary : StreamUtil.toIterable(binaryRoot.vertices(Direction.OUT, "HAS_BINARY"))) {
			String foundHash = binary.<String>property(NEW_HASH_KEY).orElse(null);
			if (hash.equals(foundHash)) {
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
