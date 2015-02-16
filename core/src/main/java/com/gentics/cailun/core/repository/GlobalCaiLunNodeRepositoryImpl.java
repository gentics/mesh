package com.gentics.cailun.core.repository;

import org.springframework.beans.factory.annotation.Autowired;

import com.gentics.cailun.core.repository.action.GlobalI18NActions;
import com.gentics.cailun.core.rest.model.CaiLunNode;

public class GlobalCaiLunNodeRepositoryImpl implements GlobalI18NActions<CaiLunNode> {
	
	@Autowired
	GlobalI18NValueRepository i18nValueRepository;

	
}
