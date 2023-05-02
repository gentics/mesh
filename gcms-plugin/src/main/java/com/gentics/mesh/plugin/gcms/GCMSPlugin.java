package com.gentics.mesh.plugin.gcms;

import com.gentics.mesh.plugin.common.license.LicenseService;
import com.gentics.mesh.plugin.common.license.LicensedPlugin;
import java.io.File;
import java.io.FileInputStream;
import java.util.Collections;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.pf4j.PluginWrapper;

import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.user.UserUpdateRequest;
import com.gentics.mesh.plugin.AbstractCommercialPlugin;
import com.gentics.mesh.plugin.auth.AuthServicePlugin;
import com.gentics.mesh.plugin.auth.MappingResult;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.gcms.config.GCMSPluginConfig;

import io.reactivex.Completable;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;

/**
 * Implementation of {@link AuthServicePlugin} for mapping GCMS Users and groups onto Mesh Users, Groups and Roles
 */
public class GCMSPlugin extends AbstractCommercialPlugin<GCMSPluginConfig> implements AuthServicePlugin {
	private static final Logger log = LoggerFactory.getLogger(GCMSPlugin.class);

	/**
	 * Name of the groups claim
	 */
	protected final static String GROUPS_CLAIM = "gcms_groups";

	/**
	 * Public key
	 */
	protected JsonObject publicKey;

	/**
	 * Create instance
	 * @param wrapper wrapper
	 * @param env environment
	 */
	public GCMSPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Completable initialize() {
		return loadConfig(GCMSPluginConfig.class).map(config -> {
			config.overrideWithEnv();

			LicenseService licenseService = new LicenseService(config.getLicenseKey());
			licenseService.isLicenseSufficient(LicensedPlugin.GCMS);

			config.validate();
			return config;
		}).flatMapCompletable(config -> loadPublicKey(config));
	}

	@Override
	public MappingResult mapToken(HttpServerRequest req, String userUuid, JsonObject token) {
		GCMSPluginConfig config = getConfig();

		String issuer = token.getString("iss");
		// if the token was not issued by GCMS, we ignore it (make no mapping)
		if (!StringUtils.equalsIgnoreCase(config.getIssuer(), issuer)) {
			log.trace(String.format("Ignoring JWT, which was issued by %s and not %s", issuer, config.getIssuer()));
			return null;
		}

		log.trace("Map JWT coming from GCMS");

		MappingResult result = new MappingResult().setUser(new UserUpdateRequest()
				.setUsername(token.getString("preferred_username"))
				.setFirstname(token.getString("given_name"))
				.setLastname(token.getString("family_name"))
				.setEmailAddress(token.getString("email")));

		result.setGroups(
				token.getJsonArray(GROUPS_CLAIM).stream().map(group -> new GroupResponse().setName(group.toString())
						.setRoles(new RoleReference().setName(group.toString()))).collect(Collectors.toList()));
		result.setRoles(token.getJsonArray(GROUPS_CLAIM).stream()
				.map(group -> new RoleResponse().setName(group.toString())).collect(Collectors.toList()));
		result.setGroupFilter(group -> !token.getJsonArray(GROUPS_CLAIM).contains(group));
		return result;
	}

	@Override
	public Set<JsonObject> getPublicKeys() {
		log.trace(String.format("Public Key %s", publicKey));
		return Collections.singleton(publicKey);
	}

	/**
	 * Load public key from file
	 * @param config config
	 * @return completable
	 */
	protected Completable loadPublicKey(GCMSPluginConfig config) {
		return Completable.fromCallable(() -> {
			File publicKeyFile = new File(getPluginBaseDir(), config.getPublicKeyPath());
			log.trace("Checking public key file {}", publicKeyFile.getAbsolutePath());
			assert publicKeyFile.exists() : String.format("Public keys file %s not found", publicKeyFile.getAbsolutePath());
			try (FileInputStream in = new FileInputStream(publicKeyFile)) {
				publicKey = new JsonObject(IOUtils.toString(in));
			}
			return true;
		});
	}
}
