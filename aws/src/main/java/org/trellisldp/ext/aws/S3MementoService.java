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
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;

import com.amazonaws.services.s3.AmazonS3;

import java.time.Instant;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.apache.commons.lang3.Range;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.tamaya.ConfigurationProvider;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.Resource;

/**
 * An S3-based Memento service.
 */
public class S3MementoService implements MementoService {

    public static final String TRELLIS_MEMENTO_BUCKET = "trellis.s3.bucket.mementos";

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
            // TODO
            // write data quads to file at /tmp, then upload as a client.pubObject(pubObjectRequest)
            // with new PutObjectRequest(bucket, key, file).withVersionId(time.toString())
        });
    }

    @Override
    public CompletableFuture<Resource> get(final IRI identifier, final Instant time) {
        return supplyAsync(() -> {
            // TODO
            // fetch a particular resource at the S3 location
            // new GetObjectRequest(bucket, key).withVersionId(time.toString());
            return null;
        });
    }

    @Override
    public CompletableFuture<List<Range<Instant>>> list(final IRI identifier) {
        return supplyAsync(() -> {
            // TODO
            // List the objects for this resource
            // client.listVersions(...)
            // new ListVersionsRequest(bucket, key...)
            return null;
        });
    }

    @Override
    public CompletableFuture<Void> delete(final IRI identifier, final Instant time) {
        return runAsync(() -> {
            // TODO
            // client.deleteVersion(bucket, key, time.toString())
        });
    }
}
