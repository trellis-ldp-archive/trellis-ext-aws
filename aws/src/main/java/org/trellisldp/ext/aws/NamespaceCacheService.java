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

import static java.net.URI.create;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.function.Predicate.isEqual;
import static org.apache.tamaya.ConfigurationProvider.getConfiguration;

import java.util.Map;
import java.util.Optional;

import org.trellisldp.api.NamespaceService;

import redis.clients.jedis.Jedis;

public class NamespaceCacheService implements NamespaceService {

    public static final String TRELLIS_REDIS_NAMESPACES_URL = "trellis.redis.namespaces.url";

    private final Jedis client;
    private final String NS = "namespaces";

    /**
     * Create an ElastiCache-based namespace service.
     */
    public NamespaceCacheService() {
        this(new Jedis(create(getConfiguration().get(TRELLIS_REDIS_NAMESPACES_URL))));
    }

    /**
     * Create an ElastiCache-based namespace service.
     * @param client the Redis client
     */
    public NamespaceCacheService(final Jedis client) {
        this.client = client;
    }

    @Override
    public Map<String, String> getNamespaces() {
        return client.hgetAll(NS);
    }

    @Override
    public Optional<String> getNamespace(final String prefix) {
        return empty();
    }

    @Override
    public Optional<String> getPrefix(final String namespace) {
        return of(client.hget(NS, namespace)).filter(isEqual("nil").negate());
    }

    @Override
    public Boolean setPrefix(final String prefix, final String namespace) {
        client.hset(NS, namespace, prefix);
        return true;
    }
}
