package com.gentics.mesh.changelog.changes;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.changelog.Change;

/**
 * Stores a list of all changes in the system. Please note that the order of changes is very important and new changes should always be appended to the list
 * (bottom).
 */
public final class ChangesList {

	public static List<Change> getList() {
		List<Change> list = new ArrayList<>();
		list.add(new ChangeFixReleaseRelationship());
		//list.add(new Change_A36C972476C147F3AC972476C157F3EF());
		list.add(new ChangeReindex3());
		list.add(new ChangeReindex());
		list.add(new ChangeReindex2());
		list.add(new ChangeAddPublishFlag());
		list.add(new ChangeAddVersioning());
		return list;
	}
}
