package io.littlehorse.example;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Squadron {

    private String id;

    private List<SquadronUnit> units;

    @Override
    public String toString() {
        return JsonSerializer.serialize(this);
    }
}
