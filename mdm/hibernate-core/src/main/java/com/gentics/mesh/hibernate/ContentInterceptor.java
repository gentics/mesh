package com.gentics.mesh.hibernate;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.hibernate.Interceptor;
import org.hibernate.Transaction;

import com.gentics.mesh.contentoperation.ContentStorage;
import com.gentics.mesh.context.InternalActionContext;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.database.HibernateTx;
import com.gentics.mesh.hibernate.data.domain.HibMicronodeContainerImpl;
import com.gentics.mesh.hibernate.data.domain.HibNodeFieldContainerImpl;
import com.gentics.mesh.hibernate.data.loader.DataLoaders;

import jakarta.persistence.EntityTransaction;

/**
 * {@link Interceptor} implementation which will persist instances of
 * {@link HibNodeFieldContainerImpl} and {@link HibMicronodeContainerImpl} when
 * a {@link Transaction} is completed.
 */
public class ContentInterceptor implements Interceptor, Serializable {
	/**
	 * Map of transaction runnables per transaction.
	 * All runnables will be executed before the transaction is completed.
	 */
	protected Map<EntityTransaction, List<Runnable>> transactionRunnables = Collections
			.synchronizedMap(new HashMap<>());

	/**
	 * Map of containers to be persisted per transaction
	 */
	protected Map<EntityTransaction, Map<UUID, HibNodeFieldContainerImpl>> containers = Collections
			.synchronizedMap(new HashMap<>());

	/**
	 * Map of micronodes to be persisted per transaction
	 */
	protected Map<EntityTransaction, Map<UUID, HibMicronodeContainerImpl>> micronodes = Collections
			.synchronizedMap(new HashMap<>());

	/**
	 * Map of data loaders per transaction
	 */
	protected Map<EntityTransaction, DataLoaders> dataLoaders = Collections.
			synchronizedMap(new HashMap<>());

	/**
	 * Serial Version UUId
	 */
	private static final long serialVersionUID = 1706697856003083045L;

	@Override
	public void beforeTransactionCompletion(Transaction tx) {
		// execute all runnables, which will persist all containers and micronodes
		transactionRunnables.getOrDefault(tx, Collections.emptyList()).forEach(Runnable::run);
	}

	@Override
	public void afterTransactionCompletion(Transaction tx) {
		// clear all runnables, containers, micronodes and data loaders for the transaction
		transactionRunnables.remove(tx);
		containers.remove(tx);
		micronodes.remove(tx);
		dataLoaders.remove(tx);
	}

	/**
	 * Persist the container. Actual persisting will be delayed until transaction completion
	 * @param container container to be persisted
	 */
	public void persist(HibNodeFieldContainerImpl container) {
		HibernateTx tx = HibernateTx.get();
		EntityTransaction entityTx = tx.entityTransaction();
		ContentStorage contentStorage = tx.data().getContentStorage();

		// store the container, so that it can be retrieved again before the transaction is completed
		containers.computeIfAbsent(entityTx, key -> Collections.synchronizedMap(new HashMap<>()))
				.put(container.getDbUuid(), container);

		// add a transaction runnable that will insert the container into the content table
		AtomicBoolean persisted = new AtomicBoolean(false);
		transactionRunnables.computeIfAbsent(entityTx, key -> new ArrayList<>()).add(() -> {
			if (!persisted.getAndSet(true)) {
				contentStorage.insert(container, container.getSchemaContainerVersion());
			}
		});
	}

	/**
	 * Persist the micronode. Actual persisting will be delayed until transaction completion
	 * @param container micronode to be persisted
	 */
	public void persist(HibMicronodeContainerImpl container) {
		HibernateTx tx = HibernateTx.get();
		EntityTransaction entityTx = tx.entityTransaction();
		ContentStorage contentStorage = tx.data().getContentStorage();

		// store the micronode, so that it can be retrieved again before the transaction is completed
		micronodes.computeIfAbsent(entityTx, k -> Collections.synchronizedMap(new HashMap<>())).put(container.getDbUuid(), container);

		// add a transaction runnable that will insert the micronode into the content table
		AtomicBoolean persisted = new AtomicBoolean(false);
		transactionRunnables.computeIfAbsent(entityTx, k -> new ArrayList<>()).add(() -> {
			if (!persisted.getAndSet(true)) {
				contentStorage.insert(container, container.getSchemaContainerVersion());
			}
		});
	}

	/**
	 * Get the container with given UUID from the internal storage, if available
	 * @param containerUUID container UUID
	 * @return optional container
	 */
	public Optional<HibNodeFieldContainerImpl> get(UUID containerUUID) {
		Map<UUID, HibNodeFieldContainerImpl> containerMap = containers
				.getOrDefault(HibernateTx.get().entityTransaction(), Collections.emptyMap());
		if (containerMap.containsKey(containerUUID)) {
			return Optional.of(containerMap.get(containerUUID));
		} else {
			return Optional.empty();
		}
	}

	/**
	 * @return the ids of the field containers stored in the internal storage
	 */
	public List<UUID> getFieldContainersUuids() {
		Map<UUID, HibNodeFieldContainerImpl> containerMap = containers
				.getOrDefault(HibernateTx.get().entityTransaction(), Collections.emptyMap());

		return containerMap.values().stream().map(HibNodeFieldContainerImpl::getDbUuid).collect(Collectors.toList());
	}

	/**
	 * @return the ids of the micro field containers stored in the internal storage
	 */
	public List<UUID> getMicronodeUuids() {
		Map<UUID, HibMicronodeContainerImpl> containerMap = micronodes
				.getOrDefault(HibernateTx.get().entityTransaction(), Collections.emptyMap());

		return containerMap.values().stream().map(HibMicronodeContainerImpl::getDbUuid).collect(Collectors.toList());
	}

	/**
	 * Get the micronode with given UUID from the internal storage, if available
	 * @param containerUUID micronode UUID
	 * @return optional micronode
	 */
	public Optional<HibMicronodeContainerImpl> getMicronode(UUID containerUUID) {
		Map<UUID, HibMicronodeContainerImpl> containerMap = micronodes
				.getOrDefault(HibernateTx.get().entityTransaction(), Collections.emptyMap());
		if (containerMap.containsKey(containerUUID)) {
			return Optional.of(containerMap.get(containerUUID));
		} else {
			return Optional.empty();
		}
	}

	/**
	 * Initialize {@link DataLoaders} for the current transaction
	 * @param nodes a collection of nodes
	 * @param ac the action context
	 * @param dataloaders the loaders to initialize
	 */
	public DataLoaders initializeDataLoaders(Collection<? extends Node> nodes, InternalActionContext ac, List<DataLoaders.Loader> dataloaders) {
		HibernateTx tx = HibernateTx.get();
		EntityTransaction entityTx = tx.entityTransaction();
		// TODO check if dataloaders wrapper already exists, and if yes, merge
		DataLoaders dataLoader = new DataLoaders(nodes, ac, dataloaders);
		dataLoaders.put(entityTx, dataLoader);
		return dataLoader;
	}

	/**
	 * @return an optional {@link DataLoaders} object
	 */
	public Optional<DataLoaders> getDataLoaders() {
		HibernateTx tx = HibernateTx.get();
		EntityTransaction entityTx = tx.entityTransaction();
		DataLoaders dataLoader = dataLoaders.get(entityTx);

		return Optional.ofNullable(dataLoader);
	}
}
