package io.littlehorse.kafka.connect;

import java.util.Collection;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.apache.kafka.connect.sink.SinkRecord;
import org.apache.kafka.connect.sink.SinkTask;

@Slf4j
public class LHSinkTask extends SinkTask {

    @Override
    public String version() {
        return "";
    }

    @Override
    public void start(Map<String, String> props) {}

    @Override
    public void put(Collection<SinkRecord> records) {}

    @Override
    public void stop() {}
}
