package com.gentics.cailun.core.repository.project;

import com.gentics.cailun.core.repository.UUIDGraphRepository;
import com.gentics.cailun.core.rest.model.AbstractPersistable;

public abstract class AbstractProjectRepository<T extends AbstractPersistable> extends AbstractSecuredRepository<T> {

	public abstract UUIDGraphRepository<T> getRepository();

	@Override
	public T findByUUID(String uuid) {
		return getRepository().findByUUID(uuid);
	}

	@Override
	public void delete(T entity) {
		getRepository().delete(entity);
	}

	@Override
	public void delete(String uuid) {
		getRepository().delete(uuid);
	}

	@Override
	public T save(T entity) {
		return getRepository().save(entity);
	}

	@Override
	public long count() {
		// TODO make this query project aware
		return getRepository().count();
	}

}
