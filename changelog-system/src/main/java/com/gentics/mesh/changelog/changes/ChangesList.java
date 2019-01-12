package com.gentics.mesh.changelog.changes;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.changelog.Change;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Stores a list of all changes in the system. Please note that the order of changes is very important and new changes should always be appended to the list
 * (bottom).
 */
public final class ChangesList {

	private static List<Change> list = new ArrayList<>();

	public static List<Change> getList() {

		list.addAll(Arrays.asList(
			new ReindexDatabase(),
			new ChangeAddPublishFlag(),
			new ChangeAddVersioning(),
			new ChangeFixReleaseRelationship(),
			new ChangeRemoveSearchQueueNodes(),
			new ChangeReindexAll(),
			new ChangeSanitizeSchemaJson(),
			new AddTagFamiliesToNodeIndex(),
			new RestructureTags(),
			new ReindexAllToFixFailedMigrations(),
			new SanitizeMicroschemaJson(),
			new ChangeSchemaVersionType(),
			new SanitizeSchemaNames(),
			new CreateMissingDraftEdges(),
			new RemoveBogusWebrootProperty(),
			new PurgeOldJobs(),
			new UpdateReleaseSchemaEdge(),
			new MigrateSchemaRawInfo(),
			new BinaryStorageMigration(),
			new ChangeNumberStringsToNumber(),
			new RenameReleasesToBranches(),
			new NodeContentLanguageMigration()
		// ADD NEW CHANGES HERE!
		));

		return list;
	}

	public static void main(String[] args) {
		System.out.println(UUIDUtil.randomUUID().toUpperCase());
	}

}
