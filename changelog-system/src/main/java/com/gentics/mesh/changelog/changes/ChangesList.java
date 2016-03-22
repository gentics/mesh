package com.gentics.mesh.changelog.changes;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.changelog.Change;

/**
 * Static list of all changes in the system. Please note that the order of changes is very important and new changes should always be appended to the list.
 */
public final class ChangesList {

	public static List<Change> list = new ArrayList<>();

	static {
		list.add(new Change_093BEFB47FA4476FBE37FD27C613F7AA());
	}

	public static List<Change> getList() {
		return list;
	}
}
