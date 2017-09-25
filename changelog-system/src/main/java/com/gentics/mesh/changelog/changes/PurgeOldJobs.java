package com.gentics.mesh.changelog.changes;

import static com.tinkerpop.blueprints.Direction.OUT;

import com.gentics.mesh.changelog.AbstractChange;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Vertex;

public class PurgeOldJobs extends AbstractChange {

	@Override
	public String getName() {
		return "Purge old jobs";
	}

	@Override
	public String getDescription() {
		return "Purges any listed job in order to allow custom job types.";
	}

	@Override
	public void apply() {

		// 1. Remove jobs
		Vertex meshRoot = getMeshRootVertex();
		Vertex jobRoot = meshRoot.getVertices(Direction.OUT, "HAS_JOB_ROOT").iterator().next();
		Iterable<Vertex> jobIt = jobRoot.getVertices(OUT, "HAS_JOB");
		for (Vertex v : jobIt) {
			v.remove();
		}

		// 2. Remove JobImpl type
		getDb().removeVertexType("JobImpl");

	}

	@Override
	public String getUuid() {
		return "4AE75B065A4A452CA75B065A4A052CDA";
	}

}
