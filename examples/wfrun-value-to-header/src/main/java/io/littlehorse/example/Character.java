package io.littlehorse.example;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Character {

    private String id;
    private String name;

    @Override
    public String toString() {
        return JsonSerializer.serialize(this);
    }
}
