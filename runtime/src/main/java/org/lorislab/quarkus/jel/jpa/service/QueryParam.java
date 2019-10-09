package org.lorislab.quarkus.jel.jpa.service;

import java.util.HashMap;
import java.util.Map;

public class QueryParam {

    private Map<String, Object> map = new HashMap<>();

    public static QueryParam with(String parameter, Object value) {
        return new QueryParam().and(parameter, value);
    }

    public QueryParam and(String parameter, Object value) {
        map.put(parameter, value);
        return this;
    }

    public Map<String, Object> map() {
        return map;
    }

    @Override
    public String toString() {
        return map.toString();
    }
}
