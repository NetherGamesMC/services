package org.nethergames.gsms.data.model;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.Map;

public class FluxDataPoint {
    @JsonProperty(required = true)
    private String name;
    @JsonProperty(required = true)
    private Map<String, String> labels;
    @JsonProperty(required = true)
    private Map<String, Object> fields;
    @JsonProperty(required = true)
    private long time;

    public FluxDataPoint(String name, Map<String, String> labels, Map<String, Object> fields, long time) {
        this.name = name;
        this.labels = labels;
        this.fields = fields;
        this.time = time;
    }

    public FluxDataPoint() {
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public Map<String, String> getLabels() {
        return labels;
    }

    public void setLabels(Map<String, String> labels) {
        this.labels = labels;
    }

    public Map<String, Object> getFields() {
        return fields;
    }

    public void setFields(Map<String, Object> fields) {
        this.fields = fields;
    }

    public long getTime() {
        return time;
    }

    public void setTime(long time) {
        this.time = time;
    }

    @Override
    public String toString() {
        return "FluxDataPoint{" +
                "name='" + name + '\'' +
                ", labels=" + labels +
                ", fields=" + fields +
                ", time=" + time +
                '}';
    }
}
