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
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.CompletableFuture.allOf;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.trellisldp.api.TrellisUtils.getInstance;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.io.IOException;
import java.io.InputStream;

import org.apache.commons.io.IOUtils;
import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.text.RandomStringGenerator;
import org.apache.tamaya.ConfigurationProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.BinaryService;

public class S3BinaryServiceTest {

    private static final RDF rdf = getInstance();
    private static final Integer length = 10;
    private static final String base = new RandomStringGenerator.Builder().withinRange('a', 'z')
        .build().generate(length);

    @AfterAll
    public static void tearDown() throws Exception {
        final AmazonS3 client = defaultClient();
        final String bucket = ConfigurationProvider.getConfiguration().get(S3BinaryService.TRELLIS_BINARY_BUCKET);
        client.listObjects(bucket, "binaries/" + base).getObjectSummaries().stream()
            .map(S3ObjectSummary::getKey).forEach(key -> client.deleteObject(bucket, key));
    }

    @Test
    public void testBinary() {
        final IRI identifier = rdf.createIRI("s3://binaries/" + base + "/resource");
        final BinaryService svc = new S3BinaryService();
        final InputStream input = getClass().getResourceAsStream("/file.txt");
        assertDoesNotThrow(svc.setContent(identifier, input)::join);
        svc.getContent(identifier).thenAccept(content -> {
            try {
                assertEquals("A sample binary file.", IOUtils.toString(content, UTF_8).trim());
            } catch (final IOException ex) {
                fail("Error reading IO stream", ex);
            }
        }).join();

        svc.getContent(identifier, 5, 10).thenAccept(content -> {
            try {
                assertEquals("ple bi", IOUtils.toString(content, UTF_8).trim());
            } catch (final IOException ex) {
                fail("Error reading IO stream", ex);
            }
        }).join();

        allOf(
            svc.calculateDigest(identifier, "FOO").thenAccept(digest -> assertNull(digest)),
            svc.calculateDigest(identifier, "MD5").thenAccept(digest ->
                assertEquals("ZyNmQT2UvueO5DCvzcaLZw==", digest)),
            svc.calculateDigest(identifier, "SHA").thenAccept(digest ->
                assertEquals("o4nMi5wcx3VRpahNOT6Rfh9Pd3c=", digest))).join();

        assertDoesNotThrow(svc.purgeContent(identifier)::join);
    }

    @Test
    public void testIdentifier() {
        final BinaryService svc = new S3BinaryService();
        assertTrue(svc.generateIdentifier().startsWith("s3://"));
    }

    @Test
    public void testInvalidIdentifier() {
        final IRI identifier = rdf.createIRI("file://binaries/" + base + "/resource");
        final BinaryService svc = new S3BinaryService();
        svc.getContent(identifier).handle((v, err) -> {
            assertNotNull(err);
            return null;
        });
    }

    @Test
    public void testAlgorithms() {
        final BinaryService svc = new S3BinaryService();
        assertTrue(svc.supportedAlgorithms().contains("MD5"));
        assertTrue(svc.supportedAlgorithms().contains("SHA"));
        assertTrue(svc.supportedAlgorithms().contains("SHA-1"));
        assertTrue(svc.supportedAlgorithms().contains("SHA-256"));
    }
}
