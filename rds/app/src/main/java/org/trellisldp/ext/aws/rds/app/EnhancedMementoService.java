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

import static java.util.concurrent.CompletableFuture.completedFuture;

import java.time.Instant;
import java.util.SortedSet;
import java.util.concurrent.CompletableFuture;

import javax.sql.DataSource;

import org.apache.commons.rdf.api.IRI;
import org.jdbi.v3.core.Jdbi;
import org.trellisldp.api.Resource;
import org.trellisldp.ext.aws.S3MementoService;
import org.trellisldp.ext.db.DBMementoUtils;

public class EnhancedMementoService extends S3MementoService {

    private final DBMementoUtils utils;

    /**
     * Create a db/file memento service.
     * @param ds the db connection
     */
    public EnhancedMementoService(final DataSource ds) {
        this(Jdbi.create(ds));
    }

    /**
     * Create a db/file memento service.
     * @param jdbi the db connection
     */
    public EnhancedMementoService(final Jdbi jdbi) {
        super();
        this.utils = new DBMementoUtils(jdbi);
    }

    @Override
    public CompletableFuture<Void> put(final Resource resource) {
        return super.put(resource).thenAccept(future ->
                utils.put(resource.getIdentifier(), resource.getModified()));
    }

    @Override
    public CompletableFuture<Resource> get(final IRI identifier, final Instant time) {
        return super.get(identifier, utils.get(identifier, time).orElse(time));
    }

    @Override
    public CompletableFuture<SortedSet<Instant>> mementos(final IRI identifier) {
        final SortedSet<Instant> instants = utils.mementos(identifier);
        if (instants.isEmpty()) {
            return super.mementos(identifier).thenApply(m -> {
                m.forEach(i -> utils.put(identifier, i));
                return m;
            });
        } else {
            return completedFuture(instants);
        }
    }
}
