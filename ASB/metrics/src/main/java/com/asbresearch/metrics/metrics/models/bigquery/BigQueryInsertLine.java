package com.asbresearch.metrics.metrics.models.bigquery;

import java.util.List;

public class BigQueryInsertLine {

    List<Row> rows;

    public BigQueryInsertLine(List<Row> rows) {
        this.rows = rows;
    }
}
