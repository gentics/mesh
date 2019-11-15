package com.gentics.mesh.changelog.changes;

import java.util.Arrays;
import java.util.List;

import com.gentics.mesh.changelog.Change;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.util.UUIDUtil;

/**
 * Stores a list of all changes in the system. Please note that the order of changes is very important and new changes should always be appended to the list
 * (bottom).
 */
public final class ChangesList {

	public static List<Change> getList(MeshOptions options) {
		return Arrays.asList(
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
			new BinaryStorageMigration(options),
			new ChangeNumberStringsToNumber(),
			new RenameReleasesToBranches(),
			new NodeContentLanguageMigration(),
			new NodeContentEditorMigration(),
			new ReplacePermissionEdges(),
			new RemoveReleaseIndices(),
			new ReplaceSchemaEdges(),
			new ReplaceSchemaVersionEdges(),
			new ReplaceMicroschemaVersionEdges(),
			new ReplaceParentEdges()
			// ADD NEW CHANGES HERE!
		);
	}

	public static void main(String[] args) {
		System.out.println(UUIDUtil.randomUUID().toUpperCase());
	}

}
