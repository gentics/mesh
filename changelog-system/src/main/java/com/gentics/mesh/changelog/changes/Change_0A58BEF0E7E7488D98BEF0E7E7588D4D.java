package com.gentics.mesh.changelog.changes;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;
import com.tinkerpop.blueprints.Vertex;

public class Change_0A58BEF0E7E7488D98BEF0E7E7588D4D extends AbstractChange {

	@Override
	public String getName() {
		return "Add versioing support";
	}

	@Override
	public String getDescription() {
		return "Adds various vertices and edges that will enable versioning support";
	}

	@Override
	public void apply() {
		Vertex meshRoot = getMeshRootVertex();
		Vertex projectRoot = meshRoot.getVertices(Direction.OUT, "HAS_PROJECT_ROOT").iterator().next();

		// Iterate over all projects
		for (Vertex project : projectRoot.getVertices(Direction.OUT, "HAS_PROJECT")) {
			Vertex releaseRoot = getGraph().addVertex("class:ReleaseRootImpl");
			releaseRoot.setProperty("uuid", randomUUID());
			project.addEdge("HAS_RELEASE_ROOT", releaseRoot);

			// Create release and edges
			Vertex release = getGraph().addVertex("class:ReleaseImpl");
			release.setProperty("uuid", randomUUID());
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
			migrateNode(baseNode, release.getProperty("uuid"));
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

		Iterable<Edge> containerEdges = node.getEdges(Direction.OUT, "HAS_FIELD_CONTAINER");
		for (Edge containerEdge : containerEdges) {
			containerEdge.setProperty("releaseUuid", releaseUuid);
			containerEdge.setProperty("edgeType", "I");

			// Set version to found container
			Vertex fieldContainer = containerEdge.getVertex(Direction.IN);
			fieldContainer.setProperty("version", "0.1");

			// Migrate webroot path
			String oldPathInfo = fieldContainer.getProperty("webrootPathInfo");
			int lastIdx = oldPathInfo.lastIndexOf("-");
			String name = oldPathInfo.substring(0, lastIdx);
			String folderUuid = oldPathInfo.substring(lastIdx + 1);
			fieldContainer.setProperty("webrootPathInfo", name + "-" + releaseUuid + "-" + folderUuid);

			// Create additional draft edge
			Edge draftEdge = node.addEdge("HAS_FIELD_CONTAINER", fieldContainer);
			draftEdge.setProperty("releaseUuid", releaseUuid);
			draftEdge.setProperty("edgeType", "D");
			draftEdge.setProperty("languageTag", containerEdge.getProperty("languageTag"));

			//Migrate editor
			Edge editorEdge = node.getEdges(Direction.OUT, "HAS_EDITOR").iterator().next();
			fieldContainer.addEdge("HAS_EDITOR", editorEdge.getVertex(Direction.IN));
			editorEdge.remove();

			//Migrate last edited
			fieldContainer.setProperty("last_edited_timestamp", node.getProperty("last_edited_timestamp"));
			node.removeProperty("last_edited_timestamp");

			if (isPublished) {

				// Now duplicate the field container and create a published version 1.0
				Vertex publishedContainer = getGraph().addVertex("class:NodeGraphFieldContainerImpl");

				// Copy properties
				for (String key : fieldContainer.getPropertyKeys()) {
					publishedContainer.setProperty(key, fieldContainer.getProperty(key));
				}
				publishedContainer.setProperty("uuid", randomUUID());
				publishedContainer.setProperty("version", "1.0");
				publishedContainer.setProperty("publishedWebrootPathInfo", fieldContainer.getProperty("webrootPathInfo"));
				publishedContainer.removeProperty("webrootPathInfo");

				// Copy edges
				for (Edge edge : fieldContainer.getEdges(Direction.OUT)) {
					publishedContainer.addEdge(edge.getLabel(), edge.getVertex(Direction.IN));
				}
				for (Edge edge : fieldContainer.getEdges(Direction.IN)) {
					// Skip field container edges. We'll create our own
					if ("HAS_FIELD_CONTAINER".equals(edge.getLabel())) {
						continue;
					}
					edge.getVertex(Direction.OUT).addEdge(edge.getLabel(), publishedContainer);
				}

				// Create the published edge. No need to remove the old HAS_FIELD_CONTAINER because it was not cloned
				Edge publishedEdge = node.addEdge("HAS_FIELD_CONTAINER", publishedContainer);
				publishedEdge.setProperty("releaseUuid", releaseUuid);
				publishedEdge.setProperty("edgeType", "P");
				publishedEdge.setProperty("languageTag", containerEdge.getProperty("languageTag"));
			}

		}

		//Migrate tagging
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

	}

}
