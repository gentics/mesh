package com.gentics.mesh.auth;

import static com.gentics.mesh.test.TestSize.PROJECT;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.apache.commons.collections4.CollectionUtils;
import org.assertj.core.util.Objects;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import org.junit.runners.Parameterized.Parameter;
import org.junit.runners.Parameterized.Parameters;

import com.gentics.mesh.auth.oauth2.CannotWriteException;
import com.gentics.mesh.auth.oauth2.MeshOAuth2ServiceImpl;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.role.HibRole;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.TxAction2;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.core.rest.event.MeshEventModel;
import com.gentics.mesh.core.rest.event.group.GroupRoleAssignModel;
import com.gentics.mesh.core.rest.event.group.GroupUserAssignModel;
import com.gentics.mesh.core.rest.event.impl.MeshElementEventModelImpl;
import com.gentics.mesh.core.rest.group.GroupReference;
import com.gentics.mesh.core.rest.group.GroupResponse;
import com.gentics.mesh.core.rest.role.RoleReference;
import com.gentics.mesh.core.rest.role.RoleResponse;
import com.gentics.mesh.core.rest.user.UserReference;
import com.gentics.mesh.distributed.RequestDelegator;
import com.gentics.mesh.event.EventQueueBatch;
import com.gentics.mesh.plugin.auth.MappingResult;
import com.gentics.mesh.test.MeshTestSetting;
import com.gentics.mesh.test.context.AbstractMeshTest;

/**
 * Test cases for mapping groups and roles with the {@link MeshOAuthService}
 */
@RunWith(Parameterized.class)
@MeshTestSetting(testSize = PROJECT, startServer = false)
public class TokenMappingTest extends AbstractMeshTest {
	/**
	 * Name of the test user
	 */
	public final static String TESTUSER_NAME = "testuser";

	/**
	 * Test parameters
	 * @return parameters
	 */
	@Parameters(name = "writable: {0}")
	public static Collection<Object[]> paramData() {
		Collection<Object[]> data = new ArrayList<>();
		for (boolean writable : Arrays.asList(true, false)) {
			data.add(new Object[] { writable });
		}
		return data;
	}

	/**
	 * Comparator for events
	 */
	private final static Comparator<? super MeshEventModel> EVENT_COMPARATOR = (e1, e2) -> {
		if (Objects.areEqual(e1, e2)) {
			return 0;
		}
		if (e1 == null && e2 != null) {
			return -1;
		}
		if (e1 != null && e2 == null) {
			return -1;
		}

		if (!Objects.areEqual(e1.getEvent(), e2.getEvent())) {
			return -1;
		}
		if (!Objects.areEqual(e1.getClass(), e2.getClass())) {
			return -1;
		}
		if (e1 instanceof MeshElementEventModel) {
			return Objects.areEqual(MeshElementEventModel.class.cast(e1).getName(),
					MeshElementEventModel.class.cast(e2).getName()) ? 0 : -1;
		} else if (e1 instanceof GroupUserAssignModel) {
			GroupUserAssignModel g1 = GroupUserAssignModel.class.cast(e1);
			GroupUserAssignModel g2 = GroupUserAssignModel.class.cast(e2);

			if (Objects.areEqual(g1.getUser().getFirstName(), g2.getUser().getFirstName())
					&& Objects.areEqual(g1.getUser().getLastName(), g2.getUser().getLastName())
					&& Objects.areEqual(g1.getGroup().getName(), g2.getGroup().getName())) {
				return 0;
			} else {
				return -1;
			}
		} else if (e1 instanceof GroupRoleAssignModel) {
			GroupRoleAssignModel g1 = GroupRoleAssignModel.class.cast(e1);
			GroupRoleAssignModel g2 = GroupRoleAssignModel.class.cast(e2);

			if (Objects.areEqual(g1.getRole().getName(), g2.getRole().getName())
					&& Objects.areEqual(g1.getGroup().getName(), g2.getGroup().getName())) {
				return 0;
			} else {
				return -1;
			}
		}
		return -1;
	};

	/**
	 * Whether the instance is assumed to be writable
	 */
	@Parameter(0)
	public boolean writable;

	/**
	 * Tested service instance
	 */
	private MeshOAuth2ServiceImpl service;

	/**
	 * Setup basic data and the {@link #service}
	 */
	@Before
	public void setup() {
		tx(tx -> {
			HibUser admin = tx.userDao().findByName("admin");
			assertThat(admin).as("Admin User").isNotNull();
			HibUser testUser = tx.userDao().create(TESTUSER_NAME, admin);
			testUser.setFirstname(TESTUSER_NAME);
			testUser.setLastname(TESTUSER_NAME);
			assertThat(testUser).as("Test User").isNotNull();
		});

		service = tx(tx -> {
			mockActionContext();
			RequestDelegator delegator = mock(RequestDelegator.class);
			when(delegator.canWrite()).thenReturn(writable);
			return new MeshOAuth2ServiceImpl(db(), options(), null, null, null, null, delegator, tx.data().permissionRoots());
		});
	}

