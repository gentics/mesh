package com.gentics.mesh.changelog.changes;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.changelog.Change;

/**
 * Static list of all changes in the system.
 */
public final class ChangesList {

	public static List<Change> getList() {
		List<Change> list = new ArrayList<>();
		list.add(new Change_424FA7436B6541269E6CE90C8C3D812D());
		return list;
	}
}
