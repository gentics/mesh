package com.gentics.mesh.search.verticle.eventhandler;

import static com.gentics.mesh.core.rest.MeshEvent.GROUP_CREATED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_DELETED;
import static com.gentics.mesh.core.rest.MeshEvent.GROUP_UPDATED;
import static com.gentics.mesh.search.verticle.eventhandler.Util.requireType;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.function.Supplier;

import javax.inject.Inject;
import javax.inject.Singleton;

import com.gentics.mesh.context.SimpleDataHolderContext;
import com.gentics.mesh.core.data.dao.GroupDao;
import com.gentics.mesh.core.data.dao.RoleDao;
import com.gentics.mesh.core.data.dao.UserDao;
import com.gentics.mesh.core.data.group.HibGroup;
import com.gentics.mesh.core.data.perm.InternalPermission;
import com.gentics.mesh.core.data.search.request.CreateDocumentRequest;
import com.gentics.mesh.core.data.search.request.SearchRequest;
import com.gentics.mesh.core.data.user.HibUser;
import com.gentics.mesh.core.db.CommonTx;
import com.gentics.mesh.core.rest.MeshEvent;
import com.gentics.mesh.core.rest.event.MeshElementEventModel;
import com.gentics.mesh.etc.config.MeshOptions;
import com.gentics.mesh.etc.config.search.ComplianceMode;
import com.gentics.mesh.handler.DataHolderContext;
import com.gentics.mesh.search.index.BucketManager;
import com.gentics.mesh.search.verticle.MessageEvent;
import com.gentics.mesh.search.verticle.entity.MeshEntities;
import com.gentics.mesh.util.PreparationUtil;

import io.reactivex.Flowable;

/**
 * Search index handler for group events.
 */
@Singleton
public class GroupEventHandler implements EventHandler {
	private final MeshHelper helper;
	private final MeshEntities entities;
	private final ComplianceMode complianceMode;
	private final BucketManager bucketManager;

	@Inject
	public GroupEventHandler(MeshHelper helper, MeshEntities entities, MeshOptions options, BucketManager bucketManager) {
		this.helper = helper;
		this.entities = entities;
		this.complianceMode = options.getSearchOptions().getComplianceMode();
		this.bucketManager = bucketManager;
	}

	@Override
	public Collection<MeshEvent> handledEvents() {
		return Arrays.asList(GROUP_CREATED, GROUP_UPDATED, GROUP_DELETED);
	}

	@Override
	public Flowable<SearchRequest> handle(MessageEvent messageEvent) {
		return Flowable.defer(() -> {
			MeshEvent event = messageEvent.event;
			MeshElementEventModel model = requireType(MeshElementEventModel.class, messageEvent.message);
			if (event == GROUP_CREATED || event == GROUP_UPDATED) {
				Optional<HibGroup> groupOptional = helper.getDb().tx(tx -> {
					return entities.group.getElement(model);
				});

				if (groupOptional.isPresent()) {
					Flowable<CreateDocumentRequest> flowableIndexGroup = groupOptional.stream().map(g -> {
						return helper.getDb().tx(tx -> {
							HibGroup group = CommonTx.get().attach(g, false);
							return entities.createRequest(group);
						});
					}).collect(Util.toFlowable());

					Supplier<Long> userCounter = () -> {
						return helper.getDb().tx(tx -> {
							HibGroup group = CommonTx.get().attach(groupOptional.get(), false);
							return tx.groupDao().countUsers(group);
						});
					};

					Flowable<CreateDocumentRequest> flowableIndexUsers = bucketManager.doWithBuckets(userCounter, bucket -> {
						return helper.getDb().tx(tx -> {
							HibGroup group = CommonTx.get().attach(groupOptional.get(), false);

							GroupDao groupDao = tx.groupDao();
							UserDao userDao = tx.userDao();
							RoleDao roleDao = tx.roleDao();
							DataHolderContext dhc = new SimpleDataHolderContext();
							List<HibUser> users = new ArrayList<>(groupDao.getUsers(group, bucket).list());

							PreparationUtil.prepareData(users, dhc, "user", "groups", userDao::getGroups);
							PreparationUtil.prepareData(users, dhc, "user", "permissions",
									elements -> roleDao.getRoleUuidsForPerm(elements, InternalPermission.READ_PERM));

							return users.stream().map(u -> entities.createRequest(u, dhc)).collect(Util.toFlowable());
						});
					});

					return Flowable.concat(flowableIndexGroup, flowableIndexUsers);
				} else {
					return Flowable.empty();
				}
			} else if (event == GROUP_DELETED) {
				// TODO Update users that were part of that group.
				// At the moment we cannot look up users that were in the group if the group is already deleted.
				return Flowable.just(helper.deleteDocumentRequest(HibGroup.composeIndexName(), model.getUuid(), complianceMode));
			} else {
				throw new RuntimeException("Unexpected event " + event.address);
			}
		});
	}
}
