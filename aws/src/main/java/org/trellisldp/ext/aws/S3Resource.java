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

import static org.trellisldp.api.RDFUtils.TRELLIS_DATA_PREFIX;
import static org.trellisldp.api.RDFUtils.getInstance;

import com.amazonaws.services.s3.model.S3Object;

import java.time.Instant;
import java.util.Optional;
import java.util.stream.Stream;

import org.apache.commons.rdf.api.IRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.api.RDF;
import org.trellisldp.api.Binary;
import org.trellisldp.api.Resource;

/**
 * An S3-based resource.
 */
public class S3Resource implements Resource {

    private static final RDF rdf = getInstance();

    private final S3Object s3Object;

    /**
     * Create a Trellis resource from an S3Object.
     * @param s3Object the object from S3
     */
    public S3Resource(final S3Object s3Object) {
        this.s3Object = s3Object;
    }

    @Override
    public Stream<Quad> stream() {
        // TODO
        return null;
    }

    @Override
    public IRI getIdentifier() {
        return rdf.createIRI(TRELLIS_DATA_PREFIX + s3Object.getKey());
    }

    @Override
    public IRI getInteractionModel() {
        // TODO
        return null;
    }

    @Override
    public Optional<IRI> getMembershipResource() {
        // TODO
        return null;
    }

    @Override
    public Optional<IRI> getMemberRelation() {
        // TODO
        return null;
    }

    @Override
    public Optional<IRI> getMemberOfRelation() {
        // TODO
        return null;
    }

    @Override
    public Optional<IRI> getInsertedContentRelation() {
        // TODO
        return null;
    }

    @Override
    public Optional<Binary> getBinary() {
        // TODO
        return null;
    }

    @Override
    public Instant getModified() {
        // TODO
        return null;
    }

    @Override
    public Boolean hasAcl() {
        // TODO
        return null;
    }
}
