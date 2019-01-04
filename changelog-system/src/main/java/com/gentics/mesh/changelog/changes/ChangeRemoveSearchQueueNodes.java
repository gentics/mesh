package com.gentics.mesh.changelog.changes;

import static org.apache.tinkerpop.gremlin.structure.Direction.OUT;

import com.gentics.mesh.changelog.AbstractChange;
import org.apache.tinkerpop.gremlin.structure.Vertex;

public class ChangeRemoveSearchQueueNodes extends AbstractChange {

	@Override
	public String getName() {
		return "Remove Search Queue Nodes";
	}

	@Override
	public String getDescription() {
		return "Remove the search queue nodes which are no longer needed since the search queue is now handled within the jvm memory";
	}

	@Override
	public void apply() {
		Vertex meshRoot = getMeshRootVertex();
		Vertex searchQueueRoot = meshRoot.vertices(OUT, "HAS_SEARCH_QUEUE_ROOT").next();

		for (Vertex batch : (Iterable<Vertex>) () -> searchQueueRoot.vertices(OUT, "HAS_BATCH")) {
			for (Vertex entry : (Iterable<Vertex>) () -> batch.vertices(OUT, "HAS_ITEM")) {
				entry.remove();
			}
			batch.remove();
		}
		searchQueueRoot.remove();
	}

	@Override
	public String getUuid() {
		return "EFFF56B1AD304FB5BF56B1AD306FB5F3";
	}

}
