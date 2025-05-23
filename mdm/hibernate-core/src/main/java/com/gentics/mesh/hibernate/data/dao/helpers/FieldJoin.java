package com.gentics.mesh.hibernate.data.dao.helpers;

import static com.gentics.mesh.hibernate.util.HibernateUtil.makeAlias;
import static com.gentics.mesh.hibernate.util.HibernateUtil.makeParamName;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.query.NativeJoin;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;

/**
 * Many-to-one join.
 */
public class FieldJoin<CHILDIMPL extends HibBaseElement, ROOTIMPL extends HibBaseElement> extends AbstractRootJoin<CHILDIMPL, ROOTIMPL> implements RootJoin<CHILDIMPL, ROOTIMPL> {
	
	private final String fieldName;

	public FieldJoin(Class<CHILDIMPL> domainClass, Class<ROOTIMPL> rootClass, String fieldName) {
		super(domainClass, rootClass);
		this.fieldName = fieldName;
	}

	@Override
	public NativeJoin makeJoin(String myAlias, HibBaseElement root) {
		DatabaseConnector dc = HibernateTx.get().data().getDatabaseConnector();
		String rootAlias = makeAlias(dc.maybeGetDatabaseEntityName(root.getClass()).get());
		NativeJoin mj = new NativeJoin();
		mj.addJoin(dc.maybeGetPhysicalTableName(root.getClass()).get(), rootAlias, 
				new String[] {myAlias + "." + dc.renderNonContentColumn(fieldName + "_dbUuid")}, new String[] {dc.renderNonContentColumn("dbUuid")},
				JoinType.INNER,
				myAlias);
		String rootParam = makeParamName(root.getId());
		mj.appendWhereClause(String.format(" ( %s.%s = :%s ) ", rootAlias, dc.renderNonContentColumn("dbUuid"), rootParam));
		mj.getParameters().put(rootParam, root.getId());
		return mj;
	}

	@Override
	public Join<CHILDIMPL, ROOTIMPL> getJoin(Root<CHILDIMPL> dRoot, EntityType<CHILDIMPL> rootMetamodel) {
		return dRoot.join(rootMetamodel.getSingularAttribute(fieldName, rootClass));
	}
}
