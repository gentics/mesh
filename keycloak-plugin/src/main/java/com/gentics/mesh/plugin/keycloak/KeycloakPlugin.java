package com.gentics.mesh.plugin.keycloak;

import com.gentics.mesh.auth.util.KeycloakUtils;
import com.gentics.mesh.core.rest.group.GroupCreateRequest;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleCreateRequest;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.plugin.AbstractCommercialPlugin;
import com.gentics.mesh.plugin.auth.AuthServicePlugin;
import com.gentics.mesh.plugin.auth.MappingResult;
import com.gentics.mesh.plugin.common.MeshPluginException;
import com.gentics.mesh.plugin.common.license.LicenseService;
import com.gentics.mesh.plugin.common.license.LicensedPlugin;
import com.gentics.mesh.plugin.env.PluginEnvironment;
import com.gentics.mesh.plugin.keycloak.config.KeycloakPluginConfig;
import com.gentics.mesh.rest.client.MeshRestClient;
import groovy.lang.Binding;
import groovy.lang.GroovyShell;
import io.reactivex.Completable;
import io.reactivex.Observable;
import io.reactivex.Single;
import io.vertx.core.http.HttpServerRequest;
import io.vertx.core.json.JsonObject;
import io.vertx.core.logging.Logger;
import io.vertx.core.logging.LoggerFactory;
import io.vertx.reactivex.core.buffer.Buffer;
import io.vertx.reactivex.core.file.FileSystem;
import org.apache.commons.io.IOUtils;
import org.pf4j.PluginWrapper;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


public class KeycloakPlugin extends AbstractCommercialPlugin<KeycloakPluginConfig> implements AuthServicePlugin {

	protected static final String DEFAULT_SCRIPT_RESOURCE = "/defaultMapperScript.groovy";

	private static final String MAPPER_SCRIPT_NAME = "mapperScript.groovy";
	private static final Logger log = LoggerFactory.getLogger(KeycloakPlugin.class);

	private Set<JsonObject> publicKeys = Collections.emptySet();
	private String mapperScript;

	public KeycloakPlugin(PluginWrapper wrapper, PluginEnvironment env) {
		super(wrapper, env);
	}

	@Override
	public Set<JsonObject> getPublicKeys() {
		return publicKeys;
	}

	@Override
	public Completable initialize() {
		return loadConfig(KeycloakPluginConfig.class)
				.doOnSuccess(KeycloakPluginConfig::overrideWithEnv)
				.doOnSuccess(this::performLicenseCheck)
				.ignoreElement()
				// TODO Make loading of public keys non-blocking
				.andThen(Completable.defer(this::loadPublicKeys))
				.andThen(loadMapperScript())
				.andThen(initializeRolesAndGroups());
	}

	private void performLicenseCheck(KeycloakPluginConfig configuration) throws MeshPluginException {
		LicenseService licenseService = new LicenseService(configuration);
		licenseService.isLicenseSufficient(LicensedPlugin.MKC);
	}


	@Override
	public MappingResult mapToken(HttpServerRequest httpServerRequest, String uuid, JsonObject token) {
		return evaluateMapper(httpServerRequest, uuid, token);
	}

	@Override
	public boolean acceptToken(HttpServerRequest httpServerRequest, JsonObject token) {
		log.debug("Accepting token");
		printToken(token);
		return true;
	}

	public MappingResult evaluateMapper(HttpServerRequest httpServerRequest, String uuid, JsonObject token) {
		Binding binding = new Binding();

		binding.setVariable("request", httpServerRequest);
		binding.setVariable("uuid", uuid);
		binding.setVariable("token", token);
		binding.setVariable("log", log);
		binding.setVariable("config", getConfig());

		GroovyShell shell = new GroovyShell(binding);
		Object value = shell.evaluate(getMapperScript());

		if (value instanceof MappingResult) {
			return (MappingResult) value;
		} else {
			log.error("The mapper script did not return the expected {" + MappingResult.class.getName() + "}");
			throw new RuntimeException("Mapper script failed for {" + uuid + "}");
		}
	}

