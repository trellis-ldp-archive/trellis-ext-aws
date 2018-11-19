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
import static java.lang.String.join;
import static java.time.temporal.ChronoUnit.SECONDS;
import static java.util.Collections.unmodifiableSortedSet;
import static java.util.Objects.nonNull;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.of;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static org.apache.jena.riot.Lang.NQUADS;
import static org.apache.tamaya.ConfigurationProvider.getConfiguration;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ListObjectsRequest;
import com.amazonaws.services.s3.model.ObjectListing;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.file.Files;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.jena.JenaDataset;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.riot.RDFDataMgr;
import org.apache.tamaya.Configuration;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.Resource;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.vocabulary.Trellis;

/**
 * An S3-based Memento service.
 */
public class S3MementoService implements MementoService {

    public static final String TRELLIS_MEMENTO_BUCKET = "trellis.s3.memento.bucket";
    public static final String TRELLIS_MEMENTO_PATH_PREFIX = "trellis.s3.memento.path.prefix";

    private static final JenaRDF rdf = new JenaRDF();
    private static final Configuration config = getConfiguration();

    private final AmazonS3 client;
    private final String bucketName;
    private final String pathPrefix;

    /**
     * Create an S3-based memento service.
     */
    public S3MementoService() {
        this(defaultClient(), config.get(TRELLIS_MEMENTO_BUCKET), config.get(TRELLIS_MEMENTO_PATH_PREFIX));
    }

    /**
     * Create an S3-based memento service.
     * @param client the client
     * @param bucketName the bucket name
     * @param pathPrefix the path prefix for mementos, may be {@code null}
     */
    public S3MementoService(final AmazonS3 client, final String bucketName, final String pathPrefix) {
        this.client = requireNonNull(client, "Client may not be null!");
        this.bucketName = requireNonNull(bucketName, "AWS Bucket may not be null!");
        this.pathPrefix = ofNullable(pathPrefix).orElse("");
    }

    @Override
    public CompletableFuture<Void> put(final Resource resource) {
        return runAsync(() -> {
            final File file = getTempFile();
            file.deleteOnExit();
            final Map<String, String> metadata = new HashMap<>();
            metadata.put(S3Resource.INTERACTION_MODEL, resource.getInteractionModel().getIRIString());
            metadata.put(S3Resource.MODIFIED, resource.getModified().toString());
            resource.getContainer().map(IRI::getIRIString).ifPresent(c -> metadata.put(S3Resource.CONTAINER, c));
            resource.getBinaryMetadata().ifPresent(b -> {
                metadata.put(S3Resource.BINARY_LOCATION, b.getIdentifier().getIRIString());
                b.getMimeType().ifPresent(m -> metadata.put(S3Resource.BINARY_TYPE, m));
                b.getSize().ifPresent(s -> metadata.put(S3Resource.BINARY_SIZE, Long.toString(s)));
            });
            resource.getMembershipResource().map(IRI::getIRIString)
                .ifPresent(m -> metadata.put(S3Resource.MEMBERSHIP_RESOURCE, m));
            resource.getMemberRelation().map(IRI::getIRIString)
                .ifPresent(m -> metadata.put(S3Resource.MEMBER_RELATION, m));
            resource.getMemberOfRelation().map(IRI::getIRIString)
                .ifPresent(m -> metadata.put(S3Resource.MEMBER_OF_RELATION, m));
            resource.getInsertedContentRelation().map(IRI::getIRIString)
                .ifPresent(m -> metadata.put(S3Resource.INSERTED_CONTENT_RELATION, m));

            try (final JenaDataset dataset = rdf.createDataset();
                    final OutputStream output = new FileOutputStream(file);
                    final Stream<? extends Quad> quads = resource.stream()) {
                quads.forEachOrdered(dataset::add);

                if (dataset.contains(of(Trellis.PreferAccessControl), null, null, null)) {
                    metadata.put(S3Resource.HAS_ACL, "true");
                }
                RDFDataMgr.write(output, dataset.asJenaDatasetGraph(), NQUADS);
            } catch (final Exception ex) {
                throw new RuntimeTrellisException("Error closing dataset", ex);
            }
            final ObjectMetadata md = new ObjectMetadata();
            md.setContentType("application/n-quads");
            md.setUserMetadata(metadata);
            final PutObjectRequest req = new PutObjectRequest(bucketName, getKey(resource.getIdentifier(),
                        resource.getModified()), file);
            client.putObject(req.withMetadata(md));
            try {
                Files.delete(file.toPath());
            } catch (final IOException ex) {
                throw new RuntimeTrellisException("Error deleting locally buffered file", ex);
            }
        });
    }

    @Override
    public CompletableFuture<Resource> get(final IRI identifier, final Instant time) {
        return supplyAsync(() ->  new S3Resource(client.getObject(new GetObjectRequest(bucketName,
                            getKey(identifier, time)))));
    }

    @Override
    public CompletableFuture<SortedSet<Instant>> mementos(final IRI identifier) {
        return supplyAsync(() -> {
            final SortedSet<Instant> versions = new TreeSet<>();
            final ListObjectsRequest req = new ListObjectsRequest().withBucketName(bucketName)
                .withPrefix(getKey(identifier, null)).withDelimiter("/");
            ObjectListing objs = client.listObjects(req);
            objs.getObjectSummaries().stream().map(S3ObjectSummary::getKey).map(this::getInstant)
                .map(i -> i.truncatedTo(SECONDS)).forEachOrdered(versions::add);
            while (objs.isTruncated()) {
                objs = client.listNextBatchOfObjects(objs);
                objs.getObjectSummaries().stream().map(S3ObjectSummary::getKey).map(this::getInstant)
                    .map(i -> i.truncatedTo(SECONDS)).forEachOrdered(versions::add);
            }
            return unmodifiableSortedSet(versions);
        });
    }

    private Instant getInstant(final String key) {
        return of(key).map(k -> k.split("\\?version=", 2)).filter(p -> p.length == 2).map(p -> p[1])
            .map(Long::parseLong).map(Instant::ofEpochSecond).map(i -> i.truncatedTo(SECONDS)).orElse(null);
    }

    private String getKey(final IRI identifier, final Instant time) {
        final String version = "?version=";
        if (nonNull(time)) {
            return pathPrefix + join(version, identifier.getIRIString().substring(TRELLIS_DATA_PREFIX.length()),
                    Long.toString(time.getEpochSecond()));
        }
        return pathPrefix + identifier.getIRIString().substring(TRELLIS_DATA_PREFIX.length()) + version;
    }

    private static File getTempFile() {
        try {
            return createTempFile("trellis-memento-", ".nq");
        } catch (final IOException ex) {
            throw new RuntimeTrellisException("Error creating temporary file", ex);
        }
    }
}