	/**
	 * Test that a mapped role is created
	 */
	@Test
	public void testCreateRole() {
		String mappedRoleName = "mapped_role";

		new TestCase()
			.mapRole(mappedRoleName)
			.expectThat(roleExists(mappedRoleName))
			.expectThat(roleHasGroups(mappedRoleName))
			.expectUpdate(true)
			.expect(event(MeshEvent.ROLE_CREATED, mappedRoleName))
			.test();
	}

	/**
	 * Test mapping an existing role
	 */
	@Test
	public void testMapExistingRole() {
		String mappedRoleName = "mapped_role";
		tx (tx -> {
			HibUser admin = tx.userDao().findByName("admin");
			tx.roleDao().create(mappedRoleName, admin);
		});

		new TestCase()
			.mapRole(mappedRoleName)
			.expectThat(roleExists(mappedRoleName))
			.expectThat(roleHasGroups(mappedRoleName))
			.expectUpdate(false)
			.expectNoEvents()
			.test();
	}

	/**
	 * Test that a mapped group is created and assigned to the user
	 */
	@Test
	public void testCreateGroup() {
		String mappedGroupName = "mapped_group";

		new TestCase()
			.mapGroup(mappedGroupName)
			.expectThat(groupExists(mappedGroupName))
			.expectThat(groupHasRoles(mappedGroupName))
			.expectThat(groupHasUsers(mappedGroupName, TESTUSER_NAME))
			.expectUpdate(true)
			.expect(event(MeshEvent.GROUP_CREATED, mappedGroupName))
			.expect(event(MeshEvent.GROUP_USER_ASSIGNED, groupRef(mappedGroupName), testUserRef()))
			.test();
	}

	/**
	 * Test mapping an existing group
	 */
	@Test
	public void testMapExistingGroup() {
		String mappedGroupName = "mapped_group";
		tx(tx -> {
			HibUser admin = tx.userDao().findByName("admin");
			tx.groupDao().create(mappedGroupName, admin);
		});

		new TestCase()
			.mapGroup(mappedGroupName)
			.expectThat(groupExists(mappedGroupName))
			.expectThat(groupHasRoles(mappedGroupName))
			.expectThat(groupHasUsers(mappedGroupName, TESTUSER_NAME))
			.expectUpdate(true)
			.expect(event(MeshEvent.GROUP_UPDATED, mappedGroupName))
			.expect(event(MeshEvent.GROUP_USER_ASSIGNED, groupRef(mappedGroupName), testUserRef()))
			.test();
	}

	/**
	 * Test mapping an existing and already assigned group
	 */
	@Test
	public void testMapAssignedGroup() {
		String mappedGroupName = "mapped_group";
		tx(tx -> {
			HibUser admin = tx.userDao().findByName("admin");
			HibUser testUser = tx.userDao().findByName(TESTUSER_NAME);
			HibGroup group = tx.groupDao().create(mappedGroupName, admin);
			tx.userDao().addGroup(testUser, group);
		});

		new TestCase()
			.mapGroup(mappedGroupName)
			.expectThat(groupExists(mappedGroupName))
			.expectThat(groupHasRoles(mappedGroupName))
			.expectThat(groupHasUsers(mappedGroupName, TESTUSER_NAME))
			.expectUpdate(false)
			.expectNoEvents()
			.test();
	}

	/**
	 * Test mapping a group and a role
	 */
	@Test
	public void testCreateGroupAndRole() {
		String mappedGroupName = "mapped_group";
		String mappedRoleName = "mapped_role";

		new TestCase()
			.mapGroup(mappedGroupName, mappedRoleName)
			.mapRole(mappedRoleName)
			.expectThat(groupExists(mappedGroupName))
			.expectThat(roleExists(mappedRoleName))
			.expectThat(groupHasRoles(mappedGroupName, mappedRoleName))
			.expectThat(groupHasUsers(mappedGroupName, TESTUSER_NAME))
			.expectUpdate(true)
			.expect(event(MeshEvent.GROUP_CREATED, mappedGroupName))
			.expect(event(MeshEvent.ROLE_CREATED, mappedRoleName))
			.expect(event(MeshEvent.GROUP_USER_ASSIGNED, groupRef(mappedGroupName), testUserRef()))
			.expect(event(MeshEvent.GROUP_ROLE_ASSIGNED, groupRef(mappedGroupName), roleRef(mappedRoleName)))
			.test();
	}

