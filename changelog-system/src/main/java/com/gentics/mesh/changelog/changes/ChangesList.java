package com.gentics.mesh.changelog.changes;

import java.util.ArrayList;
import java.util.List;

import com.gentics.mesh.changelog.Change;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Stores a list of all changes in the system. Please note that the order of changes is very important and new changes should always be appended to the list
 * (bottom).
 */
public final class ChangesList {

	public static List<Change> getList() {
		List<Change> list = new ArrayList<>();
		// list.add(new Change_A36C972476C147F3AC972476C157F3EF());
		list.add(new ChangeAddPublishFlag());
		list.add(new ChangeAddVersioning());
		list.add(new ChangeFixReleaseRelationship());
		list.add(new ChangeRemoveSearchQueueNodes());
		list.add(new ChangeReindexAll());
		list.add(new ChangeSanitizeSchemaJson());
		list.add(new AddTagFamiliesToNodeIndex());
		list.add(new RestructureTags());
		list.add(new ReindexAllToFixFailedMigrations());
		list.add(new SanitizeMicroschemaJson());
		list.add(new ChangeSchemaVersionType());
		list.add(new SanitizeSchemaNames());
		list.add(new CreateMissingDraftEdges());
		list.add(new RemoveBogusWebrootProperty());
		list.add(new PurgeOldJobs());
		list.add(new UpdateReleaseSchemaEdge());
		list.add(new MigrateSchemaRawInfo());
		list.add(new BinaryStorageMigration());
		list.add(new ChangeNumberStringsToNumber());
		list.add(new RenameReleasesToBranches());
		list.add(new NodeContentLanguageMigration());
		// ADD NEW CHANGES HERE!
		return list;
	}

	public static void main(String[] args) {
		System.out.println(UUIDUtil.randomUUID().toUpperCase());
	}

}
