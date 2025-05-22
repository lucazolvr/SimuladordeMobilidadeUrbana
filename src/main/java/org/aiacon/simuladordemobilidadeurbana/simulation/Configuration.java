package org.aiacon.simuladordemobilidadeurbana.simulation;

public class Configuration {
    private double vehicleGenerationRate;
    private double simulationDuration;
    private int trafficLightMode;
    private int redirectThreshold;
    private boolean peakHour;

    private double fixedGreenTime;
    private double fixedYellowTime;

    private double adaptiveBaseGreen;
    private double adaptiveYellowTime;
    private double adaptiveMaxGreen; // Teto máximo para o verde adaptativo
    private double adaptiveMinGreenTime; // Mínimo absoluto para verde adaptativo
    private double adaptiveIncrement; // Incremento por veículo acima do threshold
    private int adaptiveQueueThreshold;

    private double energySavingBaseGreen;
    private double energySavingYellowTime;
    private double energySavingMinGreen;
    private int energySavingThreshold;
    private double energySavingMaxGreenTime; // Teto máximo para verde no modo economia

    private double vehicleGenerationStopTime;

    public Configuration() {
        this.vehicleGenerationRate = 0.3; // Ajustado para testes de calibração
        this.simulationDuration = 600.0;
        this.trafficLightMode = 1;
        this.redirectThreshold = 0; // Redirecionamento desabilitado por padrão
        this.peakHour = false;
        this.vehicleGenerationStopTime = 300.0;

        this.fixedGreenTime = 15.0;
        this.fixedYellowTime = 3.0;

        this.adaptiveBaseGreen = 10.0;
        this.adaptiveYellowTime = 3.0;
        this.adaptiveMaxGreen = 30.0;
        this.adaptiveMinGreenTime = 7.0;
        this.adaptiveIncrement = 1.0;
        this.adaptiveQueueThreshold = 2;

        this.energySavingBaseGreen = 20.0;
        this.energySavingYellowTime = 3.0;
        this.energySavingMinGreen = 7.0;
        this.energySavingThreshold = 1;
        this.energySavingMaxGreenTime = 40.0;
    }

    // Getters e Setters
    public double getVehicleGenerationRate() { return vehicleGenerationRate; }
    public void setVehicleGenerationRate(double rate) { this.vehicleGenerationRate = rate; }

    public double getSimulationDuration() { return simulationDuration; }
    public void setSimulationDuration(double duration) { this.simulationDuration = duration; }

    public int getTrafficLightMode() { return trafficLightMode; }
    public void setTrafficLightMode(int mode) { this.trafficLightMode = mode; }

    public int getRedirectThreshold() { return redirectThreshold; }
    public void setRedirectThreshold(int threshold) { this.redirectThreshold = threshold; }

    public boolean isPeakHour() { return peakHour; }
    public void setPeakHour(boolean peakHour) { this.peakHour = peakHour; }

    public double getFixedGreenTime() { return fixedGreenTime; }
    public void setFixedGreenTime(double fixedGreenTime) { this.fixedGreenTime = fixedGreenTime; }
    public double getFixedYellowTime() { return fixedYellowTime; }
    public void setFixedYellowTime(double fixedYellowTime) { this.fixedYellowTime = fixedYellowTime; }

    public double getAdaptiveBaseGreen() { return adaptiveBaseGreen; }
    public void setAdaptiveBaseGreen(double adaptiveBaseGreen) { this.adaptiveBaseGreen = adaptiveBaseGreen; }
    public double getAdaptiveYellowTime() { return adaptiveYellowTime; }
    public void setAdaptiveYellowTime(double adaptiveYellowTime) { this.adaptiveYellowTime = adaptiveYellowTime; }
    public double getAdaptiveMaxGreen() { return adaptiveMaxGreen; }
    public void setAdaptiveMaxGreen(double adaptiveMaxGreen) { this.adaptiveMaxGreen = adaptiveMaxGreen; }
    public double getAdaptiveIncrement() { return adaptiveIncrement; }
    public void setAdaptiveIncrement(double adaptiveIncrement) { this.adaptiveIncrement = adaptiveIncrement; }
    public int getAdaptiveQueueThreshold() { return adaptiveQueueThreshold; }
    public void setAdaptiveQueueThreshold(int adaptiveQueueThreshold) { this.adaptiveQueueThreshold = adaptiveQueueThreshold; }
    public double getAdaptiveMinGreenTime() { return adaptiveMinGreenTime; }
    public void setAdaptiveMinGreenTime(double adaptiveMinGreenTime) { this.adaptiveMinGreenTime = adaptiveMinGreenTime; }

    public double getEnergySavingBaseGreen() { return energySavingBaseGreen; }
    public void setEnergySavingBaseGreen(double energySavingBaseGreen) { this.energySavingBaseGreen = energySavingBaseGreen; }
    public double getEnergySavingYellowTime() { return energySavingYellowTime; }
    public void setEnergySavingYellowTime(double energySavingYellowTime) { this.energySavingYellowTime = energySavingYellowTime; }
    public double getEnergySavingMinGreen() { return energySavingMinGreen; }
    public void setEnergySavingMinGreen(double energySavingMinGreen) { this.energySavingMinGreen = energySavingMinGreen; }
    public int getEnergySavingThreshold() { return energySavingThreshold; }
    public void setEnergySavingThreshold(int energySavingThreshold) { this.energySavingThreshold = energySavingThreshold; }
    public double getEnergySavingMaxGreenTime() { return energySavingMaxGreenTime; }
    public void setEnergySavingMaxGreenTime(double energySavingMaxGreenTime) { this.energySavingMaxGreenTime = energySavingMaxGreenTime; }

    public double getVehicleGenerationStopTime() { return vehicleGenerationStopTime; }
    public void setVehicleGenerationStopTime(double vehicleGenerationStopTime) { this.vehicleGenerationStopTime = vehicleGenerationStopTime; }
}