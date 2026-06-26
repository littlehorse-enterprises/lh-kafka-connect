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
@LHStructDef("example-wfrun-transform-franchise")
public class Franchise {

    private String name;
    private String producer;

    @Override
    public String toString() {
        return JsonSerializer.serialize(this);
    }
}
