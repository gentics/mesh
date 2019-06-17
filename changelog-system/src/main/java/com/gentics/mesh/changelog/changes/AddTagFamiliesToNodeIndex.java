package com.gentics.mesh.changelog.changes;

import com.gentics.mesh.changelog.AbstractChange;

/**
 * Reindex is necessary
 */
public class AddTagFamiliesToNodeIndex extends AbstractChange {
	/**
	 * Return the name of the change.
	 *
	 * @return
	 */
	@Override
	public String getName() {
		return "Add tag families to node index";
	}

	/**
	 * Return the description of the change.
	 *
	 * @return
	 */
	@Override
	public String getDescription() {
		return "Adds all tags grouped by tag families";
	}

	@Override
	public String getUuid() {
		return "4509765177F942BC82D064CF1DBA6108";
	}

	@Override
	public boolean requiresReindex() {
		return true;
	}
}
