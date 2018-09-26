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
import static java.util.Arrays.asList;
import static java.util.Base64.getEncoder;
import static java.util.concurrent.CompletableFuture.runAsync;
import static java.util.concurrent.CompletableFuture.supplyAsync;
import static java.util.stream.Collectors.toSet;
import static org.apache.commons.codec.digest.DigestUtils.getDigest;
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
import static org.slf4j.LoggerFactory.getLogger;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.GetObjectRequest;
import com.amazonaws.services.s3.model.ObjectMetadata;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.security.MessageDigest;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;

import org.apache.commons.rdf.api.IRI;
import org.apache.tamaya.ConfigurationProvider;
import org.slf4j.Logger;
import org.trellisldp.api.BinaryService;
import org.trellisldp.api.IdentifierService;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.id.UUIDGenerator;

/**
 * An S3-based binary service.
 */
public class S3BinaryService implements BinaryService {

    public static final String TRELLIS_BINARY_BUCKET = "trellis.s3.bucket.binaries";

    private static final String SHA = "SHA";
    private static final Logger LOGGER = getLogger(S3BinaryService.class);
    private static final Set<String> algorithms = asList(MD5, MD2, SHA, SHA_1, SHA_256, SHA_384, SHA_512,
            SHA3_256, SHA3_384, SHA3_512).stream()
        .collect(toSet());

    private final IdentifierService idService = new UUIDGenerator();
    private final AmazonS3 client;
    private final String bucketName;

    /**
     * Create an S3-based binary service.
     */
    public S3BinaryService() {
        this(ConfigurationProvider.getConfiguration().get(TRELLIS_BINARY_BUCKET), defaultClient());
    }

    /**
     * Create an S3-based binary service.
     * @param bucketName the bucket name
     * @param client the client
     */
    public S3BinaryService(final String bucketName, final AmazonS3 client) {
        this.bucketName = bucketName;
        this.client = client;
    }

    @Override
    public CompletableFuture<InputStream> getContent(final IRI identifier) {
        return supplyAsync(() -> {
            final S3Resource res = new S3Resource(identifier);
            return client.getObject(res.getBucket(), res.getKey()).getObjectContent();
        });
    }

    @Override
    public CompletableFuture<InputStream> getContent(final IRI identifier, final Integer from, final Integer to) {
        return supplyAsync(() -> {
            final S3Resource res = new S3Resource(identifier);
            return client.getObject(new GetObjectRequest(res.getBucket(), res.getKey()).withRange(from, to))
                .getObjectContent();
        });
    }

    @Override
    public CompletableFuture<Void> purgeContent(final IRI identifier) {
        return runAsync(() -> {
            final S3Resource res = new S3Resource(identifier);
            client.deleteObject(res.getBucket(), res.getKey());
        });
    }

    @Override
    public CompletableFuture<Void> setContent(final IRI identifier, final InputStream stream,
            final Map<String, String> metadata) {
        return runAsync(() -> {
            final S3Resource res = new S3Resource(identifier);
            final ObjectMetadata md = new ObjectMetadata();
            md.setUserMetadata(metadata);
            client.putObject(res.getBucket(), res.getKey(), stream, md);
        });
    }

    @Override
    public CompletableFuture<String> calculateDigest(final IRI identifier, final String algorithm) {
        return supplyAsync(() -> {
            final S3Resource res = new S3Resource(identifier);
            if (SHA.equals(algorithm)) {
                return computeDigest(res.getBucket(), res.getKey(), getDigest(SHA_1));
            } else if (supportedAlgorithms().contains(algorithm)) {
                return computeDigest(res.getBucket(), res.getKey(), getDigest(algorithm));
            }
            LOGGER.warn("Algorithm not supported: {}", algorithm);
            return null;
        });
    }

    @Override
    public Set<String> supportedAlgorithms() {
        return algorithms;
    }

    @Override
    public String generateIdentifier() {
        return idService.getSupplier("s3://" + bucketName + "/").get();
    }

    private String computeDigest(final String bucket, final String key, final MessageDigest algorithm) {
        try (final InputStream input = client.getObject(bucket, key).getObjectContent()) {
            return getEncoder().encodeToString(updateDigest(algorithm, input).digest());
        } catch (final IOException ex) {
            LOGGER.error("Error computing digest", ex);
            throw new UncheckedIOException(ex);
        }
    }

    private static class S3Resource {
        private final String bucketName;
        private final String key;

        public S3Resource(final IRI identifier) {
            final String[] parts = identifier.getIRIString().split("/", 4);
            if (parts.length == 4 && parts[0].equals("s3:")) {
                this.bucketName = parts[2];
                this.key = parts[3];
            } else {
                throw new RuntimeTrellisException("Invalid identifier: " + identifier);
            }
        }

        public String getBucket() {
            return bucketName;
        }

        public String getKey() {
            return key;
        }
    }
}
