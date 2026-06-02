package com.dz.couple.module.period.dto;

public class PeriodStatusVO {
    private PeriodSettingsVO settings;
    private PeriodPredictionVO prediction;

    public PeriodSettingsVO getSettings() {
        return settings;
    }

    public void setSettings(PeriodSettingsVO settings) {
        this.settings = settings;
    }

    public PeriodPredictionVO getPrediction() {
        return prediction;
    }

    public void setPrediction(PeriodPredictionVO prediction) {
        this.prediction = prediction;
    }
}

