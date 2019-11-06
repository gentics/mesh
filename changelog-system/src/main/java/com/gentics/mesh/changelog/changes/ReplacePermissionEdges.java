package com.gentics.mesh.changelog.changes;

import static com.gentics.mesh.core.data.relationship.GraphPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.google.common.collect.ImmutableMap;
import com.tinkerpop.blueprints.Direction;
import com.tinkerpop.blueprints.Edge;

public class ReplacePermissionEdges extends AbstractChange {
	private final Map<String, GraphPermission> permLabels = ImmutableMap.<String, GraphPermission>builder()
		.put("HAS_CREATE_PERMISSION", CREATE_PERM)
		.put("HAS_READ_PERMISSION", READ_PERM)
		.put("HAS_UPDATE_PERMISSION", UPDATE_PERM)
		.put("HAS_DELETE_PERMISSION", DELETE_PERM)
		.put("HAS_READ_PUBLISHED_PERMISSION", READ_PUBLISHED_PERM)
		.put("HAS_PUBLISH_PERMISSION", PUBLISH_PERM)
		.build();

	@Override
	public String getUuid() {
		return "2F2817213D1A43BFA817213D1AC3BF70";
	}

	@Override
	public String getName() {
		return "ReplacePermissionEdges";
	}

	@Override
	public String getDescription() {
		return "Replaces permission edges by sets of strings of role uuids.";
	}

	@Override
	public void applyInTx() {
		String[] labels = permLabels.keySet().toArray(new String[0]);
		iterateWithCommit(getGraph().getVertices(), vertex -> {
			for (Edge permEdge : vertex.getEdges(Direction.IN, labels)) {
				String roleUuid = permEdge.getVertex(Direction.OUT).getProperty("uuid");
				String permissionPropertyKey = permLabels.get(permEdge.getLabel()).propertyKey();
				Set<String> perms = Optional.ofNullable(vertex.<Set<String>>getProperty(permissionPropertyKey))
					.map(set -> {
						set.add(roleUuid);
						return set;
					}).orElseGet(() -> Collections.singleton(roleUuid));
				vertex.setProperty(permissionPropertyKey, perms);
				permEdge.remove();
			}
		});
	}
}
