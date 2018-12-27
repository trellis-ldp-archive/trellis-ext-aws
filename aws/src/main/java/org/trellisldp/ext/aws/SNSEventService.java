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

import static com.amazonaws.services.sns.AmazonSNSClientBuilder.defaultClient;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.ServiceLoader.load;
import static org.apache.tamaya.ConfigurationProvider.getConfiguration;
import static org.slf4j.LoggerFactory.getLogger;

import com.amazonaws.services.sns.AmazonSNS;

import java.util.Iterator;
import java.util.ServiceLoader;

import org.slf4j.Logger;
import org.trellisldp.api.ActivityStreamService;
import org.trellisldp.api.Event;
import org.trellisldp.api.EventService;
import org.trellisldp.api.RuntimeTrellisException;


/**
 * An SNS notification service.
 */
public class SNSEventService implements EventService {

    public static final String TRELLIS_SNS_TOPIC = "trellis.sns.topic";

    private static final Logger LOGGER = getLogger(SNSEventService.class);
    private static final ActivityStreamService service = of(load(ActivityStreamService.class))
        .map(ServiceLoader::iterator).filter(Iterator::hasNext).map(Iterator::next)
        .orElseThrow(() -> new RuntimeTrellisException("No ActivityStream service available!"));

    private final AmazonSNS sns;
    private final String topic;

    /**
     * Cretae an SNS-bases notification service.
     */
    public SNSEventService() {
        this(defaultClient(), getConfiguration().get(TRELLIS_SNS_TOPIC));
    }

    /**
     * Cretae an SNS-bases notification service.
     * @param client the SNS client
     * @param topic the topic ARN
     */
    public SNSEventService(final AmazonSNS client, final String topic) {
        this.sns = requireNonNull(client, "the SNS client may not be null!");
        this.topic = topic;
    }

    @Override
    public void emit(final Event event) {
        requireNonNull(event, "Cannot emit a null event!");
        service.serialize(event).ifPresent(json -> {
            try {
                sns.publish(topic, json);
            } catch (final Exception ex) {
                LOGGER.error("Error writing to SNS topic {}: {}", topic, ex.getMessage());
            }
        });
    }
}
