package com.gentics.mesh.changelog.changes;

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.changelog.MeshGraphHelper;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;

import io.vertx.core.json.JsonObject;

/**
 * Changelog entry which adds versioning support.
 */
public class ChangeAddVersioning extends AbstractChange {

	@Override
	public String getUuid() {
		return "0A58BEF0E7E7488D98BEF0E7E7588D4D";
	}
	
	@Override
	public String getName() {
		return "Add versioning support";
	}

	@Override
	public String getDescription() {
		return "Adds various vertices and edges that will enable versioning support";
	}

	@Override
	public void applyInTx() {
		Vertex meshRoot = MeshGraphHelper.getMeshRootVertex(getGraph());
		Vertex projectRoot = meshRoot.getVertices(Direction.OUT, "HAS_PROJECT_ROOT").iterator().next();
		Vertex admin = findAdmin();

		// Iterate over all projects
		for (Vertex project : projectRoot.getVertices(Direction.OUT, "HAS_PROJECT")) {
			Vertex releaseRoot = getGraph().addVertex("class:ReleaseRootImpl");
			releaseRoot.setProperty("ferma_type", "ReleaseRootImpl");
			releaseRoot.setProperty("uuid", randomUUID());
			project.addEdge("HAS_RELEASE_ROOT", releaseRoot);

			// Create release and edges
			String branchUuid = randomUUID();
			Vertex release = getGraph().addVertex("class:ReleaseImpl");
			release.setProperty("ferma_type", "ReleaseImpl");
			release.setProperty("uuid", branchUuid);
			release.setProperty("name", project.getProperty("name"));
			release.setProperty("active", true);
			releaseRoot.addEdge("HAS_LATEST_RELEASE", release);
			releaseRoot.addEdge("HAS_INITIAL_RELEASE", release);
			releaseRoot.addEdge("HAS_RELEASE", release);

			// Assign all latest schema versions to the release
			Vertex schemaRoot = project.getVertices(Direction.OUT, "HAS_ROOT_SCHEMA").iterator().next();
			for (Vertex schemaContainer : schemaRoot.getVertices(Direction.OUT, "HAS_SCHEMA_CONTAINER_ITEM")) {
				Vertex latestSchemaVersion = schemaContainer.getVertices(Direction.OUT, "HAS_LATEST_VERSION").iterator().next();
				log.info("Assigning schema version {" + latestSchemaVersion.getId() + " / " + latestSchemaVersion.getProperty("uuid")
						+ "} to release");
				release.addEdge("HAS_SCHEMA_VERSION", latestSchemaVersion);
			}

			getOrFixUserReference(project, "HAS_EDITOR");
			getOrFixUserReference(project, "HAS_CREATOR");

			// Migrate all nodes of the project
			Vertex baseNode = project.getVertices(Direction.OUT, "HAS_ROOT_NODE").iterator().next();

			migrateBaseNode(baseNode, admin);
			migrateNode(baseNode, branchUuid);
		}

		// Migrate tags
		migrateTags(meshRoot);

		// Migrate users
		migrateUsers(meshRoot);

		// Strip all package paths from all ferma type properties
		for (Vertex vertex : getGraph().getVertices()) {
			migrateType(vertex);
		}
		for (Edge edge : getGraph().getEdges()) {
			migrateType(edge);
		}

		// Migrate TranslatedImpl edges to GraphFieldContainerEdgeImpl
		for (Edge edge : getGraph().getEdges("ferma_type", "TranslatedImpl")) {
			edge.setProperty("ferma_type", "GraphFieldContainerEdgeImpl");
		}

	}

	private void migrateUsers(Vertex meshRoot) {
		Vertex userRoot = meshRoot.getVertices(Direction.OUT, "HAS_USER_ROOT").iterator().next();

		for (Vertex user : userRoot.getVertices(Direction.OUT, "HAS_USER")) {
			// Check editor/creator
			getOrFixUserReference(user, "HAS_EDITOR");
			getOrFixUserReference(user, "HAS_CREATOR");
		}
	}

	/**
	 * Tags no longer have a TagGraphFieldContainerImpl. The value is now stored directly in the tag vertex.
	 * 
	 * @param meshRoot
	 */
	private void migrateTags(Vertex meshRoot) {
		Vertex tagRoot = meshRoot.getVertices(Direction.OUT, "HAS_TAG_ROOT").iterator().next();
		for (Vertex tag : tagRoot.getVertices(Direction.OUT, "HAS_TAG")) {
			Iterator<Vertex> tagFieldIterator = tag.getVertices(Direction.OUT, "HAS_FIELD_CONTAINER").iterator();
			Vertex tagFieldContainer = tagFieldIterator.next();
			if (tagFieldIterator.hasNext()) {
				fail("The tag with uuid {" + tag.getProperty("uuid") + "} got more than one field container.");
			}
			// Load the tag value from the field container and store it directly into the tag. Remove the now no longer needed field container from the graph.
			String tagValue = tagFieldContainer.getProperty("name");
			tag.setProperty("tagValue", tagValue);
			tagFieldContainer.remove();

			// Check editor /creator
			getOrFixUserReference(tag, "HAS_EDITOR");
			getOrFixUserReference(tag, "HAS_CREATOR");
		}
	}

