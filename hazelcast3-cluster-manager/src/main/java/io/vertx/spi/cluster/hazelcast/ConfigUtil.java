/*
 * Copyright 2018 Red Hat, Inc.
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

package io.vertx.spi.cluster.hazelcast;

import com.hazelcast.config.Config;
import com.hazelcast.config.XmlConfigBuilder;
import io.vertx.core.impl.logging.Logger;
import io.vertx.core.impl.logging.LoggerFactory;

import java.io.*;

/**
 * @author Thomas Segismont
 */
public class ConfigUtil {

  private static final Logger log = LoggerFactory.getLogger(ConfigUtil.class);

  // Hazelcast config file
  private static final String DEFAULT_CONFIG_FILE = "default-cluster.xml";
  private static final String CONFIG_FILE = "cluster.xml";

  /**
   * Loads Hazelcast config XML and transform it into a {@link Config} object.
   *
   * The content is read from:
   * <ol>
   * <li>the location denoted by the {@code vertx.hazelcast.config} sysprop, if present, or</li>
   * <li>the {@code cluster.xml} file on the classpath, if present, or</li>
   * <li>the default config file</li>
   * </ol>
   *
   * @return a config object
   */
  public static Config loadConfig() {
    Config cfg = null;
    try (InputStream is = getConfigStream();
         InputStream bis = new BufferedInputStream(is)) {
      cfg = new XmlConfigBuilder(bis).build();
    } catch (IOException ex) {
      log.error("Failed to read config", ex);
    }
    return cfg;
  }

  private static InputStream getConfigStream() {
    InputStream is = getConfigStreamFromSystemProperty();
    if (is == null) {
      is = getConfigStreamFromClasspath(CONFIG_FILE, DEFAULT_CONFIG_FILE);
    }
    return is;
  }

  private static InputStream getConfigStreamFromSystemProperty() {
    String configProp = System.getProperty("vertx.hazelcast.config");
    InputStream is = null;
    if (configProp != null) {
      if (configProp.startsWith("classpath:")) {
        return getConfigStreamFromClasspath(configProp.substring("classpath:".length()), CONFIG_FILE);
      }
      File cfgFile = new File(configProp);
      if (cfgFile.exists()) {
        try {
          is = new FileInputStream(cfgFile);
        } catch (FileNotFoundException ex) {
          log.warn("Failed to open file '" + configProp + "' defined in 'vertx.hazelcast.config'. Continuing " +
            "classpath search for " + CONFIG_FILE);
        }
      }
    }
    return is;
  }

  private static InputStream getConfigStreamFromClasspath(String configFile, String defaultConfig) {
    InputStream is = null;
    ClassLoader ctxClsLoader = Thread.currentThread().getContextClassLoader();
    if (ctxClsLoader != null) {
      is = ctxClsLoader.getResourceAsStream(configFile);
    }
    if (is == null) {
      is = ConfigUtil.class.getClassLoader().getResourceAsStream(configFile);
      if (is == null) {
        is = ConfigUtil.class.getClassLoader().getResourceAsStream(defaultConfig);
      }
    }
    return is;
  }

  private ConfigUtil() {
    // Utility class
  }
}
