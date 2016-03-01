package com.gentics.mesh.core.data.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NEXT_RELEASE;

import java.util.HashSet;
import java.util.Set;

import com.gentics.mesh.core.data.Release;
import com.gentics.mesh.core.data.generic.AbstractMeshCoreVertex;
import com.gentics.mesh.core.data.search.SearchQueueBatch;
import com.gentics.mesh.core.data.search.SearchQueueEntryAction;
import com.gentics.mesh.core.rest.release.ReleaseResponse;
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;

import rx.Observable;

public class ReleaseImpl extends AbstractMeshCoreVertex<ReleaseResponse, Release> implements Release {

	public static void init(Database database) {
		database.addVertexType(ReleaseImpl.class);
	}

	@Override
	public Observable<? extends Release> update(InternalActionContext ac) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public String getType() {
		return Release.TYPE;
	}

	@Override
	public void addRelatedEntries(SearchQueueBatch batch, SearchQueueEntryAction action) {
		// TODO Auto-generated method stub

	}

	@Override
	public Observable<ReleaseResponse> transformToRestSync(InternalActionContext ac, String... languageTags) {
		Set<Observable<ReleaseResponse>> obsParts = new HashSet<>();

		ReleaseResponse restRelease = new ReleaseResponse();
		restRelease.setName(getName());

		// Add common fields
		obsParts.add(fillCommonRestFields(ac, restRelease));

		// Merge and complete
		return Observable.merge(obsParts).last();
	}

	@Override
	public String getName() {
		return getProperty("name");
	}

	@Override
	public void setName(String name) {
		setProperty("name", name);
	}

	@Override
	public boolean isActive() {
		return getProperty("active");
	}

	@Override
	public void setActive(boolean active) {
		setProperty("active", active);
	}

	@Override
	public Release getNextRelease() {
		return out(HAS_NEXT_RELEASE).has(ReleaseImpl.class).nextOrDefaultExplicit(ReleaseImpl.class, null);
	}

	@Override
	public void setNextRelease(Release release) {
		setUniqueLinkOutTo(release.getImpl(), HAS_NEXT_RELEASE);
	}

	@Override
	public Release getPreviousRelease() {
		return in(HAS_NEXT_RELEASE).has(ReleaseImpl.class).nextOrDefault(ReleaseImpl.class, null);
	}
}
