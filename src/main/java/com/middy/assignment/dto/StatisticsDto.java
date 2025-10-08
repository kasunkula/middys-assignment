package com.middy.assignment.dto;

import java.math.BigDecimal;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import com.middy.assignment.model.Statistics;

@NoArgsConstructor
@AllArgsConstructor
@Getter
@Setter
@ToString
@EqualsAndHashCode
public class StatisticsDto {

    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal sum;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal avg;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal max;
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private BigDecimal min;
    private Long count;

    public StatisticsDto(Statistics stat) {
        this.sum = stat.getSum();
        this.avg = stat.getAvg();
        this.max = stat.getMax();
        this.min = stat.getMin();
        this.count = stat.getCount();
    }
}
