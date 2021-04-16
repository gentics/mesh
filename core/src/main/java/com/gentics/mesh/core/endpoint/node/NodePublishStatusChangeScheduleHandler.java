package com.gentics.mesh.core.endpoint.node;

import java.time.ZonedDateTime;
import java.util.Optional;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.apache.commons.lang.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.context.impl.NodePublishStatusChangeContextImpl;
import com.gentics.mesh.core.data.Project;
import com.gentics.mesh.core.data.job.Job;
import com.gentics.mesh.core.data.node.Node;
import com.gentics.mesh.core.project.maintenance.ProjectVersionPurgeHandler;
import com.gentics.mesh.core.rest.job.JobStatus;
import com.gentics.mesh.core.verticle.handler.HandlerUtilities;
import com.gentics.mesh.graphdb.spi.Database;

import io.reactivex.Completable;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Singleton
public class NodePublishStatusChangeScheduleHandler {

	private static final Logger log = LoggerFactory.getLogger(ProjectVersionPurgeHandler.class);

	private final Database db;
	private final BootstrapInitializer boot;
	private final HandlerUtilities utils;
	
	@Inject
	public NodePublishStatusChangeScheduleHandler(Database db, HandlerUtilities utils, BootstrapInitializer boot) {
		this.db = db;
		this.boot = boot;
		this.utils = utils;
    }
	
	// TODO document
	public Completable schedulePublishStatusChange(Project project, Node node, Optional<String> maybeLanguageTag, ZonedDateTime fireAt,
			boolean publish, Optional<Job> maybeJob) {
		
		String jobDescription = logMessageFromJobDescription(project, node, maybeLanguageTag, fireAt, publish, maybeJob);
		log.debug("Scheduling job for " + jobDescription);

		maybeJob.ifPresent(job -> db.tx(() -> job.setStatus(JobStatus.RUNNING)));
		return Completable.fromAction(() -> {
			db.tx(tx -> {
				ZonedDateTime now = ZonedDateTime.now();
				
				// TODO cluster: optionally check the published status + version of the currently stored node, to avoid double publish. 
				
				if (fireAt.isAfter(now)) {
					boot.vertx().setTimer(fireAt.toInstant().toEpochMilli() - now.toInstant().toEpochMilli(), l -> {
						db.tx(() -> utils.bulkableAction(bac -> {
							log.debug("Triggering job " + jobDescription);
							NodePublishStatusChangeContextImpl iac = new NodePublishStatusChangeContextImpl();
							if (maybeLanguageTag.isPresent()) {
								if (publish) {
									node.publish(iac, bac, maybeLanguageTag.get());
								} else {
									node.takeOffline(iac, bac, null, maybeLanguageTag.get());
								}
							} else {
								if (publish) {
									node.publish(iac, bac);
								} else {
									node.takeOffline(iac, bac);
								}
							}
							maybeJob.ifPresent(job -> job.setStatus(JobStatus.COMPLETED));
							log.info("Job done: " + jobDescription);
						}));
					});
				} else {
					log.error("Job is outdated: " + jobDescription);
					maybeJob.ifPresent(job -> {
						job.setStatus(JobStatus.FAILED);
						job.delete();
					});
					// TODO proper error
					
				}
			});
		});
	}
	
	protected String logMessageFromJobDescription(Project project, Node node, Optional<String> maybeLanguageTag, ZonedDateTime fireAt,
			boolean publish, Optional<Job> maybeJob) {
		return db.tx(() -> "project:{" + project.getName() + "} node:{" + node.getUuid() + "}, "
				+ (publish ? StringUtils.EMPTY : "un") + "publish "
				+ (maybeLanguageTag.isPresent() ? maybeLanguageTag.get() : StringUtils.EMPTY));
	}
}
