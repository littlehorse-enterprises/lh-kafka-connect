package io.littlehorse.connect;

import java.io.IOException;
import java.net.URL;
import java.util.jar.Attributes;
import java.util.jar.Manifest;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class LHSinkConnectorVersion {

    private static final String MANIFEST_FILENAME = "META-INF/MANIFEST.MF";
    private static final String UNKNOWN_VERSION = "unknown";

    private LHSinkConnectorVersion() {}

    public static String version() {
        URL resource =
            LHSinkConnectorVersion.class.getClassLoader()
                .getResource(MANIFEST_FILENAME);

        if (resource != null) {
            try {
                Manifest manifest = new Manifest(resource.openStream());
                return manifest
                    .getMainAttributes()
                    .getValue(Attributes.Name.IMPLEMENTATION_VERSION);
            } catch (IOException e) {
                log.error("Error trying to reach {}", MANIFEST_FILENAME, e);
            }
        }

        return UNKNOWN_VERSION;
    }
}
