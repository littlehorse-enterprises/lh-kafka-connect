package io.littlehorse.kafka.connect;

import java.util.Collection;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;

@Slf4j
public class LHSinkTask extends SinkTask {

    private LHSinkClient sinkClient;

    @Override
    public String version() {
        return LHSinkConnectorVersion.version();
    }

    @Override
    public void start(Map<String, String> props) {
        LHSinkConnectorConfig connectorConfig = new LHSinkConnectorConfig(
            props
        );
        //        sinkClient = new LHSinkClient(connectorConfig.toLHConfig());

        log.info(
            "PEDROOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOOO Connecting to LH Server with version {},{},{}",
            connectorConfig.toLHConfig().getApiBootstrapHost(),
            connectorConfig.toLHConfig().getApiBootstrapPort(),
            connectorConfig.toLHConfig().getApiProtocol()
        );
    }

    @Override
    public void put(Collection<SinkRecord> records) {}

    @Override
    public void stop() {}
}
