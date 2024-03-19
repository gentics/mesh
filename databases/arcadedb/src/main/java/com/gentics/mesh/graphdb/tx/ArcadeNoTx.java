package com.gentics.mesh.graphdb.tx;

import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.Callable;

import com.arcadedb.ContextConfiguration;
import com.arcadedb.database.Database;
import com.arcadedb.database.DocumentCallback;
import com.arcadedb.database.MutableDocument;
import com.arcadedb.database.RID;
import com.arcadedb.database.Record;
import com.arcadedb.database.RecordCallback;
import com.arcadedb.database.RecordEvents;
import com.arcadedb.database.async.DatabaseAsyncExecutor;
import com.arcadedb.database.async.ErrorCallback;
import com.arcadedb.database.async.OkCallback;
import com.arcadedb.engine.ComponentFile.MODE;
import com.arcadedb.engine.ErrorRecordCallback;
import com.arcadedb.engine.WALFile.FLUSH_TYPE;
import com.arcadedb.graph.Edge;
import com.arcadedb.graph.MutableVertex;
import com.arcadedb.graph.Vertex;
import com.arcadedb.index.IndexCursor;
import com.arcadedb.query.QueryEngine;
import com.arcadedb.query.select.Select;
import com.arcadedb.query.sql.executor.ResultSet;
import com.arcadedb.schema.Schema;

public class ArcadeNoTx implements Database {

	private final Database wrapped;

	public ArcadeNoTx(Database database) {
		this.wrapped = database;
	}

	public String getName() {
		return wrapped.getName();
	}

	public String getDatabasePath() {
		return wrapped.getDatabasePath();
	}

	public boolean isOpen() {
		return wrapped.isOpen();
	}

	public void close() {
		// Embedded database taken from the server are shared and therefore cannot be closed
		//wrapped.close();
	}

	public void drop() {
		wrapped.drop();
	}

	public MutableDocument newDocument(String typeName) {
		return wrapped.newDocument(typeName);
	}

	public ContextConfiguration getConfiguration() {
		return wrapped.getConfiguration();
	}

	public MODE getMode() {
		return wrapped.getMode();
	}

	public DatabaseAsyncExecutor async() {
		return wrapped.async();
	}

	public String getCurrentUserName() {
		return wrapped.getCurrentUserName();
	}

	public MutableVertex newVertex(String typeName) {
		return wrapped.newVertex(typeName);
	}

	public Select select() {
		return wrapped.select();
	}

	public ResultSet command(String language, String query, ContextConfiguration configuration,
			Map<String, Object> args) {
		return wrapped.command(language, query, configuration, args);
	}

	public boolean isTransactionActive() {
		return wrapped.isTransactionActive();
	}

	public int getNestedTransactions() {
		return wrapped.getNestedTransactions();
	}

	public void transaction(TransactionScope txBlock) {
		wrapped.transaction(txBlock);
	}

	public ResultSet execute(String language, String script, Map<String, Object> args) {
		return wrapped.execute(language, script, args);
	}

	public boolean isAutoTransaction() {
		return wrapped.isAutoTransaction();
	}

	public boolean transaction(TransactionScope txBlock, boolean joinCurrentTx) {
		return wrapped.transaction(txBlock, joinCurrentTx);
	}

	public void setAutoTransaction(boolean autoTransaction) {
		wrapped.setAutoTransaction(autoTransaction);
	}

	public void rollbackAllNested() {
		wrapped.rollbackAllNested();
	}

	public void scanType(String typeName, boolean polymorphic, DocumentCallback callback) {
		wrapped.scanType(typeName, polymorphic, callback);
	}

	public boolean transaction(TransactionScope txBlock, boolean joinCurrentTx, int retries) {
		return wrapped.transaction(txBlock, joinCurrentTx, retries);
	}

	public void scanType(String typeName, boolean polymorphic, DocumentCallback callback,
			ErrorRecordCallback errorRecordCallback) {
		wrapped.scanType(typeName, polymorphic, callback, errorRecordCallback);
	}

	public boolean transaction(TransactionScope txBlock, boolean joinCurrentTx, int attempts, OkCallback ok,
			ErrorCallback error) {
		return wrapped.transaction(txBlock, joinCurrentTx, attempts, ok, error);
	}

	public void scanBucket(String bucketName, RecordCallback callback) {
		wrapped.scanBucket(bucketName, callback);
	}

	public void scanBucket(String bucketName, RecordCallback callback, ErrorRecordCallback errorRecordCallback) {
		wrapped.scanBucket(bucketName, callback, errorRecordCallback);
	}

	public void begin() {
		wrapped.begin();
	}

	public void begin(TRANSACTION_ISOLATION_LEVEL isolationLevel) {
		wrapped.begin(isolationLevel);
	}

