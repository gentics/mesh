package com.gentics.mesh.core.data.root.impl;

import static com.gentics.mesh.core.data.relationship.GraphPermission.UPDATE_PERM;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_INITIAL_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_LATEST_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_RELEASE_ROOT;
import static com.gentics.mesh.core.rest.error.Errors.conflict;
import static com.gentics.mesh.core.rest.error.Errors.error;
import static io.netty.handler.codec.http.HttpResponseStatus.BAD_REQUEST;
import static io.netty.handler.codec.http.HttpResponseStatus.FORBIDDEN;

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.core.data.MeshAuthUser;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.User;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.impl.ReleaseImpl;
import com.gentics.mesh.core.data.relationship.GraphPermission;
import com.gentics.mesh.core.data.root.ReleaseRoot;
import com.gentics.mesh.core.data.schema.SchemaContainer;
import com.gentics.mesh.core.rest.release.ReleaseCreateRequest;
import com.gentics.mesh.etc.MeshSpringConfiguration;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;

import rx.Observable;

public class ReleaseRootImpl extends AbstractRootVertex<Release> implements ReleaseRoot {

	public static void init(Database database) {
		database.addVertexType(ReleaseRootImpl.class);
	}

	@Override
	public Project getProject() {
		return in(HAS_RELEASE_ROOT).has(ProjectImpl.class).nextOrDefaultExplicit(ProjectImpl.class, null);
	}

	@Override
	public Release create(String name, User creator) {
		Release latestRelease = getLatestRelease();

		Release release = getGraph().addFramedVertex(ReleaseImpl.class);
		addItem(release);
		release.setCreated(creator);
		release.setName(name);
		release.setActive(true);

		if (latestRelease == null) {
			// if this is the first release, make it the initial release
			setSingleLinkOutTo(release.getImpl(), HAS_INITIAL_RELEASE);
		} else {
			// otherwise link the releases
			latestRelease.setNextRelease(release);
		}

		// make the new release the latest
		setSingleLinkOutTo(release.getImpl(), HAS_LATEST_RELEASE);

		// set initial permissions on the release
		creator.addCRUDPermissionOnRole(getProject(), UPDATE_PERM, release);

		// assign the newest schema versions of all project schemas to the release
		List<? extends SchemaContainer> projectSchemas = getProject().getSchemaContainerRoot().findAll();
		for (SchemaContainer schemaContainer : projectSchemas) {
			release.assignSchemaVersion(schemaContainer.getLatestVersion());
		}

		return release;
	}

	@Override
	public Release getInitialRelease() {
		return out(HAS_INITIAL_RELEASE).has(ReleaseImpl.class).nextOrDefaultExplicit(ReleaseImpl.class, null);
	}

	@Override
	public Release getLatestRelease() {
		return out(HAS_LATEST_RELEASE).has(ReleaseImpl.class).nextOrDefaultExplicit(ReleaseImpl.class, null);
	}

	@Override
	public Observable<Release> create(InternalActionContext ac) {
		Database db = MeshSpringConfiguration.getInstance().database();

		ReleaseCreateRequest createRequest = ac.fromJson(ReleaseCreateRequest.class);
		MeshAuthUser requestUser = ac.getUser();

		// check for completeness of request
		if (StringUtils.isEmpty(createRequest.getName())) {
			throw error(BAD_REQUEST, "release_missing_name");
		}

		return db.noTrx(() -> {
			Project project = getProject();
			String projectName = project.getName();
			String projectUuid = project.getUuid();

			return requestUser.hasPermissionAsync(ac, project, GraphPermission.UPDATE_PERM).flatMap(hasPerm -> {
				if (hasPerm) {

					Release createdRelease = db.trx(() -> {
						requestUser.reload();

						// check for uniqueness of release name (per project)
						Release conflictingRelease = db.checkIndexUniqueness(ReleaseImpl.UNIQUENAME_INDEX_NAME,
								ReleaseImpl.class, getUniqueNameKey(createRequest.getName()));
						if (conflictingRelease != null) {
							throw conflict(conflictingRelease.getUuid(), conflictingRelease.getName(),
									"release_conflicting_name", createRequest.getName());
						}

						Release release = create(createRequest.getName(), requestUser);

						return release;
					});

					return Observable.just(createdRelease);
				} else {
					throw error(FORBIDDEN, "error_missing_perm", projectUuid + "/" + projectName);
				}
			});
		});
	}

	@Override
	public Class<? extends Release> getPersistanceClass() {
		return ReleaseImpl.class;
	}

	@Override
	public String getRootLabel() {
		return HAS_RELEASE;
	}

	@Override
	public String getUniqueNameKey(String name) {
		return getUuid() + "-" + name;
	}
}
