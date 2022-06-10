package org.example;

import lombok.Data;

@Data
public class ColumnItem {
    private String primary;
    private String remark;
    private String name;
    private String type;
    private int length;
    private String nullable;
    private int scale;
}
