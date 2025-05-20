package org.aiacon.simuladordemobilidadeurbana.simulation;

public class Configuration {
    private double vehicleGenerationRate; // Veículos por segundo
    private int trafficLightMode;         // 1, 2, ou 3
    private boolean peakHour;             // Horário de pico
    private double simulationDuration;    // Duração total da simulação em segundos
    private int redirectThreshold;        // Limiar da fila para considerar redirecionamento

    public Configuration() {
        this.vehicleGenerationRate = 0.1; // Ex: 1 veículo a cada 10 segundos
        this.trafficLightMode = 1;        // Modo de tempo fixo por padrão
        this.peakHour = false;
        this.simulationDuration = 3600.0; // Padrão: 1 hora de simulação
        this.redirectThreshold = 5;       // Padrão: redirecionar se a fila tiver mais de 5 veículos
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

    public double getSimulationDuration() {
        return simulationDuration;
    }

    public void setSimulationDuration(double durationInSeconds) {
        this.simulationDuration = durationInSeconds;
    }

    public int getRedirectThreshold() {
        return redirectThreshold;
    }

    public void setRedirectThreshold(int threshold) {
        this.redirectThreshold = threshold;
    }
}