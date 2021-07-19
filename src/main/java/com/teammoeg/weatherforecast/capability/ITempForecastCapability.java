package com.teammoeg.weatherforecast.capability;

public interface ITempForecastCapability {
    int getRainTime();
    int getThunderTime();
    int getClearTime();
    void setRainTime(int rainTime);
    void setThunderTime(int thunderTime);
    void setClearTime(int clearTime);
    boolean getIsRaining();
    boolean getIsThunder();
    void setIsRaining(boolean isRaining);
    void setIsThunder(boolean isThunder);
    boolean getEveningForecastStatus();
    void setEveningForecastStatus(boolean forecastStatus);
    boolean getMorningForecastStatus();
    void setMorningForecastStatus(boolean forecastStatus);
}
