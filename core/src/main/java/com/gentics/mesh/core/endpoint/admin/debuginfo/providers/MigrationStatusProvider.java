package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.Branch;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoBufferEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.admin.debuginfo.LoadLevel;
import com.gentics.mesh.core.endpoint.branch.BranchCrudHandler;
import com.gentics.mesh.core.rest.common.RestModel;
import com.gentics.mesh.graphdb.spi.Database;

import io.reactivex.Flowable;

@Singleton
public class MigrationStatusProvider implements DebugInfoProvider {
	private final Database db;
	private final BootstrapInitializer boot;
	private final BranchCrudHandler branchCrudHandler;

	@Inject
	public MigrationStatusProvider(Database db, BootstrapInitializer boot, BranchCrudHandler branchCrudHandler) {
		this.db = db;
		this.boot = boot;
		this.branchCrudHandler = branchCrudHandler;
	}

	@Override
	public String name() {
		return "migrationStatus";
	}

	@Override
	public LoadLevel loadLevel() {
		return LoadLevel.LIGHT;
	}

	@Override
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		return getAllBranches()
			.flatMap(projectBranch -> Flowable.mergeArray(
				getSchemastatus(projectBranch.branch).map(json -> toDebugInfoEntry(json, projectBranch, "schemas.json")),
				getMicroschemastatus(projectBranch.branch).map(json -> toDebugInfoEntry(json, projectBranch, "microschemas.json"))
			));
	}

	private DebugInfoEntry toDebugInfoEntry(String json, ProjectBranch projectBranch, String filename) {
		return DebugInfoBufferEntry.fromString(
			String.format("migrationStatus/%s/%s/%s", projectBranch.projectName, projectBranch.branchName, filename),
			json
		);
	}

	private Flowable<ProjectBranch> getAllBranches() {
		return db.singleTx(() -> boot.projectRoot().findAll().stream()
			.flatMap(project -> project.getBranchRoot().findAll().stream()
			.map(branch -> new ProjectBranch(project.getName(), branch.getName(), branch)))
			.collect(Collectors.toList()))
			.flatMapPublisher(Flowable::fromIterable);
	}

	private Flowable<String> getSchemastatus(Branch branch) {
		return db.singleTx(() -> branchCrudHandler.getSchemaVersionsInfo(branch))
			.map(RestModel::toJson)
			.toFlowable();
	}

	private Flowable<String> getMicroschemastatus(Branch branch) {
		return db.singleTx(() -> branchCrudHandler.getMicroschemaVersions(branch))
			.map(RestModel::toJson)
			.toFlowable();
	}

	private static class ProjectBranch {
		private final String projectName;
		private final String branchName;
		private final Branch branch;

		private ProjectBranch(String projectName, String branchName, Branch branch) {
			this.projectName = projectName;
			this.branchName = branchName;
			this.branch = branch;
		}
	}
}
