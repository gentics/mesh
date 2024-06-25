package com.gentics.mesh.enterprise.dialect;

import java.sql.Types;
import java.util.UUID;

import org.hibernate.type.descriptor.java.BasicJavaDescriptor;
import org.hibernate.type.descriptor.sql.BinaryTypeDescriptor;
import org.hibernate.type.spi.TypeConfiguration;

/**
 * Hibernate Java type descriptor for MariaDB, supporting {@link UUID} Java type.
 * 
 * @author plyhun
 *
 */
class MariaDBUuidTypeDescriptor extends BinaryTypeDescriptor {

	private static final long serialVersionUID = 1L;

	public static final MariaDBUuidTypeDescriptor INSTANCE = new MariaDBUuidTypeDescriptor();

	public MariaDBUuidTypeDescriptor() {
		super();
	}

	@Override
	public int getSqlType() {
		return Types.OTHER;
	}
	
	@Override
	@SuppressWarnings("unchecked")
	public BasicJavaDescriptor<UUID> getJdbcRecommendedJavaTypeMapping(TypeConfiguration typeConfiguration) {
		return (BasicJavaDescriptor<UUID>) typeConfiguration.getJavaTypeDescriptorRegistry().getDescriptor( UUID.class );
	}
}
