package io.vertx.spi.cluster.hazelcast.impl;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.shareddata.impl.ClusterSerializable;

import java.io.IOException;

public class ConversionUtils {

  @SuppressWarnings("unchecked")
  public static <T> T convertParam(T obj) {
    if (obj instanceof ClusterSerializable) {
      ClusterSerializable cobj = (ClusterSerializable) obj;
      return (T) (new DataSerializableHolder(cobj));
    } else {
      return obj;
    }
  }

  @SuppressWarnings("unchecked")
  public static <T> T convertReturn(Object obj) {
    if (obj instanceof DataSerializableHolder) {
      DataSerializableHolder cobj = (DataSerializableHolder) obj;
      return (T) cobj.clusterSerializable();
    } else {
      return (T) obj;
    }
  }

  @SuppressWarnings("unchecked")
  private static final class DataSerializableHolder implements DataSerializable {

    private ClusterSerializable clusterSerializable;

    public DataSerializableHolder() {
    }

    private DataSerializableHolder(ClusterSerializable clusterSerializable) {
      this.clusterSerializable = clusterSerializable;
    }

    @Override
    public void writeData(ObjectDataOutput objectDataOutput) throws IOException {
      objectDataOutput.writeUTF(clusterSerializable.getClass().getName());
      Buffer buffer = Buffer.buffer();
      clusterSerializable.writeToBuffer(buffer);
      byte[] bytes = buffer.getBytes();
      objectDataOutput.writeInt(bytes.length);
      objectDataOutput.write(bytes);
    }

    @Override
    public void readData(ObjectDataInput objectDataInput) throws IOException {
      String className = objectDataInput.readUTF();
      int length = objectDataInput.readInt();
      byte[] bytes = new byte[length];
      objectDataInput.readFully(bytes);
      try {
        Class<?> clazz = Thread.currentThread().getContextClassLoader().loadClass(className);
        clusterSerializable = (ClusterSerializable) clazz.newInstance();
        clusterSerializable.readFromBuffer(0, Buffer.buffer(bytes));
      } catch (Exception e) {
        throw new IllegalStateException("Failed to load class " + e.getMessage(), e);
      }
    }

    @Override
    public boolean equals(Object o) {
      if (this == o) return true;
      if (!(o instanceof DataSerializableHolder)) return false;
      DataSerializableHolder that = (DataSerializableHolder) o;
      if (clusterSerializable != null ? !clusterSerializable.equals(that.clusterSerializable) : that.clusterSerializable != null) {
        return false;
      }
      return true;
    }

    @Override
    public int hashCode() {
      return clusterSerializable != null ? clusterSerializable.hashCode() : 0;
    }

    public ClusterSerializable clusterSerializable() {
      return clusterSerializable;
    }
  }
}