	public String getMapperScript() {
		return mapperScript;
	}

	private void printToken(JsonObject token) {
		if (log.isTraceEnabled()) {
			log.trace(
				"Token for user {}:\n{}",
				token.getString("preferred_username"),
				token.encodePrettily());
		}
	}

	/**
	 * Load the public keys for the configured realm from keycloak.
	 */
	private Completable loadPublicKeys() {
		KeycloakPluginConfig config = getConfig();

		publicKeys = Collections.emptySet();

		if (!config.isLoadPublicKeys()) {
			return Completable.complete();
		}

		try {
			URL url = new URL(config.getUrl());
			String proto = url.getProtocol();
			String host = url.getHost();
			int port = url.getPort();

			if (port == -1) {
				port = proto.equalsIgnoreCase("https") ? 443 : 80;
			}

			String realmName = config.getRealmName();
			Set<JsonObject> keys = KeycloakUtils.loadJWKs(proto, host, port, realmName);

			if (log.isDebugEnabled()) {
				for (JsonObject key : keys) {
					log.debug("Loaded public key\n{}\n", key.encodePrettily());
				}
			}

			publicKeys = keys;
		} catch (Exception e) {
			log.error("Error while loading public keys: {}", e.getMessage());

			return Completable.error(e);
		}

		return Completable.complete();
	}

	private Completable loadMapperScript() {
		File mapperScriptFile = new File(getStorageDir(), MAPPER_SCRIPT_NAME);
		FileSystem fs = rxVertx().fileSystem();

		return fs.rxReadFile(mapperScriptFile.getPath())
			.map(Buffer::toString)
			.onErrorResumeNext(this::loadDefaultMapperScript)
			.doOnSuccess(script -> mapperScript = script)
			.ignoreElement();
	}

	private Single<String> loadDefaultMapperScript(Throwable error) {
		log.error("Could not load mapper script: {}", error.getMessage());

		if (log.isTraceEnabled()) {
			log.trace(error);
		}

		log.debug("Using default mapper script");

		String script;

		try {
			script = IOUtils.toString(
				getClass().getResourceAsStream(DEFAULT_SCRIPT_RESOURCE),
					StandardCharsets.UTF_8);
		} catch (IOException e) {
			log.error("Could not load default mapper script: {}", e.getMessage());

			return Single.error(e);
		}

		return Single.just(script);
	}

	/**
	 * Creates and updates managed roles and their respective groups and assigns them together
	 *
	 * @return Completable when everything worked fine
	 */
	private Completable initializeRolesAndGroups() {
		return Completable.defer(() -> {

			if (getConfig().getManagedRoles() == null || getConfig().getManagedRoles().isEmpty()) {
				log.info("No roles and group initialisation, because there are none defined");
				return Completable.complete();
			}

			// We create or read out the roles and then create our groups based on them
			return initializeRoles()
					.toList()
					.flatMapCompletable(this::initializeRoleGroups);
		});

	}

	/**
	 * Creates the managed roles if they do not exist
	 * <p>
	 * Process:
	 * <ol>
	 * 		<li>Find all roles in mesh that are managed by us</li>
	 * 		<li>Create the ones that do not exist</li>
	 * </ol>
	 *
	 * @return Observable containing RoleResponses for all managed roles
	 */
	private Observable<RoleResponse> initializeRoles() {

		List<String> roles = getConfig().getManagedRoles();
		MeshRestClient client = adminClient();

		return client.findRoles()
				.toSingle()
				.flatMapObservable(meshRoles -> {
					List<RoleResponse> foundRoles = new ArrayList<>();
					List<String> notFoundRoles = new ArrayList<>();
					Map<String, RoleResponse> meshRolesMap = meshRoles.getData()
							.stream().collect(Collectors.toMap(RoleResponse::getName, RoleResponse -> RoleResponse));

					// Separating the roles into ones that already exists and those that need to be created
					for (String role : roles) {
						if (meshRolesMap.containsKey(role)) {
							foundRoles.add(meshRolesMap.get(role));
						} else {
							notFoundRoles.add(role);
						}
					}

					log.debug("Already existing roles '{}', and not existing '{}'", foundRoles, notFoundRoles);
					// Roles that were not found will be created and the found Roles are then merged with the created
					// roles
					return Observable.merge(Observable.fromIterable(foundRoles), Observable.fromIterable(notFoundRoles)
							.flatMapSingle(role -> {
								RoleCreateRequest request = new RoleCreateRequest();
								request.setName(role);

								log.info("Creating role '{}'", role);
								return client.createRole(request).toSingle();
							}));
				});
	}

