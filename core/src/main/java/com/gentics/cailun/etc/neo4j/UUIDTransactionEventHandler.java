package com.gentics.cailun.etc.neo4j;

import java.util.UUID;

import org.neo4j.graphdb.GraphDatabaseService;
import org.neo4j.graphdb.Node;
import org.neo4j.graphdb.PropertyContainer;
import org.neo4j.graphdb.Relationship;
import org.neo4j.graphdb.event.PropertyEntry;
import org.neo4j.graphdb.event.TransactionData;
import org.neo4j.graphdb.event.TransactionEventHandler;
import org.neo4j.graphdb.index.Index;
import org.neo4j.graphdb.index.IndexManager;
import org.neo4j.graphdb.index.RelationshipIndex;

import com.fasterxml.uuid.Generators;
import com.fasterxml.uuid.impl.RandomBasedGenerator;

public class UUIDTransactionEventHandler implements TransactionEventHandler {

	public static final String UUID_PROPERTY_NAME = "uuid";
	public static final String UUID_INDEX_NAME = "uuid";

	private static final RandomBasedGenerator UUID_GENERATOR = Generators.randomBasedGenerator();
	private final GraphDatabaseService graphDatabaseService;
	private Index<Node> nodeUuidIndex;
	private RelationshipIndex relationshipUuidIndex;

	public UUIDTransactionEventHandler(GraphDatabaseService graphDatabaseService) {
		this.graphDatabaseService = graphDatabaseService;
	}

	@Override
	public Object beforeCommit(TransactionData transactionData) throws Exception {

		checkForUuidDeletion(transactionData.removedNodeProperties(), transactionData);
		checkForUuidAssignment(transactionData.assignedNodeProperties());
		checkForUuidDeletion(transactionData.removedRelationshipProperties(), transactionData);
		checkForUuidAssignment(transactionData.assignedRelationshipProperties());

		initIndexes();
		populateUuidsFor(transactionData.createdNodes(), nodeUuidIndex);
		populateUuidsFor(transactionData.createdRelationships(), relationshipUuidIndex);

		return null;
	}

	private void initIndexes() {
		if (nodeUuidIndex == null) {
			IndexManager indexManager = graphDatabaseService.index();
			nodeUuidIndex = indexManager.forNodes(UUID_INDEX_NAME);
		}
		if (relationshipUuidIndex == null) {
			IndexManager indexManager = graphDatabaseService.index();
			relationshipUuidIndex = indexManager.forRelationships(UUID_INDEX_NAME);
		}
	}

	@Override
	public void afterCommit(TransactionData data, java.lang.Object state) {
	}

	@Override
	public void afterRollback(TransactionData data, java.lang.Object state) {
	}

	/**
	 * @param propertyContainers
	 *            set UUID property for a iterable on nodes or relationships
	 * @param index
	 *            index to be used
	 */
	private void populateUuidsFor(Iterable<? extends PropertyContainer> propertyContainers, Index index) {

		for (PropertyContainer propertyContainer : propertyContainers) {
			if (!propertyContainer.hasProperty(UUID_PROPERTY_NAME)) {

				final UUID uuid = UUID_GENERATOR.generate();
				final StringBuilder sb = new StringBuilder();
				sb.append(Long.toHexString(uuid.getMostSignificantBits())).append(Long.toHexString(uuid.getLeastSignificantBits()));
				String uuidAsString = sb.toString();

				propertyContainer.setProperty(UUID_PROPERTY_NAME, uuidAsString);
				// index.add(propertyContainer, UUID_PROPERTY_NAME, uuidAsString);
			}
		}
	}

	private void checkForUuidAssignment(Iterable<? extends PropertyEntry<? extends PropertyContainer>> changeList) {
		for (PropertyEntry<? extends PropertyContainer> changendPropertyEntry : changeList) {
			if (UUID_PROPERTY_NAME.equals(changendPropertyEntry.key())
					&& (!changendPropertyEntry.previouslyCommitedValue().equals(changendPropertyEntry.value()))) {
				throw new IllegalStateException("You are not allowed to assign " + UUID_PROPERTY_NAME + " properties");
			}
		}
	}

	private void checkForUuidDeletion(Iterable<? extends PropertyEntry<? extends PropertyContainer>> changeList, TransactionData transactionData) {
		for (PropertyEntry<? extends PropertyContainer> changendPropertyEntry : changeList) {
			if (UUID_PROPERTY_NAME.equals(changendPropertyEntry.key()) && (!isPropertyContainerDeleted(transactionData, changendPropertyEntry))) {
				throw new IllegalStateException("You are not allowed to remove " + UUID_PROPERTY_NAME + " properties.");
			}
		}
	}

	private boolean isPropertyContainerDeleted(TransactionData transactionData, PropertyEntry<? extends PropertyContainer> propertyEntry) {
		PropertyContainer entity = propertyEntry.entity();
		if (entity instanceof Node) {
			return transactionData.isDeleted((Node) entity);
		} else {
			return transactionData.isDeleted((Relationship) entity);
		}
	}

}
