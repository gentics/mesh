package com.gentics.mesh.core.verticle.node;

import java.lang.management.ManagementFactory;

import javax.management.MBeanServer;
import javax.management.ObjectName;

import org.jacpfx.vertx.spring.SpringVerticle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.gentics.mesh.core.AbstractSpringVerticle;
import com.gentics.mesh.core.data.node.handler.NodeMigrationHandler;

import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

@Component
@Scope("singleton")
@SpringVerticle
public class NodeMigrationVerticle extends AbstractSpringVerticle {
	public final static String JMX_MBEAN_NAME = "com.gentics.mesh:type=NodeMigration";

	private static Logger log = LoggerFactory.getLogger(NodeMigrationVerticle.class);

	@Autowired
	protected NodeMigrationHandler nodeMigrationHandler;

	public final static String MIGRATION_ADDRESS = NodeMigrationVerticle.class.getName() + ".migrate";

	public final static String SCHEMA_UUID_HEADER = "schemaUuid";

	@Override
	public void start() throws Exception {
		if (log.isDebugEnabled()) {
			log.debug("Starting " + getClass().getName());
		}

		vertx.eventBus().consumer(MIGRATION_ADDRESS, (message) -> {
			String schemaUuid = message.headers().get(SCHEMA_UUID_HEADER);
			if (log.isDebugEnabled()) {
				log.debug("Node Migration for schema " + schemaUuid + " was requested");
			}

			MBeanServer mbs = ManagementFactory.getPlatformMBeanServer();
			try {
				ObjectName statusMBeanName = new ObjectName(JMX_MBEAN_NAME + ",name=" + schemaUuid);
				// TODO when mesh is running in a cluster, this check is not enough, since JMX beans are bound to the JVM
				if (mbs.isRegistered(statusMBeanName)) {
					message.fail(0, "Migration for schema " + schemaUuid + " is already running");
				} else {
					db.noTrx(() -> boot.schemaContainerRoot().findByUuid(schemaUuid)).subscribe(schemaContainer -> {
						NodeMigrationStatus statusBean = db.noTrx(() -> {
							return new NodeMigrationStatus(schemaContainer.getName(), schemaContainer.getVersion());
						});
						try {
							mbs.registerMBean(statusBean, statusMBeanName);
						} catch (Exception e1) {
						}
						nodeMigrationHandler.migrateNodes(schemaContainer, statusBean);
					} , (e) -> message.fail(0, e.getLocalizedMessage()), () -> {
						try {
							mbs.unregisterMBean(statusMBeanName);
						} catch (Exception e1) {
						}
						message.reply(null);
					});
				}
			} catch (Exception e2) {
				message.fail(0, "Migration for schema " + schemaUuid + " failed: " + e2.getLocalizedMessage());
			}
		});
	}
}
