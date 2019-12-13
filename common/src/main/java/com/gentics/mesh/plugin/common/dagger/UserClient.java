package com.gentics.mesh.plugin.common.dagger;

import javax.inject.Qualifier;

/**
 * Qualifier annotation to mark an injected
 * {@link com.gentics.mesh.rest.client.MeshRestClient} as an user client.
 */
@Qualifier
public @interface UserClient {
}
