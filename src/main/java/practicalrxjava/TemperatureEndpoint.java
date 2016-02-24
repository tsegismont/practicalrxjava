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

import com.datastax.driver.core.PreparedStatement;
import com.datastax.driver.core.ResultSet;
import com.datastax.driver.core.Session;
import com.datastax.driver.core.utils.UUIDs;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.google.common.base.Throwables;
import org.hawkular.rx.cassandra.driver.RxSession;
import org.hawkular.rx.cassandra.driver.RxSessionImpl;
import rx.Observable;

import javax.annotation.PostConstruct;
import javax.enterprise.context.ApplicationScoped;
import javax.inject.Inject;
import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Properties;

/**
 * @author Thomas Segismont
 */
@Path("/temperature")
@ApplicationScoped
public class TemperatureEndpoint {

  ObjectMapper mapper;
  Properties queries;
  PreparedStatement insertData;
  PreparedStatement findDataByDateRange;
  RxSession rxSession;

  @Inject
  Session session;

  @PostConstruct
  void init() {
    mapper = new ObjectMapper();
    queries = new Properties();
    loadQueries();
    insertData = session.prepare(queries.getProperty("insertData"));
    findDataByDateRange = session.prepare(queries.getProperty("findDataByDateRange"));
    rxSession = new RxSessionImpl(session);
  }

  private void loadQueries() {
    ClassLoader classLoader = getClass().getClassLoader();
    try (InputStream stream = classLoader.getResourceAsStream("practicalrxjava/queries.properties")) {
      queries.load(stream);
    } catch (IOException e) {
      throw new RuntimeException(e);
    }
  }

  @POST
  @Path("/data")
  @Consumes("application/json")
  public void addDataPoint(@Suspended AsyncResponse asyncResponse, JsonNode json) {
    Observable.just(json)
      .map(jsonNode -> {
        String city = json.get("city").textValue();
        double value = json.get("value").doubleValue();
        long timestamp = System.currentTimeMillis();

        return insertData.bind(value, city, UUIDGen.getTimeUUID(timestamp));
      })
      .flatMap(rxSession::execute)
      .map(ResultSet::wasApplied)
      .subscribe(wasApplied -> {
        if (wasApplied) {
          asyncResponse.resume(Response.ok().build());
        } else {
          asyncResponse.resume(Response.serverError().build());
        }
      }, t -> {
        asyncResponse.resume(Response.serverError().entity(Throwables.getStackTraceAsString(t)).build());
      });
  }

  @GET
  @Path("/data")
  @Produces("application/json")
  public void getDataPoints(@Suspended AsyncResponse asyncResponse, @QueryParam("city") String city, @QueryParam("from") String from, @QueryParam("to") String to) {
    long start = toTimestamp(from);
    long end = toTimestamp(to);
    Observable.just(findDataByDateRange.bind(city, UUIDGen.getTimeUUID(start), UUIDGen.getTimeUUID(end)))
      .flatMap(rxSession::execute)
      .flatMap(Observable::from)
      .map(row -> {
        ObjectNode objectNode = mapper.createObjectNode();
        objectNode.put("time", UUIDs.unixTimestamp(row.getUUID("time")));
        objectNode.put("value", row.getDouble("value"));
        return objectNode;
      })
      .collect(() -> mapper.createArrayNode(), ArrayNode::add)
      .subscribe(jsonNode -> asyncResponse.resume(Response.ok(jsonNode).build()), t -> {
        asyncResponse.resume(Response.serverError().entity(Throwables.getStackTraceAsString(t)).build());
    });
  }

  private long toTimestamp(String localDateTimeString) {
    return LocalDateTime.parse(localDateTimeString).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli();
  }
}
