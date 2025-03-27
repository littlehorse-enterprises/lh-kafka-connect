package io.littlehorse.e2e.configs;

import io.littlehorse.test.LHTest;

import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;

@LHTest
public abstract class E2ETest {
    public URL getKafkaConnectUrl(final String withPath) throws MalformedURLException {
        return URI.create("http://localhost:38083").resolve(withPath).toURL();
    }
}
