package com.gentics.mesh.hibernate.type;

import org.hibernate.boot.model.TypeContributions;
import org.hibernate.boot.model.TypeContributor;
import org.hibernate.service.ServiceRegistry;

/**
 * Custom Mesh SQL/Java type contributor.
 */
public class CustomTypeContributor implements TypeContributor {

	@Override
	public void contribute(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		typeContributions.contributeType(JsonObjectType.INSTANCE);
	}
}
