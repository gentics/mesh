package com.gentics.mesh.enterprise.dialect;

import java.util.UUID;

import org.hibernate.type.AbstractSingleColumnStandardBasicType;
import org.hibernate.type.descriptor.java.UUIDTypeDescriptor;

/**
 * Hibernate type implementation for {@link UUIDTypeDescriptor} for MariaDB 10.3+.
 * 
 * @author plyhun
 *
 */
class MariaDBUuidType extends AbstractSingleColumnStandardBasicType<UUID> {

	private static final long serialVersionUID = 1L;

	public static final MariaDBUuidType INSTANCE = new MariaDBUuidType();

	public MariaDBUuidType() {
		super( MariaDBUuidTypeDescriptor.INSTANCE, UUIDTypeDescriptor.INSTANCE );
	}

	@Override
	public String getName() {
		return "mariadb-uuid-binary";
	}

	@Override
	protected boolean registerUnderJavaType() {
		return true;
	}
}