	/**
	 * Test assigning an existing group
	 */
	@Test
	public void testAssignExistingGroup() {
		String mappedRoleName = "mapped_role";
		String existingGroupName = "existing_group";
		tx(tx -> {
			HibUser admin = tx.userDao().findByName("admin");
			tx.groupDao().create(existingGroupName, admin);
		});

		new TestCase()
			.mapRole(mappedRoleName, existingGroupName)
			.expectThat(roleExists(mappedRoleName))
			.expectThat(roleHasGroups(mappedRoleName, existingGroupName))
			.expectThat(groupExists(existingGroupName))
			.expectThat(groupHasRoles(existingGroupName, mappedRoleName))
			.expectThat(groupHasUsers(existingGroupName))
			.expectUpdate(true)
			.expect(event(MeshEvent.ROLE_CREATED, mappedRoleName))
			.expect(event(MeshEvent.GROUP_ROLE_ASSIGNED, groupRef(existingGroupName), roleRef(mappedRoleName)))
			.test();
	}

	/**
	 * Test assigning an existing role
	 */
	@Test
	public void testAssignExistingRole() {
		String mappedGroupName = "mapped_group";
		String mappedRoleName = "mapped_role";
		tx(tx -> {
			HibUser admin = tx.userDao().findByName("admin");
			tx.roleDao().create(mappedRoleName, admin);
		});

		new TestCase()
			.mapGroup(mappedGroupName, mappedRoleName)
			.mapRole(mappedRoleName)
			.expectThat(groupExists(mappedGroupName))
			.expectThat(roleExists(mappedRoleName))
			.expectThat(groupHasRoles(mappedGroupName, mappedRoleName))
			.expectThat(groupHasUsers(mappedGroupName, TESTUSER_NAME))
			.expectUpdate(true)
			.expect(event(MeshEvent.GROUP_CREATED, mappedGroupName))
			.expect(event(MeshEvent.GROUP_USER_ASSIGNED, groupRef(mappedGroupName), testUserRef()))
			.expect(event(MeshEvent.GROUP_ROLE_ASSIGNED, groupRef(mappedGroupName), roleRef(mappedRoleName)))
			.test();
	}

	@Test
	public void testAssignExistingRoleToExistingAssignedGroup1() {
		String existingRoleName = "existing_role";
		String existingGroupName = "existing_group";
		tx(tx -> {
			HibUser admin = tx.userDao().findByName("admin");
			HibUser testUser = tx.userDao().findByName(TESTUSER_NAME);
			HibGroup group = tx.groupDao().create(existingGroupName, admin);
			tx.roleDao().create(existingRoleName, admin);
			tx.userDao().addGroup(testUser, group);
		});

		new TestCase()
			.mapGroup(existingGroupName)
			.mapRole(existingRoleName, existingGroupName)
			.expectThat(groupExists(existingGroupName))
			.expectThat(roleExists(existingRoleName))
			.expectThat(userHasGroups(TESTUSER_NAME, existingGroupName))
			.expectThat(groupHasRoles(existingGroupName, existingRoleName))
			.expectUpdate(true)
			.expect(event(MeshEvent.GROUP_ROLE_ASSIGNED, groupRef(existingGroupName), roleRef(existingRoleName)))
			.test();
	}

	@Test
	public void testAssignExistingRoleToExistingAssignedGroup2() {
		String existingRoleName = "existing_role";
		String existingGroupName = "existing_group";
		tx(tx -> {
			HibUser admin = tx.userDao().findByName("admin");
			HibUser testUser = tx.userDao().findByName(TESTUSER_NAME);
			HibGroup group = tx.groupDao().create(existingGroupName, admin);
			tx.roleDao().create(existingRoleName, admin);
			tx.userDao().addGroup(testUser, group);
		});

		new TestCase()
			.mapGroup(existingGroupName, existingRoleName)
			.mapRole(existingRoleName)
			.expectThat(groupExists(existingGroupName))
			.expectThat(roleExists(existingRoleName))
			.expectThat(userHasGroups(TESTUSER_NAME, existingGroupName))
			.expectThat(groupHasRoles(existingGroupName, existingRoleName))
			.expectUpdate(true)
			.expect(event(MeshEvent.GROUP_ROLE_ASSIGNED, groupRef(existingGroupName), roleRef(existingRoleName)))
			.test();
	}

