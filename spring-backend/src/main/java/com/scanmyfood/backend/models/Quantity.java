package com.scanmyfood.backend.models;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.scanmyfood.backend.utils.FlexibleDoubleDeserializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class Quantity {
    @JsonDeserialize(using = FlexibleDoubleDeserializer.class)
    private Double value;
    private String unit;

    public void setValue(Double value) {
        if (value != null){
            this.value = Math.round(value * 100.0) / 100.0;
        } else {
            this.value = null;
        }
    }
}
