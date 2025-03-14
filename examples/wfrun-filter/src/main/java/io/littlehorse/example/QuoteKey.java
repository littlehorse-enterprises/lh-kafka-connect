package io.littlehorse.example;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class QuoteKey {
    private String wfSpecName;
    private UUID id;

    @Override
    public String toString() {
        return JsonSerializer.serialize(this);
    }
}
