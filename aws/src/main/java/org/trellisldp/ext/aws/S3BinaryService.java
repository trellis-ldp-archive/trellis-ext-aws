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
import static java.nio.file.StandardOpenOption.WRITE;
import static java.util.Arrays.asList;
import static java.util.Objects.requireNonNull;
import static java.util.Optional.ofNullable;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.codec.digest.DigestUtils.updateDigest;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD2;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.MD5;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA3_256;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA3_384;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA3_512;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_1;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_256;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_384;
import static org.apache.commons.codec.digest.MessageDigestAlgorithms.SHA_512;
import static org.apache.tamaya.ConfigurationProvider.getConfiguration;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.ObjectMetadata;
import com.amazonaws.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.IRI;
import org.apache.tamaya.Configuration;
import org.trellisldp.api.Binary;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.id.UUIDGenerator;

/**
 * An S3-based binary service.
 */
public class S3BinaryService implements BinaryService {

    public static final String CONFIG_BINARY_BUCKET = "trellis.s3.binary.bucket";
    public static final String CONFIG_BINARY_PATH_PREFIX = "trellis.s3.binary.path.prefix";

    private static final String PREFIX = "s3://";
    private static final String SHA = "SHA";
    private static final Set<String> algorithms = asList(MD5, MD2, SHA, SHA_1, SHA_256, SHA_384, SHA_512,
            SHA3_256, SHA3_384, SHA3_512).stream()
        .collect(toSet());

    private final IdentifierService idService = new UUIDGenerator();
    private final AmazonS3 client;
    private final String bucketName;
    private final String pathPrefix;

    /**
     * Create an S3-based binary service.
     */
    public S3BinaryService() {
        this(defaultClient(), getConfiguration());
    }

    private S3BinaryService(final AmazonS3 client, final Configuration config) {
        this(client, config.get(CONFIG_BINARY_BUCKET), config.get(CONFIG_BINARY_PATH_PREFIX));
    }

    /**
     * Create an S3-based binary service.
     * @param client the client
     * @param bucketName the bucket name
     * @param pathPrefix the path prefix, may be {@code null}
     */
    public S3BinaryService(final AmazonS3 client, final String bucketName, final String pathPrefix) {
        this.client = requireNonNull(client, "client may not be null!");
        this.bucketName = requireNonNull(bucketName, "bucket name may not be null!");
        this.pathPrefix = ofNullable(pathPrefix).orElse("");
    }

    @Override
    public CompletableFuture<Binary> get(final IRI identifier) {
        return supplyAsync(() -> new S3Binary(client, bucketName, getKey(identifier)));
    }

    @Override
    public CompletableFuture<Void> purgeContent(final IRI identifier) {
        return runAsync(() -> client.deleteObject(bucketName, getKey(identifier)));
    }

    @Override
    public CompletableFuture<Void> setContent(final BinaryMetadata metadata, final InputStream stream,
            final Map<String, List<String>> hints) {
        return runAsync(() -> {
            try {
                bufferUpload(metadata, stream, Files.createTempFile("trellis-binary", ".tmp"));
            } catch (final IOException ex) {
                throw new UncheckedIOException("Error buffering binary to local file", ex);
            }
        });
    }

    @Override
    public CompletableFuture<byte[]> calculateDigest(final IRI identifier, final MessageDigest algorithm) {
        return supplyAsync(() -> computeDigest(bucketName, getKey(identifier), algorithm));
    }

    @Override
    public Set<String> supportedAlgorithms() {
        return algorithms;
    }

    @Override
    public String generateIdentifier() {
        return idService.getSupplier(PREFIX).get();
    }

    private void bufferUpload(final BinaryMetadata metadata, final InputStream stream, final Path path)
            throws IOException {
        // Buffer the file locally so that the PUT request can be parallelized for large objects
        try {
            try (final OutputStream output = Files.newOutputStream(path, WRITE)) {
                IOUtils.copy(stream, output);
            }
            final ObjectMetadata md = new ObjectMetadata();
            metadata.getMimeType().ifPresent(md::setContentType);
            final PutObjectRequest req = new PutObjectRequest(bucketName, getKey(metadata.getIdentifier()),
                    path.toFile()).withMetadata(md);
            client.putObject(req);
        } finally {
            Files.delete(path);
        }
    }

    private byte[] computeDigest(final String bucket, final String key, final MessageDigest algorithm) {
        try (final InputStream input = client.getObject(bucket, key).getObjectContent()) {
            return updateDigest(algorithm, input).digest();
        } catch (final IOException ex) {
            throw new UncheckedIOException("Error computing digest", ex);
        }
    }

    private String getKey(final IRI identifier) {
        final String id = identifier.getIRIString();
        if (id.startsWith(PREFIX)) {
            return pathPrefix + id.substring(PREFIX.length());
        }
        throw new RuntimeTrellisException("Invalid identifier: " + identifier);
    }
}
