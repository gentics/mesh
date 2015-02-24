package com.gentics.cailun.core.data.service;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import com.gentics.cailun.core.data.model.Tag;
import com.gentics.cailun.core.data.model.generic.GenericFile;
import com.gentics.cailun.core.data.service.generic.GenericTagServiceImpl;

@Component
@Transactional
public class TagServiceImpl extends GenericTagServiceImpl<Tag, GenericFile> implements TagService {

}
