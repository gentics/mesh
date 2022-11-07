/*
 * Copyright 2020 Red Hat, Inc.
 *
 * Red Hat licenses this file to you under the Apache License, version 2.0
 * (the "License"); you may not use this file except in compliance with the
 * License.  You may obtain a copy of the License at:
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.  See the
 * License for the specific language governing permissions and limitations
 * under the License.
 */

package io.vertx.spi.cluster.hazelcast.impl;

import com.hazelcast.nio.ObjectDataInput;
import com.hazelcast.nio.ObjectDataOutput;
import com.hazelcast.nio.serialization.DataSerializable;
import io.vertx.core.buffer.Buffer;
import io.vertx.core.json.JsonObject;
import io.vertx.core.spi.cluster.NodeInfo;

import java.io.IOException;

/**
 * @author Thomas Segismont
 */
public class HazelcastNodeInfo implements DataSerializable {

  private NodeInfo nodeInfo;

  public HazelcastNodeInfo() {
  }

  public HazelcastNodeInfo(NodeInfo nodeInfo) {
    this.nodeInfo = nodeInfo;
  }

  @Override
  public void writeData(ObjectDataOutput dataOutput) throws IOException {
    dataOutput.writeUTF(nodeInfo.host());
    dataOutput.writeInt(nodeInfo.port());
    JsonObject metadata = nodeInfo.metadata();
    dataOutput.writeByteArray(metadata != null ? metadata.toBuffer().getBytes() : null);
  }

  @Override
  public void readData(ObjectDataInput dataInput) throws IOException {
    String host = dataInput.readUTF();
    int port = dataInput.readInt();
    byte[] bytes = dataInput.readByteArray();
    nodeInfo = new NodeInfo(host, port, bytes != null ? new JsonObject(Buffer.buffer(bytes)) : null);
  }

  public NodeInfo unwrap() {
    return nodeInfo;
  }
}
