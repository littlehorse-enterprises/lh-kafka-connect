package io.littlehorse.example;

import io.littlehorse.sdk.worker.LHStructDef;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@LHStructDef("example-external-event-json-path-pilot")
public class Pilot {

    private String name;
    private Vehicle vehicle;

    @Override
    public String toString() {
        return JsonSerializer.serialize(this);
    }
}
