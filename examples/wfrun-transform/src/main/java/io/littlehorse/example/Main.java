package io.littlehorse.example;

import io.littlehorse.sdk.common.config.LHConfig;
import io.littlehorse.sdk.common.proto.StructDefCompatibilityType;
import io.littlehorse.sdk.wfsdk.WfRunVariable;
import io.littlehorse.sdk.wfsdk.Workflow;
import io.littlehorse.sdk.worker.LHTaskMethod;
import io.littlehorse.sdk.worker.LHTaskWorker;

import lombok.extern.slf4j.Slf4j;

import java.util.List;

@Slf4j
public class Main {

    public static final String TASK_DEF_NAME = "example-wfrun-transform-announce";
    public static final String WF_NAME = "example-wfrun-transform";
    public static final String VARIABLE_FILM = "film";
    public static final String VARIABLE_EPISODE = "episode";
    public static final String VARIABLE_CAST = "cast";
    public static final String VARIABLE_SUMMARY = "summary";
    public static final String VARIABLE_BOX_OFFICE = "boxOffice";
    public static final String VARIABLE_FRANCHISE = "franchise";

    public static Workflow getWorkflow() {
        return Workflow.newWorkflow(WF_NAME, wf -> {
            // The JsonPathMapperTransform builds these variables from each record:
            //   - film: a two-level STRUCT (Film with a nested Director) from flat fields.
            //   - episode: an INT read directly from the record.
            //   - cast: a JSON_ARR read directly from the record's array field.
            //   - summary: a STR derived with the JSONPath concat() function.
            //   - boxOffice: a DOUBLE derived with the JSONPath sum() function.
            // The LiteralMapperTransform then merges a constant onto the value:
            //   - franchise: a STRUCT (name + producer) with the same constant value for
            //     every record, built from nested literal mappings.
            WfRunVariable film = wf.declareStruct(VARIABLE_FILM, Film.class);
            WfRunVariable episode = wf.declareInt(VARIABLE_EPISODE);
            WfRunVariable cast = wf.declareJsonArr(VARIABLE_CAST);
            WfRunVariable summary = wf.declareStr(VARIABLE_SUMMARY);
            WfRunVariable boxOffice = wf.declareDouble(VARIABLE_BOX_OFFICE);
            WfRunVariable franchise = wf.declareStruct(VARIABLE_FRANCHISE, Franchise.class);
            wf.execute(TASK_DEF_NAME, film, episode, cast, summary, boxOffice, franchise);
        });
    }

    private static LHTaskWorker getTaskWorker(LHConfig lhConfig) {
        LHTaskWorker worker = new LHTaskWorker(new AnnounceWorker(), TASK_DEF_NAME, lhConfig);
        Runtime.getRuntime().addShutdownHook(new Thread(worker::close));
        return worker;
    }

    public static void main(String[] args) {
        LHConfig lhConfig = new LHConfig();

        LHTaskWorker worker = getTaskWorker(lhConfig);

        // StructDefs must be registered before the TaskDef and WfSpec that use them.
        // Dependencies first: Director is nested inside Film.
        worker.registerStructDef(Director.class, StructDefCompatibilityType.NO_SCHEMA_UPDATES);
        worker.registerStructDef(Film.class, StructDefCompatibilityType.NO_SCHEMA_UPDATES);
        worker.registerStructDef(Franchise.class, StructDefCompatibilityType.NO_SCHEMA_UPDATES);

        worker.registerTaskDef();

        Workflow workflow = getWorkflow();
        workflow.registerWfSpec(lhConfig.getBlockingStub());

        worker.start();
    }

    public static class AnnounceWorker {

        @LHTaskMethod(TASK_DEF_NAME)
        public String announce(
                Film film,
                long episode,
                List<String> cast,
                String summary,
                double boxOffice,
                Franchise franchise) {
            String message = String.format(
                    "[%s by %s] Episode %d: %s by %s grossed $%.2fM, starring %s (%s)",
                    franchise.getName(),
                    franchise.getProducer(),
                    episode,
                    film.getTitle(),
                    film.getDirector().getName(),
                    boxOffice,
                    String.join(", ", cast),
                    summary);
            log.info(message);
            return message;
        }
    }
}
