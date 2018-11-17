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
import static java.time.Instant.now;
import static java.util.Optional.empty;
import static java.util.Optional.of;
import static java.util.function.Predicate.isEqual;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.trellisldp.api.TrellisUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.TrellisUtils.getInstance;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import java.time.Instant;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.apache.commons.text.RandomStringGenerator;
import org.apache.tamaya.ConfigurationProvider;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.trellisldp.api.BinaryMetadata;
import org.trellisldp.api.MementoService;
import org.trellisldp.api.Resource;
import org.trellisldp.vocabulary.ACL;
import org.trellisldp.vocabulary.DC;
import org.trellisldp.vocabulary.LDP;
import org.trellisldp.vocabulary.Trellis;

public class S3MementoServiceTest {

    private static final RDF rdf = getInstance();
    private static final IRI root = rdf.createIRI(TRELLIS_DATA_PREFIX);
    private static final Integer length = 10;
    private static final String base = new RandomStringGenerator.Builder().withinRange('a', 'z')
        .build().generate(length);

    @AfterAll
    public static void tearDown() throws Exception {
        final AmazonS3 client = defaultClient();
        final String bucket = ConfigurationProvider.getConfiguration().get(S3MementoService.TRELLIS_MEMENTO_BUCKET);
        client.listObjects(bucket, "mementos/" + base).getObjectSummaries().stream()
            .map(S3ObjectSummary::getKey).forEach(key -> client.deleteObject(bucket, key));
    }

    @Test
    public void testMementoRDFSource() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "mementos/" + base + "/resource");
        final Resource res = mock(Resource.class);
        final Instant time = now();
        final Quad quad = rdf.createQuad(Trellis.PreferUserManaged, identifier, DC.title, rdf.createLiteral("Title"));
        final Quad acl = rdf.createQuad(Trellis.PreferAccessControl, identifier, ACL.mode, ACL.Read);
        when(res.getModified()).thenReturn(time);
        when(res.getIdentifier()).thenReturn(identifier);
        when(res.getContainer()).thenReturn(of(root));
        when(res.getInteractionModel()).thenReturn(LDP.RDFSource);
        when(res.getBinaryMetadata()).thenReturn(empty());
        when(res.getMembershipResource()).thenReturn(empty());
        when(res.getMemberRelation()).thenReturn(empty());
        when(res.getMemberOfRelation()).thenReturn(empty());
        when(res.getInsertedContentRelation()).thenReturn(empty());
        when(res.stream()).thenAnswer(inv -> Stream.of(quad, acl));

        final MementoService svc = new S3MementoService();
        assertDoesNotThrow(svc.put(res)::join);
        svc.get(identifier, time).thenAccept(r -> {
            assertEquals(identifier, r.getIdentifier());
            assertEquals(LDP.RDFSource, r.getInteractionModel());
            assertEquals(time, r.getModified());
            assertEquals(of(root), r.getContainer());
            assertTrue(r.stream().anyMatch(isEqual(quad)));
            assertFalse(r.getBinaryMetadata().isPresent());
            assertFalse(r.getMembershipResource().isPresent());
            assertFalse(r.getMemberRelation().isPresent());
            assertFalse(r.getMemberOfRelation().isPresent());
            assertFalse(r.getInsertedContentRelation().isPresent());
            assertTrue(r.hasAcl());
        }).join();

        svc.mementos(identifier).thenAccept(mementos -> assertTrue(mementos.contains(time)));
    }

    @Test
    public void testMementoNonRDFSource() {
        final IRI identifier = rdf.createIRI(TRELLIS_DATA_PREFIX + "mementos/" + base + "/binary");
        final BinaryMetadata binary = BinaryMetadata.builder(rdf.createIRI("s3://bucket/binary")).mimeType("text/plain")
            .size(40L).build();
        final Resource res = mock(Resource.class);
        final Instant time = now();
        final Quad quad = rdf.createQuad(Trellis.PreferUserManaged, identifier, DC.title, rdf.createLiteral("Title"));
        when(res.getModified()).thenReturn(time);
        when(res.getIdentifier()).thenReturn(identifier);
        when(res.getInteractionModel()).thenReturn(LDP.NonRDFSource);
        when(res.getContainer()).thenReturn(empty());
        when(res.getBinaryMetadata()).thenReturn(of(binary));
        when(res.getMembershipResource()).thenReturn(empty());
        when(res.getMemberRelation()).thenReturn(empty());
        when(res.getMemberOfRelation()).thenReturn(empty());
        when(res.getInsertedContentRelation()).thenReturn(empty());
        when(res.stream()).thenAnswer(inv -> Stream.of(quad));

        final MementoService svc = new S3MementoService();
        assertDoesNotThrow(svc.put(res)::join);
        svc.get(identifier, time).thenAccept(r -> {
            assertEquals(identifier, r.getIdentifier());
            assertEquals(LDP.NonRDFSource, r.getInteractionModel());
            assertEquals(time, r.getModified());
            assertTrue(r.stream().anyMatch(isEqual(quad)));
            assertTrue(r.getBinaryMetadata().isPresent());
            r.getBinaryMetadata().ifPresent(b -> {
                assertEquals(binary.getIdentifier(), b.getIdentifier());
                assertEquals(binary.getSize(), b.getSize());
                assertEquals(binary.getMimeType(), b.getMimeType());
            });
            assertFalse(r.getContainer().isPresent());
            assertFalse(r.getMembershipResource().isPresent());
            assertFalse(r.getMemberRelation().isPresent());
            assertFalse(r.getMemberOfRelation().isPresent());
            assertFalse(r.getInsertedContentRelation().isPresent());
            assertFalse(r.hasAcl());
        }).join();

        svc.mementos(identifier).thenAccept(mementos -> assertTrue(mementos.contains(time)));
    }
}
