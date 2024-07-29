package com.gentics.mesh.core.endpoint.admin.debuginfo.providers;

import java.util.stream.Collectors;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.branch.HibBranch;
import com.gentics.mesh.core.data.dao.BranchDao;
import com.gentics.mesh.core.data.dao.ProjectDao;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.db.Database;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoBufferEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoEntry;
import com.gentics.mesh.core.endpoint.admin.debuginfo.DebugInfoProvider;
import com.gentics.mesh.core.endpoint.branch.BranchCrudHandler;

import io.reactivex.Flowable;

/**
 * Debug info provider for migration status information.
 */
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
	public Flowable<DebugInfoEntry> debugInfoEntries(InternalActionContext ac) {
		return getAllBranches()
			.flatMap(projectBranch -> Flowable.mergeArray(
				getSchemastatus(projectBranch.branch).map(json -> toDebugInfoEntry(json, projectBranch, "schemas.json")),
				getMicroschemastatus(projectBranch.branch).map(json -> toDebugInfoEntry(json, projectBranch, "microschemas.json"))));
	}

	private DebugInfoEntry toDebugInfoEntry(String json, ProjectBranch projectBranch, String filename) {
		return DebugInfoBufferEntry.fromString(
			String.format("migrationStatus/%s/%s/%s", projectBranch.projectName, projectBranch.branchName, filename),
			json);
	}

	private Flowable<ProjectBranch> getAllBranches() {
		return db.singleTx(tx -> {
			BranchDao branchDao = tx.branchDao();
			ProjectDao projectDao = tx.projectDao();
			return projectDao.findAll().stream()
				.flatMap(project -> branchDao.findAll(project).stream()
					.map(branch -> new ProjectBranch(project.getName(), branch.getName(), branch)))
				.collect(Collectors.toList());
		})
			.flatMapPublisher(Flowable::fromIterable);
	}

	private Flowable<String> getSchemastatus(HibBranch branch) {
		return db.singleTx(() -> branchCrudHandler.getSchemaVersionsInfo(CommonTx.get().branchDao().mergeIntoPersisted(branch.getProject(), branch)))
			.map(o -> o.toJson(boot.mesh().getOptions().getHttpServerOptions().isMinifyJson()))
			.toFlowable();
	}

	private Flowable<String> getMicroschemastatus(HibBranch branch) {
		return db.singleTx(() -> branchCrudHandler.getMicroschemaVersions(CommonTx.get().branchDao().mergeIntoPersisted(branch.getProject(), branch)))
			.map(o -> o.toJson(boot.mesh().getOptions().getHttpServerOptions().isMinifyJson()))
			.toFlowable();
	}

	private static class ProjectBranch {
		private final String projectName;
		private final String branchName;
		private final HibBranch branch;

		private ProjectBranch(String projectName, String branchName, HibBranch branch) {
			this.projectName = projectName;
			this.branchName = branchName;
			this.branch = branch;
		}
	}
}
