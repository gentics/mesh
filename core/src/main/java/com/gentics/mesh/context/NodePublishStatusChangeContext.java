package com.gentics.mesh.context;

import java.time.ZonedDateTime;
import java.util.Optional;

import com.gentics.mesh.core.data.node.Node;

public interface NodePublishStatusChangeContext {

	public Node getNode();
	
	public Optional<String> getLanguage();
	
	public ZonedDateTime getFireAt();
	
	public boolean isPublish();
}
