package com.gentics.mesh.changelog.changes;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.security.DigestInputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.apache.commons.io.FileUtils;
import org.apache.commons.lang.StringUtils;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

public class ChangeTVCMigration extends AbstractChange {

	protected static final char[] hexArray = "0123456789abcdef".toCharArray();

	@Override
	public String getUuid() {
		return "A36C972476C147F3AC972476C157F3EF";
	}

	/**
	 * Dummy map which will be used to detect webroot path conflicts without using the graph index
	 */
	Map<String, String> webrootIndexMap = new HashMap<>();

	@Override
	public String getName() {
		return "TVC to dev migration";
	}

	@Override
	public String getDescription() {
		return "Migrate the tvc graph structure to dev";
	}

	@Override
	public void actualApply() {
		try {
			FileUtils.moveDirectory(new File("data/binaryFiles"), new File("data/binaryFilesOld"));
		} catch (IOException e) {
			throw new RuntimeException("Could not move binary files to backup location.", e);
		}

		Vertex meshRoot = getMeshRootVertex();
		meshRoot.removeProperty("databaseVersion");
		Vertex projectRoot = meshRoot.getVertices(Direction.OUT, "HAS_PROJECT_ROOT").iterator().next();

		// Delete bogus vertices
		List<String> ids = Arrays.asList("#70:4", "#79:148", "#62:5");
		for (String id : ids) {
			Vertex vertex = getGraph().getVertex(id);
			getGraph().removeVertex(vertex);
		}

		migrateSchemaContainers();

		Vertex schemaRoot = meshRoot.getVertices(Direction.OUT, "HAS_ROOT_SCHEMA").iterator().next();
		migrateSchemaContainerRootEdges(schemaRoot);

		// Iterate over all projects
		for (Vertex project : projectRoot.getVertices(Direction.OUT, "HAS_PROJECT")) {
			Vertex baseNode = project.getVertices(Direction.OUT, "HAS_ROOT_NODE").iterator().next();
			migrateNode(baseNode, project);
			migrateTagFamilies(project);
			Vertex projectSchemaRoot = project.getVertices(Direction.OUT, "HAS_ROOT_SCHEMA").iterator().next();
			migrateSchemaContainerRootEdges(projectSchemaRoot);
		}

		migrateFermaTypes();
		purgeSearchQueue();

	}

	/**
	 * The edge label has change. Migrate existing edges.
	 */
	private void migrateSchemaContainerRootEdges(Vertex schemaRoot) {
		for (Edge edge : schemaRoot.getEdges(Direction.OUT, "HAS_SCHEMA_CONTAINER")) {
			Vertex container = edge.getVertex(Direction.IN);
			schemaRoot.addEdge("HAS_SCHEMA_CONTAINER_ITEM", container);
			edge.remove();
		}
	}

	private void migrateFermaTypes() {

		int i = 0;
		log.info("Migrating vertices");
		for (Vertex vertex : getGraph().getVertices()) {
			String type = vertex.getProperty("ferma_type");
			if (type != null && type.endsWith("TagGraphFieldContainerImpl")) {
				vertex.setProperty("ferma_type", "com.gentics.mesh.core.data.container.impl.TagGraphFieldContainerImpl");
				i++;
			}

			if (type != null && type.endsWith("SchemaContainerImpl")) {
				vertex.setProperty("ferma_type", "com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl");
				i++;
			}

			if (type != null && type.endsWith("NodeGraphFieldContainerImpl")) {
				vertex.setProperty("ferma_type", "com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl");
				i++;
			}

		}
		log.info("Completed migration of " + i + " vertices.");

		log.info("Migrating edges");
		i = 0;
		for (Edge edge : getGraph().getEdges()) {
			if ("com.gentics.mesh.core.data.node.field.impl.nesting.NodeGraphFieldImpl".equals(edge.getProperty("ferma_type"))) {
				edge.setProperty("ferma_type", "com.gentics.mesh.core.data.node.field.impl.NodeGraphFieldImpl");
				i++;
			}
		}
		log.info("Completed migration of " + i + " edges.");

	}

	private Vertex getOrFixUserReference(Vertex element, String edge) {
		Vertex creator;
		Iterator<Vertex> creatorIterator = element.getVertices(Direction.OUT, edge).iterator();
		if (!creatorIterator.hasNext()) {
			log.error("The element {" + element.getProperty("uuid") + "} has no {" + edge + "}. Using admin instead.");
			creator = findAdmin();
			element.addEdge(edge, creator);
		} else {
			creator = creatorIterator.next();
		}
		return creator;
	}

