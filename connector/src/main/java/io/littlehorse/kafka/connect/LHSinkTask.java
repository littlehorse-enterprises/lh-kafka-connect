package io.littlehorse.kafka.connect;

import java.util.Collection;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;

@Slf4j
public class LHSinkTask extends SinkTask {

    private LHSinkClient lhClient;

    @Override
    public String version() {
        return LHSinkConnectorVersion.version();
    }

    @Override
    public void start(Map<String, String> props) {
        LHSinkConnectorConfig connectorConfig = new LHSinkConnectorConfig(
            props
        );
        lhClient = new LHSinkClient(connectorConfig.toLHConfig());

        log.info(
            "Connecting to LH Server with version {}",
            lhClient.getServerVersion()
        );
    }

    @Override
    public void put(Collection<SinkRecord> records) {}

    @Override
    public void stop() {
        log.debug("Closing LHSinkTask");
        if (lhClient == null) return;
        lhClient.close();
    }
}
