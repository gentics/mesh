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
import io.vertx.core.spi.cluster.RegistrationInfo;

import java.io.IOException;
import java.util.Objects;

/**
 * @author Thomas Segismont
 */
public class HazelcastRegistrationInfo implements DataSerializable {

  private RegistrationInfo registrationInfo;

  public HazelcastRegistrationInfo() {
  }

  public HazelcastRegistrationInfo(RegistrationInfo registrationInfo) {
    this.registrationInfo = Objects.requireNonNull(registrationInfo);
  }

  public RegistrationInfo unwrap() {
    return registrationInfo;
  }

  @Override
  public void writeData(ObjectDataOutput dataOutput) throws IOException {
    dataOutput.writeUTF(registrationInfo.nodeId());
    dataOutput.writeLong(registrationInfo.seq());
    dataOutput.writeBoolean(registrationInfo.localOnly());
  }

  @Override
  public void readData(ObjectDataInput dataInput) throws IOException {
    registrationInfo = new RegistrationInfo(dataInput.readUTF(), dataInput.readLong(), dataInput.readBoolean());
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) return true;
    if (o == null || getClass() != o.getClass()) return false;

    HazelcastRegistrationInfo that = (HazelcastRegistrationInfo) o;

    return registrationInfo.equals(that.registrationInfo);
  }

  @Override
  public int hashCode() {
    return registrationInfo.hashCode();
  }

  @Override
  public String toString() {
    return registrationInfo.toString();
  }
}
