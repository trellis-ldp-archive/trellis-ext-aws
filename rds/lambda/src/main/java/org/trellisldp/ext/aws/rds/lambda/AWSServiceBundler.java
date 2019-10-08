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

import static java.util.Collections.singletonList;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;

import org.eclipse.microprofile.config.Config;
import org.jdbi.v3.core.Jdbi;
import org.trellisldp.api.NamespaceService;
import org.trellisldp.app.BaseServiceBundler;
import org.trellisldp.app.DefaultConstraintServices;
import org.trellisldp.audit.DefaultAuditService;
import org.trellisldp.constraint.LdpConstraintService;
import org.trellisldp.event.jackson.DefaultActivityStreamService;
import org.trellisldp.ext.aws.S3BinaryService;
import org.trellisldp.ext.aws.S3MementoService;
import org.trellisldp.ext.aws.SNSEventService;
import org.trellisldp.ext.db.DBNamespaceService;
import org.trellisldp.ext.db.DBResourceService;
import org.trellisldp.ext.db.DBWrappedMementoService;
import org.trellisldp.http.core.DefaultTimemapGenerator;
import org.trellisldp.io.JenaIOService;
import org.trellisldp.rdfa.DefaultRdfaWriterService;

/**
 * An AWS-Based service bundler.
 */
public class AWSServiceBundler extends BaseServiceBundler {

    /**
     * Get an AWS-based service bundler using Newton.
     */
    public AWSServiceBundler() {
        final Config config = getConfig();
        final Jdbi jdbi = Jdbi.create(config.getValue("trellis.jdbc.url", String.class),
                config.getValue("trellis.jdbc.username", String.class),
                config.getValue("trellis.jdbc.password", String.class));
        final NamespaceService nsService = new DBNamespaceService(jdbi);

        resourceService = new DBResourceService(jdbi);
        ioService = new JenaIOService(nsService, new DefaultRdfaWriterService(nsService));
        auditService = new DefaultAuditService();
        mementoService = new DBWrappedMementoService(jdbi, new S3MementoService());
        binaryService = new S3BinaryService();
        eventService = new SNSEventService(new DefaultActivityStreamService());
        timemapGenerator = new DefaultTimemapGenerator();
        constraintServices = new DefaultConstraintServices(singletonList(new LdpConstraintService()));
    }
}
