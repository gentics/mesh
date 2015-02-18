package com.gentics.cailun.core.rest.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.rest.model.Tag;
import com.gentics.cailun.core.rest.model.generic.GenericFile;
import com.gentics.cailun.core.rest.service.generic.GenericTagServiceImpl;

@Component
@Transactional
public class TagServiceImpl extends GenericTagServiceImpl<Tag, GenericFile> implements TagService {

}
