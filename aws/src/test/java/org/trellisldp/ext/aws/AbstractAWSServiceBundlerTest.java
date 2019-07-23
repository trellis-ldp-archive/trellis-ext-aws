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

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Test;
import org.trellisldp.api.AgentService;
import org.trellisldp.api.AuditService;
import org.trellisldp.api.IOService;
import org.trellisldp.api.ResourceService;
import org.trellisldp.http.core.ServiceBundler;

public class AbstractAWSServiceBundlerTest {

    private static class TestServiceBundler extends AbstractAWSServiceBundler {

        private final AuditService auditService;
        private final ResourceService resourceService;
        private final AgentService agentService;
        private final IOService ioService;

        public TestServiceBundler(final AuditService auditService, final ResourceService resourceService,
                final IOService ioService, final AgentService agentService) {
            super();
            this.ioService = ioService;
            this.agentService = agentService;
            this.auditService = auditService;
            this.resourceService = resourceService;
        }

        @Override
        public ResourceService getResourceService() {
            return resourceService;
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
        public IOService getIOService() {
            return ioService;
        }
    }

    @Test
    public void testServiceBundler() throws Exception {
        final AuditService mockAuditService = mock(AuditService.class);
        final ResourceService mockResourceService = mock(ResourceService.class);
        final IOService mockIOService = mock(IOService.class);
        final AgentService mockAgentService = mock(AgentService.class);

        final ServiceBundler bundler = new TestServiceBundler(mockAuditService,
                mockResourceService, mockIOService, mockAgentService);

        assertNotNull(bundler.getMementoService(), "Missing memento service!");
        assertNotNull(bundler.getBinaryService(), "Missing binary service!");
        assertNotNull(bundler.getEventService(), "Missing event service!");
        assertEquals(mockAgentService, bundler.getAgentService(), "Missing agent service!");
        assertEquals(mockAuditService, bundler.getAuditService(), "Missing audit service!");
        assertEquals(mockIOService, bundler.getIOService(), "Missing I/O service!");
        assertEquals(mockResourceService, bundler.getResourceService(), "Missing resource service!");
    }
}
