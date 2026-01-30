package io.littlehorse.example;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Payment {

    private String id;
    private String droid;
    private double credits;

    @Override
    public String toString() {
        return JsonSerializer.serialize(this);
    }
}
