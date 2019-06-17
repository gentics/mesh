package com.gentics.mesh.changelog.changes;

import static com.tinkerpop.blueprints.Direction.OUT;

import java.util.Iterator;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Vertex;

public class RestructureTags extends AbstractChange {

	@Override
	public String getUuid() {
		return "510E2B1CC6734AA48E2B1CC673DAA41C";
	}

	@Override
	public String getName() {
		return "Restructure Tags Graph";
	}

	@Override
	public String getDescription() {
		return "Restructure the tag graph model and remove the tag family tag root vertex";
	}

	@Override
	public void applyInTx() {
		Vertex root = getMeshRootVertex();
		Vertex tagFamilyRoot = root.getVertices(OUT, "HAS_TAGFAMILY_ROOT").iterator().next();
		// Iterate over all tag families
		Iterator<Vertex> iterator = tagFamilyRoot.getVertices(OUT, "HAS_TAG_FAMILY").iterator();
		while (iterator.hasNext()) {
			Vertex tagFamily = iterator.next();
			Vertex tagRoot = tagFamily.getVertices(OUT, "HAS_TAG_ROOT").iterator().next();
			// Now iterate over all tags and assign them directly to the tag family 
			Iterator<Vertex> tagIterator = tagRoot.getVertices(OUT, "HAS_TAG").iterator();
			while (tagIterator.hasNext()) {
				Vertex tag = tagIterator.next();
				tagFamily.addEdge("HAS_TAG", tag);
			}
			// Remove the tag family root vertex
			tagRoot.remove();
		}

		// Locate all tag roots for all projects and remove them
		Vertex projectRoot = root.getVertices(OUT, "HAS_PROJECT_ROOT").iterator().next();
		Iterator<Vertex> projectIterator = projectRoot.getVertices(OUT, "HAS_PROJECT").iterator();
		while (projectIterator.hasNext()) {
			Vertex project = projectIterator.next();
			Vertex tagRoot = project.getVertices(OUT, "HAS_TAG_ROOT").iterator().next();
			tagRoot.remove();
		}
	}

	@Override
	public boolean requiresReindex() {
		return true;
	}

}
