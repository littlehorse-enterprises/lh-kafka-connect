package io.littlehorse.example;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Person {

    private String name;
    private Vehicle vehicle;

    @Override
    public String toString() {
        return JsonSerializer.serialize(this);
    }
}
