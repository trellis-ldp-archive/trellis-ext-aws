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

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.codahale.metrics.health.HealthCheckRegistry;

import io.dropwizard.configuration.YamlConfigurationFactory;
import io.dropwizard.jackson.Jackson;
import io.dropwizard.jersey.validation.Validators;
import io.dropwizard.setup.Environment;

import java.io.File;

import org.junit.jupiter.api.Test;
import org.trellisldp.http.core.ServiceBundler;

public class TrellisServiceBundlerTest {

    @Test
    public void testServiceBundler() throws Exception {
        final AppConfiguration config = new YamlConfigurationFactory<>(AppConfiguration.class,
                Validators.newValidator(), Jackson.newMinimalObjectMapper(), "")
            .build(new File(getClass().getResource("/config.yml").toURI()));

        config.setNamespaces(resourceFilePath("data/namespaces.json"));

        final Environment mockEnv = mock(Environment.class);
        final HealthCheckRegistry mockHealthChecks = mock(HealthCheckRegistry.class);

        when(mockEnv.healthChecks()).thenReturn(mockHealthChecks);

        final ServiceBundler bundler = new TrellisServiceBundler(config, mockEnv);

        assertNotNull(bundler.getBinaryService(), "Missing binary service!");
        assertNotNull(bundler.getEventService(), "Missing event service!");
        assertNotNull(bundler.getAgentService(), "Missing agent service!");
        assertNotNull(bundler.getAuditService(), "Missing audit service!");
        assertNotNull(bundler.getIOService(), "Missing I/O service!");
        assertNotNull(bundler.getMementoService(), "Missing memento service!");
        assertNotNull(bundler.getResourceService(), "Missing resource service!");
        assertEquals(bundler.getResourceService(), bundler.getAuditService(), "Incorrect audit/resource services!");

        verify(mockHealthChecks).register(eq("rdfconnection"), any(RDFConnectionHealthCheck.class));
    }
}
