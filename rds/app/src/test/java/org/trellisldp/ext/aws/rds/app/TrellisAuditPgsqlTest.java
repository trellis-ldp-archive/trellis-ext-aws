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
package org.trellisldp.ext.aws.rds.app;

import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static org.eclipse.microprofile.config.ConfigProvider.getConfig;
import static org.junit.jupiter.api.Assertions.fail;
import static org.trellisldp.ext.aws.S3BinaryService.CONFIG_BINARY_BUCKET;
import static org.trellisldp.ext.aws.S3BinaryService.CONFIG_BINARY_PATH_PREFIX;
import static org.trellisldp.ext.aws.S3MementoService.CONFIG_MEMENTO_BUCKET;
import static org.trellisldp.ext.aws.S3MementoService.CONFIG_MEMENTO_PATH_PREFIX;

import io.dropwizard.testing.DropwizardTestSupport;

import java.io.IOException;

import javax.ws.rs.client.Client;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;

/**
 * Audit tests.
 */
@EnabledIfEnvironmentVariable(named = "TRAVIS", matches = "true")
public class TrellisAuditPgsqlTest extends AbstractAuditTests {

    private static final String mementos = TestUtils.randomString(10) + "/";
    private static final String binaries = TestUtils.randomString(10) + "/";
    private static DropwizardTestSupport<AppConfiguration> PG_APP;
    private static Client CLIENT;

    static {
        System.setProperty(CONFIG_MEMENTO_PATH_PREFIX, mementos);
        System.setProperty(CONFIG_BINARY_PATH_PREFIX, binaries);
        PG_APP = TestUtils.buildPgsqlApp("jdbc:postgresql://localhost/trellis", "postgres", "");
        CLIENT = TestUtils.buildClient(PG_APP);
        try {
            PG_APP.getApplication().run("db", "migrate", resourceFilePath("trellis-config.yml"));
        } catch (final Exception ex) {
            fail(ex.getMessage(), ex);
        }
    }

    @Override
    public Client getClient() {
        return CLIENT;
    }

    @Override
    public String getBaseURL() {
        return "http://localhost:" + PG_APP.getLocalPort() + "/";
    }

    @AfterAll
    public static void cleanup() throws IOException {
        System.clearProperty(CONFIG_MEMENTO_PATH_PREFIX);
        System.clearProperty(CONFIG_BINARY_PATH_PREFIX);
        PG_APP.after();
        TestUtils.cleanupS3(getConfig().getValue(CONFIG_MEMENTO_BUCKET, String.class), mementos);
        TestUtils.cleanupS3(getConfig().getValue(CONFIG_BINARY_BUCKET, String.class), binaries);
    }
}