	/**
	 * Test creating and assigning multiple groups and roles
	 */
	@Test
	public void testMultipleGroupsAndRoles() {
		String groupName1 = "mapped_group_1";
		String groupName2 = "mapped_group_2";
		String groupName3 = "mapped_group_3";
		String groupName4 = "mapped_group_4";
		String groupName5 = "mapped_group_5";
		String roleName1 = "mapped_role1";
		String roleName2 = "mapped_role2";
		String roleName3 = "mapped_role3";
		String roleName4 = "mapped_role4";
		String roleName5 = "mapped_role5";

		tx (tx -> {
			HibUser admin = tx.userDao().findByName("admin");
			HibGroup group3 = tx.groupDao().create(groupName3, admin);

			HibRole role2 = tx.roleDao().create(roleName2, admin);
			tx.roleDao().create(roleName4, admin);

			tx.groupDao().addRole(group3, role2);
		});

		new TestCase()
			.mapRole(roleName1)
			.mapRole(roleName2)
			.mapRole(roleName3)
			.mapRole(roleName4)
			.mapRole(roleName5)
			.mapGroup(groupName1, roleName2, roleName3)
			.mapGroup(groupName2, roleName3, roleName4)
			.mapGroup(groupName3, roleName4, roleName5)
			.mapGroup(groupName4, roleName5, roleName1)
			.mapGroup(groupName5, roleName1, roleName2)
			.expectThat(groupExists(groupName1))
			.expectThat(groupExists(groupName2))
			.expectThat(groupExists(groupName3))
			.expectThat(groupExists(groupName4))
			.expectThat(groupExists(groupName5))
			.expectThat(groupHasRoles(groupName1, roleName2, roleName3))
			.expectThat(groupHasRoles(groupName2, roleName3, roleName4))
			.expectThat(groupHasRoles(groupName3, roleName2, roleName4, roleName5))
			.expectThat(groupHasRoles(groupName4, roleName5, roleName1))
			.expectThat(groupHasRoles(groupName5, roleName1, roleName2))
			.expectThat(userHasGroups(TESTUSER_NAME, groupName1, groupName2, groupName3, groupName4, groupName5))
			.expectUpdate(true)

			.expect(event(MeshEvent.GROUP_CREATED, groupName1))
			.expect(event(MeshEvent.GROUP_CREATED, groupName2))
			.expect(event(MeshEvent.GROUP_UPDATED, groupName3))
			.expect(event(MeshEvent.GROUP_CREATED, groupName4))
			.expect(event(MeshEvent.GROUP_CREATED, groupName5))

			.expect(event(MeshEvent.ROLE_CREATED, roleName1))
			.expect(event(MeshEvent.ROLE_CREATED, roleName3))
			.expect(event(MeshEvent.ROLE_CREATED, roleName5))

			.expect(event(MeshEvent.GROUP_USER_ASSIGNED, groupRef(groupName1), testUserRef()))
			.expect(event(MeshEvent.GROUP_USER_ASSIGNED, groupRef(groupName2), testUserRef()))
			.expect(event(MeshEvent.GROUP_USER_ASSIGNED, groupRef(groupName3), testUserRef()))
			.expect(event(MeshEvent.GROUP_USER_ASSIGNED, groupRef(groupName4), testUserRef()))
			.expect(event(MeshEvent.GROUP_USER_ASSIGNED, groupRef(groupName5), testUserRef()))

			.expect(event(MeshEvent.GROUP_ROLE_ASSIGNED, groupRef(groupName1), roleRef(roleName2)))
			.expect(event(MeshEvent.GROUP_ROLE_ASSIGNED, groupRef(groupName1), roleRef(roleName3)))
			.expect(event(MeshEvent.GROUP_ROLE_ASSIGNED, groupRef(groupName2), roleRef(roleName3)))
			.expect(event(MeshEvent.GROUP_ROLE_ASSIGNED, groupRef(groupName2), roleRef(roleName4)))
			.expect(event(MeshEvent.GROUP_ROLE_ASSIGNED, groupRef(groupName3), roleRef(roleName4)))
			.expect(event(MeshEvent.GROUP_ROLE_ASSIGNED, groupRef(groupName3), roleRef(roleName5)))
			.expect(event(MeshEvent.GROUP_ROLE_ASSIGNED, groupRef(groupName4), roleRef(roleName5)))
			.expect(event(MeshEvent.GROUP_ROLE_ASSIGNED, groupRef(groupName4), roleRef(roleName1)))
			.expect(event(MeshEvent.GROUP_ROLE_ASSIGNED, groupRef(groupName5), roleRef(roleName1)))
			.expect(event(MeshEvent.GROUP_ROLE_ASSIGNED, groupRef(groupName5), roleRef(roleName2)))

			.test();
	}

	/**
	 * Test mapping a group when user is already assigned to another group
	 */
	@Test
	public void testOtherAssignedGroup() {
		String mappedGroupName = "mapped_group";
		String otherGroupName = "other_group";
		tx(tx -> {
			HibUser admin = tx.userDao().findByName("admin");
			HibUser testUser = tx.userDao().findByName(TESTUSER_NAME);
			HibGroup group = tx.groupDao().create(otherGroupName, admin);
			tx.userDao().addGroup(testUser, group);
		});

		new TestCase()
			.mapGroup(mappedGroupName)
			.expectThat(groupExists(otherGroupName))
			.expectThat(groupExists(mappedGroupName))
			.expectThat(userHasGroups(TESTUSER_NAME, mappedGroupName, otherGroupName))
			.expectUpdate(true)
			.expect(event(MeshEvent.GROUP_CREATED, mappedGroupName))
			.expect(event(MeshEvent.GROUP_USER_ASSIGNED, groupRef(mappedGroupName), testUserRef()))
			.test();
	}

