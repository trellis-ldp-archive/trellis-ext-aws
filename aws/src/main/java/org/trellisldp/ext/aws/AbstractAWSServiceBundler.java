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

import org.trellisldp.api.BinaryService;
import org.trellisldp.api.EventService;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.ServiceBundler;

/**
 * An AWS-based service bundler for Trellis.
 *
 * <p>This service bundler provides the basis for specialized application environments.
 */
public abstract class AbstractAWSServiceBundler implements ServiceBundler {

    private final MementoService mementoService;
    private final BinaryService binaryService;
    private final EventService eventService;

    /**
     * Create a new application service bundler.
     */
    public AbstractAWSServiceBundler() {
        this.mementoService = new S3MementoService();
        this.binaryService = new S3BinaryService();
        this.eventService = new SNSEventService();
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
