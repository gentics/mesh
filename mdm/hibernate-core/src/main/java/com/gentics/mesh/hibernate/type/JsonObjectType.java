package com.gentics.mesh.hibernate.type;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.StringWriter;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Types;

import org.hibernate.HibernateException;
import org.hibernate.engine.spi.SharedSessionContractImplementor;
import org.hibernate.type.SqlTypes;
import org.hibernate.usertype.UserType;

import com.gentics.mesh.json.JsonUtil;

import io.vertx.core.json.JsonObject;

/**
 * JSON object mapping type
 */
public class JsonObjectType implements UserType<JsonObject> {

	public static final JsonObjectType INSTANCE = new JsonObjectType();

	@Override
	public JsonObject nullSafeGet(ResultSet rs, int position, SharedSessionContractImplementor session, Object owner) throws SQLException {
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
	public void nullSafeSet(PreparedStatement st, JsonObject value, int index, SharedSessionContractImplementor session) throws SQLException {
		if (value == null) {
			st.setNull(index, Types.OTHER);
			return;
		}
		try {
			final StringWriter w = new StringWriter();
			JsonUtil.getMapper().writeValue(w, value);
			w.flush();
			st.setObject(index, w.toString(), Types.OTHER);
		} catch (final Exception ex) {
			throw new RuntimeException("Failed to convert MyJson to JsonObject: " + ex.getMessage(), ex);
		}
	}

	@Override
    public JsonObject deepCopy(JsonObject value) {
        try {
            // use serialization to create a deep copy
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            ObjectOutputStream oos = new ObjectOutputStream(bos);
            oos.writeObject(value);
            oos.flush();
            oos.close();
            bos.close();
             
            ByteArrayInputStream bais = new ByteArrayInputStream(bos.toByteArray());
            JsonObject obj = (JsonObject)new ObjectInputStream(bais).readObject();
            bais.close();
            return obj;
        } catch (ClassNotFoundException | IOException ex) {
            throw new HibernateException(ex);
        }
    }

	@Override
	public int getSqlType() {
		return SqlTypes.JSON;
	}

	@Override
	public Class<JsonObject> returnedClass() {
		return JsonObject.class;
	}

	@Override
	public boolean isMutable() {
		return true;
	}
}
