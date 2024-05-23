package com.asb.analytics.bigquery;

import java.util.Random;

public class Row<T> {
    private int insertId = new Random().nextInt(100000);

    private T json;

    public Row(T json) {
        this.json = json;
    }

    public T getJson() {
        return json;
    }

    public void setJson(T json) {
        this.json = json;
    }

    public int getInsertId() {
        return insertId;
    }

    public void setInsertId(int insertId) {
        this.insertId = insertId;
    }
}