	/**
	 * Test mapping a role when the group already is assigned to another role
	 */
	@Test
	public void testOtherAssignedRole() {
		String mappedRoleName = "mapped_role";
		String otherRoleName = "other_role";
		String mappedGroupName = "mapped_group";
		tx(tx -> {
			HibUser admin = tx.userDao().findByName("admin");
			HibGroup group = tx.groupDao().create(mappedGroupName, admin);
			HibRole role = tx.roleDao().create(otherRoleName, admin);
			tx.groupDao().addRole(group, role);
		});

		new TestCase()
			.mapGroup(mappedGroupName, mappedRoleName)
			.mapRole(mappedRoleName)
			.expectThat(groupExists(mappedGroupName))
			.expectThat(roleExists(mappedRoleName))
			.expectThat(groupHasRoles(mappedGroupName, mappedRoleName, otherRoleName))
			.expectThat(userHasGroups(TESTUSER_NAME, mappedGroupName))
			.expectUpdate(true)
			.expect(event(MeshEvent.ROLE_CREATED, mappedRoleName))
			.expect(event(MeshEvent.GROUP_UPDATED, mappedGroupName))
			.expect(event(MeshEvent.GROUP_USER_ASSIGNED, groupRef(mappedGroupName), testUserRef()))
			.expect(event(MeshEvent.GROUP_ROLE_ASSIGNED, groupRef(mappedGroupName), roleRef(mappedRoleName)))
			.test();
	}

	/**
	 * Test assigning an unexisting role (which is not mapped)
	 */
	@Test
	public void testAssignUnmappedRole() {
		String mappedRoleName = "mapped_role";
		String assignedRoleName = "assigned_role";
		String mappedGroupName = "mapped_group";

		new TestCase()
			.mapGroup(mappedGroupName, mappedRoleName, assignedRoleName)
			.mapRole(mappedRoleName)
			.expectThat(groupExists(mappedGroupName))
			.expectThat(roleExists(mappedRoleName))
			.expectThat(roleDoesNotExist(assignedRoleName))
			.expectThat(groupHasRoles(mappedGroupName, mappedRoleName))
			.expectThat(userHasGroups(TESTUSER_NAME, mappedGroupName))
			.expectUpdate(true)
			.expect(event(MeshEvent.GROUP_CREATED, mappedGroupName))
			.expect(event(MeshEvent.ROLE_CREATED, mappedRoleName))
			.expect(event(MeshEvent.GROUP_USER_ASSIGNED, groupRef(mappedGroupName), testUserRef()))
			.expect(event(MeshEvent.GROUP_ROLE_ASSIGNED, groupRef(mappedGroupName), roleRef(mappedRoleName)))
			.test();
	}

	@Test
	public void testAssignedUnmappedGroup() {
		String mappedRoleName = "mapped_role";
		String assignedGroupName = "assigned_group";

		new TestCase()
			.mapRole(mappedRoleName, assignedGroupName)
			.expectThat(roleExists(mappedRoleName))
			.expectThat(groupDoesNotExist(assignedGroupName))
			.expectThat(roleHasGroups(mappedRoleName))
			.expectUpdate(true)
			.expect(event(MeshEvent.ROLE_CREATED, mappedRoleName))
			.test();
	}

	/**
	 * Test filtering a group
	 */
	@Test
	public void testGroupFilter() {
		String mappedGroupName = "mapped_group";
		String assignedGroupName = "assigned_group";
		String filteredGroupName = "filtered_group";
		tx(tx -> {
			HibUser admin = tx.userDao().findByName("admin");
			HibUser testUser = tx.userDao().findByName(TESTUSER_NAME);
			HibGroup assignedGroup = tx.groupDao().create(assignedGroupName, admin);
			HibGroup filteredGroup = tx.groupDao().create(filteredGroupName, admin);
			tx.userDao().addGroup(testUser, assignedGroup);
			tx.userDao().addGroup(testUser, filteredGroup);
		});

		new TestCase()
			.mapGroup(mappedGroupName)
			.filterGroup(filteredGroupName)
			.expectThat(groupExists(mappedGroupName))
			.expectThat(groupExists(assignedGroupName))
			.expectThat(groupExists(filteredGroupName))
			.expectThat(userHasGroups(TESTUSER_NAME, mappedGroupName, assignedGroupName))
			.expectUpdate(true)
			.expect(event(MeshEvent.GROUP_CREATED, mappedGroupName))
			.expect(event(MeshEvent.GROUP_USER_ASSIGNED, groupRef(mappedGroupName), testUserRef()))
			.expect(event(MeshEvent.GROUP_USER_UNASSIGNED, groupRef(filteredGroupName), testUserRef()))
			.test();
	}

