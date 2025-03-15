package io.littlehorse.example;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonInclude.Include;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Builder
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Character {

    private String name;

    @JsonInclude(Include.NON_NULL)
    private String description;

    @Override
    public String toString() {
        return JsonSerializer.serialize(this);
    }
}