	private Vertex findAdmin() {
		Vertex admin = null;
		Iterator<Vertex> langIt = getMeshRootVertex().getVertices(Direction.OUT, "HAS_USER_ROOT").iterator().next()
				.getVertices(Direction.OUT, "HAS_USER").iterator();
		while (langIt.hasNext()) {
			Vertex user = langIt.next();
			if (user.getProperty("username").equals("admin")) {
				admin = user;
			}
		}
		return admin;
	}

	private void migrateTagFamilies(Vertex project) {
		Vertex tagFamilyRoot = project.getVertices(Direction.OUT, "HAS_TAGFAMILY_ROOT").iterator().next();
		for (Vertex tagFamily : tagFamilyRoot.getVertices(Direction.OUT, "HAS_TAG_FAMILY")) {

			// Check dates
			Object tagFamilyCreationTimeStamp = tagFamily.getProperty("creation_timestamp");
			if (tagFamilyCreationTimeStamp == null) {
				tagFamily.setProperty("creation_timestamp", System.currentTimeMillis());
			}
			Object tagFamilyEditTimeStamp = tagFamily.getProperty("last_edited_timestamp");
			if (tagFamilyEditTimeStamp == null) {
				tagFamily.setProperty("last_edited_timestamp", System.currentTimeMillis());
			}

			// Create a new tag root vertex for the tagfamily and link the tags to this vertex instead to the tag family itself.
			Vertex tagRoot = getGraph().addVertex("class:TagRootImpl");
			tagRoot.setProperty("ferma_type", "com.gentics.mesh.core.data.root.impl.TagRootImpl");
			tagRoot.setProperty("uuid", randomUUID());
			for (Edge tagEdge : tagFamily.getEdges(Direction.OUT, "HAS_TAG")) {
				Vertex tag = tagEdge.getVertex(Direction.IN);
				tagEdge.remove();
				tagRoot.addEdge("HAS_TAG", tag);
				tag.getEdges(Direction.OUT, "HAS_TAGFAMILY_ROOT").forEach(edge -> edge.remove());
				tag.addEdge("HAS_TAGFAMILY_ROOT", tagFamily);
				if (!tag.getEdges(Direction.OUT, "ASSIGNED_TO_PROJECT").iterator().hasNext()) {
					log.error("Tag {" + tag.getProperty("uuid") + " has no project assigned to it. Fixing it...");
					tag.addEdge("ASSIGNED_TO_PROJECT", project);
				}
				Object creationTimeStamp = tag.getProperty("creation_timestamp");
				if (creationTimeStamp == null) {
					tag.setProperty("creation_timestamp", System.currentTimeMillis());
				}
				Object editTimeStamp = tag.getProperty("last_edited_timestamp");
				if (editTimeStamp == null) {
					tag.setProperty("last_edited_timestamp", System.currentTimeMillis());
				}
			}
			tagFamily.addEdge("HAS_TAG_ROOT", tagRoot);
			if (!tagFamily.getEdges(Direction.OUT, "ASSIGNED_TO_PROJECT").iterator().hasNext()) {
				log.error("TagFamily {" + tagFamily.getProperty("uuid") + " has no project assigned to it. Fixing it...");
				tagFamily.addEdge("ASSIGNED_TO_PROJECT", project);
			}

			getOrFixUserReference(tagFamily, "HAS_EDITOR");
			getOrFixUserReference(tagFamily, "HAS_CREATOR");
		}

	}

	private void purgeSearchQueue() {
		Vertex meshRoot = getMeshRootVertex();
		Vertex sqRoot = meshRoot.getVertices(Direction.OUT, "HAS_SEARCH_QUEUE_ROOT").iterator().next();
		for (Vertex batch : sqRoot.getVertices(Direction.OUT, "HAS_BATCH")) {
			for (Vertex entry : batch.getVertices(Direction.OUT, "HAS_ITEM")) {
				entry.remove();
			}
			batch.remove();
		}

	}