	@Test
	public void testGroupFilterExistingGroup() {
		String existingGroupName = "existing_group";
		tx(tx -> {
			HibUser admin = tx.userDao().findByName("admin");
			HibUser testUser = tx.userDao().findByName(TESTUSER_NAME);
			HibGroup group = tx.groupDao().create(existingGroupName, admin);
			tx.userDao().addGroup(testUser, group);
		});

		new TestCase()
			.filterGroup(existingGroupName)
			.expectThat(groupExists(existingGroupName))
			.expectThat(userHasGroups(TESTUSER_NAME))
			.expectUpdate(true)
			.expect(event(MeshEvent.GROUP_USER_UNASSIGNED, groupRef(existingGroupName), testUserRef()))
			.test();
	}

	/**
	 * Test filtering a group/role will remove the filtered roles from groups, if the group itself is mapped
	 */
	@Test
	public void testRoleFilter() {
		String mappedGroupName = "mapped_group";
		String assignedGroupName = "assigned_group";
		String filteredGroupName = "filtered_group";

		String mappedRoleName = "mapped_role";
		String assignedRoleName = "assigned_role";
		String filteredRoleName = "filtered_role";

		tx(tx -> {
			HibUser admin = tx.userDao().findByName("admin");
			HibUser testUser = tx.userDao().findByName(TESTUSER_NAME);
			HibGroup assignedGroup = tx.groupDao().create(assignedGroupName, admin);
			HibGroup filteredGroup = tx.groupDao().create(filteredGroupName, admin);
			HibRole assignedRole = tx.roleDao().create(assignedRoleName, admin);
			HibRole filteredRole = tx.roleDao().create(filteredRoleName, admin);
			tx.userDao().addGroup(testUser, assignedGroup);
			tx.userDao().addGroup(testUser, filteredGroup);
			tx.groupDao().addRole(assignedGroup, assignedRole);
			tx.groupDao().addRole(assignedGroup, filteredRole);
			tx.groupDao().addRole(filteredGroup, assignedRole);
			tx.groupDao().addRole(filteredGroup, filteredRole);
		});

		new TestCase()
			.mapGroup(mappedGroupName, mappedRoleName)
			.mapGroup(filteredGroupName)
			.mapRole(mappedRoleName)
			.filterRoleForGroup(filteredGroupName, filteredRoleName)
			.filterRoleForGroup(assignedGroupName, filteredRoleName)
			.expectThat(groupExists(mappedGroupName))
			.expectThat(groupExists(assignedGroupName))
			.expectThat(groupExists(filteredGroupName))
			.expectThat(roleExists(mappedRoleName))
			.expectThat(roleExists(assignedRoleName))
			.expectThat(roleExists(filteredRoleName))
			.expectThat(groupHasRoles(mappedGroupName, mappedRoleName))
			.expectThat(groupHasRoles(assignedGroupName, assignedRoleName, filteredRoleName))
			.expectThat(groupHasRoles(filteredGroupName, assignedRoleName))
			.expectThat(userHasGroups(TESTUSER_NAME, mappedGroupName, assignedGroupName, filteredGroupName))
			.expectUpdate(true)
			.expect(event(MeshEvent.GROUP_CREATED, mappedGroupName))
			.expect(event(MeshEvent.ROLE_CREATED, mappedRoleName))
			.expect(event(MeshEvent.GROUP_USER_ASSIGNED, groupRef(mappedGroupName), testUserRef()))
			.expect(event(MeshEvent.GROUP_ROLE_ASSIGNED, groupRef(mappedGroupName), roleRef(mappedRoleName)))
			.expect(event(MeshEvent.GROUP_ROLE_UNASSIGNED, groupRef(filteredGroupName), roleRef(filteredRoleName)))
			.test();
	}

	/**
	 * Create an event model on a named entity
	 * @param event event
	 * @param name entity name
	 * @return model
	 */
	protected MeshEventModel event(MeshEvent event, String name) {
		MeshElementEventModelImpl model = new MeshElementEventModelImpl();
		model.setEvent(event);
		model.setName(name);
		return model;
	}

	/**
	 * Create an event model referencing a group and a user
	 * @param event event
	 * @param group group reference
	 * @param user user reference
	 * @return model
	 */
	protected MeshEventModel event(MeshEvent event, GroupReference group, UserReference user) {
		GroupUserAssignModel model = new GroupUserAssignModel();
		model.setEvent(event);
		model.setGroup(group);
		model.setUser(user);
		return model;
	}

	/**
	 * Create an event model referencing a group and a role
	 * @param event event
	 * @param group group reference
	 * @param role role reference
	 * @return model
	 */
	protected MeshEventModel event(MeshEvent event, GroupReference group, RoleReference role) {
		GroupRoleAssignModel model = new GroupRoleAssignModel();
		model.setEvent(event);
		model.setGroup(group);
		model.setRole(role);
		return model;
	}

