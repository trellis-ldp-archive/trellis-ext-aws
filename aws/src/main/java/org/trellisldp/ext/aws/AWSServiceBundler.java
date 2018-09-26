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
package org.trellisldp.ext.aws;

import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.apache.tamaya.ConfigurationProvider.getConfiguration;

import org.apache.jena.rdfconnection.RDFConnection;
import org.trellisldp.agent.SimpleAgentService;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.EventService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.api.ServiceBundler;
import org.trellisldp.id.UUIDGenerator;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.rdfa.HtmlSerializer;
import org.trellisldp.triplestore.TriplestoreResourceService;

/**
 * An AWS-Based service bundler.
 */
public class AWSServiceBundler implements ServiceBundler {

    public static final String TRELLIS_NEPTUNE_URL = "trellis.neptune.url";

    private AgentService agentService;
    private AuditService auditService;
    private BinaryService binaryService;
    private EventService eventService;
    private IOService ioService;
    private MementoService mementoService;
    private TriplestoreResourceService resourceService;

    /**
     * Get an AWS-based service bundler using Newton.
     */
    public AWSServiceBundler() {
        final IdentifierService idService = new UUIDGenerator();
        final NamespaceService nsService = new NamespaceCacheService();
        final RDFConnection rdfConnection = connect(getConfiguration().get(TRELLIS_NEPTUNE_URL));

        eventService = new SNSEventService();
        agentService = new SimpleAgentService();
        binaryService = new S3BinaryService();
        mementoService = new S3MementoService();
        ioService = new JenaIOService(nsService, new HtmlSerializer(nsService), new ProfileCacheService());
        auditService = resourceService = new TriplestoreResourceService(rdfConnection, idService);
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
    public BinaryService getBinaryService() {
        return binaryService;
    }

    @Override
    public MementoService getMementoService() {
        return mementoService;
    }

    @Override
    public EventService getEventService() {
        return eventService;
    }
}
