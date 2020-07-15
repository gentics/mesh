package com.gentics.mesh.changelog.highlevel.change;

import javax.inject.Inject;

import com.gentics.mesh.changelog.highlevel.AbstractHighLevelChange;
import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.Group;
import com.gentics.mesh.core.data.Role;
import com.gentics.mesh.core.data.User;

import dagger.Lazy;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

public class SetAdminUserFlag extends AbstractHighLevelChange {

	private static final Logger log = LoggerFactory.getLogger(SetAdminUserFlag.class);

	private final Lazy<BootstrapInitializer> boot;

	@Inject
	public SetAdminUserFlag(Lazy<BootstrapInitializer> boot) {
		this.boot = boot;
	}

	@Override
	public String getUuid() {
		return "21093BF213A244B484F3369A14AE7076";
	}

	@Override
	public String getName() {
		return "AdminUserFlagUpdater";
	}

	@Override
	public String getDescription() {
		return "This change assigns the admin user flag to all users which have access to the admin role";
	}

	@Override
	public void apply() {
		log.info("Applying change: " + getName());
		for (Role role : boot.get().roleRoot().findAll()) {
			for (Group group : role.getGroups()) {
				for (User user : group.getUsers()) {
					log.info("Setting admin flag for user " + user.getUsername());
					user.setAdmin(true);
				}
			}
		}
	}
}
