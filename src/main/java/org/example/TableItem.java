package org.example;

import lombok.Data;

import java.util.List;

@Data
public class TableItem {
    private String tableName;
    private String remark;
    private List<ColumnItem> columns;
}
