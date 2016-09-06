package com.gentics.mesh.changelog.changes;

import java.util.Iterator;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.changelog.MeshGraphHelper;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Element;
import com.tinkerpop.blueprints.Vertex;

public class Change_0A58BEF0E7E7488D98BEF0E7E7588D4D extends AbstractChange {

	@Override
	public String getName() {
		return "Add versioning support";
	}

	@Override
	public String getDescription() {
		return "Adds various vertices and edges that will enable versioning support";
	}

	@Override
	public void apply() {
		Vertex meshRoot = MeshGraphHelper.getMeshRootVertex(getGraph());
		Vertex projectRoot = meshRoot.getVertices(Direction.OUT, "HAS_PROJECT_ROOT").iterator().next();

		// Iterate over all projects
		for (Vertex project : projectRoot.getVertices(Direction.OUT, "HAS_PROJECT")) {
			Vertex releaseRoot = getGraph().addVertex("class:ReleaseRootImpl");
			releaseRoot.setProperty("ferma_type", "ReleaseRootImpl");
			releaseRoot.setProperty("uuid", randomUUID());
			project.addEdge("HAS_RELEASE_ROOT", releaseRoot);

			// Create release and edges
			String releaseUuid = randomUUID();
			Vertex release = getGraph().addVertex("class:ReleaseImpl");
			release.setProperty("ferma_type", "ReleaseImpl");
			release.setProperty("uuid", releaseUuid);
			release.setProperty("name", project.getProperty("name"));
			release.setProperty("active", true);
			releaseRoot.addEdge("HAS_LATEST_RELEASE", release);
			releaseRoot.addEdge("HAS_INITIAL_RELEASE", release);
			releaseRoot.addEdge("HAS_RELEASE", release);

			// Assign all latest schema versions to the release
			Vertex schemaRoot = project.getVertices(Direction.OUT, "HAS_ROOT_SCHEMA").iterator().next();
			for (Vertex schemaContainer : schemaRoot.getVertices(Direction.OUT, "HAS_SCHEMA_CONTAINER_ITEM")) {
				Vertex latestSchemaVersion = schemaContainer.getVertices(Direction.OUT, "HAS_LATEST_VERSION").iterator().next();
				release.addEdge("HAS_VERSION", latestSchemaVersion);
			}

			// Migrate all nodes of the project
			Vertex baseNode = project.getVertices(Direction.OUT, "HAS_ROOT_NODE").iterator().next();
			migrateBaseNode(baseNode);
			migrateNode(baseNode, releaseUuid);
		}

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

	/**
	 * Migrate the basenode and create a new NodeGraphFieldContainer for it.
	 * 
	 * @param baseNode
	 */
	private void migrateBaseNode(Vertex baseNode) {

		log.info("Migrating basenode {" + baseNode.getProperty("uuid") + "}");
		Vertex schemaContainer = baseNode.getVertices(Direction.OUT, "HAS_SCHEMA_CONTAINER").iterator().next();
		Vertex schemaVersion = schemaContainer.getVertices(Direction.OUT, "HAS_LATEST_VERSION").iterator().next();
		Iterator<Edge> it = baseNode.getEdges(Direction.OUT, "HAS_FIELD_CONTAINER").iterator();

		Vertex english = getGraph().getVertices("languageTag", "en").iterator().next();

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
	 * @param releaseUuid
	 */
	private void migrateNode(Vertex node, String releaseUuid) {

		// Extract and remove the published property. We'll use it later on to create a published version if needed.
		boolean isPublished = Boolean.valueOf(node.getProperty("published"));
		node.removeProperty("published");
		log.info("Migrating node: " + node.getProperty("uuid") + " published: " + String.valueOf(isPublished));

		Iterable<Edge> containerEdges = node.getEdges(Direction.OUT, "HAS_FIELD_CONTAINER");
		for (Edge containerEdge : containerEdges) {
			containerEdge.setProperty("releaseUuid", releaseUuid);
			containerEdge.setProperty("edgeType", "I");

			// Set version to found container
			Vertex fieldContainer = containerEdge.getVertex(Direction.IN);
			fieldContainer.setProperty("version", "0.1");

			// Migrate webroot path
			String oldPathInfo = fieldContainer.getProperty("webrootPathInfo");
			if (oldPathInfo != null) {
				int lastIdx = oldPathInfo.lastIndexOf("-");
				String name = oldPathInfo.substring(0, lastIdx);
				String folderUuid = oldPathInfo.substring(lastIdx + 1);
				fieldContainer.setProperty("webrootPathInfo", name + "-" + releaseUuid + "-" + folderUuid);
			}

			// Create additional draft edge
			if (!isPublished) {
				Edge draftEdge = node.addEdge("HAS_FIELD_CONTAINER", fieldContainer);
				draftEdge.setProperty("ferma_type", "GraphFieldContainerEdgeImpl");
				draftEdge.setProperty("releaseUuid", releaseUuid);
				draftEdge.setProperty("edgeType", "D");
				draftEdge.setProperty("languageTag", containerEdge.getProperty("languageTag"));
			}

			// Migrate editor
			Iterator<Edge> editorIterator = node.getEdges(Direction.OUT, "HAS_EDITOR").iterator();
			if(!editorIterator.hasNext()) {
				fail("The node {" + node.getProperty("uuid") + "} has no editor edge.");
			}
			Edge editorEdge = 		editorIterator.next();
			fieldContainer.addEdge("HAS_EDITOR", editorEdge.getVertex(Direction.IN));
			editorEdge.remove();

			// Migrate last edited
			fieldContainer.setProperty("last_edited_timestamp", node.getProperty("last_edited_timestamp"));
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
				publishedEdge.setProperty("releaseUuid", releaseUuid);
				publishedEdge.setProperty("edgeType", "P");
			}

		}

		// Migrate tagging
		Iterable<Edge> tagEdges = node.getEdges(Direction.OUT, "HAS_TAG");
		for (Edge tagEdge : tagEdges) {
			tagEdge.setProperty("releaseUuid", releaseUuid);
		}

		// Now check the children and migrate structure
		Iterable<Edge> childrenEdges = node.getEdges(Direction.IN, "HAS_PARENT_NODE");
		for (Edge childEdge : childrenEdges) {
			childEdge.setProperty("releaseUuid", releaseUuid);
			migrateNode(childEdge.getVertex(Direction.OUT), releaseUuid);
		}

		log.info("Granting permissions to node {" + node.getProperty("uuid") + "}");
		// Grant publish permissions to all roles+objects which grant update
		for (Edge edge : node.getEdges(Direction.IN, "HAS_UPDATE_PERMISSION")) {
			Vertex role = edge.getVertex(Direction.OUT);
			role.addEdge("HAS_PUBLISH_PERMISSION", node);
		}

		// Grant read published permissions to all roles+objects which grant read
		for (Edge edge : node.getEdges(Direction.IN, "HAS_READ_PERMISSION")) {
			Vertex role = edge.getVertex(Direction.OUT);
			role.addEdge("HAS_READ_PUBLISHED_PERMISSION", node);
		}
	}

}
