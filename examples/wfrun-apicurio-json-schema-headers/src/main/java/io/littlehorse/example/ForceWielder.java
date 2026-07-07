package io.littlehorse.example;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForceWielder {

    private String name;
    private String type;
    private String lightsaberColor;

    @Override
    public String toString() {
        return JsonSerializer.serialize(this);
    }
}
