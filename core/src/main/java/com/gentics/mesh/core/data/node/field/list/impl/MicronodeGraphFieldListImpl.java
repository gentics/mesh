package com.gentics.mesh.core.data.node.field.list.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
import com.gentics.mesh.core.data.MicroschemaContainer;
import com.gentics.mesh.core.data.node.Micronode;
import com.gentics.mesh.core.data.node.field.impl.MicronodeGraphFieldImpl;
import com.gentics.mesh.core.data.node.field.list.AbstractReferencingGraphFieldList;
import com.gentics.mesh.core.data.node.field.list.MicronodeGraphFieldList;
import com.gentics.mesh.core.data.node.field.nesting.MicronodeGraphField;
import com.gentics.mesh.core.data.node.impl.MicronodeImpl;
import com.gentics.mesh.core.rest.micronode.MicronodeResponse;
import com.gentics.mesh.core.rest.node.field.MicronodeField;
import com.gentics.mesh.core.rest.node.field.list.MicronodeFieldList;
import com.gentics.mesh.core.rest.node.field.list.impl.MicronodeFieldListImpl;
import com.gentics.mesh.core.rest.schema.MicroschemaReference;
import com.gentics.mesh.handler.ActionContext;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.util.RxUtil;

import io.vertx.core.AsyncResult;
import io.vertx.core.Future;
import io.vertx.core.Handler;
import io.vertx.rx.java.ObservableFuture;
import io.vertx.rx.java.RxHelper;
import rx.Observable;

public class MicronodeGraphFieldListImpl extends AbstractReferencingGraphFieldList<MicronodeGraphField, MicronodeFieldList> implements MicronodeGraphFieldList {

	@Override
	public Class<? extends MicronodeGraphField> getListType() {
		return MicronodeGraphFieldImpl.class;
	}

	@Override
	public void transformToRest(InternalActionContext ac, String fieldKey,
			Handler<AsyncResult<MicronodeFieldList>> handler) {

		MicronodeFieldList restModel = new MicronodeFieldListImpl();

		List<ObservableFuture<MicronodeResponse>> futures = new ArrayList<>();
		for (MicronodeGraphField item : getList()) {
			ObservableFuture<MicronodeResponse> obsItemTransformed = RxHelper.observableFuture();
			futures.add(obsItemTransformed);
			item.getMicronode().transformToRest(ac, rh -> {
				if (rh.failed()) {
					obsItemTransformed.toHandler().handle(Future.failedFuture(rh.cause()));
				} else {
					obsItemTransformed.toHandler().handle(Future.succeededFuture(rh.result()));
				}
			});
		}

		RxUtil.concatList(futures).collect(() -> {
			return restModel.getItems();
		} , (x, y) -> {
			x.add(y);
		}).subscribe(list -> {
			handler.handle(Future.succeededFuture(restModel));
		} , error -> {
			handler.handle(Future.failedFuture(error));
		});
	}

	@Override
	public Micronode createMicronode(MicronodeField field) {
		MicronodeImpl micronode = getGraph().addFramedVertex(MicronodeImpl.class);
		addItem(String.valueOf(getSize() + 1), micronode);

		return micronode;
	}

	@Override
	public Observable<Boolean> update(ActionContext ac, MicronodeFieldList list) {
		BootstrapInitializer boot = BootstrapInitializer.getBoot();

		Map<String, Micronode> existing = getList().stream().collect(Collectors.toMap(field -> {
			return field.getMicronode().getUuid();
		}, field -> {
			return field.getMicronode();
		}, (a, b) -> {
			return a;
		}));

		return Observable.create(subscriber -> {
			Observable.from(list.getItems()).flatMap(node -> {
				MicroschemaReference microschemaReference = node.getMicroschema();
				if (microschemaReference == null) {
					return Observable.error(new NullPointerException("Found micronode without microschema reference"));
				}

				String microschemaName = microschemaReference.getName();
				String microschemaUuid = microschemaReference.getUuid();
				if (!StringUtils.isEmpty(microschemaName)) {
					return Observable.just(boot.microschemaContainerRoot().findByName(microschemaName));
				} else {
					ObservableFuture<MicroschemaContainer> microschemaContainer = RxHelper.observableFuture();
					boot.microschemaContainerRoot().findByUuid(microschemaUuid, microschemaContainer.toHandler());
					return microschemaContainer;
				}
			} , (node, microschemaContainer) -> {
				Micronode micronode = existing.get(node.getUuid());
				if (micronode == null) {
					micronode = getGraph().addFramedVertex(MicronodeImpl.class);
					micronode.setMicroschemaContainer(microschemaContainer);
				} else {
					if (!StringUtils.equalsIgnoreCase(micronode.getMicroschemaContainer().getUuid(),
							microschemaContainer.getUuid())) {
						// TODO proper exception
						throw new Error();
					}
				}
				try {
					micronode.updateFieldsFromRest(ac, node.getFields());
				} catch (Exception e) {
					throw new Error(e);
				}
				return micronode;
			}).toList().subscribe(micronodeList -> {
				removeAll();
				int counter = 1;
				for (Micronode micronode : micronodeList) {
					existing.remove(micronode.getUuid());
					addItem(String.valueOf(counter++), micronode);
				}
				existing.values().stream().forEach(Micronode::delete);
			}, e -> {
				subscriber.onError(e);
			}, () -> {
				subscriber.onNext(true);
				subscriber.onCompleted();
			});
		});
	}
}
