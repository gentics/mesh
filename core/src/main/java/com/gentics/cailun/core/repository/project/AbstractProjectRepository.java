package com.gentics.cailun.core.repository.project;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.repository.FileRepository;
import com.gentics.cailun.core.rest.model.File;

public abstract class AbstractProjectRepository<T extends File> extends AbstractSecuredRepository<T> {

	@Autowired
	FileRepository<T> contentRepository;

	@Override
	public T findByUUID(String uuid) {
		return (T) contentRepository.findByUUID(uuid);
	}

	@Override
	public void delete(T entity) {
//		contentRepository.delete(entity);
	}

	@Override
	public void delete(String uuid) {
		contentRepository.delete(uuid);
	}

	@Override
	public T save(T entity) {
		// return getRepository().save( entity);
		return null;
	}

	@Override
	public long count() {
		// TODO make this query project aware
		return contentRepository.count();
	}

}
