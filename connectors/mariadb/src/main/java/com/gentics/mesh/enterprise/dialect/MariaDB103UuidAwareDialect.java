package com.gentics.mesh.enterprise.dialect;

import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.boot.model.TypeContributions;
import org.hibernate.dialect.MariaDB103Dialect;
import org.hibernate.service.ServiceRegistry;

/**
 * An extension for existing Hibernate dialect for MariaDB 10.3+, considering using UUIDs as binary SQL type of 16 bytes length. 
 * 
 * @author plyhun
 *
 */
public class MariaDB103UuidAwareDialect extends MariaDB103Dialect {

	@Override
	public void contributeTypes(TypeContributions typeContributions, ServiceRegistry serviceRegistry) {
		super.contributeTypes( typeContributions, serviceRegistry );

		registerColumnType( Types.OTHER, "uuid" );
		typeContributions.contributeType( MariaDBUuidType.INSTANCE );
	}

	@Override
	public String getTypeName(int code, long length, int precision, int scale) throws HibernateException {
		String typeName = super.getTypeName(code, length, precision, scale);
		if (Types.OTHER == code && "uuid".equals(typeName)) {
			return "binary(16)";
		} else {
			return typeName;
		}
	}
}
