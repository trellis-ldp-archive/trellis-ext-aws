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

import static java.time.Instant.parse;
import static java.util.Objects.nonNull;
import static java.util.Optional.ofNullable;
import static org.apache.jena.query.DatasetFactory.create;
import static org.apache.jena.riot.Lang.NQUADS;
import static org.slf4j.LoggerFactory.getLogger;
import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;

import com.amazonaws.services.s3.model.S3Object;

import java.io.IOException;
import java.io.InputStream;
import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.jena.JenaRDF;
import org.apache.jena.query.Dataset;
import org.apache.jena.riot.RDFParser;
import org.slf4j.Logger;
import org.trellisldp.api.Binary;
import org.trellisldp.api.Resource;
import org.trellisldp.api.RuntimeTrellisException;

/**
 * An S3-based resource.
 */
public class S3Resource implements Resource {

    public static final String INTERACTION_MODEL = "trellis.interactionModel";
    public static final String MODIFIED = "trellis.modified";
    public static final String HAS_ACL = "trellis.hasACL";
    public static final String MEMBERSHIP_RESOURCE = "trellis.membershipResource";
    public static final String MEMBER_RELATION = "trellis.hasMemberRelation";
    public static final String MEMBER_OF_RELATION = "trellis.isMemberOfRelation";
    public static final String INSERTED_CONTENT_RELATION = "trellis.insertedContentRelation";
    public static final String BINARY_LOCATION = "trellis.binaryLocation";
    public static final String BINARY_DATE = "trellis.binaryDate";
    public static final String BINARY_TYPE = "trellis.binaryMimeType";
    public static final String BINARY_SIZE = "trellis.binarySize";

    private static final JenaRDF rdf = new JenaRDF();
    private static final Logger LOGGER = getLogger(S3Resource.class);

    private final S3Object res;

    /**
     * Create a Trellis resource from an S3Object.
     * @param s3Object the object from S3
     */
    public S3Resource(final S3Object s3Object) {
        this.res = s3Object;
    }

    @Override
    public IRI getIdentifier() {
        return rdf.createIRI(TRELLIS_DATA_PREFIX + res.getKey());
    }

    @Override
    public Instant getModified() {
        return ofNullable(getMetadata(MODIFIED)).map(Instant::parse).orElse(null);
    }

    @Override
    public IRI getInteractionModel() {
        return ofNullable(getMetadata(INTERACTION_MODEL)).map(rdf::createIRI).orElse(null);
    }

    @Override
    public Optional<IRI> getMembershipResource() {
        return ofNullable(getMetadata(MEMBERSHIP_RESOURCE)).map(rdf::createIRI);
    }

    @Override
    public Optional<IRI> getMemberRelation() {
        return ofNullable(getMetadata(MEMBER_RELATION)).map(rdf::createIRI);
    }

    @Override
    public Optional<IRI> getMemberOfRelation() {
        return ofNullable(getMetadata(MEMBER_OF_RELATION)).map(rdf::createIRI);
    }

    @Override
    public Optional<IRI> getInsertedContentRelation() {
        return ofNullable(getMetadata(INSERTED_CONTENT_RELATION)).map(rdf::createIRI);
    }

    @Override
    public Boolean hasAcl() {
        return ofNullable(getMetadata(HAS_ACL)).isPresent();
    }

    @Override
    public Optional<Binary> getBinary() {
        final String binaryLocation = getMetadata(BINARY_LOCATION);
        final String binaryDate = getMetadata(BINARY_DATE);
        final String binaryType = getMetadata(BINARY_TYPE);
        final Long binarySize = ofNullable(getMetadata(BINARY_SIZE)).map(Long::parseLong).orElse(null);
        return ofNullable(binaryLocation).filter(x -> nonNull(binaryDate)).map(rdf::createIRI)
            .map(loc -> new Binary(loc, parse(binaryDate), binaryType, binarySize));
    }

    @Override
    public Stream<? extends Quad> stream() {
        final Dataset dataset = create();
        try (final InputStream input = res.getObjectContent()) {
            RDFParser.source(input).lang(NQUADS).parse(dataset);
        } catch (final IOException ex) {
            LOGGER.error("Error parsing input from S3: {}", ex.getMessage());
            dataset.close();
            throw new RuntimeTrellisException(ex);
        }
        return rdf.asDataset(dataset).stream().onClose(dataset::close);
    }

    private String getMetadata(final String key) {
        return res.getObjectMetadata().getUserMetaDataOf(key);
    }
}
