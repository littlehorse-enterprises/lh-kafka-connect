package io.littlehorse.example;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Quote {

    private String quote;
    private Integer length;

    @Override
    public String toString() {
        return JsonSerializer.serialize(this);
    }
}
