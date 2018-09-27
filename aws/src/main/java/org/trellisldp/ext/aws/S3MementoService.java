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

import static com.amazonaws.services.s3.AmazonS3ClientBuilder.defaultClient;
import static java.io.File.createTempFile;
import static java.lang.Long.parseLong;
import static java.lang.String.join;
import static java.time.Instant.now;
import static java.time.Instant.ofEpochSecond;
import static java.util.Collections.unmodifiableList;
import static java.util.Objects.nonNull;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.apache.commons.lang3.Range.between;
import static org.apache.jena.riot.Lang.NQUADS;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.apache.commons.lang3.Range;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.jena.JenaDataset;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.tamaya.ConfigurationProvider;
import org.slf4j.Logger;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.RuntimeTrellisException;

/**
 * An S3-based Memento service.
 */
public class S3MementoService implements MementoService {

    public static final String TRELLIS_MEMENTO_BUCKET = "trellis.s3.bucket.mementos";

    private static final Logger LOGGER = getLogger(S3MementoService.class);
    private static final JenaRDF rdf = new JenaRDF();

    private final AmazonS3 client;
    private final String bucketName;

    /**
     * Create an S3-based memento service.
     */
    public S3MementoService() {
        this(ConfigurationProvider.getConfiguration().get(TRELLIS_MEMENTO_BUCKET), defaultClient());
    }

    /**
     * Create an S3-based memento service.
     * @param bucketName the bucket name
     * @param client the client
     */
    public S3MementoService(final String bucketName, final AmazonS3 client) {
        this.bucketName = bucketName;
        this.client = client;
    }

    @Override
    public CompletableFuture<Void> put(final IRI identifier, final Instant time, final Stream<? extends Quad> data) {
        return runAsync(() -> {
            final File file = getTempFile();
            try (final JenaDataset dataset = rdf.createDataset();
                    final OutputStream output = new FileOutputStream(file)) {
                data.forEach(dataset::add);
                // TODO
                // separate out any server-managed triples and put them into the S3 object metadata
                RDFDataMgr.write(output, dataset.asJenaDatasetGraph(), NQUADS);
            } catch (final Exception ex) {
                LOGGER.error("Error closing dataset: {}", ex.getMessage());
                throw new RuntimeTrellisException(ex);
            }
            client.putObject(new PutObjectRequest(bucketName, getKey(identifier, time), file));
        });
    }

    @Override
    public CompletableFuture<Resource> get(final IRI identifier, final Instant time) {
        return supplyAsync(() -> {
            final GetObjectRequest req = new GetObjectRequest(bucketName, getKey(identifier, time));
            return new S3Resource(client.getObject(req));
        });
    }

    @Override
    public CompletableFuture<List<Range<Instant>>> list(final IRI identifier) {
        return supplyAsync(() -> {
            final List<Instant> versions = new ArrayList<>();
            final ListObjectsRequest req = new ListObjectsRequest().withBucketName(bucketName)
                .withPrefix(getKey(identifier, null)).withDelimiter("/");
            ObjectListing objs = client.listObjects(req);
            objs.getObjectSummaries().stream().map(S3ObjectSummary::getKey).map(this::getInstant)
                .forEachOrdered(versions::add);
            while (objs.isTruncated()) {
                objs = client.listNextBatchOfObjects(objs);
                objs.getObjectSummaries().stream().map(S3ObjectSummary::getKey).map(this::getInstant)
                    .forEachOrdered(versions::add);
            }

            final List<Range<Instant>> mementos = new ArrayList<>();
            Instant last = null;
            for (final Instant time : versions) {
                if (nonNull(last)) {
                    mementos.add(between(last, time));
                }
                last = time;
            }
            if (nonNull(last)) {
                mementos.add(between(last, now()));
            }
            return unmodifiableList(mementos);
        });
    }

    private Instant getInstant(final String key) {
        final String[] parts = key.split("?version=", 2);
        if (parts.length == 2) {
            return ofEpochSecond(parseLong(parts[1]));
        }
        return null;
    }

    @Override
    public CompletableFuture<Void> delete(final IRI identifier, final Instant time) {
        return runAsync(() ->
            client.deleteObject(bucketName, getKey(identifier, time)));
    }

    private static String getKey(final IRI identifier, final Instant time) {
        if (nonNull(time)) {
            return join("?version=", identifier.getIRIString().substring(TRELLIS_DATA_PREFIX.length()),
                    Long.toString(time.getEpochSecond()));
        }
        return identifier.getIRIString().substring(TRELLIS_DATA_PREFIX.length()) + "?version=";
    }

    private static File getTempFile() {
        try {
            return createTempFile("trellis-memento-", ".nq");
        } catch (final IOException ex) {
            LOGGER.error("Error creating temporary file: {}", ex.getMessage());
            throw new RuntimeTrellisException(ex);
        }
    }
}
