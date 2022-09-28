:icons: font

////

* Write changelog entries in present tense
* Include GitHub issue or PR if possible using link:http://...[#123] format
* Review your changelog entries
* Don't include security sensitive information in the changelog
* Include links to documentation if possible ////

= Mesh Keycloak Plugin Changelog

[[v1.9.2]]
== 1.9.2 (2022-09-28)

CAUTION: The plugin requires Mesh >= 1.9.3 to run.

icon:check[] The dependency to Mesh has been updated to 1.9.3 and various possible connection leaks have been fixed.

[[v1.8.5]]
== 1.8.5 (2022-09-28)

CAUTION: The plugin requires Mesh >= 1.8.10 to run.

icon:check[] The dependency to Mesh has been updated to 1.8.10 and various possible connection leaks have been fixed.

[[v1.8.1]]
== 1.8.1 (2022-04-05)

icon:plus[] The initialization of the plugin will now fail if no public-keys are provided or can be loaded during startup.

[[v1.8.0]]
== 1.8.0 (2022-03-25)

icon:plus[] The dependency to Gentics Mesh has been updated to 1.8.1.

[[v1.7.1]]
== 1.7.1 (2022-04-05)

icon:plus[] The initialization of the plugin will now fail if no public-keys are provided or can be loaded during startup.

[[v1.7.0]]
== 1.7.0 (2021-03-18)

icon:plus[] It's now possible to define roles that are managed by the plugin

icon:plus[] The configuration can now be accessed in the mapper script via `config` variable

[[v1.6.26]]
== 1.6.26 (2022-09-28)

CAUTION: The plugin requires Mesh >= 1.6.34 to run.

icon:check[] The dependency to Mesh has been updated to 1.6.34 and various possible connection leaks have been fixed.

[[v1.6.18]]
== 1.6.18 (2022-02-01)

icon:plus[] The initialization of the plugin will now fail if no public-keys are provided or can be loaded during startup.

[[v1.6.4]]
== 1.6.4 (2021-03-18)

icon:plus[] It's now possible to define roles that are managed by the plugin

icon:plus[] The configuration can now be accessed in the mapper script via `config` variable

[[v1.5.12]]
== 1.5.12 (2021-03-18)

icon:plus[] It's now possible to define roles that are managed by the plugin

icon:plus[] The configuration can now be accessed in the mapper script via `config` variable

[[v1.4.19]]
== 1.4.19 (2021-04-12)

icon:plus[] The configuration to load the public keys can now be changed via the environment
variable `MESH_PLUGIN_KEYCLOAK_LOAD_PUBLIC_KEYS`

[[v1.4.18]]
== 1.4.18 (2021-03-18)

icon:plus[] It's now possible to define roles that are managed by the plugin

icon:plus[] The configuration can now be accessed in the mapper script via `config` variable