	public IndexCursor lookupByKey(String type, String keyName, Object keyValue) {
		return wrapped.lookupByKey(type, keyName, keyValue);
	}

	public void commit() {
		wrapped.commit();
	}

	public void rollback() {
		wrapped.rollback();
	}

	public IndexCursor lookupByKey(String type, String[] keyNames, Object[] keyValues) {
		return wrapped.lookupByKey(type, keyNames, keyValues);
	}

	public Record lookupByRID(RID rid, boolean loadContent) {
		return wrapped.lookupByRID(rid, loadContent);
	}

	public Edge newEdgeByKeys(String sourceVertexType, String[] sourceVertexKeyNames, Object[] sourceVertexKeyValues,
			String destinationVertexType, String[] destinationVertexKeyNames, Object[] destinationVertexKeyValues,
			boolean createVertexIfNotExist, String edgeType, boolean bidirectional, Object... properties) {
		return wrapped.newEdgeByKeys(sourceVertexType, sourceVertexKeyNames, sourceVertexKeyValues,
				destinationVertexType, destinationVertexKeyNames, destinationVertexKeyValues, createVertexIfNotExist,
				edgeType, bidirectional, properties);
	}

	public boolean existsRecord(RID rid) {
		return wrapped.existsRecord(rid);
	}

	public void deleteRecord(Record record) {
		wrapped.deleteRecord(record);
	}

	public Iterator<Record> iterateType(String typeName, boolean polymorphic) {
		return wrapped.iterateType(typeName, polymorphic);
	}

	public Iterator<Record> iterateBucket(String bucketName) {
		return wrapped.iterateBucket(bucketName);
	}

	public ResultSet command(String language, String query, Map<String, Object> args) {
		return wrapped.command(language, query, args);
	}

	public ResultSet command(String language, String query, ContextConfiguration configuration, Object... args) {
		return wrapped.command(language, query, configuration, args);
	}

	public Edge newEdgeByKeys(Vertex sourceVertex, String destinationVertexType, String[] destinationVertexKeyNames,
			Object[] destinationVertexKeyValues, boolean createVertexIfNotExist, String edgeType, boolean bidirectional,
			Object... properties) {
		return wrapped.newEdgeByKeys(sourceVertex, destinationVertexType, destinationVertexKeyNames,
				destinationVertexKeyValues, createVertexIfNotExist, edgeType, bidirectional, properties);
	}

	public ResultSet command(String language, String query, Object... args) {
		return wrapped.command(language, query, args);
	}

	public QueryEngine getQueryEngine(String language) {
		return wrapped.getQueryEngine(language);
	}

	public ResultSet query(String language, String query, Object... args) {
		return wrapped.query(language, query, args);
	}

	public Schema getSchema() {
		return wrapped.getSchema();
	}

	public RecordEvents getEvents() {
		return wrapped.getEvents();
	}

	public <RET> RET executeInReadLock(Callable<RET> callable) {
		return wrapped.executeInReadLock(callable);
	}

	public <RET> RET executeInWriteLock(Callable<RET> callable) {
		return wrapped.executeInWriteLock(callable);
	}

	public boolean isReadYourWrites() {
		return wrapped.isReadYourWrites();
	}

	public ResultSet query(String language, String query, Map<String, Object> args) {
		return wrapped.query(language, query, args);
	}

	public Database setReadYourWrites(boolean value) {
		return wrapped.setReadYourWrites(value);
	}

	public Database setTransactionIsolationLevel(TRANSACTION_ISOLATION_LEVEL level) {
		return wrapped.setTransactionIsolationLevel(level);
	}

	public ResultSet execute(String language, String script, Object... args) {
		return wrapped.execute(language, script, args);
	}

	public long countType(String typeName, boolean polymorphic) {
		return wrapped.countType(typeName, polymorphic);
	}

	public TRANSACTION_ISOLATION_LEVEL getTransactionIsolationLevel() {
		return wrapped.getTransactionIsolationLevel();
	}

	public int getEdgeListSize() {
		return wrapped.getEdgeListSize();
	}

	public long countBucket(String bucketName) {
		return wrapped.countBucket(bucketName);
	}

	public Database setEdgeListSize(int size) {
		return wrapped.setEdgeListSize(size);
	}

	public Map<String, Object> getStats() {
		return wrapped.getStats();
	}

	public Database setUseWAL(boolean useWAL) {
		return wrapped.setUseWAL(useWAL);
	}

	public Database setWALFlush(FLUSH_TYPE flush) {
		return wrapped.setWALFlush(flush);
	}

	public boolean isAsyncFlush() {
		return wrapped.isAsyncFlush();
	}

	public Database setAsyncFlush(boolean value) {
		return wrapped.setAsyncFlush(value);
	}
}
