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
package org.trellisldp.ext.aws.rds.app;

import static com.google.common.cache.CacheBuilder.newBuilder;
import static java.util.Collections.singletonList;
import static java.util.concurrent.TimeUnit.HOURS;

import com.google.common.cache.Cache;

import io.dropwizard.jdbi3.JdbiFactory;
import io.dropwizard.setup.Environment;

import java.util.List;

import org.jdbi.v3.core.Jdbi;
import org.trellisldp.agent.SimpleAgentService;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.ConstraintService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.api.RDFaWriterService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.app.TrellisCache;
import org.trellisldp.constraint.LdpConstraints;
import org.trellisldp.ext.aws.AbstractAWSServiceBundler;
import org.trellisldp.ext.aws.S3MementoService;
import org.trellisldp.ext.db.DBNamespaceService;
import org.trellisldp.ext.db.DBResourceService;
import org.trellisldp.ext.db.DBWrappedMementoService;
import org.trellisldp.http.core.EtagGenerator;
import org.trellisldp.http.core.TimemapGenerator;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.rdfa.HtmlSerializer;

/**
 * An RDBMS-based service bundler for Trellis.
 *
 * <p>This service bundler implementation is used with a Dropwizard-based application.
 * It combines an RDBMS-based resource service along with S3-based binary and
 * memento storage. RDF processing is handled with Apache Jena.
 */
public class TrellisServiceBundler extends AbstractAWSServiceBundler {

    private DBResourceService resourceService;
    private AuditService auditService;
    private IOService ioService;
    private AgentService agentService;
    private MementoService mementoService;
    private List<ConstraintService> constraintServices;
    private TimemapGenerator timemapGenerator;
    private EtagGenerator etagGenerator;

    /**
     * Create a new application service bundler.
     * @param config the application configuration
     * @param environment the dropwizard environment
     */
    public TrellisServiceBundler(final AppConfiguration config, final Environment environment) {
        super();
        final Jdbi jdbi = new JdbiFactory().build(environment, config.getDataSourceFactory(), "trellis");

        auditService = resourceService = new DBResourceService(jdbi);
        ioService = buildIoService(config, jdbi);
        agentService = new SimpleAgentService();
        mementoService = new DBWrappedMementoService(jdbi, new S3MementoService());
        constraintServices = singletonList(new LdpConstraints());
        timemapGenerator = new TimemapGenerator() { };
        etagGenerator = new EtagGenerator() { };
    }

    @Override
    public ResourceService getResourceService() {
        return resourceService;
    }

    @Override
    public IOService getIOService() {
        return ioService;
    }

    @Override
    public AgentService getAgentService() {
        return agentService;
    }

    @Override
    public AuditService getAuditService() {
        return auditService;
    }

    @Override
    public MementoService getMementoService() {
        return mementoService;
    }

    @Override
    public Iterable<ConstraintService> getConstraintServices() {
        return constraintServices;
    }

    @Override
    public TimemapGenerator getTimemapGenerator() {
        return timemapGenerator;
    }

    @Override
    public EtagGenerator getEtagGenerator() {
        return etagGenerator;
    }

    private static IOService buildIoService(final AppConfiguration config, final Jdbi jdbi) {
        final long cacheSize = config.getJsonld().getCacheSize();
        final long hours = config.getJsonld().getCacheExpireHours();
        final Cache<String, String> cache = newBuilder().maximumSize(cacheSize).expireAfterAccess(hours, HOURS).build();
        final TrellisCache<String, String> profileCache = new TrellisCache<>(cache);
        final NamespaceService namespaceService = new DBNamespaceService(jdbi);
        final RDFaWriterService htmlSerializer = new HtmlSerializer(namespaceService, config.getAssets().getTemplate(),
                config.getAssets().getCss(), config.getAssets().getJs(), config.getAssets().getIcon());
        return new JenaIOService(namespaceService, htmlSerializer, profileCache,
                config.getJsonld().getContextWhitelist(), config.getJsonld().getContextDomainWhitelist());
    }
}
