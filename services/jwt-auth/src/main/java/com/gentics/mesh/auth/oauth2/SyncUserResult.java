package com.gentics.mesh.auth.oauth2;

import com.gentics.mesh.core.data.MeshAuthUser;

/**
 * Represents a MeshAuthUser. Alternatively this can also signal that a redirect is required.
 */
interface SyncUserResult {
	boolean requiresRedirect();
	MeshAuthUser meshAuthUser();

	SyncUserResult redirectInstance = new SyncUserResult() {
		@Override
		public boolean requiresRedirect() {
			return true;
		}

		@Override
		public MeshAuthUser meshAuthUser() {
			throw new IllegalStateException("Requires redirect!");
		}
	};

	static SyncUserResult redirect() {
		return redirectInstance;
	}

	static SyncUserResult just(MeshAuthUser user) {
		return new SyncUserResult() {
			@Override
			public boolean requiresRedirect() {
				return false;
			}

			@Override
			public MeshAuthUser meshAuthUser() {
				return user;
			}
		};
	}
}
