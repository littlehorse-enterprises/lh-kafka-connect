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
public class Droids {

    private List<Droid> droids;

    @Override
    public String toString() {
        return JsonSerializer.serialize(this);
    }
}
