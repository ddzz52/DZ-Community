package com.dz.couple.module.period.dto;

public class PeriodPredictionVO {
    private Integer cycleDay;
    private Integer daysToNextPeriod;
    private String phase;

    private String nextPeriodStart;
    private String nextPeriodEnd;

    private String ovulationDay;
    private String fertileStart;
    private String fertileEnd;

    private String next2PeriodStart;
    private String next2PeriodEnd;

    private String ovulationDay2;
    private String fertileStart2;
    private String fertileEnd2;

    // 当前周期的经期（用于日历着色）
    private String currentPeriodStart;
    private String currentPeriodEnd;

    // 预测是否已过（需用户手动确认是否来潮）
    private boolean predictedDatePassed;

    public Integer getCycleDay() {
        return cycleDay;
    }

    public void setCycleDay(Integer cycleDay) {
        this.cycleDay = cycleDay;
    }

    public Integer getDaysToNextPeriod() {
        return daysToNextPeriod;
    }

    public void setDaysToNextPeriod(Integer daysToNextPeriod) {
        this.daysToNextPeriod = daysToNextPeriod;
    }

    public String getPhase() {
        return phase;
    }

    public void setPhase(String phase) {
        this.phase = phase;
    }

    public String getNextPeriodStart() {
        return nextPeriodStart;
    }

    public void setNextPeriodStart(String nextPeriodStart) {
        this.nextPeriodStart = nextPeriodStart;
    }

    public String getNextPeriodEnd() {
        return nextPeriodEnd;
    }

    public void setNextPeriodEnd(String nextPeriodEnd) {
        this.nextPeriodEnd = nextPeriodEnd;
    }

    public String getOvulationDay() {
        return ovulationDay;
    }

    public void setOvulationDay(String ovulationDay) {
        this.ovulationDay = ovulationDay;
    }

    public String getFertileStart() {
        return fertileStart;
    }

    public void setFertileStart(String fertileStart) {
        this.fertileStart = fertileStart;
    }

    public String getFertileEnd() {
        return fertileEnd;
    }

    public void setFertileEnd(String fertileEnd) {
        this.fertileEnd = fertileEnd;
    }

    public String getNext2PeriodStart() {
        return next2PeriodStart;
    }

    public void setNext2PeriodStart(String next2PeriodStart) {
        this.next2PeriodStart = next2PeriodStart;
    }

    public String getNext2PeriodEnd() {
        return next2PeriodEnd;
    }

    public void setNext2PeriodEnd(String next2PeriodEnd) {
        this.next2PeriodEnd = next2PeriodEnd;
    }

    public String getOvulationDay2() {
        return ovulationDay2;
    }

    public void setOvulationDay2(String ovulationDay2) {
        this.ovulationDay2 = ovulationDay2;
    }

    public String getFertileStart2() {
        return fertileStart2;
    }

    public void setFertileStart2(String fertileStart2) {
        this.fertileStart2 = fertileStart2;
    }

    public String getFertileEnd2() {
        return fertileEnd2;
    }

    public void setFertileEnd2(String fertileEnd2) {
        this.fertileEnd2 = fertileEnd2;
    }

    public String getCurrentPeriodStart() { return currentPeriodStart; }
    public void setCurrentPeriodStart(String currentPeriodStart) { this.currentPeriodStart = currentPeriodStart; }
    public String getCurrentPeriodEnd() { return currentPeriodEnd; }
    public void setCurrentPeriodEnd(String currentPeriodEnd) { this.currentPeriodEnd = currentPeriodEnd; }
    public boolean isPredictedDatePassed() { return predictedDatePassed; }
    public void setPredictedDatePassed(boolean predictedDatePassed) { this.predictedDatePassed = predictedDatePassed; }
}
