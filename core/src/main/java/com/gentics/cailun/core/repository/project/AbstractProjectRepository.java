package com.gentics.cailun.core.repository.project;

import com.gentics.cailun.core.repository.FileRepository;
import com.gentics.cailun.core.rest.model.File;

public abstract class AbstractProjectRepository<T extends File> extends AbstractSecuredRepository<T> {


	abstract FileRepository<T> getRepository();

	@Override
	public T findByUUID(String uuid) {
		return (T) getRepository().findByUUID(uuid);
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
//		return getRepository().save( entity);
		return null;
	}

	@Override
	public long count() {
		// TODO make this query project aware
		return getRepository().count();
	}

}
