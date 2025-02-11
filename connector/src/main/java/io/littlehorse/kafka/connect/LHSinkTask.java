package io.littlehorse.kafka.connect;

import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
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
    public void stop() {
        log.debug("Closing LHSinkTask");
        if (lhClient == null) return;
        lhClient.close();
    }

    // extract fields
    // extract wf run id
    // extract wf name
    // if this operation fails, the SinkTask may throw a org. apache. kafka. connect. errors. RetriableExceptio
    // client timeout
    // manage schema
    @Override
    public void put(Collection<SinkRecord> records) {
        records.forEach(sinkRecord -> {
            System.out.println("HERE " + sinkRecord.value());
            lhClient.runWf(
                UUID.randomUUID().toString(),
                Map.of("name", sinkRecord.value())
            );
        });
    }

    // send the offset safe to commit
    @Override
    public Map<TopicPartition, OffsetAndMetadata> preCommit(
        Map<TopicPartition, OffsetAndMetadata> currentOffsets
    ) {
        return super.preCommit(currentOffsets);
    }

    @Override
    public void open(Collection<TopicPartition> partitions) {
        super.open(partitions);
    }

    @Override
    public void close(Collection<TopicPartition> partitions) {
        super.close(partitions);
    }
}
