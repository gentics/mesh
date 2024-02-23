package com.gentics.mesh.changelog.changes;

import static com.gentics.mesh.core.data.perm.InternalPermission.CREATE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.DELETE_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.PUBLISH_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.READ_PUBLISHED_PERM;
import static com.gentics.mesh.core.data.perm.InternalPermission.UPDATE_PERM;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.apache.tinkerpop.gremlin.structure.Direction;
import org.apache.tinkerpop.gremlin.structure.Edge;

import com.gentics.mesh.changelog.AbstractChange;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.util.StreamUtil;
import com.google.common.collect.ImmutableMap;


/**
 * Changelog entry which removes the permission edges.
 */
public class ReplacePermissionEdges extends AbstractChange {

	private final Map<String, InternalPermission> permLabels = ImmutableMap.<String, InternalPermission>builder()
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
		String[] labels = permLabels.keySet().toArray(new String[permLabels.size()]);
		iterateWithCommit(StreamUtil.toIterable(getGraph().vertices()), vertex -> {
			for (Edge permEdge : StreamUtil.toIterable(vertex.edges(Direction.IN, labels))) {
				String roleUuid = permEdge.outVertex().<String>property("uuid").orElse(null);
				String permissionPropertyKey = permLabels.get(permEdge.label()).propertyKey();
				Set<String> perms = Optional.ofNullable(vertex.<Set<String>>property(permissionPropertyKey).orElse(null))
					.map(set -> {
						set.add(roleUuid);
						return set;
					}).orElseGet(() -> Collections.singleton(roleUuid));
				vertex.property(permissionPropertyKey, perms);
				permEdge.remove();
			}
		});
	}
}
