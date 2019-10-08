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

import org.apache.jena.rdfconnection.RDFConnection;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.app.BaseServiceBundler;
import org.trellisldp.app.DefaultConstraintServices;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.constraint.LdpConstraintService;
import org.trellisldp.event.jackson.DefaultActivityStreamService;
import org.trellisldp.ext.aws.DefaultNamespaceService;
import org.trellisldp.ext.aws.S3BinaryService;
import org.trellisldp.ext.aws.S3MementoService;
import org.trellisldp.ext.aws.SNSEventService;
import org.trellisldp.http.core.DefaultTimemapGenerator;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.rdfa.DefaultRdfaWriterService;
import org.trellisldp.triplestore.TriplestoreResourceService;

/**
 * A triplestore-based service bundler for Trellis.
 *
 * <p>This service bundler implementation is used with a Dropwizard-based application.
 * It combines a Triplestore-based resource service along with file-based binary and
 * memento storage. RDF processing is handled with Apache Jena.
 */
public class TrellisServiceBundler extends BaseServiceBundler {

    /** The configuration key for the Neptune URL. **/
    public static final String TRELLIS_NEPTUNE_URL = "trellis.neptune.url";

    /**
     * Create a new application service bundler.
     */
    public TrellisServiceBundler() {
        final NamespaceService nsService = new DefaultNamespaceService();
        final RDFConnection rdfConnection = connect(getConfig().getValue(TRELLIS_NEPTUNE_URL, String.class));
        resourceService = new TriplestoreResourceService(rdfConnection);
        ioService = new JenaIOService(nsService, new DefaultRdfaWriterService(nsService));
        auditService = new DefaultAuditService();
        mementoService = new S3MementoService();
        binaryService = new S3BinaryService();
        eventService = new SNSEventService(new DefaultActivityStreamService());
        timemapGenerator = new DefaultTimemapGenerator();
        constraintServices = new DefaultConstraintServices(singletonList(new LdpConstraintService()));
    }
}
