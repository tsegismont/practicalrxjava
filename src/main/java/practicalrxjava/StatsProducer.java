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

import javax.annotation.Resource;
import javax.inject.Inject;
import javax.jms.JMSContext;
import javax.jms.JMSDestinationDefinition;
import javax.jms.Topic;
import javax.servlet.ServletContextEvent;
import javax.servlet.ServletContextListener;
import javax.servlet.annotation.WebListener;

/**
 * @author Thomas Segismont
 */
@JMSDestinationDefinition(name = "java:/topic/stats", interfaceName = "javax.jms.Topic", destinationName = "StatsTopic")
@WebListener
public class StatsProducer implements ServletContextListener {

  @Inject
  JMSContext context;

  @Resource(lookup = "java:/topic/stats")
  Topic topic;

  @Override
  public void contextInitialized(ServletContextEvent sce) {
    // FIXME
  }

  @Override
  public void contextDestroyed(ServletContextEvent sce) {
    // FIXME
  }
}
