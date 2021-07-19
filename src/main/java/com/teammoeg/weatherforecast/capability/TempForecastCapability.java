package com.teammoeg.weatherforecast.capability;

public class TempForecastCapability implements ITempForecastCapability {

    private int rainTime, thunderTime, clearTime;
    private boolean isRaining, isThunder;
    private boolean eveningStatus = false;
    private boolean morningStatus = false;

    public TempForecastCapability(int rainTime, int thunderTime, int clearTime, boolean isRaining, boolean isThunder) {
        this.rainTime = rainTime;
        this.thunderTime = thunderTime;
        this.clearTime = clearTime;
        this.isRaining = isRaining;
        this.isThunder = isThunder;
    }

    @Override
    public int getRainTime() {
        return rainTime;
    }

    @Override
    public int getThunderTime() {
        return thunderTime;
    }

    @Override
    public int getClearTime() {
        return clearTime;
    }

    @Override
    public void setRainTime(int rainTime) {
        this.rainTime = rainTime;
    }

    @Override
    public void setThunderTime(int thunderTime) {
        this.thunderTime = thunderTime;
    }

    @Override
    public void setClearTime(int clearTime) {
        this.clearTime = clearTime;
    }

    @Override
    public boolean getIsRaining() {
        return isRaining;
    }

    @Override
    public boolean getIsThunder() {
        return isThunder;
    }

    @Override
    public void setIsRaining(boolean isRaining) {
        this.isRaining = isRaining;
    }

    @Override
    public void setIsThunder(boolean isThunder) {
        this.isThunder = isThunder;
    }

    @Override
    public boolean getEveningForecastStatus() {
        return eveningStatus;
    }

    @Override
    public void setEveningForecastStatus(boolean forecastStatus) {
        this.eveningStatus = forecastStatus;
    }

    @Override
    public boolean getMorningForecastStatus() {
        return morningStatus;
    }

    @Override
    public void setMorningForecastStatus(boolean forecastStatus) {
        this.morningStatus = forecastStatus;
    }
}
