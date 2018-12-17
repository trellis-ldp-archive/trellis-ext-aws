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

import static com.amazonaws.services.s3.AmazonS3ClientBuilder.defaultClient;
import static io.dropwizard.testing.ConfigOverride.config;
import static io.dropwizard.testing.ResourceHelpers.resourceFilePath;
import static java.util.Arrays.asList;
import static org.glassfish.jersey.client.ClientProperties.CONNECT_TIMEOUT;
import static org.glassfish.jersey.client.ClientProperties.READ_TIMEOUT;

import com.amazonaws.services.s3.AmazonS3;
import com.amazonaws.services.s3.model.S3ObjectSummary;

import io.dropwizard.client.JerseyClientBuilder;
import io.dropwizard.testing.ConfigOverride;
import io.dropwizard.testing.DropwizardTestSupport;

import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.Client;

import org.apache.commons.text.RandomStringGenerator;

/**
 * Testing utilities.
 */
final class TestUtils {

    private static final AmazonS3 client = defaultClient();

    public static String randomString(final int length) {
        return new RandomStringGenerator.Builder().withinRange('a', 'z').build().generate(length);
    }

    public static DropwizardTestSupport<AppConfiguration> buildPgsqlApp(final String dbUrl, final String dbUser,
            final String dbPassword, final ConfigOverride... overrides) {
        final List<ConfigOverride> confs = new ArrayList<>(asList(config("database.url", dbUrl),
                    config("database.user", dbUser), config("database.password", dbPassword)));
        for (final ConfigOverride o : overrides) {
            confs.add(o);
        }
        return buildGenericApp(confs);
    }

    public static DropwizardTestSupport<AppConfiguration> buildGenericApp(final List<ConfigOverride> overrides) {
        final DropwizardTestSupport<AppConfiguration> app = new DropwizardTestSupport<>(TrellisApplication.class,
                resourceFilePath("trellis-config.yml"), overrides.stream().toArray(ConfigOverride[]::new));
        app.before();
        return app;
    }

    public static void cleanupS3(final String bucket, final String prefix) {
        client.listObjects(bucket, prefix).getObjectSummaries().stream()
            .map(S3ObjectSummary::getKey).forEach(key -> client.deleteObject(bucket, key));
    }

    public static Client buildClient(final DropwizardTestSupport<AppConfiguration> app) {
        final Client client = new JerseyClientBuilder(app.getEnvironment()).build("test client");
        client.property(CONNECT_TIMEOUT, 10000);
        client.property(READ_TIMEOUT, 10000);
        return client;
    }

    private TestUtils() {
        // prevent instantiation
    }
}
