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

import static java.lang.String.format;
import static java.util.Optional.of;
import static java.util.stream.Collectors.joining;

import org.apache.commons.rdf.api.BlankNodeOrIRI;
import org.apache.commons.rdf.api.Quad;
import org.apache.commons.rdf.jena.JenaDataset;
import org.apache.jena.sparql.core.DatasetGraph;
import org.trellisldp.api.RuntimeTrellisException;
import org.trellisldp.vocabulary.Trellis;

/**
 * @author acoburn
 */
class CloseableJenaDataset implements AutoCloseable {

    private final JenaDataset dataset;

    /**
     * Create a new dataset.
     *
     * @param dataset the dataset
     */
    public CloseableJenaDataset(final JenaDataset dataset) {
        this.dataset = dataset;
    }

    @Override
    public void close() {
        try {
            dataset.close();
        } catch (final Exception ex) {
            throw new RuntimeTrellisException("Error closing dataset", ex);
        }
    }

    /**
     * Add a quad to the dataset.
     *
     * @param quad an RDF Quad
     */
    public void add(final Quad quad) {
        dataset.add(quad);
    }

    /**
     * Get the underlying dataset.
     *
     * @return the dataset
     */
    public DatasetGraph asJenaDatasetGraph() {
        return dataset.asJenaDatasetGraph();
    }

    /**
     * Check for ACL triples.
     * @return true if there are ACL triples; false otherwise
     */
    public Boolean hasAcl() {
        return dataset.contains(of(Trellis.PreferAccessControl), null, null, null);
    }

    @Override
    public String toString() {
        return dataset.stream()
                        .map(q -> format("%1$s %2$s %3$s %4$s .",
                                        q.getSubject().ntriplesString(),
                                        q.getPredicate().ntriplesString(),
                                        q.getObject().ntriplesString(),
                                        q.getGraphName().map(BlankNodeOrIRI::ntriplesString).orElse("")))
                        .collect(joining("\n"));
    }
}
