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
package org.trellisldp.ext.aws.app;

import com.fasterxml.jackson.annotation.JsonProperty;

import javax.validation.constraints.NotNull;

import org.trellisldp.app.config.TrellisConfiguration;

public class AppConfiguration extends TrellisConfiguration {

    @NotNull
    private String mementos;

    @NotNull
    private String binaries;

    @NotNull
    private String namespaces;

    @NotNull
    private String resourceLocation;

    /**
     * Get the Memento configuration.
     * @return the Memento resource location
     */
    @JsonProperty
    public String getMementos() {
        return mementos;
    }

    /**
     * Set the Memento resource configuration.
     * @param config the Memento resource location
     */
    @JsonProperty
    public void setMementos(final String config) {
        this.mementos = config;
    }

    /**
     * Get the binary configuration.
     * @return the binary configuration
     */
    @JsonProperty
    public String getBinaries() {
        return binaries;
    }

    /**
     * Set the binary configuration.
     * @param config the binary configuration
     */
    @JsonProperty
    public void setBinaries(final String config) {
        this.binaries = config;
    }

    /**
     * Set the RDF Connection configuration.
     * @param config the RDF Connection location
     */
    @JsonProperty
    public void setResources(final String config) {
        this.resourceLocation = config;
    }

    /**
     * Get the RDF Connection configuration.
     * @return the RDF Connection location
     */
    @JsonProperty
    public String getResources() {
        return resourceLocation;
    }

    /**
     * Set the namespaces filename.
     * @param namespaces the namespaces filename
     */
    @JsonProperty
    public void setNamespaces(final String namespaces) {
        this.namespaces = namespaces;
    }

    /**
     * Get the namespace filename.
     * @return the namespace filename
     */
    @JsonProperty
    public String getNamespaces() {
        return namespaces;
    }

}
