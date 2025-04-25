package io.littlehorse.connect;

import lombok.Getter;

import org.apache.kafka.common.config.ConfigDef;
import org.apache.kafka.common.config.ConfigDef.Importance;
import org.apache.kafka.common.config.ConfigDef.Type;

import java.util.Map;

@Getter
public class WfRunSinkConnectorConfig extends LHSinkConnectorConfig {
    public static final String WF_SPEC_NAME_KEY = "wf.spec.name";
    public static final String WF_SPEC_MAJOR_VERSION_KEY = "wf.spec.major.version";
    public static final String WF_SPEC_REVISION_KEY = "wf.spec.revision";
    public static final String WF_RUN_PARENT_ID_KEY = "wf.run.parent.id";

    public static final ConfigDef CONFIG_DEF = new ConfigDef(LHSinkConnectorConfig.BASE_CONFIG_DEF)
            .define(
                    WF_SPEC_NAME_KEY,
                    Type.STRING,
                    Importance.HIGH,
                    "The name of the WfSpec to run.")
            .define(
                    WF_SPEC_MAJOR_VERSION_KEY,
                    Type.INT,
                    null,
                    Importance.LOW,
                    "Optionally specify the major version of the WfSpec to run. This guarantees that the \"signature\" of the WfSpec (i.e. the required input variables, and searchable variables) will not change for this app.")
            .define(
                    WF_SPEC_REVISION_KEY,
                    Type.INT,
                    null,
                    Importance.LOW,
                    "Optionally specify the specific revision of the WfSpec to run. It is not recommended to use this in practice, as the WfSpec logic should be de-coupled from the applications that run WfRun's.")
            .define(
                    WF_RUN_PARENT_ID_KEY,
                    Type.STRING,
                    null,
                    Importance.LOW,
                    "Optionally specify the parent WfRunId");

    private final String wfSpecName;
    private final Integer wfSpecRevision;
    private final Integer wfSpecMajorVersion;
    private final String wfRunParentId;

    public WfRunSinkConnectorConfig(Map<?, ?> props) {
        super(CONFIG_DEF, props);
        wfSpecName = getString(WF_SPEC_NAME_KEY);
        wfSpecRevision = getInt(WF_SPEC_REVISION_KEY);
        wfSpecMajorVersion = getInt(WF_SPEC_MAJOR_VERSION_KEY);
        wfRunParentId = getString(WF_RUN_PARENT_ID_KEY);
    }
}
