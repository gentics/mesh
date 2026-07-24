package com.gentics.mesh.hibernate.type;

import java.io.Serializable;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;

import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.usertype.UserType;

import com.gentics.mesh.core.rest.node.field.JsonContent;
import com.gentics.mesh.json.JsonUtil;

/**
 * JSON content mapping type
 */
public class JsonContentType implements UserType<JsonContent> {

	public static final JsonContentType INSTANCE = new JsonContentType();

	@Override
	public JsonContent nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
		final String cellContent = rs.getString(position);
		if (cellContent == null) {
			return null;
		}
		try {
			return JsonUtil.getMapper().readValue(cellContent.getBytes("UTF-8"), returnedClass());
		} catch (final Exception ex) {
			throw new RuntimeException("Failed to convert String to JsonObject: " + ex.getMessage(), ex);
		}
	}

	@Override
	public Serializable disassemble(JsonContent value) {
		if (value == null) {
			return null;
		}
		return JsonUtil.toJson(value);
	}

	@Override
	public void nullSafeSet(PreparedStatement st, JsonContent value, int index, SharedSessionContractImplementor session) throws SQLException {
		if (value == null) {
			st.setNull(index, SqlTypes.LONGVARCHAR);
			return;
		}
		try {
			final StringWriter w = new StringWriter();
			JsonUtil.getMapper().writeValue(w, value);
			w.flush();
			st.setObject(index, w.toString(), SqlTypes.LONGVARCHAR);
		} catch (final Exception ex) {
			throw new RuntimeException("Failed to convert JsonObject to String: " + ex.getMessage(), ex);
		}
	}

	@Override
    public JsonContent deepCopy(JsonContent value) {
		String value1 = JsonUtil.toJson(value);
        return JsonUtil.readValue(value1, JsonContent.class);
    }

	@Override
	public int getSqlType() {
		return SqlTypes.JSON;
	}

	@Override
	public Class<JsonContent> returnedClass() {
		return JsonContent.class;
	}

	@Override
	public boolean isMutable() {
		return true;
	}
}
