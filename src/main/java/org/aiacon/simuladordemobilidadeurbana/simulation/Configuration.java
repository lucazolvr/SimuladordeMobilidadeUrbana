package org.aiacon.simuladordemobilidadeurbana.simulation;

// Configurações da simulação
public class Configuration {
    private double vehicleGenerationRate; // Veículos por segundo
    private int trafficLightMode; // 1, 2, ou 3
    private boolean peakHour; // Horário de pico

    public Configuration() {
        vehicleGenerationRate = 0.1;
        trafficLightMode = 1;
        peakHour = false;
    }

    public double getVehicleGenerationRate() {
        return vehicleGenerationRate;
    }

    public void setVehicleGenerationRate(double rate) {
        this.vehicleGenerationRate = rate;
    }

    public int getTrafficLightMode() {
        return trafficLightMode;
    }

    public void setTrafficLightMode(int mode) {
        this.trafficLightMode = mode;
    }

    public boolean isPeakHour() {
        return peakHour;
    }

    public void setPeakHour(boolean peakHour) {
        this.peakHour = peakHour;
    }
}
