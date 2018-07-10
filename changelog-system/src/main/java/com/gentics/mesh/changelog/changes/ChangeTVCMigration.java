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
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

import com.gentics.mesh.changelog.AbstractChange;

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
	public void apply() {
		try {
			FileUtils.moveDirectory(new File("data/binaryFiles"), new File("data/binaryFilesOld"));
		} catch (IOException e) {
			throw new RuntimeException("Could not move binary files to backup location.", e);
		}

		Vertex meshRoot = getMeshRootVertex();
		meshRoot.property("databaseVersion").remove();
		Vertex projectRoot = meshRoot.vertices(Direction.OUT, "HAS_PROJECT_ROOT").next();

		// Delete bogus vertices
		List<String> ids = Arrays.asList("#70:4", "#79:148", "#62:5");
		for (String id : ids) {
			Vertex vertex = getGraph().vertices(id).next();
			vertex.remove();
		}

		migrateSchemaContainers();

		Vertex schemaRoot = meshRoot.vertices(Direction.OUT, "HAS_ROOT_SCHEMA").next();
		migrateSchemaContainerRootEdges(schemaRoot);

		// Iterate over all projects
		for (Vertex project : (Iterable<Vertex>) () -> projectRoot.vertices(Direction.OUT, "HAS_PROJECT")) {
			Vertex baseNode = project.vertices(Direction.OUT, "HAS_ROOT_NODE").next();
			migrateNode(baseNode, project);
			migrateTagFamilies(project);
			Vertex projectSchemaRoot = project.vertices(Direction.OUT, "HAS_ROOT_SCHEMA").next();
			migrateSchemaContainerRootEdges(projectSchemaRoot);
		}

		migrateFermaTypes();
		purgeSearchQueue();

	}

	/**
	 * The edge label has change. Migrate existing edges.
	 */
	private void migrateSchemaContainerRootEdges(Vertex schemaRoot) {
		for (Edge edge : (Iterable<Edge>) () -> schemaRoot.edges(Direction.OUT, "HAS_SCHEMA_CONTAINER")) {
			Vertex container = edge.inVertex();
			schemaRoot.addEdge("HAS_SCHEMA_CONTAINER_ITEM", container);
			edge.remove();
		}
	}

	private void migrateFermaTypes() {

		int i = 0;
		log.info("Migrating vertices");
		for (Vertex vertex : (Iterable<Vertex>) () -> getGraph().vertices()) {
			String type = vertex.value("ferma_type");
			if (type != null && type.endsWith("TagGraphFieldContainerImpl")) {
				vertex.property("ferma_type", "com.gentics.mesh.core.data.container.impl.TagGraphFieldContainerImpl");
				i++;
			}

			if (type != null && type.endsWith("SchemaContainerImpl")) {
				vertex.property("ferma_type", "com.gentics.mesh.core.data.schema.impl.SchemaContainerImpl");
				i++;
			}

			if (type != null && type.endsWith("NodeGraphFieldContainerImpl")) {
				vertex.property("ferma_type", "com.gentics.mesh.core.data.container.impl.NodeGraphFieldContainerImpl");
				i++;
			}

		}
		log.info("Completed migration of " + i + " vertices.");

		log.info("Migrating edges");
		i = 0;
		for (Edge edge : (Iterable<Edge>) () -> getGraph().edges()) {
			if ("com.gentics.mesh.core.data.node.field.impl.nesting.NodeGraphFieldImpl".equals(edge.value("ferma_type"))) {
				edge.property("ferma_type", "com.gentics.mesh.core.data.node.field.impl.NodeGraphFieldImpl");
				i++;
			}
		}
		log.info("Completed migration of " + i + " edges.");

	}

	private Vertex getOrFixUserReference(Vertex element, String edge) {
		Vertex creator;
		Iterator<Vertex> creatorIterator = element.vertices(Direction.OUT, edge);
		if (!creatorIterator.hasNext()) {
			log.error("The element {" + element.value("uuid") + "} has no {" + edge + "}. Using admin instead.");
			creator = findAdmin();
			element.addEdge(edge, creator);
		} else {
			creator = creatorIterator.next();
		}
		return creator;
	}

	private Vertex findAdmin() {
		Vertex admin = null;
		Iterator<Vertex> langIt = getMeshRootVertex().vertices(Direction.OUT, "HAS_USER_ROOT").next()
			.vertices(Direction.OUT, "HAS_USER");
		while (langIt.hasNext()) {
			Vertex user = langIt.next();
			if (user.value("username").equals("admin")) {
				admin = user;
			}
		}
		return admin;
	}

	private void migrateTagFamilies(Vertex project) {
		Vertex tagFamilyRoot = project.vertices(Direction.OUT, "HAS_TAGFAMILY_ROOT").next();
		for (Vertex tagFamily : (Iterable<Vertex>) () -> tagFamilyRoot.vertices(Direction.OUT, "HAS_TAG_FAMILY")) {

			// Check dates
			Object tagFamilyCreationTimeStamp = tagFamily.property("creation_timestamp");
			if (tagFamilyCreationTimeStamp == null) {
				tagFamily.property("creation_timestamp", System.currentTimeMillis());
			}
			Object tagFamilyEditTimeStamp = tagFamily.property("last_edited_timestamp");
			if (tagFamilyEditTimeStamp == null) {
				tagFamily.property("last_edited_timestamp", System.currentTimeMillis());
			}

			// Create a new tag root vertex for the tagfamily and link the tags to this vertex instead to the tag family itself.
			Vertex tagRoot = getGraph().addVertex("class:TagRootImpl");
			tagRoot.property("ferma_type", "com.gentics.mesh.core.data.root.impl.TagRootImpl");
			tagRoot.property("uuid", randomUUID());
			for (Edge tagEdge : (Iterable<Edge>) () -> tagFamily.edges(Direction.OUT, "HAS_TAG")) {
				Vertex tag = tagEdge.inVertex();
				tagEdge.remove();
				tagRoot.addEdge("HAS_TAG", tag);
				tag.edges(Direction.OUT, "HAS_TAGFAMILY_ROOT").forEachRemaining(edge -> edge.remove());
				tag.addEdge("HAS_TAGFAMILY_ROOT", tagFamily);
				if (!tag.edges(Direction.OUT, "ASSIGNED_TO_PROJECT").hasNext()) {
					log.error("Tag {" + tag.property("uuid") + " has no project assigned to it. Fixing it...");
					tag.addEdge("ASSIGNED_TO_PROJECT", project);
				}
				Object creationTimeStamp = tag.property("creation_timestamp");
				if (creationTimeStamp == null) {
					tag.property("creation_timestamp", System.currentTimeMillis());
				}
				Object editTimeStamp = tag.property("last_edited_timestamp");
				if (editTimeStamp == null) {
					tag.property("last_edited_timestamp", System.currentTimeMillis());
				}
			}
			tagFamily.addEdge("HAS_TAG_ROOT", tagRoot);
			if (!tagFamily.edges(Direction.OUT, "ASSIGNED_TO_PROJECT").hasNext()) {
				log.error("TagFamily {" + tagFamily.property("uuid") + " has no project assigned to it. Fixing it...");
				tagFamily.addEdge("ASSIGNED_TO_PROJECT", project);
			}

			getOrFixUserReference(tagFamily, "HAS_EDITOR");
			getOrFixUserReference(tagFamily, "HAS_CREATOR");
		}

	}

	private void purgeSearchQueue() {
		Vertex meshRoot = getMeshRootVertex();
		Vertex sqRoot = meshRoot.vertices(Direction.OUT, "HAS_SEARCH_QUEUE_ROOT").next();
		for (Vertex batch : (Iterable<Vertex>) () -> sqRoot.vertices(Direction.OUT, "HAS_BATCH")) {
			for (Vertex entry : (Iterable<Vertex>) () -> batch.vertices(Direction.OUT, "HAS_ITEM")) {
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
		for (Vertex schemaContainer : (Iterable<Vertex>) () -> getGraph().vertices()) {
			String type = schemaContainer.value("ferma_type");
			if (type != null && type.endsWith("SchemaContainerImpl")) {
				String name = schemaContainer.value("name");
				log.info("Migrating schema {" + name + "}");
				String json = schemaContainer.value("json");
				schemaContainer.property("json").remove();
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
				version.property("uuid", randomUUID());
				version.property("name", name);
				version.property("json", json);
				version.property("version", 1);
				version.property("ferma_type", "com.gentics.mesh.core.data.schema.impl.SchemaContainerVersionImpl");
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
		String uuid = node.value("uuid");
		log.info("Migrating node {" + uuid + "}");
		Iterator<Vertex> it = node.vertices(Direction.OUT, "HAS_PARENT_NODE");
		Vertex parentNode = null;
		if (it.hasNext()) {
			parentNode = it.next();
		}

		if (!node.vertices(Direction.OUT, "ASSIGNED_TO_PROJECT").hasNext()) {
			log.error("Node {" + node.value("uuid") + "} has no project assigned to it. Fixing inconsistency...");
			node.addEdge("ASSIGNED_TO_PROJECT", project);
		}

		// Check whether the node has binary property information. We need to migrate those to a new binary graph field.
		String fileName = node.value("binaryFilename");
		if (fileName != null) {
			String oldPath = getSegmentedPath(uuid);
			File oldBinaryFile = new File("data" + File.separator + "binaryFilesOld" + File.separator + oldPath + File.separator + uuid + ".bin");

			node.value("binaryFilename");
			Object fileSize = node.value("binaryFileSize");
			node.value("binaryFileSize");

			node.value("binarySha512Sum");

			String sha512sum = hashFile(oldBinaryFile.getAbsolutePath());
			String mimeType = node.value("binaryContentType");
			node.property("binaryContentType").remove();

			Object dpi = node.value("binaryImageDPI");
			node.property("binaryImageDPI").remove();

			Object width = node.value("binaryImageWidth");
			node.property("binaryImageWidth").remove();

			Object height = node.value("binaryImageHeight");
			node.property("binaryImageHeight").remove();

			Iterable<Vertex> containers = (Iterable<Vertex>) () -> node.vertices(Direction.OUT, "HAS_FIELD_CONTAINER");
			// Create the binary field for all found containers
			for (Vertex container : containers) {
				Vertex binaryField = getGraph().addVertex("class:BinaryGraphFieldImpl");
				String binaryFieldUuid = randomUUID();
				binaryField.property("uuid", binaryFieldUuid);
				binaryField.property("ferma_type", "com.gentics.mesh.core.data.node.field.impl.BinaryGraphFieldImpl");
				binaryField.property("binaryFileSize", fileSize);
				binaryField.property("binaryFilename", fileName);
				binaryField.property("binarySha512Sum", sha512sum);
				binaryField.property("binaryContentType", mimeType);
				if (dpi != null) {
					binaryField.property("binaryImageDPI", dpi);
				}
				if (width != null) {
					binaryField.property("binaryImageWidth", width);
				}
				if (height != null) {
					binaryField.property("binaryImageHeight", height);
				}
				binaryField.property("fieldkey", "binary");
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
		Vertex schemaContainer = node.vertices(Direction.OUT, "HAS_SCHEMA_CONTAINER").next();
		Vertex schemaVersion = schemaContainer.vertices(Direction.OUT, "HAS_LATEST_VERSION").next();
		String json = schemaVersion.value("json");
		String segmentField = null;
		try {
			JSONObject schema = new JSONObject(json);
			if (schema.has("segmentField")) {
				segmentField = schema.getString("segmentField");
			}
		} catch (JSONException e) {
			throw new RuntimeException("Could not parse schema json {" + json + "}", e);
		}

		Iterable<Vertex> containers = (Iterable<Vertex>) () -> node.vertices(Direction.OUT, "HAS_FIELD_CONTAINER");
		for (Vertex container : containers) {

			// Fix date fields which were stored in seconds instead of miliseconds
			for (String key : container.keys()) {
				if (key.endsWith("-date")) {
					Long date = Long.valueOf(container.value(key));
					// Check whether the timestamp is seconds based or miliseconds based
					if (date < 10081440150L) {
						Long newDate = date * 1000;
						log.info("Fixing date for field {" + key + "} from " + date + " to " + newDate);
						container.property(key, String.valueOf(newDate));
					}
				}
			}

			container.addEdge("HAS_SCHEMA_CONTAINER_VERSION", schemaVersion);
			// Add webrootPathInfo property (SegmentValue-ParentFolderUuid)
			if (segmentField != null) {
				String segmentFieldValue = container.value(segmentField + "-string");
				if (!StringUtils.isEmpty(segmentFieldValue)) {
					String webRootPath = segmentFieldValue;
					if (parentNode != null) {
						webRootPath += "-" + parentNode.value("uuid");
					}
					String id = uuid + "-" + container.value("uuid");
					if (webrootIndexMap.containsKey(webRootPath)) {
						log.error("Found conflicting node:" + id + " with webroot path info " + webRootPath);
						// Randomize the value
						segmentFieldValue = segmentFieldValue + "_" + randomUUID();
						container.property(segmentField + "-string", segmentFieldValue);
						// Set the new webroot path
						webRootPath = segmentFieldValue;
						if (parentNode != null) {
							webRootPath += "-" + parentNode.value("uuid");
						}
					}
					webrootIndexMap.put(webRootPath, id);
					container.property("webrootPathInfo", webRootPath);
				}
			}
		}

		// Now check the children and migrate structure
		Iterable<Edge> childrenEdges = (Iterable<Edge>) () -> node.edges(Direction.IN, "HAS_PARENT_NODE");
		for (Edge childEdge : childrenEdges) {
			migrateNode(childEdge.outVertex(), project);
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
