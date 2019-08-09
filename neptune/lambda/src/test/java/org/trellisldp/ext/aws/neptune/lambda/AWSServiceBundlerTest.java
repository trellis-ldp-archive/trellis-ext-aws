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

import static org.junit.jupiter.api.Assertions.*;

import org.junit.jupiter.api.Test;
import org.trellisldp.http.core.ServiceBundler;

public class AWSServiceBundlerTest {

    @Test
    public void testServiceBundler() {
        final ServiceBundler bundler = new AWSServiceBundler();

        assertNotNull(bundler.getBinaryService(), "Missing binary service!");
        assertNotNull(bundler.getEventService(), "Missing event service!");
        assertNotNull(bundler.getAuditService(), "Missing audit service!");
        assertNotNull(bundler.getMementoService(), "Missing memento service!");
        assertNotNull(bundler.getIOService(), "Missing I/O service!");
        assertNotNull(bundler.getResourceService(), "Missing resource service!");
        assertNotNull(bundler.getAgentService(), "Missing agent service!");
        assertNotNull(bundler.getConstraintServices(), "Missing constraint services!");
        assertNotNull(bundler.getTimemapGenerator(), "Missing timemap generator!");
        assertNotNull(bundler.getEtagGenerator(), "Missing etag generator!");
    }
}
