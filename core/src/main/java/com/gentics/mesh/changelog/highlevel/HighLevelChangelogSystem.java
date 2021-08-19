package com.gentics.mesh.changelog.highlevel;

import java.util.function.Predicate;

import com.gentics.mesh.cli.PostProcessFlags;
import com.gentics.mesh.core.data.changelog.HighLevelChange;
import com.gentics.mesh.core.data.root.MeshRoot;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * The {@link HighLevelChangelogSystem} is the second layer of changelog processing in Gentics Mesh. In contrast to the low level system does the high level
 * system work on OGM types. The system changelog entries can however only work on the current OGM classes. Thus the changelog entry can't be used to apply low
 * level changes. (e.g. change the type of a graph element, modify indices)
 * 
 * It is generally advised to avoid changelog entries and instead come up with a way to migrate the data on-the-fly.
 */
public interface HighLevelChangelogSystem {

	static final Logger log = LoggerFactory.getLogger(HighLevelChangelogSystem.class);

	/**
	 * Apply the changes which were not yet applied.
	 * 
	 * @param flags Flags which will be used to control the post process actions
	 * @param meshRoot mesh root
	 * @param filter optional filter
	 */
	void apply(PostProcessFlags flags, MeshRoot meshRoot, Predicate<? super HighLevelChange> filter);

	/**
	 * Check whether the change has already been applied.
	 *
	 * @param root
	 * @param change
	 * @return
	 */
	default boolean isApplied(MeshRoot root, HighLevelChange change) {
		if (log.isDebugEnabled()) {
			log.debug("Checking change {" + change.getName() + "}/{" + change.getUuid() + "}");
		}
		return root.getChangelogRoot().hasChange(change);
	}

	/**
	 * Mark all high level changes as applied.
	 *
	 * @param meshRoot
	 */
	void markAllAsApplied(MeshRoot meshRoot);

	/**
	 * Check whether any high level changelog entry needs to be applied.
	 * 
	 * @param meshRoot
	 * @param filter optional filter for high level changes to check (may be null to check all high level changes)
	 * @return
	 */
	boolean requiresChanges(MeshRoot meshRoot, Predicate<? super HighLevelChange> filter);
}
