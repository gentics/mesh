package com.gentics.mesh.changelog.changes;

import static org.apache.tinkerpop.gremlin.structure.Direction.OUT;

import java.util.Iterator;

import com.gentics.mesh.changelog.AbstractChange;
import org.apache.tinkerpop.gremlin.structure.Vertex;

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
	public void apply() {
		Vertex root = getMeshRootVertex();
		Vertex tagFamilyRoot = root.vertices(OUT, "HAS_TAGFAMILY_ROOT").next();
		// Iterate over all tag families
		Iterator<Vertex> iterator = tagFamilyRoot.vertices(OUT, "HAS_TAG_FAMILY");
		while (iterator.hasNext()) {
			Vertex tagFamily = iterator.next();
			Vertex tagRoot = tagFamily.vertices(OUT, "HAS_TAG_ROOT").next();
			// Now iterate over all tags and assign them directly to the tag family 
			Iterator<Vertex> tagIterator = tagRoot.vertices(OUT, "HAS_TAG");
			while (tagIterator.hasNext()) {
				Vertex tag = tagIterator.next();
				tagFamily.addEdge("HAS_TAG", tag);
			}
			// Remove the tag family root vertex
			tagRoot.remove();
		}

		// Locate all tag roots for all projects and remove them
		Vertex projectRoot = root.vertices(OUT, "HAS_PROJECT_ROOT").next();
		Iterator<Vertex> projectIterator = projectRoot.vertices(OUT, "HAS_PROJECT");
		while (projectIterator.hasNext()) {
			Vertex project = projectIterator.next();
			Vertex tagRoot = project.vertices(OUT, "HAS_TAG_ROOT").next();
			tagRoot.remove();
		}
	}

	@Override
	public boolean requiresReindex() {
		return true;
	}

}
