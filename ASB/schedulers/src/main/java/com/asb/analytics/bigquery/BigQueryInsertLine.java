package com.asb.analytics.bigquery;

import java.util.List;

public class BigQueryInsertLine {

    boolean ignoreUnknownValues = true;
    List<Row> rows;

    public BigQueryInsertLine(List<Row> rows) {
        this.rows = rows;
    }
}
