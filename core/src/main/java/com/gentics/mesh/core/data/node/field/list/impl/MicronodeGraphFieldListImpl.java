package com.gentics.mesh.core.data.node.field.list.impl;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;

import com.gentics.mesh.cli.BootstrapInitializer;
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
import com.gentics.mesh.graphdb.spi.Database;
import com.gentics.mesh.handler.InternalActionContext;
import com.gentics.mesh.util.RxUtil;

import rx.Observable;

/**
 * @see MicronodeGraphFieldList
 */
public class MicronodeGraphFieldListImpl extends AbstractReferencingGraphFieldList<MicronodeGraphField, MicronodeFieldList, Micronode>
		implements MicronodeGraphFieldList {

	public static void checkIndices(Database database) {
		database.addVertexType(MicronodeGraphFieldListImpl.class);
	}

	@Override
	public Class<? extends MicronodeGraphField> getListType() {
		return MicronodeGraphFieldImpl.class;
	}

	@Override
	public Observable<MicronodeFieldList> transformToRest(InternalActionContext ac, String fieldKey, List<String> languageTags) {

		MicronodeFieldList restModel = new MicronodeFieldListImpl();

		List<Observable<MicronodeResponse>> obs = new ArrayList<>();
		for (MicronodeGraphField item : getList()) {
			obs.add(item.getMicronode().transformToRestSync(ac));
		}

		return RxUtil.concatList(obs).collect(() -> {
			return restModel.getItems();
		} , (x, y) -> {
			x.add(y);
		}).map(i -> restModel);
	}

	@Override
	public Micronode createMicronode(MicronodeField field) {
		MicronodeImpl micronode = getGraph().addFramedVertex(MicronodeImpl.class);
		addItem(String.valueOf(getSize() + 1), micronode);

		return micronode;
	}

	@Override
	public Observable<Boolean> update(InternalActionContext ac, MicronodeFieldList list) {
		BootstrapInitializer boot = BootstrapInitializer.getBoot();

		Map<String, Micronode> existing = getList().stream().collect(Collectors.toMap(field -> {
			return field.getMicronode().getUuid();
		} , field -> {
			return field.getMicronode();
		} , (a, b) -> {
			return a;
		}));

		return Observable.create(subscriber -> {
			Observable.from(list.getItems()).flatMap(item -> {
				MicroschemaReference microschemaReference = item.getMicroschema();
				if (microschemaReference == null) {
					return Observable.error(new NullPointerException("Found micronode without microschema reference"));
				}

				String microschemaName = microschemaReference.getName();
				String microschemaUuid = microschemaReference.getUuid();
				if (!StringUtils.isEmpty(microschemaName)) {
					return boot.microschemaContainerRoot().findByName(microschemaName);
				} else {
					return boot.microschemaContainerRoot().findByUuid(microschemaUuid);
				}
			} , (node, microschemaContainer) -> {
				Micronode micronode = existing.get(node.getUuid());
				if (micronode == null) {
					micronode = getGraph().addFramedVertex(MicronodeImpl.class);
					micronode.setMicroschemaContainer(microschemaContainer);
				} else {
					if (!StringUtils.equalsIgnoreCase(micronode.getMicroschemaContainer().getUuid(), microschemaContainer.getUuid())) {
						// TODO proper exception
						throw new Error();
					}
				}
				try {
					micronode.updateFieldsFromRest(ac, node.getFields(), micronode.getMicroschema());
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
			} , e -> {
				subscriber.onError(e);
			} , () -> {
				subscriber.onNext(true);
				subscriber.onCompleted();
			});
		});
	}

	@Override
	public void delete() {
		getList().stream().map(MicronodeGraphField::getMicronode).forEach(Micronode::delete);
		getElement().remove();
	}

	@Override
	public List<Micronode> getValues() {
		return getList().stream().map(MicronodeGraphField::getMicronode).collect(Collectors.toList());
	}
}
