package com.gentics.mesh.graphdb;

import com.orientechnologies.orient.core.config.OGlobalConfiguration;
import com.orientechnologies.orient.core.db.ODatabaseDocumentInternal;
import com.orientechnologies.orient.core.metadata.OMetadataDefault;
import com.orientechnologies.orient.core.metadata.security.ORole;
import com.orientechnologies.orient.core.metadata.security.ORule;
import com.orientechnologies.orient.core.metadata.security.OSecurity;
import com.orientechnologies.orient.core.metadata.security.OSecurityShared;
import com.orientechnologies.orient.core.metadata.security.OToken;
import com.orientechnologies.orient.core.metadata.security.OUser;

public class OSecuritySharedPatched extends OSecurityShared {

	public OSecuritySharedPatched(final OSecurity iDelegate, final ODatabaseDocumentInternal iDatabase) {

	}

	public OUser authenticate(String iUsername, String iUserPassword) {
		return null;
	}

	public OUser authenticate(OToken authToken) {
		return null;
	}

	@Override
	public OUser create() {
		final OUser adminUser = createMetadata();
		final ORole readerRole = createRole("reader", ORole.ALLOW_MODES.DENY_ALL_BUT);
		readerRole.addRule(ORule.ResourceGeneric.DATABASE, null, ORole.PERMISSION_READ);
		readerRole.addRule(ORule.ResourceGeneric.SCHEMA, null, ORole.PERMISSION_READ);
		readerRole.addRule(ORule.ResourceGeneric.CLUSTER, OMetadataDefault.CLUSTER_INTERNAL_NAME, ORole.PERMISSION_READ);
		readerRole.addRule(ORule.ResourceGeneric.CLUSTER, "orole", ORole.PERMISSION_NONE);
		readerRole.addRule(ORule.ResourceGeneric.CLUSTER, "ouser", ORole.PERMISSION_NONE);
		readerRole.addRule(ORule.ResourceGeneric.CLASS, null, ORole.PERMISSION_READ);
		readerRole.addRule(ORule.ResourceGeneric.CLASS, "OUser", ORole.PERMISSION_NONE);
		readerRole.addRule(ORule.ResourceGeneric.CLUSTER, null, ORole.PERMISSION_READ);
		readerRole.addRule(ORule.ResourceGeneric.COMMAND, null, ORole.PERMISSION_READ);
		readerRole.addRule(ORule.ResourceGeneric.RECORD_HOOK, null, ORole.PERMISSION_READ);
		readerRole.addRule(ORule.ResourceGeneric.FUNCTION, null, ORole.PERMISSION_READ);
		readerRole.addRule(ORule.ResourceGeneric.SYSTEM_CLUSTERS, null, ORole.PERMISSION_NONE);
		readerRole.save();

		// This will return the global value if a local storage context configuration value does not exist.
		boolean createDefUsers = getDatabase().getStorage().getConfiguration().getContextConfiguration()
				.getValueAsBoolean(OGlobalConfiguration.CREATE_DEFAULT_USERS);

		if (createDefUsers)
			createUser("reader", "reader", new String[] { readerRole.getName() });

		final ORole writerRole = createRole("writer", ORole.ALLOW_MODES.DENY_ALL_BUT);
		writerRole.addRule(ORule.ResourceGeneric.DATABASE, null, ORole.PERMISSION_READ);
		writerRole.addRule(ORule.ResourceGeneric.SCHEMA, null, ORole.PERMISSION_READ);
		writerRole.addRule(ORule.ResourceGeneric.CLUSTER, OMetadataDefault.CLUSTER_INTERNAL_NAME, ORole.PERMISSION_READ);
		readerRole.addRule(ORule.ResourceGeneric.CLUSTER, "orole", ORole.PERMISSION_NONE);
		readerRole.addRule(ORule.ResourceGeneric.CLUSTER, "ouser", ORole.PERMISSION_NONE);
		writerRole.addRule(ORule.ResourceGeneric.CLASS, null, ORole.PERMISSION_ALL);
		writerRole.addRule(ORule.ResourceGeneric.CLASS, "OUser", ORole.PERMISSION_NONE);
		writerRole.addRule(ORule.ResourceGeneric.CLUSTER, null, ORole.PERMISSION_ALL);
		writerRole.addRule(ORule.ResourceGeneric.COMMAND, null, ORole.PERMISSION_ALL);
		writerRole.addRule(ORule.ResourceGeneric.RECORD_HOOK, null, ORole.PERMISSION_ALL);
		writerRole.addRule(ORule.ResourceGeneric.FUNCTION, null, ORole.PERMISSION_READ);
		writerRole.addRule(ORule.ResourceGeneric.SYSTEM_CLUSTERS, null, ORole.PERMISSION_NONE);
		writerRole.save();

		if (createDefUsers) {
			createUser("writer", "writer", new String[] { writerRole.getName() });
		}

		return adminUser;
	}

}
