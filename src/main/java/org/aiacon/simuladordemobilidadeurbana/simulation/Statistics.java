package org.aiacon.simuladordemobilidadeurbana.simulation;

// Registra estatísticas da simulação
public class Statistics {
    private int vehiclesGenerated;
    private int vehiclesArrived;
    private double totalTravelTime;
    private double totalWaitTime;
    private int maxCongestion;

    public Statistics() {
        vehiclesGenerated = 0;
        vehiclesArrived = 0;
        totalTravelTime = 0.0;
        totalWaitTime = 0.0;
        maxCongestion = 0;
    }

    public void vehicleGenerated() {
        vehiclesGenerated++;
    }

    public void vehicleArrived(double travelTime, double waitTime) {
        vehiclesArrived++;
        totalTravelTime += travelTime;
        totalWaitTime += waitTime;
    }

    public void updateCongestion(int congestion) {
        if (congestion > maxCongestion) {
            maxCongestion = congestion;
        }
    }

    public int getCongestion() {
        // Soma de todas as filas
        return maxCongestion; // Simplificado, pode ser expandido
    }

    public void printSummary() {
        System.out.println("Resumo da Simulação:");
        System.out.printf("Veículos Gerados: %d%n", vehiclesGenerated);
        System.out.printf("Veículos Chegados: %d%n", vehiclesArrived);
        System.out.printf("Tempo Médio de Viagem: %.2fs%n", vehiclesArrived > 0 ? totalTravelTime / vehiclesArrived : 0);
        System.out.printf("Tempo Médio de Espera: %.2fs%n", vehiclesArrived > 0 ? totalWaitTime / vehiclesArrived : 0);
        System.out.printf("Congestionamento Máximo: %d veículos%n", maxCongestion);
    }
}
