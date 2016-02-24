/*
 * Copyright 2016 Red Hat, Inc. and/or its affiliates
 * and other contributors as indicated by the @author tags.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package practicalrxjava;

import com.datastax.driver.core.Cluster;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.ejb.Singleton;
import javax.ejb.Startup;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.Produces;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * @author Thomas Segismont
 */
@Singleton
@Startup
public class Initializer {

  Properties schemaCQL;

  Session session;

  @PostConstruct
  void init() {
    schemaCQL = new Properties();
    loadSchemaCQL();

    session = createSession();
    if (!schemaReady()) {
      initSchema();
    }
    session.execute("USE " + schemaCQL.getProperty("keyspace"));
  }

  private void loadSchemaCQL() {
    ClassLoader classLoader = getClass().getClassLoader();
    try (InputStream stream = classLoader.getResourceAsStream("practicalrxjava/schema.properties")) {
      schemaCQL.load(stream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  private boolean schemaReady() {
    ResultSet resultSet = session.execute(schemaCQL.getProperty("selectKeyspace"));
    return !resultSet.isExhausted();
  }

  private void initSchema() {
    session.execute(schemaCQL.getProperty("createKeyspace"));
    session.execute(schemaCQL.getProperty("createDataTable"));
  }

  private Session createSession() {
    Cluster cluster = new Cluster.Builder()
      .addContactPoint("127.0.0.1")
      .withPort(9042)
      .build();

    cluster.init();

    Session createdSession = null;
    try {
      createdSession = cluster.connect("system");
      return createdSession;
    } finally {
      if (createdSession == null) {
        cluster.close();
      }
    }
  }

  @Produces
  @ApplicationScoped
  public Session getSession() {
    return session;
  }

  @PreDestroy
  void shutdown() {
    if (session != null) {
      session.close();
      session.getCluster().close();
    }
  }
}