	/**
	 * Migrate the basenode and create a new NodeGraphFieldContainer for it.
	 * 
	 * @param baseNode
	 * @param admin
	 */
	private void migrateBaseNode(Vertex baseNode, Vertex admin) {

		log.info("Migrating basenode {" + baseNode.getProperty("uuid") + "}");
		Vertex schemaContainer = baseNode.getVertices(Direction.OUT, "HAS_SCHEMA_CONTAINER").iterator().next();
		Vertex schemaVersion = schemaContainer.getVertices(Direction.OUT, "HAS_LATEST_VERSION").iterator().next();

		Vertex english = findEnglish();
		Iterator<Edge> it = baseNode.getEdges(Direction.OUT, "HAS_FIELD_CONTAINER").iterator();

		// The base node has no field containers. Lets create the default one
		if (!it.hasNext()) {
			Vertex container = getGraph().addVertex("class:NodeGraphFieldContainerImpl");
			container.setProperty("ferma_type", "NodeGraphFieldContainerImpl");
			container.setProperty("uuid", randomUUID());

			// Fields
			container.setProperty("name-field", "name");
			container.setProperty("name-string", "");

			// field container edge which will later be migrated
			Edge edge = baseNode.addEdge("HAS_FIELD_CONTAINER", container);
			edge.setProperty("ferma_type", "GraphFieldContainerEdgeImpl");
			edge.setProperty("languageTag", "en");
			container.addEdge("HAS_SCHEMA_CONTAINER_VERSION", schemaVersion);
			container.addEdge("HAS_LANGUAGE", english);
		}

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

	private Vertex findEnglish() {
		Vertex english = null;
		Iterator<Vertex> langIt = getMeshRootVertex().getVertices(Direction.OUT, "HAS_LANGUAGE_ROOT").iterator().next()
				.getVertices(Direction.OUT, "HAS_LANGUAGE").iterator();
		while (langIt.hasNext()) {
			Vertex language = langIt.next();
			if (language.getProperty("languageTag").equals("en")) {
				english = language;
			}
		}
		return english;
	}

	/**
	 * Change type: com.gentics.mesh.core.data.impl.UserImpl to UserImpl
	 * 
	 * @param element
	 */
	private void migrateType(Element element) {
		String type = element.getProperty("ferma_type");
		if (!StringUtils.isEmpty(type)) {
			int idx = type.lastIndexOf(".");
			if (idx != -1) {
				type = type.substring(idx + 1);
				element.setProperty("ferma_type", type);
			}
		}
	}

	/**
	 * Migrate the node and assign it's field containers to the release.
	 * 
	 * @param node
	 * @param branchUuid
	 */
	private void migrateNode(Vertex node, String branchUuid) {

		// Extract and remove the published property. We'll use it later on to create a published version if needed.
		boolean isPublished = Boolean.valueOf(node.getProperty("published"));
		node.removeProperty("published");
		log.info("Migrating node: " + node.getProperty("uuid") + " published: " + String.valueOf(isPublished));

		Edge editorEdge = null;
		Iterable<Edge> containerEdges = node.getEdges(Direction.OUT, "HAS_FIELD_CONTAINER");
		for (Edge containerEdge : containerEdges) {
			containerEdge.setProperty("branchUuid", branchUuid);
			containerEdge.setProperty("edgeType", "I");

			// Set version to found container
			Vertex fieldContainer = containerEdge.getVertex(Direction.IN);
			fieldContainer.setProperty("version", "0.1");

			// Add displayFieldValue
			//1. Get schema container version and extract field name
			Vertex schemaContainerVersion = fieldContainer.getVertices(Direction.OUT, "HAS_SCHEMA_CONTAINER_VERSION").iterator().next();
			String schemaJson = schemaContainerVersion.getProperty("json");
			JsonObject schema = new JsonObject(schemaJson);
			String displayFieldName = schema.getString("displayField");
			// 2. Load the field value for the given field 
			String displayFieldValue = fieldContainer.getProperty(displayFieldName + "-string");
			if (displayFieldValue != null) {
				fieldContainer.setProperty("displayFieldValue", displayFieldValue);
			}

			// Migrate webroot path
			String oldPathInfo = fieldContainer.getProperty("webrootPathInfo");
			if (oldPathInfo != null) {
				int lastIdx = oldPathInfo.lastIndexOf("-");
				String name = oldPathInfo.substring(0, lastIdx);
				String folderUuid = oldPathInfo.substring(lastIdx + 1);
				fieldContainer.setProperty("webrootPathInfo", name + "-" + branchUuid + "-" + folderUuid);
			}

			// Create additional draft edge
			if (!isPublished) {
				Edge draftEdge = node.addEdge("HAS_FIELD_CONTAINER", fieldContainer);
				draftEdge.setProperty("ferma_type", "GraphFieldContainerEdgeImpl");
				draftEdge.setProperty("branchUuid", branchUuid);
				draftEdge.setProperty("edgeType", "D");
				draftEdge.setProperty("languageTag", containerEdge.getProperty("languageTag"));
			}

			// Migrate editor
			Vertex creator = getOrFixUserReference(node, "HAS_CREATOR");

			// Migrate editor edge from node to field container
			Iterator<Edge> editorIterator = node.getEdges(Direction.OUT, "HAS_EDITOR").iterator();
			if (!editorIterator.hasNext()) {
				log.error("Could not find editor for node {" + node.getProperty("uuid") + "}. Using creator to set editor.");
				fieldContainer.addEdge("HAS_EDITOR", creator);
			} else {
				editorEdge = editorIterator.next();
				// Set the editor
				fieldContainer.addEdge("HAS_EDITOR", editorEdge.getVertex(Direction.IN));
			}

			// Migrate last edited
			Long ts = node.getProperty("last_edited_timestamp");
			if (ts == null) {
				ts = System.currentTimeMillis();
			}
			fieldContainer.setProperty("last_edited_timestamp", ts);
			node.removeProperty("last_edited_timestamp");

			// The node is published. Lets Create a published version 1.0 
			if (isPublished) {

				// Now duplicate the field container for version 1.0
				Vertex publishedContainer = getGraph().addVertex("class:NodeGraphFieldContainerImpl");
				publishedContainer.setProperty("ferma_type", "NodeGraphFieldContainerImpl");

				// Copy properties
				for (String key : fieldContainer.getPropertyKeys()) {
					publishedContainer.setProperty(key, fieldContainer.getProperty(key));
				}
				String oldPath = fieldContainer.getProperty("webrootPathInfo");
				if (oldPath != null) {
					publishedContainer.setProperty("publishedWebrootPathInfo", oldPath);
					publishedContainer.removeProperty("webrootPathInfo");
				}

				// Overwrite the previously copied properties
				publishedContainer.setProperty("uuid", randomUUID());
				publishedContainer.setProperty("version", "1.0");

				// Copy edges (OUT)
				for (Edge edge : fieldContainer.getEdges(Direction.OUT)) {

					Edge newEdge = publishedContainer.addEdge(edge.getLabel(), edge.getVertex(Direction.IN));
					for (String key : edge.getPropertyKeys()) {
						newEdge.setProperty(key, edge.getProperty(key));
					}
				}
				// Copy edges (IN)
				for (Edge edge : fieldContainer.getEdges(Direction.IN)) {
					// Skip field container edges. We'll create our own
					if ("HAS_FIELD_CONTAINER".equals(edge.getLabel())) {
						continue;
					}
					Edge newEdge = edge.getVertex(Direction.OUT).addEdge(edge.getLabel(), publishedContainer);
					for (String key : edge.getPropertyKeys()) {
						newEdge.setProperty(key, edge.getProperty(key));
					}
				}

				// Create the published edge. No need to remove the old HAS_FIELD_CONTAINER because it was not cloned
				Edge publishedEdge = node.addEdge("HAS_FIELD_CONTAINER", publishedContainer);
				publishedEdge.setProperty("ferma_type", "GraphFieldContainerEdgeImpl");
				publishedEdge.setProperty("languageTag", containerEdge.getProperty("languageTag"));
				publishedEdge.setProperty("branchUuid", branchUuid);
				publishedEdge.setProperty("edgeType", "P");
			}

		}

		if (editorEdge != null) {
			editorEdge.remove();
		}

		// Migrate tagging
		Iterable<Edge> tagEdges = node.getEdges(Direction.OUT, "HAS_TAG");
		for (Edge tagEdge : tagEdges) {
			tagEdge.setProperty("branchUuid", branchUuid);
		}

		// Now check the children and migrate structure
		Iterable<Edge> childrenEdges = node.getEdges(Direction.IN, "HAS_PARENT_NODE");
		for (Edge childEdge : childrenEdges) {
			childEdge.setProperty("branchUuid", branchUuid);
			migrateNode(childEdge.getVertex(Direction.OUT), branchUuid);
		}

		log.info("Granting permissions to node {" + node.getProperty("uuid") + "}");
		// Grant publish permission to all roles+objects which grant update
		for (Edge edge : node.getEdges(Direction.IN, "HAS_UPDATE_PERMISSION")) {
			Vertex role = edge.getVertex(Direction.OUT);
			role.addEdge("HAS_PUBLISH_PERMISSION", node);
		}

		// Grant read published permission to all roles+objects which grant read
		for (Edge edge : node.getEdges(Direction.IN, "HAS_READ_PERMISSION")) {
			Vertex role = edge.getVertex(Direction.OUT);
			role.addEdge("HAS_READ_PUBLISHED_PERMISSION", node);
		}
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

}
