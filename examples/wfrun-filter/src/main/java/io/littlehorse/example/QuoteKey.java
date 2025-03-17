package io.littlehorse.example;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuoteKey {
    private String wfSpecName;
    private String id;

    @Override
    public String toString() {
        return JsonSerializer.serialize(this);
    }
}