	/**
	 * Add the schema container versions to schema containers.
	 */
	private void migrateSchemaContainers() {
		log.info("Migrating schema containers");
		for (Vertex schemaContainer : getGraph().getVertices()) {
			String type = schemaContainer.getProperty("ferma_type");
			if (type != null && type.endsWith("SchemaContainerImpl")) {
				String name = schemaContainer.getProperty("name");
				log.info("Migrating schema {" + name + "}");
				String json = schemaContainer.getProperty("json");
				schemaContainer.removeProperty("json");
				try {
					JSONObject schema = new JSONObject(json);
					// TVC does not use segment fields. Remove the segment field properties from the schema
					if (schema.has("segmentField")) {
						schema.remove("segmentField");
					}
					if (schema.has("meshVersion")) {
						schema.remove("meshVersion");
					}
					// property was renamed
					if (schema.has("folder")) {
						schema.put("container", schema.getBoolean("folder"));
						schema.remove("folder");
					}

					if (schema.has("binary") && schema.getBoolean("binary")) {
						JSONObject binaryFieldSchema = new JSONObject();
						binaryFieldSchema.put("name", "binary");
						binaryFieldSchema.put("label", "Binary Content");
						binaryFieldSchema.put("required", false);
						binaryFieldSchema.put("type", "binary");
						schema.getJSONArray("fields").put(binaryFieldSchema);
					}

					// Check whether all fields have a name
					JSONArray fields = schema.getJSONArray("fields");
					for (int i = 0; i < fields.length(); i++) {
						// Remove fields which have no name to it.
						JSONObject field = fields.getJSONObject(i);
						if (!field.has("name")) {
							fields.remove(field);
						}
					}
					schema.remove("fields");
					schema.put("fields", fields);
					schema.put("version", "1");
					if (schema.has("binary")) {
						schema.remove("binary");
					}
					json = schema.toString();
				} catch (JSONException e) {
					throw new RuntimeException("Could not parse stored schema {" + json + "}");
				}

				Vertex version = getGraph().addVertex("class:SchemaContainerVersionImpl");
				version.setProperty("uuid", randomUUID());
				version.setProperty("name", name);
				version.setProperty("json", json);
				version.setProperty("version", 1);
				version.setProperty("ferma_type", "com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl");
				schemaContainer.addEdge("HAS_LATEST_VERSION", version);
				schemaContainer.addEdge("HAS_PARENT_CONTAINER", version);
			}
		}
		log.info("Completed migration of schema containers");
	}

	private String hashFile(String path) {
		try {
			MessageDigest md = MessageDigest.getInstance("SHA-512");
			try (InputStream is = Files.newInputStream(Paths.get(path)); DigestInputStream mis = new DigestInputStream(is, md)) {
				byte[] buffer = new byte[4096];
				while (mis.read(buffer) >= 0) {
				}
			}
			byte[] digest = md.digest();
			return bytesToHex(digest);
		} catch (NoSuchAlgorithmException | IOException e) {
			throw new RuntimeException(e);
		}
	}

	/**
	 * Convert the byte array to a hex formatted string.
	 * 
	 * @param bytes
	 * @return
	 */
	public static String bytesToHex(byte[] bytes) {
		char[] hexChars = new char[bytes.length * 2];
		for (int j = 0; j < bytes.length; j++) {
			int v = bytes[j] & 0xFF;
			hexChars[j * 2] = hexArray[v >>> 4];
			hexChars[j * 2 + 1] = hexArray[v & 0x0F];
		}
		return new String(hexChars);
	}

