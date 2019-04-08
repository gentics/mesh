package com.gentics.mesh.search.verticle.eventhandler;

import com.gentics.mesh.search.verticle.eventhandler.node.NodeContentEventHandler;
import com.gentics.mesh.search.verticle.eventhandler.node.NodeMoveEventHandler;
import com.gentics.mesh.search.verticle.eventhandler.node.NodeTagEventHandler;
import com.gentics.mesh.search.verticle.eventhandler.project.ProjectCreateEventHandler;
import com.gentics.mesh.search.verticle.eventhandler.project.ProjectDeleteEventHandler;
import com.gentics.mesh.search.verticle.eventhandler.project.ProjectUpdateEventHandler;
import dagger.internal.Factory;
import javax.annotation.Generated;
import javax.inject.Provider;

@Generated(
  value = "dagger.internal.codegen.ComponentProcessor",
  comments = "https://google.github.io/dagger"
)
public final class MainEventHandler_Factory implements Factory<MainEventHandler> {
  private final Provider<SyncEventHandler> syncEventHandlerProvider;

  private final Provider<EventHandlerFactory> eventHandlerFactoryProvider;

  private final Provider<GroupEventHandler> groupEventHandlerProvider;

  private final Provider<TagEventHandler> tagEventHandlerProvider;

  private final Provider<TagFamilyEventHandler> tagFamilyEventHandlerProvider;

  private final Provider<NodeContentEventHandler> nodeContentEventHandlerProvider;

  private final Provider<NodeTagEventHandler> nodeTagEventHandlerProvider;

  private final Provider<NodeMoveEventHandler> nodeMoveEventHandlerProvider;

  private final Provider<RoleDeletedEventHandler> roleDeletedEventHandlerProvider;

  private final Provider<ProjectDeleteEventHandler> projectDeleteEventHandlerProvider;

  private final Provider<ClearEventHandler> clearEventHandlerProvider;

  private final Provider<BranchEventHandler> branchEventHandlerProvider;

  private final Provider<SchemaMigrationEventHandler> schemaMigrationEventHandlerProvider;

  private final Provider<PermissionChangedEventHandler> permissionChangedEventHandlerProvider;

  private final Provider<GroupUserAssignmentHandler> userGroupAssignmentHandlerProvider;

  private final Provider<ProjectUpdateEventHandler> projectUpdateEventHandlerProvider;

  private final Provider<ProjectCreateEventHandler> projectCreateEventHandlerProvider;

  public MainEventHandler_Factory(
      Provider<SyncEventHandler> syncEventHandlerProvider,
      Provider<EventHandlerFactory> eventHandlerFactoryProvider,
      Provider<GroupEventHandler> groupEventHandlerProvider,
      Provider<TagEventHandler> tagEventHandlerProvider,
      Provider<TagFamilyEventHandler> tagFamilyEventHandlerProvider,
      Provider<NodeContentEventHandler> nodeContentEventHandlerProvider,
      Provider<NodeTagEventHandler> nodeTagEventHandlerProvider,
      Provider<NodeMoveEventHandler> nodeMoveEventHandlerProvider,
      Provider<RoleDeletedEventHandler> roleDeletedEventHandlerProvider,
      Provider<ProjectDeleteEventHandler> projectDeleteEventHandlerProvider,
      Provider<ClearEventHandler> clearEventHandlerProvider,
      Provider<BranchEventHandler> branchEventHandlerProvider,
      Provider<SchemaMigrationEventHandler> schemaMigrationEventHandlerProvider,
      Provider<PermissionChangedEventHandler> permissionChangedEventHandlerProvider,
      Provider<GroupUserAssignmentHandler> userGroupAssignmentHandlerProvider,
      Provider<ProjectUpdateEventHandler> projectUpdateEventHandlerProvider,
      Provider<ProjectCreateEventHandler> projectCreateEventHandlerProvider) {
    assert syncEventHandlerProvider != null;
    this.syncEventHandlerProvider = syncEventHandlerProvider;
    assert eventHandlerFactoryProvider != null;
    this.eventHandlerFactoryProvider = eventHandlerFactoryProvider;
    assert groupEventHandlerProvider != null;
    this.groupEventHandlerProvider = groupEventHandlerProvider;
    assert tagEventHandlerProvider != null;
    this.tagEventHandlerProvider = tagEventHandlerProvider;
    assert tagFamilyEventHandlerProvider != null;
    this.tagFamilyEventHandlerProvider = tagFamilyEventHandlerProvider;
    assert nodeContentEventHandlerProvider != null;
    this.nodeContentEventHandlerProvider = nodeContentEventHandlerProvider;
    assert nodeTagEventHandlerProvider != null;
    this.nodeTagEventHandlerProvider = nodeTagEventHandlerProvider;
    assert nodeMoveEventHandlerProvider != null;
    this.nodeMoveEventHandlerProvider = nodeMoveEventHandlerProvider;
    assert roleDeletedEventHandlerProvider != null;
    this.roleDeletedEventHandlerProvider = roleDeletedEventHandlerProvider;
    assert projectDeleteEventHandlerProvider != null;
    this.projectDeleteEventHandlerProvider = projectDeleteEventHandlerProvider;
    assert clearEventHandlerProvider != null;
    this.clearEventHandlerProvider = clearEventHandlerProvider;
    assert branchEventHandlerProvider != null;
    this.branchEventHandlerProvider = branchEventHandlerProvider;
    assert schemaMigrationEventHandlerProvider != null;
    this.schemaMigrationEventHandlerProvider = schemaMigrationEventHandlerProvider;
    assert permissionChangedEventHandlerProvider != null;
    this.permissionChangedEventHandlerProvider = permissionChangedEventHandlerProvider;
    assert userGroupAssignmentHandlerProvider != null;
    this.userGroupAssignmentHandlerProvider = userGroupAssignmentHandlerProvider;
    assert projectUpdateEventHandlerProvider != null;
    this.projectUpdateEventHandlerProvider = projectUpdateEventHandlerProvider;
    assert projectCreateEventHandlerProvider != null;
    this.projectCreateEventHandlerProvider = projectCreateEventHandlerProvider;
  }

