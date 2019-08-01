package com.gentics.mesh.changelog.changes;

import java.util.stream.Stream;

import com.gentics.madl.index.IndexHandler;
import com.gentics.mesh.changelog.AbstractChange;

public class RemoveReleaseIndices extends AbstractChange {
	@Override
	public String getUuid() {
		return "956D6DB886EE48D4B7CD1BA0735A9C35";
	}

	@Override
	public String getName() {
		return "Remove Release indices";
	}

	@Override
	public String getDescription() {
		return "Removes all indices which might have the old release structure";
	}

	@Override
	public void applyOutsideTx() {
		IndexHandler index = getDb().index();
		Stream.of(
			"e.has_field_container_field",
			"e.has_field_container_release_type_lang",
			"e.has_release_inout",
			"e.has_release_out",
			"e.has_parent_node_release",
			"e.has_parent_node_release_out",
			"uniqueReleaseNameIndex"
		).forEach(indexName -> {
			try {
				log.info("Removing index {}...", indexName);
				index.removeIndex(indexName);
			} catch (Throwable err) {
				if (err.getCause() != null && err.getCause() instanceof NullPointerException) {
					log.info("Index {} could not be found. It will be skipped.", indexName);
				} else {
					log.error("Error while removing index {}. Index will be skipped", err);
				}
			}
		});
	}
}
