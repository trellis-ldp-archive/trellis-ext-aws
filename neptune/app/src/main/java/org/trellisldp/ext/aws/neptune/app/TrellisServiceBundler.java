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
package org.trellisldp.ext.aws.neptune.app;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.concurrent.TimeUnit.HOURS;
import static org.apache.jena.rdfconnection.RDFConnectionFactory.connect;

import com.google.common.cache.Cache;

import io.dropwizard.setup.Environment;

import org.apache.jena.rdfconnection.RDFConnection;
import org.trellisldp.agent.SimpleAgentService;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.RDFaWriterService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.app.TrellisCache;
import org.trellisldp.ext.aws.AbstractAWSServiceBundler;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.namespaces.NamespacesJsonContext;
import org.trellisldp.rdfa.HtmlSerializer;
import org.trellisldp.triplestore.TriplestoreResourceService;

/**
 * A triplestore-based service bundler for Trellis.
 *
 * <p>This service bundler implementation is used with a Dropwizard-based application.
 * It combines a Triplestore-based resource service along with file-based binary and
 * memento storage. RDF processing is handled with Apache Jena.
 */
public class TrellisServiceBundler extends AbstractAWSServiceBundler {

    private final AuditService auditService;
    private final TriplestoreResourceService resourceService;
    private final AgentService agentService;
    private final IOService ioService;

    /**
     * Create a new application service bundler.
     * @param config the application configuration
     * @param environment the dropwizard environment
     */
    public TrellisServiceBundler(final AppConfiguration config, final Environment environment) {
        super();
        agentService = new SimpleAgentService();
        ioService = buildIoService(config);
        auditService = resourceService = buildResourceService(config, environment);
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
    public AgentService getAgentService() {
        return agentService;
    }

    @Override
    public IOService getIOService() {
        return ioService;
    }

    private static TriplestoreResourceService buildResourceService(final AppConfiguration config,
            final Environment environment) {
        final RDFConnection rdfConnection = connect(config.getResources());

        // Health checks
        environment.healthChecks().register("rdfconnection", new RDFConnectionHealthCheck(rdfConnection));
        return new TriplestoreResourceService(rdfConnection);
    }

    private static IOService buildIoService(final AppConfiguration config) {
        final Long cacheSize = config.getJsonld().getCacheSize();
        final Long hours = config.getJsonld().getCacheExpireHours();
        final Cache<String, String> cache = newBuilder().maximumSize(cacheSize).expireAfterAccess(hours, HOURS).build();
        final TrellisCache<String, String> profileCache = new TrellisCache<>(cache);
        final NamespaceService namespaceService = new NamespacesJsonContext(config.getNamespaces());
        final RDFaWriterService htmlSerializer = new HtmlSerializer(namespaceService, config.getAssets().getTemplate(),
                config.getAssets().getCss(), config.getAssets().getJs(), config.getAssets().getIcon());
        return new JenaIOService(namespaceService, htmlSerializer, profileCache,
                config.getJsonld().getContextWhitelist(), config.getJsonld().getContextDomainWhitelist());
    }
}
