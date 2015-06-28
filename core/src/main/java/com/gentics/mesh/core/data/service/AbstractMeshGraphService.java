package com.gentics.mesh.core.data.service;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;

import com.syncleus.ferma.FramedThreadedTransactionalGraph;

public abstract class AbstractMeshGraphService<T> {

	@Autowired
	protected FramedThreadedTransactionalGraph fg;

	abstract public T findByUUID(String uuid);

	abstract public List<? extends T> findAll();

	protected T findByName(String name, Class<? extends T> clazz) {
		return fg.v().has("name", name).has(clazz).nextOrDefault(clazz, null);
	}

	protected T findByUUID(String uuid, Class<? extends T> clazz) {
		return fg.v().has(clazz).has("uuid", uuid).nextOrDefaultExplicit(clazz, null);
	}
}