	/**
	 * Creates the groups for the provided roles if they do not exist and assigns the roles to the specific group.
	 *
	 * Process:
	 * <ol>
	 *     <li>Find all group in mesh for groups that we manage</li>
	 *     <li>Create groups that do not exists in mesh</li>
	 *     <li>Created and existing groups are checked if they have a role assigned that has the same name</li>
	 *     <li>If not the creates/found roles is searched for a suitable role</li>
	 *     <li>The found role is assigned to the group. If no role is found (which should never happen) an error message is logged</li>
	 * </ol>
	 *
	 * @param roles List of all RoleResponses where the groups should be checked
	 * @return Completable indicating that everything is created assigned
	 */
	private Completable initializeRoleGroups(List<RoleResponse> roles) {

		List<String> roleNames = roles.stream().map(RoleResponse::getName).collect(Collectors.toList());
		MeshRestClient client = adminClient();

		return client.findGroups()
				.toSingle()
				.flatMapObservable(meshGroups -> {
					List<GroupResponse> foundGroups = new ArrayList<>();
					List<String> notFoundGroups = new ArrayList<>();
					Map<String, GroupResponse> meshRolesMap = meshGroups.getData()
							.stream().collect(Collectors.toMap(GroupResponse::getName, GroupResponse -> GroupResponse));

					// Separating the groups into ones that already exists and those that need to be created
					for (String role : roleNames) {
						if (meshRolesMap.containsKey(role)) {
							foundGroups.add(meshRolesMap.get(role));
						} else {
							notFoundGroups.add(role);
						}
					}

					log.debug("Already existing groups '{}', and not existing '{}'", foundGroups, notFoundGroups);
					// Groups that were not found will be created and the foundGroups is then merged with the created
					// groups
					return Observable.merge(Observable.fromIterable(foundGroups), Observable.fromIterable(notFoundGroups)
							.flatMapSingle(group -> {
								GroupCreateRequest request = new GroupCreateRequest();
								request.setName(group);

								log.info("Creating group '{}'", group);
								return client.createGroup(request).toSingle();
							}));
				})
				// We filter out the groups that do not have a role named like their group
				// A more thorough check would be to go over all groups and check if they only have the role assigned
				// they should have. Currently that has to be done via the mapper script
				.filter(group -> group.getRoles().stream().noneMatch(role -> role.getName().equals(group.getName())))
				.flatMapCompletable(group -> {
					Optional<RoleResponse> groupRoles = roles
							.stream()
							.filter(checkRole -> checkRole.getName().equals(group.getName()))
							.findFirst();

					if (groupRoles.isPresent()) {
						RoleResponse groupRole = groupRoles.get();

						log.info("Assigning role '{}'({}) to group '{}'({})", groupRole.getName(), groupRole.getUuid(), group.getName(), group.getUuid());

						return client.addRoleToGroup(group.getUuid(), groupRole.getUuid()).toCompletable();
					} else {
						// This should never happen. But if, then we want to know about it!
						log.error("Role doesn't exists for group '{}' ({})", group.getName(), group.getUuid());
						// This causes no error currently and the script just continues
						return Completable.complete();
					}
				});
	}
}