	/**
	 * Create a reference to the given group 
	 * @param name group name
	 * @return group reference
	 */
	protected GroupReference groupRef(String name) {
		return new GroupReference().setName(name);
	}

	/**
	 * Create a reference to the given role
	 * @param name role name
	 * @return role reference
	 */
	protected RoleReference roleRef(String name) {
		return new RoleReference().setName(name);
	}

	/**
	 * Create a reference to the test user
	 * @return user reference
	 */
	protected UserReference testUserRef() {
		return new UserReference().setFirstName(TESTUSER_NAME).setLastName(TESTUSER_NAME);
	}

	/**
	 * Assert that the role exists
	 * @param name role name
	 * @return tx action which asserts existence of the role
	 */
	protected TxAction2 roleExists(String name) {
		return tx -> {
			HibRole role = tx.roleDao().findByName(name);
			assertThat(role).as("Role " + name).isNotNull();
		};
	}

	/**
	 * Assert that the role does not exist
	 * @param name role name
	 * @return tx action which asserts non-existence of the role
	 */
	protected TxAction2 roleDoesNotExist(String name) {
		return tx -> {
			HibRole role = tx.roleDao().findByName(name);
			assertThat(role).as("Role " + name).isNull();
		};
	}

	/**
	 * Assert that the role is assigned to exactly the given groups
	 * @param roleName role name
	 * @param groupNames group names (empty for asserting that the role is not assigned to groups at all)
	 * @return tx action for the assertion
	 */
	protected TxAction2 roleHasGroups(String roleName, String...groupNames) {
		return tx -> {
			HibRole role = tx.roleDao().findByName(roleName);
			assertThat(role).as("Role " + roleName).isNotNull();
			List<String> assignedGroupNames = role.getGroups().stream().map(HibGroup::getName).collect(Collectors.toList());
			assertThat(assignedGroupNames).as("Groups assigned to role " + roleName).containsOnly(groupNames);
		};
	}

	/**
	 * Assert that the group exists
	 * @param name group name
	 * @return tx action for the assertion
	 */
	protected TxAction2 groupExists(String name) {
		return tx -> {
			HibGroup group = tx.groupDao().findByName(name);
			assertThat(group).as("Group " + name).isNotNull();
		};
	}

	/**
	 * Assert that the group does not exists
	 * @param name group name
	 * @return tx action for the assertion
	 */
	protected TxAction2 groupDoesNotExist(String name) {
		return tx -> {
			HibGroup group = tx.groupDao().findByName(name);
			assertThat(group).as("Group " + name).isNull();
		};
	}

	/**
	 * Assert that the group is assigned exactly to the given roles
	 * @param groupName group name
	 * @param roleNames role names (empty for asserting that the group is is not assigned to roles at all)
	 * @return tx action for the assertion
	 */
	protected TxAction2 groupHasRoles(String groupName, String...roleNames) {
		return tx -> {
			HibGroup group = tx.groupDao().findByName(groupName);
			assertThat(group).as("Group " + groupName).isNotNull();
			List<String> assignedRoleNames = tx.groupDao().getRoles(group).stream().map(HibRole::getName).collect(Collectors.toList());
			assertThat(assignedRoleNames).as("Roles assigned to group " + groupName).containsOnly(roleNames);
		};
	}

	/**
	 * Assert that the group is assigned exactly to the given users
	 * @param groupName group name
	 * @param userNames user names (empty for asserting that the group is not assigned to users at all)
	 * @return tx action for the assertion
	 */
	protected TxAction2 groupHasUsers(String groupName, String...userNames) {
		return tx -> {
			HibGroup group = tx.groupDao().findByName(groupName);
			assertThat(group).as("Group " + groupName).isNotNull();
			List<String> assignedUserNames = tx.groupDao().getUsers(group).stream().map(HibUser::getUsername).collect(Collectors.toList());
			assertThat(assignedUserNames).as("Users assigned to group " + groupName).containsOnly(userNames);
		};
	}

	/**
	 * Assert that the user is assigned exactly to the given groups
	 * @param userName user name
	 * @param groupNames group names (empty for asserting that the user is not assigned to groups at all)
	 * @return tx action for the assertion
	 */
	protected TxAction2 userHasGroups(String userName, String...groupNames) {
		return tx -> {
			HibUser user = tx.userDao().findByName(userName);
			assertThat(user).as("User " + userName).isNotNull();
			List<String> assignedGroupNames = tx.userDao().getGroups(user).stream().map(HibGroup::getName).collect(Collectors.toList());
			assertThat(assignedGroupNames).as("Groups assigned to user " + userName).containsOnly(groupNames);
		};
	}