	/**
	 * Migrate the node. Create a binary field for each NGFC if the node contains binary information.
	 * 
	 * @param node
	 * @param project
	 */
	private void migrateNode(Vertex node, Vertex project) {
		String uuid = node.getProperty("uuid");
		log.info("Migrating node {" + uuid + "}");
		Iterator<Vertex> it = node.getVertices(Direction.OUT, "HAS_PARENT_NODE").iterator();
		Vertex parentNode = null;
		if (it.hasNext()) {
			parentNode = it.next();
		}

		if (!node.getVertices(Direction.OUT, "ASSIGNED_TO_PROJECT").iterator().hasNext()) {
			log.error("Node {" + node.getProperty("uuid") + "} has no project assigned to it. Fixing inconsistency...");
			node.addEdge("ASSIGNED_TO_PROJECT", project);
		}

		// Check whether the node has binary property information. We need to migrate those to a new binary graph field.
		String fileName = node.getProperty("binaryFilename");
		if (fileName != null) {
			String oldPath = getSegmentedPath(uuid);
			File oldBinaryFile = new File("data" + File.separator + "binaryFilesOld" + File.separator + oldPath + File.separator + uuid + ".bin");

			node.removeProperty("binaryFilename");
			Object fileSize = node.getProperty("binaryFileSize");
			node.removeProperty("binaryFileSize");

			node.removeProperty("binarySha512Sum");

			String sha512sum = hashFile(oldBinaryFile.getAbsolutePath());
			String mimeType = node.getProperty("binaryContentType");
			node.removeProperty("binaryContentType");

			Object dpi = node.getProperty("binaryImageDPI");
			node.removeProperty("binaryImageDPI");

			Object width = node.getProperty("binaryImageWidth");
			node.removeProperty("binaryImageWidth");

			Object height = node.getProperty("binaryImageHeight");
			node.removeProperty("binaryImageHeight");

			Iterable<Vertex> containers = node.getVertices(Direction.OUT, "HAS_FIELD_CONTAINER");
			// Create the binary field for all found containers
			for (Vertex container : containers) {
				Vertex binaryField = getGraph().addVertex("class:BinaryGraphFieldImpl");
				String binaryFieldUuid = randomUUID();
				binaryField.setProperty("uuid", binaryFieldUuid);
				binaryField.setProperty("ferma_type", "com.gentics.mesh.core.data.node.field.impl.BinaryGraphFieldImpl");
				binaryField.setProperty("binaryFileSize", fileSize);
				binaryField.setProperty("binaryFilename", fileName);
				binaryField.setProperty("binarySha512Sum", sha512sum);
				binaryField.setProperty("binaryContentType", mimeType);
				if (dpi != null) {
					binaryField.setProperty("binaryImageDPI", dpi);
				}
				if (width != null) {
					binaryField.setProperty("binaryImageWidth", width);
				}
				if (height != null) {
					binaryField.setProperty("binaryImageHeight", height);
				}
				binaryField.setProperty("fieldkey", "binary");
				container.addEdge("HAS_FIELD", binaryField);

				// Finally migrate the binary filesystem data

				if (oldBinaryFile.exists()) {
					String path = getSegmentedPath(binaryFieldUuid);
					File newBinaryFile = new File(
							"data" + File.separator + "binaryFiles" + File.separator + path + File.separator + binaryFieldUuid + ".bin");
					try {
						FileUtils.copyFile(oldBinaryFile, newBinaryFile);
					} catch (IOException e) {
						throw new RuntimeException("Could not copy binary file from {" + oldBinaryFile + "} to {" + newBinaryFile + "}", e);
					}
				}

			}
		}

		// Create edge between NGFCs and schema versions
		Vertex schemaContainer = node.getVertices(Direction.OUT, "HAS_SCHEMA_CONTAINER").iterator().next();
		Vertex schemaVersion = schemaContainer.getVertices(Direction.OUT, "HAS_LATEST_VERSION").iterator().next();
		String json = schemaVersion.getProperty("json");
		String segmentField = null;
		try {
			JSONObject schema = new JSONObject(json);
			if (schema.has("segmentField")) {
				segmentField = schema.getString("segmentField");
			}
		} catch (JSONException e) {
			throw new RuntimeException("Could not parse schema json {" + json + "}", e);
		}

		Iterable<Vertex> containers = node.getVertices(Direction.OUT, "HAS_FIELD_CONTAINER");
		for (Vertex container : containers) {

			//Fix date fields which were stored in seconds instead of miliseconds
			for (String key : container.getPropertyKeys()) {
				if (key.endsWith("-date")) {
					Long date = Long.valueOf(container.getProperty(key));
					// Check whether the timestamp is seconds based or miliseconds based
					if (date < 10081440150L) {
						Long newDate = date * 1000;
						log.info("Fixing date for field {" + key + "} from " + date + " to " + newDate);
						container.setProperty(key, String.valueOf(newDate));
					}
				}
			}

			container.addEdge("HAS_SCHEMA_CONTAINER_VERSION", schemaVersion);
			// Add webrootPathInfo property (SegmentValue-ParentFolderUuid)
			if (segmentField != null) {
				String segmentFieldValue = container.getProperty(segmentField + "-string");
				if (!StringUtils.isEmpty(segmentFieldValue)) {
					String webRootPath = segmentFieldValue;
					if (parentNode != null) {
						webRootPath += "-" + parentNode.getProperty("uuid");
					}
					String id = uuid + "-" + container.getProperty("uuid");
					if (webrootIndexMap.containsKey(webRootPath)) {
						log.error("Found conflicting node:" + id + " with webroot path info " + webRootPath);
						// Randomize the value
						segmentFieldValue = segmentFieldValue + "_" + randomUUID();
						container.setProperty(segmentField + "-string", segmentFieldValue);
						// Set the new webroot path
						webRootPath = segmentFieldValue;
						if (parentNode != null) {
							webRootPath += "-" + parentNode.getProperty("uuid");
						}
					}
					webrootIndexMap.put(webRootPath, id);
					container.setProperty("webrootPathInfo", webRootPath);
				}
			}
		}

		// Now check the children and migrate structure
		Iterable<Edge> childrenEdges = node.getEdges(Direction.IN, "HAS_PARENT_NODE");
		for (Edge childEdge : childrenEdges) {
			migrateNode(childEdge.getVertex(Direction.OUT), project);
		}

	}

	public String getSegmentedPath(String uuid) {
		String[] parts = uuid.split("(?<=\\G.{4})");
		StringBuffer buffer = new StringBuffer();
		buffer.append(File.separator);
		for (String part : parts) {
			buffer.append(part + File.separator);
		}
		return buffer.toString();
	}

}
