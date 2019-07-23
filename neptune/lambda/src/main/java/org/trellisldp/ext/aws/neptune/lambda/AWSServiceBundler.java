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
package org.trellisldp.ext.aws.neptune.lambda;

import static java.util.Collections.singletonList;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;

import java.util.List;

import org.apache.jena.rdfconnection.RDFConnection;
import org.trellisldp.agent.SimpleAgentService;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.ConstraintService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.constraint.LdpConstraints;
import org.trellisldp.ext.aws.AbstractAWSServiceBundler;
import org.trellisldp.ext.aws.SimpleNamespaceService;
import org.trellisldp.http.core.EtagGenerator;
import org.trellisldp.http.core.TimemapGenerator;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.rdfa.HtmlSerializer;
import org.trellisldp.triplestore.TriplestoreResourceService;

/**
 * An AWS-Based service bundler.
 */
public class AWSServiceBundler extends AbstractAWSServiceBundler {

    /** The configuration key for the Neptune URL. **/
    public static final String TRELLIS_NEPTUNE_URL = "trellis.neptune.url";

    private AgentService agentService;
    private AuditService auditService;
    private IOService ioService;
    private TriplestoreResourceService resourceService;
    private List<ConstraintService> constraintServices;
    private TimemapGenerator timemapGenerator;
    private EtagGenerator etagGenerator;

    /**
     * Get an AWS-based service bundler using Newton.
     */
    public AWSServiceBundler() {
        super();
        final RDFConnection rdfConnection = connect(getConfig().getValue(TRELLIS_NEPTUNE_URL, String.class));
        final NamespaceService nsService = new SimpleNamespaceService();

        agentService = new SimpleAgentService();
        ioService = new JenaIOService(nsService, new HtmlSerializer(nsService));
        auditService = resourceService = new TriplestoreResourceService(rdfConnection);
        constraintServices = singletonList(new LdpConstraints());
        timemapGenerator = new TimemapGenerator() { };
        etagGenerator = new EtagGenerator() { };
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
    public TimemapGenerator getTimemapGenerator() {
        return timemapGenerator;
    }

    @Override
    public EtagGenerator getEtagGenerator() {
        return etagGenerator;
    }

    @Override
    public Iterable<ConstraintService> getConstraintServices() {
        return constraintServices;
    }
}