	/**
	 * Class defining a test case
	 */
	protected class TestCase {
		/**
		 * Set of filtered group names
		 */
		protected Set<String> filteredGroupNames = new HashSet<>();

		/**
		 * Map of filtered role names by group name
		 */
		protected Map<String, Set<String>> filteredRolesByGroupNames = new HashMap<>();

		/**
		 * Mapping result to test
		 */
		protected MappingResult result = new MappingResult().setGroupFilter(filteredGroupNames::contains)
				.setRoleFilter((groupName, roleName) -> {
					return filteredRolesByGroupNames.getOrDefault(groupName, Collections.emptySet()).contains(roleName);
				});

		/**
		 * Asserters
		 */
		protected List<TxAction2> asserters = new ArrayList<>();

		/**
		 * Flag whether an update is expected when handling the {@link #result}
		 */
		protected boolean expectUpdate = false;

		/**
		 * Expected events (for no event expectations)
		 */
		protected List<MeshEventModel> expectedEvents;

		/**
		 * Map the given role
		 * @param name role name
		 * @param groupNames names of groups, the role shall be assigned to
		 * @return fluent API
		 */
		public TestCase mapRole(String name, String...groupNames) {
			if (result.getRoles() == null) {
				result.setRoles(new ArrayList<>());
			}
			RoleResponse role = new RoleResponse();
			role.setName(name);
			if (groupNames.length > 0) {
				role.setGroups(Stream.of(groupNames).map(groupName -> new GroupReference().setName(groupName)).collect(Collectors.toList()));
			}
			result.getRoles().add(role);
			return this;
		}

		/**
		 * Map the given group
		 * @param name group name
		 * @param roleNames names of roles, the group shall be assigned to
		 * @return fluent API
		 */
		public TestCase mapGroup(String name, String...roleNames) {
			if (result.getGroups() == null) {
				result.setGroups(new ArrayList<>());
			}
			GroupResponse group = new GroupResponse();
			group.setName(name);
			if (roleNames.length > 0) {
				group.setRoles(Stream.of(roleNames).map(roleName -> new RoleReference().setName(roleName)).collect(Collectors.toList()));
			}
			result.getGroups().add(group);
			return this;
		}

		/**
		 * Filter the given group name
		 * @param groupName group name
		 * @return fluent API
		 */
		public TestCase filterGroup(String groupName) {
			filteredGroupNames.add(groupName);
			return this;
		}

		/**
		 * Filter the given role name for the group name
		 * @param groupName group name
		 * @param roleName role name
		 * @return fluent API
		 */
		public TestCase filterRoleForGroup(String groupName, String roleName) {
			filteredRolesByGroupNames.computeIfAbsent(groupName, k -> new HashSet<>()).add(roleName);
			return this;
		}

		/**
		 * Add an asserter to the test case
		 * @param asserter asserter to add
		 * @return fluent API
		 */
		public TestCase expectThat(TxAction2 asserter) {
			this.asserters.add(asserter);
			return this;
		}

		/**
		 * Set whether we expect updates
		 * @param expectUpdate true to expect updates, false to expect no updates
		 * @return fluent API
		 */
		public TestCase expectUpdate(boolean expectUpdate) {
			this.expectUpdate = expectUpdate;
			return this;
		}

		/**
		 * Expect an event
		 * @param event expected event
		 * @return fluent API
		 */
		public TestCase expect(MeshEventModel event) {
			if (this.expectedEvents == null) {
				this.expectedEvents = new ArrayList<>();
			}
			this.expectedEvents.add(event);
			return this;
		}

		/**
		 * Expect no events
		 * @return fluent API
		 */
		public TestCase expectNoEvents() {
			this.expectedEvents = Collections.emptyList();
			return this;
		}

		/**
		 * Run the test case
		 */
		public void test() {
			List<MeshEventModel> caughtEvents = new ArrayList<>();
			boolean success = tx(tx -> {
				EventQueueBatch eqb = tx.createBatch();
				HibUser admin = tx.userDao().findByName("admin");
				HibUser testUser = tx.userDao().findByName(TESTUSER_NAME);

				try {
					service.handleMappingResult(tx, eqb, result, testUser, admin);
					if (!writable && expectUpdate) {
						fail("Handling mapping result was supposed to fail, but succeeded");
					}
					if (expectedEvents != null) {
						caughtEvents.addAll(eqb.getEntries());
					}
					return true;
				} catch (CannotWriteException e) {
					if (writable || !expectUpdate) {
						throw e;
					}
					return false;
				}
			});

			if (success && !CollectionUtils.isEmpty(asserters)) {
				tx(tx -> asserters.forEach(asserter -> asserter.accept(tx)));
			}

			if (success && expectedEvents != null) {
				assertThat(caughtEvents).as("Events").usingElementComparator(EVENT_COMPARATOR).containsOnlyElementsOf(expectedEvents);
			}
		}
	}
}
