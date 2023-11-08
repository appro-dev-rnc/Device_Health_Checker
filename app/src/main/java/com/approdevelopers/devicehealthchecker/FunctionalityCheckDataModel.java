package com.approdevelopers.devicehealthchecker;

import java.util.List;

public class FunctionalityCheckDataModel {

    List<FunctionalityCheck> functionalityChecks;
    String timestamp;

    public FunctionalityCheckDataModel() {
    }

    public List<FunctionalityCheck> getFunctionalityChecks() {
        return functionalityChecks;
    }

    public void setFunctionalityChecks(List<FunctionalityCheck> functionalityChecks) {
        this.functionalityChecks = functionalityChecks;
    }

    public String getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(String timestamp) {
        this.timestamp = timestamp;
    }

    @Override
    public String toString() {
        return "FunctionalityCheckDataModel{" +
                "functionalityChecks=" + functionalityChecks +
                ", timestamp='" + timestamp + '\'' +
                '}';
    }
}
