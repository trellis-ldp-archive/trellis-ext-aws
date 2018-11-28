/*
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package org.trellisldp.ext.aws.rds.lambda;

import static org.apache.tamaya.ConfigurationProvider.getConfiguration;

import org.apache.tamaya.Configuration;
import org.jdbi.v3.core.Jdbi;
import org.trellisldp.agent.SimpleAgentService;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.EventService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.ext.aws.S3BinaryService;
import org.trellisldp.ext.aws.S3MementoService;
import org.trellisldp.ext.aws.SNSEventService;
import org.trellisldp.ext.db.DBNamespaceService;
import org.trellisldp.ext.db.DBResourceService;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.rdfa.HtmlSerializer;

/**
 * An AWS-Based service bundler.
 */
public class AWSServiceBundler implements ServiceBundler {

    private static final Configuration config = getConfiguration();
    private AgentService agentService;
    private AuditService auditService;
    private BinaryService binaryService;
    private EventService eventService;
    private IOService ioService;
    private MementoService mementoService;
    private DBResourceService resourceService;

    /**
     * Get an AWS-based service bundler using Newton.
     */
    public AWSServiceBundler() {
        final Jdbi jdbi = Jdbi.create(config.get("trellis.jdbc.url"), config.get("trellis.jdbc.username"),
                config.get("trellis.jdbc.password"));
        final NamespaceService nsService = new DBNamespaceService(jdbi);
        agentService = new SimpleAgentService();
        eventService = new SNSEventService();
        binaryService = new S3BinaryService();
        mementoService = new S3MementoService();
        ioService = new JenaIOService(nsService, new HtmlSerializer(nsService));
        auditService = resourceService = new DBResourceService(jdbi);
    }

    @Override
    public AgentService getAgentService() {
        return agentService;
    }

    @Override
    public ResourceService getResourceService() {
        return resourceService;
    }

    @Override
    public AuditService getAuditService() {
        return auditService;
    }

    @Override
    public IOService getIOService() {
        return ioService;
    }

    @Override
    public MementoService getMementoService() {
        return mementoService;
    }

    @Override
    public BinaryService getBinaryService() {
        return binaryService;
    }

    @Override
    public EventService getEventService() {
        return eventService;
    }
}
