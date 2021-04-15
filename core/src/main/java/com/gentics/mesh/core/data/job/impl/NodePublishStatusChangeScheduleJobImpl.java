package com.gentics.mesh.core.data.job.impl;

import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_NODE;
import static com.gentics.mesh.core.data.relationship.GraphRelationships.HAS_PROJECT;

import java.time.ZonedDateTime;
import java.util.Optional;

import com.gentics.madl.index.IndexHandler;
import com.gentics.madl.type.TypeHandler;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.generic.MeshVertexImpl;
import com.gentics.mesh.core.data.impl.ProjectImpl;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.data.node.impl.NodeImpl;
import com.gentics.mesh.core.endpoint.node.NodePublishStatusChangeScheduleHandler;
import com.gentics.mesh.util.DateUtils;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class NodePublishStatusChangeScheduleJobImpl extends JobImpl {
	
	private static final Logger log = LoggerFactory.getLogger(NodePublishStatusChangeScheduleJobImpl.class);

	private static final String FIRE_AT_PROPERTY = "fireAt";
	
	private static final String LANG_PROPERTY = "lang";
	
	private static final String PUBLISH_PROPERTY = "publish";

	public static void init(TypeHandler type, IndexHandler index) {
		type.createVertexType(NodePublishStatusChangeScheduleJobImpl.class, MeshVertexImpl.class);
	}

	public Node getNode() {
		return out(HAS_NODE, NodeImpl.class).nextOrNull();
	}

	public void setNode(Node node) {
		setSingleLinkOutTo(node, HAS_NODE);
	}
	
	public Project getProject() {
		return out(HAS_PROJECT, ProjectImpl.class).nextOrNull();
	}

	public void setProject(Project project) {
		setSingleLinkOutTo(project, HAS_PROJECT);
	}
	
	public Optional<String> getLanguage() {
		String lang = getProperty(LANG_PROPERTY);
		return Optional.ofNullable(lang);
	}

	public void setLanguage(Optional<String> lang) {
		if (lang.isPresent()) {
			setProperty(LANG_PROPERTY, lang.get());
		} else {
			removeProperty(LANG_PROPERTY);
		}
	}
	
	public ZonedDateTime getFireAt() {
		Long fireAt = getProperty(FIRE_AT_PROPERTY);
		return Optional.ofNullable(fireAt).map(DateUtils::toZonedDateTime).orElse(null);
	}

	public void setFireAt(ZonedDateTime time) {
		if (time != null) {
			Long ms = time.toInstant().toEpochMilli();
			setProperty(FIRE_AT_PROPERTY, ms);
		} else {
			removeProperty(FIRE_AT_PROPERTY);
		}
	}
	
	public boolean isPublish() {
		return getProperty(PUBLISH_PROPERTY);
	}

	public void setPublish(boolean publish) {
		setProperty(PUBLISH_PROPERTY, publish);
	}

	@Override
	protected Completable processTask() {
		// TODO publish events?
		NodePublishStatusChangeScheduleHandler handler = mesh().nodePublishStatusChangeScheduleHandler();
		return handler.purgeVersions(getProject(), getNode(), getLanguage(), getFireAt(), isPublish(), Optional.of(this));		
	}	

}
