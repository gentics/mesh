package com.gentics.mesh.hibernate.data.dao.helpers;

import static com.gentics.mesh.hibernate.util.HibernateUtil.makeAlias;
import static com.gentics.mesh.hibernate.util.HibernateUtil.makeParamName;

import com.gentics.mesh.core.data.HibBaseElement;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.database.connector.DatabaseConnector;
import com.gentics.mesh.hibernate.MeshTablePrefixStrategy;
import com.gentics.mesh.query.NativeJoin;

import jakarta.persistence.criteria.Join;
import jakarta.persistence.criteria.JoinType;
import jakarta.persistence.criteria.Root;
import jakarta.persistence.metamodel.EntityType;

/**
 * Many-to-many join.
 */
public class CrossTableJoin<CHILDIMPL extends HibBaseElement, ROOTIMPL extends HibBaseElement> extends AbstractRootJoin<CHILDIMPL, ROOTIMPL> implements RootJoin<CHILDIMPL, ROOTIMPL> {

	private final String tableName;
	private final String inFieldName;
	private final String outFieldName;

	public CrossTableJoin(Class<CHILDIMPL> domainClass, Class<ROOTIMPL> rootClass, String tableName, String inFieldName, String outFieldName) {
		super(domainClass, rootClass);
		this.tableName = tableName;
		this.inFieldName = inFieldName;
		this.outFieldName = outFieldName;
	}

	@Override
	public NativeJoin makeJoin(String myAlias, ROOTIMPL root) {
		DatabaseConnector dc = HibernateTx.get().data().getDatabaseConnector();
		String rootAlias = makeAlias(dc.maybeGetDatabaseEntityName(root.getClass()).get());
		NativeJoin mj = new NativeJoin();
		String tableNameAlias = makeAlias(rootAlias + tableName);
		mj.addJoin(dc.maybeGetPhysicalTableName(root.getClass()).get(), rootAlias, 
				new String[] {tableNameAlias + "." + dc.renderNonContentColumn(inFieldName + "_dbUuid")}, new String[] {dc.renderNonContentColumn("dbUuid")},
				JoinType.INNER,
				tableNameAlias);
		mj.addJoin(MeshTablePrefixStrategy.TABLE_NAME_PREFIX + tableName, tableNameAlias, 
				new String[] {myAlias + "." + dc.renderNonContentColumn("dbUuid")}, new String[] {dc.renderNonContentColumn(outFieldName + "_dbUuid")},
				JoinType.INNER,
				myAlias);
		String rootParam = makeParamName(root.getId());
		mj.appendWhereClause(String.format(" ( %s.%s = :%s ) ", rootAlias, dc.renderNonContentColumn("dbUuid"), rootParam));
		mj.getParameters().put(rootParam, root.getId());
		return mj;
	}

	@Override
	public Join<CHILDIMPL, ROOTIMPL> getJoin(Root<CHILDIMPL> dRoot, EntityType<CHILDIMPL> rootMetamodel) {
		return dRoot.join(rootMetamodel.getSet(inFieldName, rootClass));
	}
}