  @Override
  public MainEventHandler get() {
    return new MainEventHandler(
        syncEventHandlerProvider.get(),
        eventHandlerFactoryProvider.get(),
        groupEventHandlerProvider.get(),
        tagEventHandlerProvider.get(),
        tagFamilyEventHandlerProvider.get(),
        nodeContentEventHandlerProvider.get(),
        nodeTagEventHandlerProvider.get(),
        nodeMoveEventHandlerProvider.get(),
        roleDeletedEventHandlerProvider.get(),
        projectDeleteEventHandlerProvider.get(),
        clearEventHandlerProvider.get(),
        branchEventHandlerProvider.get(),
        schemaMigrationEventHandlerProvider.get(),
        permissionChangedEventHandlerProvider.get(),
        userGroupAssignmentHandlerProvider.get(),
        projectUpdateEventHandlerProvider.get(),
        projectCreateEventHandlerProvider.get());
  }

  public static Factory<MainEventHandler> create(
      Provider<SyncEventHandler> syncEventHandlerProvider,
      Provider<EventHandlerFactory> eventHandlerFactoryProvider,
      Provider<GroupEventHandler> groupEventHandlerProvider,
      Provider<TagEventHandler> tagEventHandlerProvider,
      Provider<TagFamilyEventHandler> tagFamilyEventHandlerProvider,
      Provider<NodeContentEventHandler> nodeContentEventHandlerProvider,
      Provider<NodeTagEventHandler> nodeTagEventHandlerProvider,
      Provider<NodeMoveEventHandler> nodeMoveEventHandlerProvider,
      Provider<RoleDeletedEventHandler> roleDeletedEventHandlerProvider,
      Provider<ProjectDeleteEventHandler> projectDeleteEventHandlerProvider,
      Provider<ClearEventHandler> clearEventHandlerProvider,
      Provider<BranchEventHandler> branchEventHandlerProvider,
      Provider<SchemaMigrationEventHandler> schemaMigrationEventHandlerProvider,
      Provider<PermissionChangedEventHandler> permissionChangedEventHandlerProvider,
      Provider<GroupUserAssignmentHandler> userGroupAssignmentHandlerProvider,
      Provider<ProjectUpdateEventHandler> projectUpdateEventHandlerProvider,
      Provider<ProjectCreateEventHandler> projectCreateEventHandlerProvider) {
    return new MainEventHandler_Factory(
        syncEventHandlerProvider,
        eventHandlerFactoryProvider,
        groupEventHandlerProvider,
        tagEventHandlerProvider,
        tagFamilyEventHandlerProvider,
        nodeContentEventHandlerProvider,
        nodeTagEventHandlerProvider,
        nodeMoveEventHandlerProvider,
        roleDeletedEventHandlerProvider,
        projectDeleteEventHandlerProvider,
        clearEventHandlerProvider,
        branchEventHandlerProvider,
        schemaMigrationEventHandlerProvider,
        permissionChangedEventHandlerProvider,
        userGroupAssignmentHandlerProvider,
        projectUpdateEventHandlerProvider,
        projectCreateEventHandlerProvider);
  }
}
