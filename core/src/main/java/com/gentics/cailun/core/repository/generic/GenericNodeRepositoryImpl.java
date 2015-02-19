package com.gentics.cailun.core.repository.generic;

import com.gentics.cailun.core.repository.action.I18NActions;
import com.gentics.cailun.core.repository.action.UUIDCRUDActions;
import com.gentics.cailun.core.rest.model.generic.GenericNode;

public class GenericNodeRepositoryImpl<T extends GenericNode> implements I18NActions<T> , UUIDCRUDActions<T>{

	@Override
	public T findByUUID(String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public void delete(String uuid) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public T findByName(String project, String name) {
		// TODO Auto-generated method stub
		return null;
	}

	@Override
	public T findByUUID(String project, String uuid) {
		// TODO Auto-generated method stub
		return null;
	}

	

}
