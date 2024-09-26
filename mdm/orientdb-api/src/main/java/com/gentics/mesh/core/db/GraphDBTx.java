package com.gentics.mesh.core.db;

import com.gentics.mesh.core.data.dao.BinaryDaoWrapper;
import com.gentics.mesh.core.data.dao.BranchDaoWrapper;
import com.gentics.mesh.core.data.dao.ContentDaoWrapper;
import com.gentics.mesh.core.data.dao.GroupDaoWrapper;
import com.gentics.mesh.core.data.dao.ImageVariantDaoWrapper;
import com.gentics.mesh.core.data.dao.JobDaoWrapper;
import com.gentics.mesh.core.data.dao.LanguageDaoWrapper;
import com.gentics.mesh.core.data.dao.MicroschemaDaoWrapper;
import com.gentics.mesh.core.data.dao.NodeDaoWrapper;
import com.gentics.mesh.core.data.dao.ProjectDaoWrapper;
import com.gentics.mesh.core.data.dao.RoleDaoWrapper;
import com.gentics.mesh.core.data.dao.S3BinaryDaoWrapper;
import com.gentics.mesh.core.data.dao.SchemaDaoWrapper;
import com.gentics.mesh.core.data.dao.TagDaoWrapper;
import com.gentics.mesh.core.data.dao.TagFamilyDaoWrapper;
import com.gentics.mesh.core.data.dao.UserDaoWrapper;
import com.syncleus.ferma.FramedTransactionalGraph;

/**
 * A GraphDB-specific extension to {@link Tx}
 * 
 * @author plyhun
 *
 */
public interface GraphDBTx extends CommonTx, BaseTransaction {

	/**
	 * Return the framed graph that is bound to the transaction.
	 *
	 * @return Graph which is bound to the transaction.
	 */
	FramedTransactionalGraph getGraph();

	/**
	 * Add new isolated vertex to the graph.
	 * 
	 * @param <T>
	 *            The type used to frame the element.
	 * @param kind
	 *            The kind of the vertex
	 * @return The framed vertex
	 * 
	 */
	default <T> T addVertex(Class<T> kind) {
		return getGraph().addFramedVertex(kind);
	}
	
	/**
	 * Return the current active graph. A transaction should be the only place where this threadlocal is updated.
	 * 
	 * @return Currently active transaction
	 */
	static GraphDBTx getGraphTx() {
		return (GraphDBTx) Tx.get();
	}

	@Override
	ImageVariantDaoWrapper imageVariantDao();

	@Override
	NodeDaoWrapper nodeDao();

	@Override
	UserDaoWrapper userDao();

	@Override
	GroupDaoWrapper groupDao();

	@Override
	RoleDaoWrapper roleDao();

	@Override
	ProjectDaoWrapper projectDao();

	@Override
	LanguageDaoWrapper languageDao();

	@Override
	JobDaoWrapper jobDao();

	@Override
	TagFamilyDaoWrapper tagFamilyDao();

	@Override
	TagDaoWrapper tagDao();

	@Override
	BranchDaoWrapper branchDao();

	@Override
	MicroschemaDaoWrapper microschemaDao();

	@Override
	SchemaDaoWrapper schemaDao();

	@Override
	ContentDaoWrapper contentDao();

	@Override
	BinaryDaoWrapper binaryDao();

	@Override
	S3BinaryDaoWrapper s3binaryDao();
}
