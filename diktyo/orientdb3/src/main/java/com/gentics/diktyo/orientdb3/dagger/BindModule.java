package com.gentics.diktyo.orientdb3.dagger;

import com.gentics.diktyo.Diktyo;
import com.gentics.diktyo.db.Database;
import com.gentics.diktyo.index.Index;
import com.gentics.diktyo.index.IndexManager;
import com.gentics.diktyo.orientdb3.DiktyoImpl;
import com.gentics.diktyo.orientdb3.db.DatabaseImpl;
import com.gentics.diktyo.orientdb3.index.IndexImpl;
import com.gentics.diktyo.orientdb3.index.IndexManagerImpl;
import com.gentics.diktyo.orientdb3.server.ServerImpl;
import com.gentics.diktyo.orientdb3.server.ServerManagerImpl;
import com.gentics.diktyo.orientdb3.tx.NoTxImpl;
import com.gentics.diktyo.orientdb3.tx.TxImpl;
import com.gentics.diktyo.orientdb3.type.EdgeTypeImpl;
import com.gentics.diktyo.orientdb3.type.TypeManagerImpl;
import com.gentics.diktyo.orientdb3.type.VertexTypeImpl;
import com.gentics.diktyo.server.Server;
import com.gentics.diktyo.server.ServerManager;
import com.gentics.diktyo.tx.NoTx;
import com.gentics.diktyo.tx.Tx;
import com.gentics.diktyo.type.EdgeType;
import com.gentics.diktyo.type.TypeManager;
import com.gentics.diktyo.type.VertexType;

import dagger.Binds;
import dagger.Module;

@Module
public abstract class BindModule {

	@Binds
	public abstract Index bindIndex(IndexImpl index);

	@Binds
	public abstract IndexManager bindIndexManager(IndexManagerImpl indexManager);

	@Binds
	public abstract Diktyo bindDikto(DiktyoImpl diktyo);

	@Binds
	public abstract Database bindDatabase(DatabaseImpl db);

	@Binds
	public abstract TypeManager bindTypeManager(TypeManagerImpl tm);

	@Binds
	public abstract VertexType bindVertexType(VertexTypeImpl vertexType);

	@Binds
	public abstract EdgeType bindEdgeType(EdgeTypeImpl edgeType);

	@Binds
	public abstract Server bindServer(ServerImpl server);

	@Binds
	public abstract ServerManager bindServerManager(ServerManagerImpl serverManager);

	@Binds
	public abstract Tx bindTx(TxImpl tx);

	@Binds
	public abstract NoTx bindNoTx(NoTxImpl noTx);
}
