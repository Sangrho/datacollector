/**
 * Copyright 2015 StreamSets Inc.
 *
 * Licensed under the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.streamsets.pipeline.stage.destination.elasticsearch;

import com.google.common.base.Optional;
import com.streamsets.pipeline.api.Config;
import com.streamsets.pipeline.api.StageException;
import com.streamsets.pipeline.api.StageUpgrader;
import com.streamsets.pipeline.api.impl.Utils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ElasticSearchDTargetUpgrader implements StageUpgrader {

  @Override
  public List<Config> upgrade(
      String library,
      String stageName,
      String stageInstance,
      int fromVersion,
      int toVersion,
      List<Config> configs
  ) throws StageException {
    switch(fromVersion) {
      case 1:
        upgradeV1ToV2(configs);
        // fall through
      case 2:
        upgradeV2ToV3(configs);
        // fall through
      case 3:
        upgradeV3ToV4(configs);
        // fall through
      case 4:
        upgradeV4ToV5(configs);
        // fall through
      case 5:
        upgradeV5ToV6(configs);
        break;
      default:
        throw new IllegalStateException(Utils.format("Unexpected fromVersion {}", fromVersion));
    }
    return configs;
  }

  private static void upgradeV1ToV2(List<Config> configs) {
    configs.add(new Config("timeDriver", "${time:now()}"));
    configs.add(new Config("timeZoneID", "UTC"));
  }

  @SuppressWarnings("unchecked")
  private void upgradeV2ToV3(List<Config> configs) {
    List<Config> configsToRemove = new ArrayList<>();
    List<Config> configsToAdd = new ArrayList<>();

    for (Config config : configs) {
      switch (config.getName()) {
        case "clusterName":
        case "uris":
        case "timeDriver":
        case "timeZoneID":
        case "indexTemplate":
        case "typeTemplate":
        case "docIdTemplate":
        case "charset":
          configsToAdd.add(new Config(ElasticSearchConfigBean.CONF_PREFIX + config.getName(), config.getValue()));
          configsToRemove.add(config);
          break;
        case "configs":
          // Remove client.transport.sniff from the additional configs when loading an old pipeline.
          // This config is disabled by default, and it should be enabled only when the user explicitly
          // checks a new checkbox in the UI.
          List<Map<String, String>> keyValues = (List<Map<String, String>>) config.getValue();
          Map<String, String> clientSniffKeyValue = null;
          for (Map<String, String> keyValue : keyValues) {
            for (Map.Entry entry : keyValue.entrySet()) {
              if (entry.getValue().equals("client.transport.sniff")) {
                clientSniffKeyValue = keyValue;
              }
            }
          }
          if (clientSniffKeyValue != null) {
            keyValues.remove(clientSniffKeyValue);
          }
          configsToAdd.add(new Config(ElasticSearchConfigBean.CONF_PREFIX + config.getName(), keyValues));
          configsToRemove.add(config);
          break;
        default:
          // no op
      }
    }
    configs.addAll(configsToAdd);
    configs.removeAll(configsToRemove);
  }

  private static void upgradeV3ToV4(List<Config> configs) {
    configs.add(new Config(ElasticSearchConfigBean.CONF_PREFIX + "httpUri", ElasticSearchConfigBean.DEFAULT_HTTP_URI));
  }

  private static void upgradeV4ToV5(List<Config> configs) {
    List<Config> configsToRemove = new ArrayList<>();
    List<Config> configsToAdd = new ArrayList<>();

    // Rename useFound to useElasticCloud.
    for (Config config : configs) {
      if (config.getName().equals(ElasticSearchConfigBean.CONF_PREFIX + "useFound")) {
        configsToAdd.add(new Config(config.getName().replace("useFound", "useElasticCloud"), config.getValue()));
        configsToRemove.add(config);
        break;
      }
    }

    configs.addAll(configsToAdd);
    configs.removeAll(configsToRemove);
  }

  private static void upgradeV5ToV6(List<Config> configs) {
    List<Config> configsToRemove = new ArrayList<>();
    List<Config> configsToAdd = new ArrayList<>();

    for (Config config : configs) {
      // Rename shieldConfigBean to securityConfigBean.
      if (config.getName().equals(ElasticSearchConfigBean.CONF_PREFIX + "shieldConfigBean.shieldUser")) {
        configsToAdd.add(new Config(config.getName().replace("shield", "security"), config.getValue()));
        configsToRemove.add(config);
      }
      // Remove shieldTransportSsl.
      if (config.getName().equals(ElasticSearchConfigBean.CONF_PREFIX + "shieldConfigBean.shieldTransportSsl")) {
        configsToRemove.add(config);
      }
      // Remove sslKeystorePath.
      if (config.getName().equals(ElasticSearchConfigBean.CONF_PREFIX + "shieldConfigBean.sslKeystorePath")) {
        configsToRemove.add(config);
      }
      // Remove sslKeystorePassword.
      if (config.getName().equals(ElasticSearchConfigBean.CONF_PREFIX + "shieldConfigBean.sslKeystorePassword")) {
        configsToRemove.add(config);
      }
      // Rename shieldConfigBean to securityConfigBean.
      if (config.getName().equals(ElasticSearchConfigBean.CONF_PREFIX + "shieldConfigBean.sslTruststorePath")) {
        configsToAdd.add(new Config(config.getName().replace("shield", "security"), config.getValue()));
        configsToRemove.add(config);
      }
      // Rename shieldConfigBean to securityConfigBean.
      if (config.getName().equals(ElasticSearchConfigBean.CONF_PREFIX + "shieldConfigBean.sslTruststorePassword")) {
        configsToAdd.add(new Config(config.getName().replace("shield", "security"), config.getValue()));
        configsToRemove.add(config);
      }
      // Remove clusterName.
      if (config.getName().equals(ElasticSearchConfigBean.CONF_PREFIX + "clusterName")) {
        configsToRemove.add(config);
      }
      // Remove uris.
      if (config.getName().equals(ElasticSearchConfigBean.CONF_PREFIX + "uris")) {
        configsToRemove.add(config);
      }
      // Rename httpUri to httpUris.
      if (config.getName().equals(ElasticSearchConfigBean.CONF_PREFIX + "httpUri")) {
        String configValue = Optional.fromNullable((String)config.getValue()).or(ElasticSearchConfigBean.DEFAULT_HTTP_URI);
        if (!configValue.isEmpty() && !configValue.equals(ElasticSearchConfigBean.DEFAULT_HTTP_URI)) {
          configsToAdd.add(new Config(ElasticSearchConfigBean.CONF_PREFIX + "httpUris", Arrays.asList(configValue)));
        }
        configsToRemove.add(config);
      }
      // Rename useShield to useSecurity.
      if (config.getName().equals(ElasticSearchConfigBean.CONF_PREFIX + "useShield")) {
        configsToAdd.add(new Config(config.getName().replace("Shield", "Security"), config.getValue()));
        configsToRemove.add(config);
      }
      // Remove useElasticCloud.
      if (config.getName().equals(ElasticSearchConfigBean.CONF_PREFIX + "useElasticCloud")) {
        configsToRemove.add(config);
      }
      // Rename configs to params.
      if (config.getName().equals(ElasticSearchConfigBean.CONF_PREFIX + "configs")) {
        configsToAdd.add(new Config(config.getName().replace("configs", "params"), config.getValue()));
        configsToRemove.add(config);
      }
      // Remove upsert.
      if (config.getName().equals(ElasticSearchConfigBean.CONF_PREFIX + "upsert")) {
        if ((Boolean)config.getValue()) {
          configsToAdd.add(new Config(ElasticSearchConfigBean.CONF_PREFIX + "defaultOperation", "INDEX"));
        } else {
          configsToAdd.add(new Config(ElasticSearchConfigBean.CONF_PREFIX + "defaultOperation", "CREATE"));
        }
        configsToRemove.add(config);
      }
    }

    configs.addAll(configsToAdd);
    configs.removeAll(configsToRemove);
  }

}
