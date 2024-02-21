package com.gentics.mesh.changelog.changes;

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;
import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;
import org.apache.tinkerpop.gremlin.structure.Element;
import org.apache.tinkerpop.gremlin.structure.Property;
import org.apache.tinkerpop.gremlin.structure.Vertex;
import org.apache.tinkerpop.gremlin.structure.VertexProperty;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.changelog.MeshGraphHelper;
import com.gentics.mesh.util.StreamUtil;

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
		Vertex projectRoot = meshRoot.vertices(Direction.OUT, "HAS_PROJECT_ROOT").next();
		Vertex admin = findAdmin();

		// Iterate over all projects
		for (Vertex project : StreamUtil.toIterable(projectRoot.vertices(Direction.OUT, "HAS_PROJECT"))) {
			Vertex releaseRoot = getGraph().addVertex("class:ReleaseRootImpl");
			releaseRoot.property("ferma_type", "ReleaseRootImpl");
			releaseRoot.property("uuid", randomUUID());
			project.addEdge("HAS_RELEASE_ROOT", releaseRoot);

			// Create release and edges
			String branchUuid = randomUUID();
			Vertex release = getGraph().addVertex("class:ReleaseImpl");
			release.property("ferma_type", "ReleaseImpl");
			release.property("uuid", branchUuid);
			release.property("name", project.<String>property("name"));
			release.property("active", true);
			releaseRoot.addEdge("HAS_LATEST_RELEASE", release);
			releaseRoot.addEdge("HAS_INITIAL_RELEASE", release);
			releaseRoot.addEdge("HAS_RELEASE", release);

			// Assign all latest schema versions to the release
			Vertex schemaRoot = project.vertices(Direction.OUT, "HAS_ROOT_SCHEMA").next();
			for (Vertex schemaContainer : StreamUtil.toIterable(schemaRoot.vertices(Direction.OUT, "HAS_SCHEMA_CONTAINER_ITEM"))) {
				Vertex latestSchemaVersion = schemaContainer.vertices(Direction.OUT, "HAS_LATEST_VERSION").next();
				log.info("Assigning schema version {" + latestSchemaVersion.id() + " / " + latestSchemaVersion.<String>property("uuid")
						+ "} to release");
				release.addEdge("HAS_SCHEMA_VERSION", latestSchemaVersion);
			}

			getOrFixUserReference(project, "HAS_EDITOR");
			getOrFixUserReference(project, "HAS_CREATOR");

			// Migrate all nodes of the project
			Vertex baseNode = project.vertices(Direction.OUT, "HAS_ROOT_NODE").next();

			migrateBaseNode(baseNode, admin);
			migrateNode(baseNode, branchUuid);
		}

		// Migrate tags
		migrateTags(meshRoot);

		// Migrate users
		migrateUsers(meshRoot);

		// Strip all package paths from all ferma type properties
		for (Vertex vertex : StreamUtil.toIterable(getGraph().vertices())) {
			migrateType(vertex);
		}
		for (Edge edge : StreamUtil.toIterable(getGraph().edges())) {
			migrateType(edge);
		}

		// Migrate TranslatedImpl edges to GraphFieldContainerEdgeImpl
		for (Edge edge : StreamUtil.toIterable(getGraph().edges("ferma_type", "TranslatedImpl"))) {
			edge.property("ferma_type", "GraphFieldContainerEdgeImpl");
		}

	}

	private void migrateUsers(Vertex meshRoot) {
		Vertex userRoot = meshRoot.vertices(Direction.OUT, "HAS_USER_ROOT").next();

		for (Vertex user : StreamUtil.toIterable(userRoot.vertices(Direction.OUT, "HAS_USER"))) {
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
		Vertex tagRoot = meshRoot.vertices(Direction.OUT, "HAS_TAG_ROOT").next();
		for (Vertex tag : StreamUtil.toIterable(tagRoot.vertices(Direction.OUT, "HAS_TAG"))) {
			Iterator<Vertex> tagFieldIterator = tag.vertices(Direction.OUT, "HAS_FIELD_CONTAINER");
			Vertex tagFieldContainer = tagFieldIterator.next();
			if (tagFieldIterator.hasNext()) {
				fail("The tag with uuid {" + tag.<String>property("uuid") + "} got more than one field container.");
			}
			// Load the tag value from the field container and store it directly into the tag. Remove the now no longer needed field container from the graph.
			String tagValue = tagFieldContainer.<String>property("name").orElse(null);
			tag.property("tagValue", tagValue);
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

		log.info("Migrating basenode {" + baseNode.<String>property("uuid") + "}");
		Vertex schemaContainer = baseNode.vertices(Direction.OUT, "HAS_SCHEMA_CONTAINER").next();
		Vertex schemaVersion = schemaContainer.vertices(Direction.OUT, "HAS_LATEST_VERSION").next();

		Vertex english = findEnglish();
		Iterator<Edge> it = baseNode.edges(Direction.OUT, "HAS_FIELD_CONTAINER");

		// The base node has no field containers. Lets create the default one
		if (!it.hasNext()) {
			Vertex container = getGraph().addVertex("class:NodeGraphFieldContainerImpl");
			container.property("ferma_type", "NodeGraphFieldContainerImpl");
			container.property("uuid", randomUUID());

			// Fields
			container.property("name-field", "name");
			container.property("name-string", "");

			// field container edge which will later be migrated
			Edge edge = baseNode.addEdge("HAS_FIELD_CONTAINER", container);
			edge.property("ferma_type", "GraphFieldContainerEdgeImpl");
			edge.property("languageTag", "en");
			container.addEdge("HAS_SCHEMA_CONTAINER_VERSION", schemaVersion);
			container.addEdge("HAS_LANGUAGE", english);
		}

	}

	private Vertex findAdmin() {
		Vertex admin = null;
		Iterator<Vertex> langIt = getMeshRootVertex().vertices(Direction.OUT, "HAS_USER_ROOT").next()
				.vertices(Direction.OUT, "HAS_USER");
		while (langIt.hasNext()) {
			Vertex user = langIt.next();
			if (user.<String>property("username").equals("admin")) {
				admin = user;
			}
		}
		return admin;
	}

	private Vertex findEnglish() {
		Vertex english = null;
		Iterator<Vertex> langIt = getMeshRootVertex().vertices(Direction.OUT, "HAS_LANGUAGE_ROOT").next()
				.vertices(Direction.OUT, "HAS_LANGUAGE");
		while (langIt.hasNext()) {
			Vertex language = langIt.next();
			if (language.<String>property("languageTag").equals("en")) {
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
		String type = element.<String>property("ferma_type").orElse(null);
		if (!StringUtils.isEmpty(type)) {
			int idx = type.lastIndexOf(".");
			if (idx != -1) {
				type = type.substring(idx + 1);
				element.property("ferma_type", type);
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
		boolean isPublished = Boolean.valueOf(node.<String>property("published").orElse(null));
		node.property("published").remove();
		log.info("Migrating node: " + node.<String>property("uuid") + " published: " + String.valueOf(isPublished));

		Edge editorEdge = null;
		Iterable<Edge> containerEdges = StreamUtil.toIterable(node.edges(Direction.OUT, "HAS_FIELD_CONTAINER"));
		for (Edge containerEdge : containerEdges) {
			containerEdge.property("branchUuid", branchUuid);
			containerEdge.property("edgeType", "I");

			// Set version to found container
			Vertex fieldContainer = containerEdge.inVertex();
			fieldContainer.property("version", "0.1");

			// Add displayFieldValue
			//1. Get schema container version and extract field name
			Vertex schemaContainerVersion = fieldContainer.vertices(Direction.OUT, "HAS_SCHEMA_CONTAINER_VERSION").next();
			String schemaJson = schemaContainerVersion.<String>property("json").orElse(null);
			JsonObject schema = new JsonObject(schemaJson);
			String displayFieldName = schema.getString("displayField");
			// 2. Load the field value for the given field 
			String displayFieldValue = fieldContainer.<String>property(displayFieldName + "-string").orElse(null);
			if (displayFieldValue != null) {
				fieldContainer.property("displayFieldValue", displayFieldValue);
			}

			// Migrate webroot path
			String oldPathInfo = fieldContainer.<String>property("webrootPathInfo").orElse(null);
			if (oldPathInfo != null) {
				int lastIdx = oldPathInfo.lastIndexOf("-");
				String name = oldPathInfo.substring(0, lastIdx);
				String folderUuid = oldPathInfo.substring(lastIdx + 1);
				fieldContainer.property("webrootPathInfo", name + "-" + branchUuid + "-" + folderUuid);
			}

			// Create additional draft edge
			if (!isPublished) {
				Edge draftEdge = node.addEdge("HAS_FIELD_CONTAINER", fieldContainer);
				draftEdge.property("ferma_type", "GraphFieldContainerEdgeImpl");
				draftEdge.property("branchUuid", branchUuid);
				draftEdge.property("edgeType", "D");
				draftEdge.property("languageTag", containerEdge.<String>property("languageTag"));
			}

			// Migrate editor
			Vertex creator = getOrFixUserReference(node, "HAS_CREATOR");

			// Migrate editor edge from node to field container
			Iterator<Edge> editorIterator = node.edges(Direction.OUT, "HAS_EDITOR");
			if (!editorIterator.hasNext()) {
				log.error("Could not find editor for node {" + node.<String>property("uuid") + "}. Using creator to set editor.");
				fieldContainer.addEdge("HAS_EDITOR", creator);
			} else {
				editorEdge = editorIterator.next();
				// Set the editor
				fieldContainer.addEdge("HAS_EDITOR", editorEdge.inVertex());
			}

			// Migrate last edited
			Long ts = node.<Long>property("last_edited_timestamp").orElse(null);
			if (ts == null) {
				ts = System.currentTimeMillis();
			}
			fieldContainer.property("last_edited_timestamp", ts);
			node.property("last_edited_timestamp").remove();

			// The node is published. Lets Create a published version 1.0 
			if (isPublished) {

				// Now duplicate the field container for version 1.0
				Vertex publishedContainer = getGraph().addVertex("class:NodeGraphFieldContainerImpl");
				publishedContainer.property("ferma_type", "NodeGraphFieldContainerImpl");

				// Copy properties
				for (VertexProperty<?> p : StreamUtil.toIterable(fieldContainer.properties())) {
					publishedContainer.property(p.key(), p.orElse(null));
				}
				String oldPath = fieldContainer.<String>property("webrootPathInfo").orElse(null);
				if (oldPath != null) {
					publishedContainer.property("publishedWebrootPathInfo", oldPath);
					publishedContainer.property("webrootPathInfo").remove();
				}

				// Overwrite the previously copied properties
				publishedContainer.property("uuid", randomUUID());
				publishedContainer.property("version", "1.0");

				// Copy edges (OUT)
				for (Edge edge : StreamUtil.toIterable(fieldContainer.edges(Direction.OUT))) {

					Edge newEdge = publishedContainer.addEdge(edge.label(), edge.inVertex());
					for (Property<?> p : StreamUtil.toIterable(edge.properties())) {
						newEdge.property(p.key(), p.orElse(null));
					}
				}
				// Copy edges (IN)
				for (Edge edge : StreamUtil.toIterable(fieldContainer.edges(Direction.IN))) {
					// Skip field container edges. We'll create our own
					if ("HAS_FIELD_CONTAINER".equals(edge.label())) {
						continue;
					}
					Edge newEdge = edge.outVertex().addEdge(edge.label(), publishedContainer);
					for (Property<?> p : StreamUtil.toIterable(edge.properties())) {
						newEdge.property(p.key(), p.orElse(null));
					}
				}

				// Create the published edge. No need to remove the old HAS_FIELD_CONTAINER because it was not cloned
				Edge publishedEdge = node.addEdge("HAS_FIELD_CONTAINER", publishedContainer);
				publishedEdge.property("ferma_type", "GraphFieldContainerEdgeImpl");
				publishedEdge.property("languageTag", containerEdge.<String>property("languageTag"));
				publishedEdge.property("branchUuid", branchUuid);
				publishedEdge.property("edgeType", "P");
			}

		}

		if (editorEdge != null) {
			editorEdge.remove();
		}

		// Migrate tagging
		Iterable<Edge> tagEdges = StreamUtil.toIterable(node.edges(Direction.OUT, "HAS_TAG"));
		for (Edge tagEdge : tagEdges) {
			tagEdge.property("branchUuid", branchUuid);
		}

		// Now check the children and migrate structure
		Iterable<Edge> childrenEdges = StreamUtil.toIterable(node.edges(Direction.IN, "HAS_PARENT_NODE"));
		for (Edge childEdge : childrenEdges) {
			childEdge.property("branchUuid", branchUuid);
			migrateNode(childEdge.outVertex(), branchUuid);
		}

		log.info("Granting permissions to node {" + node.<String>property("uuid") + "}");
		// Grant publish permission to all roles+objects which grant update
		for (Edge edge : StreamUtil.toIterable(node.edges(Direction.IN, "HAS_UPDATE_PERMISSION"))) {
			Vertex role = edge.outVertex();
			role.addEdge("HAS_PUBLISH_PERMISSION", node);
		}

		// Grant read published permission to all roles+objects which grant read
		for (Edge edge : StreamUtil.toIterable(node.edges(Direction.IN, "HAS_READ_PERMISSION"))) {
			Vertex role = edge.outVertex();
			role.addEdge("HAS_READ_PUBLISHED_PERMISSION", node);
		}
	}

	private Vertex getOrFixUserReference(Vertex element, String edge) {
		Vertex creator;
		Iterator<Vertex> creatorIterator = element.vertices(Direction.OUT, edge);
		if (!creatorIterator.hasNext()) {
			log.error("The element {" + element.<String>property("uuid").orElse(null) + "} has no {" + edge + "}. Using admin instead.");
			creator = findAdmin();
			element.addEdge(edge, creator);
		} else {
			creator = creatorIterator.next();
		}
		return creator;
	}

}
