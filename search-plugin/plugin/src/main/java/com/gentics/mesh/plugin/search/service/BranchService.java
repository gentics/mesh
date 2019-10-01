package com.gentics.mesh.plugin.search.service;

import com.gentics.mesh.core.rest.branch.BranchResponse;
import com.gentics.mesh.parameter.ParameterProvider;
import com.gentics.mesh.plugin.search.config.SearchConfiguration;
import com.gentics.mesh.rest.client.MeshRestClient;
import io.reactivex.Single;

import javax.annotation.Nonnull;
import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.Objects;

/**
 * Provides Branch information from Mesh.
 */
@Singleton
public class BranchService extends AbstractCachingService<BranchService.CacheKey, BranchResponse> {

	/** Cache key comprised of project name and branch UUID. */
	static class CacheKey {

		final String project;
		final String branchUuid;

		CacheKey(String project, String branchUuid) {
			Objects.requireNonNull(project, "CacheKey project must not be null");
			Objects.requireNonNull(branchUuid, "CacheKey branchUuid must not be null");

			this.project = project;
			this.branchUuid = branchUuid;
		}

		@Override
		public boolean equals(Object other) {
			if (this == other) {
				return true;
			}

			if (other == null || getClass() != other.getClass()) {
				return false;
			}

			CacheKey otherKey = (CacheKey) other;

			return project.equals(otherKey.project)
				&& branchUuid.equals(otherKey.branchUuid);
		}

		@Override
		public int hashCode() {
			// Branches are unique anyways, so there is no need to include the project here.
			return branchUuid.hashCode();
		}

		@Override
		public String toString() {
			return project + "/" + branchUuid;
		}
	}

	private final MeshRestClient adminClient;

	@Inject
	public BranchService(SearchConfiguration config, MeshRestClient adminClient) {
		super(config);

		this.adminClient = adminClient;

		log.info("BranchService initialized");
	}

	/**
	 * Load the specified branch from the cache or from Mesh.
	 *
	 * @param project The project of the desired
	 * @param uuid The UUID of the desired branch
	 * @return A <code>Single</code> containing the requested <code>BranchResponse</code>
	 */
	public Single<BranchResponse> getBranch(String project, String uuid) {
		return getFromCache(new CacheKey(project, uuid));
	}

	/**
	 * Loads the specified branch from Mesh.
	 *
	 * @see MeshRestClient#findBranchByUuid(String, String, ParameterProvider...)
	 *
	 * @param key The cache key for the value to load
	 * @return A <code>Single</code> containing the requested branch
	 */
	@Override
	@Nonnull
	protected Single<BranchResponse> loadValue(CacheKey key) {
		if (log.isDebugEnabled()) {
			log.debug("Loading branch {} ({})", key.branchUuid, key.project);
		}

		return adminClient.findBranchByUuid(key.project, key.branchUuid).toSingle();
	}
}
