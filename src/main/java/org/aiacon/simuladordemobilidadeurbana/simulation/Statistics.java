package org.aiacon.simuladordemobilidadeurbana.simulation;

import org.aiacon.simuladordemobilidadeurbana.model.CustomLinkedList;
import org.aiacon.simuladordemobilidadeurbana.model.Graph;
import org.aiacon.simuladordemobilidadeurbana.model.TrafficLight;
import org.aiacon.simuladordemobilidadeurbana.model.Vehicle;

/**
 * Coleta e calcula estatísticas da simulação.
 * Esta classe rastreia o número de veículos gerados, veículos que chegaram ao destino,
 * tempos totais de viagem e espera, consumo total de combustível, e um índice de
 * congestionamento dinâmico.
 */
public class Statistics {
    private int vehiclesGenerated;
    private int vehiclesArrived;
    private double totalTravelTime;
    private double totalWaitTime;
    private double totalFuelConsumed;
    private double currentTime;
    private CustomLinkedList<Double> congestionIndexHistory; // Para armazenar o histórico do índice de congestionamento
    private double currentCongestionIndex; // O índice de congestionamento atual (percentual)
    private double maxRecordedCongestionRatio; // Para guardar o pico de congestionamento (percentual)

    /**
     * Construtor padrão para a classe Statistics.
     * Inicializa todas as contagens e totais em zero.
     */
    public Statistics() {
        this.vehiclesGenerated = 0;
        this.vehiclesArrived = 0;
        this.totalTravelTime = 0.0;
        this.totalWaitTime = 0.0;
        this.totalFuelConsumed = 0.0;
        this.currentTime = 0.0;
        this.currentCongestionIndex = 0.0;
        this.maxRecordedCongestionRatio = 0.0;
        this.congestionIndexHistory = new CustomLinkedList<>(); // Inicializa a lista
    }

    /**
     * Incrementa o contador de veículos gerados.
     */
    public synchronized void vehicleGenerated() {
        this.vehiclesGenerated++;
    }

    /**
     * Registra a chegada de um veículo ao seu destino e acumula suas estatísticas.
     *
     * @param travelTime O tempo total de viagem do veículo.
     * @param waitTime O tempo total que o veículo passou esperando.
     * @param fuelConsumedByVehicle O total de combustível consumido pelo veículo.
     */
    public synchronized void vehicleArrived(double travelTime, double waitTime, double fuelConsumedByVehicle) {
        this.vehiclesArrived++;
        this.totalTravelTime += travelTime;
        this.totalWaitTime += waitTime;
        this.totalFuelConsumed += fuelConsumedByVehicle;
    }

    /**
     * Atualiza o tempo corrente da simulação para referência nas estatísticas.
     * @param time O tempo atual da simulação.
     */
    public synchronized void updateCurrentTime(double time) {
        this.currentTime = time;
    }

    /**
     * Calcula e atualiza o índice de congestionamento atual da simulação.
     * O índice é baseado na densidade de veículos e na proporção de veículos enfileirados.
     * O valor calculado é armazenado em {@code currentCongestionIndex} como uma porcentagem
     * e também adicionado ao {@code congestionIndexHistory} para cálculo posterior da média.
     *
     * @param activeVehicles Lista de todos os veículos atualmente ativos na simulação.
     * @param graph          O grafo da rede urbana.
     */
    public synchronized void calculateCurrentCongestion(CustomLinkedList<Vehicle> activeVehicles, Graph graph) {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty() || activeVehicles == null) {
            this.currentCongestionIndex = 0.0;
            if (this.congestionIndexHistory != null) { // Adiciona 0 se não houver dados
                this.congestionIndexHistory.add(0.0);
            }
            return;
        }

        int numberOfActiveVehicles = activeVehicles.size();
        int totalNodes = graph.getNodes().size();
        int totalQueuedVehicles = 0;

        if (graph.getTrafficLights() != null) {
            for (TrafficLight tl : graph.getTrafficLights()) {
                if (tl != null) {
                    totalQueuedVehicles += tl.getTotalVehiclesInQueues();
                }
            }
        }

        if (totalNodes == 0) {
            // Se não houver nós, a definição de congestionamento fica ambígua.
            // Poderia ser 100% se houver veículos, ou 0% se não houver.
            // Manteremos um valor que indique erro ou situação anômala se preferir.
            // Por agora, se não há nós, o conceito de densidade é problemático.
            // Usando uma métrica que ainda possa ter algum significado (soma de veículos).
            this.currentCongestionIndex = (numberOfActiveVehicles + totalQueuedVehicles > 0) ? 100.0 : 0.0; // Saturação se houver qqr carro
        } else {
            double vehicleDensityRatio = (double) numberOfActiveVehicles / totalNodes;
            double queuedVehicleRatio = (numberOfActiveVehicles > 0) ? (double) totalQueuedVehicles / numberOfActiveVehicles : 0.0;

            // Combinação ponderada (ajuste os pesos conforme achar melhor)
            // 0.3 para densidade e 0.7 para proporção em fila para dar mais peso aos parados
            double rawCongestionScore = (0.3 * vehicleDensityRatio) + (0.7 * queuedVehicleRatio);

            // Normaliza para um valor percentual (0-100)
            // Math.min(1.0, ...) garante que não passe de 100% a menos que a interpretação de rawCongestionScore permita.
            // Se rawCongestionScore for concebido para variar naturalmente acima de 1 em casos extremos,
            // o Math.min(1.0,...) pode ser removido ou ajustado.
            this.currentCongestionIndex = Math.min(1.0, rawCongestionScore) * 100.0;
        }

        // Atualiza o pico de congestionamento
        if (this.currentCongestionIndex > this.maxRecordedCongestionRatio) {
            this.maxRecordedCongestionRatio = this.currentCongestionIndex;
        }

        // Adiciona o índice atual ao histórico para cálculo da média
        if (this.congestionIndexHistory != null) {
            this.congestionIndexHistory.add(this.currentCongestionIndex);
        }
    }

    /**
     * Retorna o índice de congestionamento calculado mais recentemente, como uma porcentagem (0-100).
     * @return O índice de congestionamento atual como porcentagem.
     */
    public synchronized double getCurrentCongestionIndex() {
        return this.currentCongestionIndex;
    }

    /**
     * Retorna o maior índice de congestionamento (como porcentagem) registrado durante a simulação.
     * @return O pico de congestionamento registrado.
     */
    public synchronized double getMaxRecordedCongestionRatio() {
        return maxRecordedCongestionRatio;
    }

    /**
     * Calcula e retorna o índice médio de congestionamento registrado durante toda a simulação.
     * @return A média do índice de congestionamento como porcentagem, ou 0.0 se nenhum dado foi registrado.
     */
    public synchronized double getAverageCongestionIndex() {
        if (congestionIndexHistory == null || congestionIndexHistory.isEmpty()) {
            return 0.0;
        }
        double sum = 0;
        int count = 0;
        // Assumindo que CustomLinkedList é iterável ou tem get(index) e size()
        for (int i = 0; i < congestionIndexHistory.size(); i++) {
            Double congestionValue = congestionIndexHistory.get(i); // Ajuste se o método de acesso for diferente
            if (congestionValue != null) {
                sum += congestionValue;
                count++;
            }
        }
        return (count > 0) ? (sum / count) : 0.0;
    }


    public synchronized int getTotalVehiclesGenerated() {
        return vehiclesGenerated;
    }
    public synchronized int getArrivedCount() { // Mantive este nome pois é comum
        return vehiclesArrived;
    }
    public synchronized double getAverageTravelTime() {
        if (vehiclesArrived == 0) return 0.0;
        return totalTravelTime / vehiclesArrived;
    }
    public synchronized double getAverageWaitTime() {
        if (vehiclesArrived == 0) return 0.0;
        return totalWaitTime / vehiclesArrived;
    }
    public synchronized double getTotalFuelConsumed() {
        return totalFuelConsumed;
    }
    public synchronized double getAverageFuelConsumptionPerVehicle() {
        if (vehiclesArrived == 0) return 0.0;
        return totalFuelConsumed / vehiclesArrived;
    }

    public int getVehiclesArrived() {
        return vehiclesArrived;
    }

    public double getCurrentTime() { // Não precisa ser synchronized se apenas lido após simulação
        return currentTime;
    }

    public synchronized void printSummary() {
        System.out.println("\n--- RESUMO DA SIMULAÇÃO ---");
        System.out.printf("Tempo Total de Simulação: %.2fs%n", currentTime);
        System.out.printf("Total de Veículos Gerados: %d%n", vehiclesGenerated);
        System.out.printf("Total de Veículos Chegados ao Destino: %d%n", vehiclesArrived);

        if (vehiclesArrived > 0) {
            System.out.printf("Tempo Médio de Viagem por Veículo: %.2fs%n", getAverageTravelTime());
            System.out.printf("Tempo Médio de Espera por Veículo: %.2fs%n", getAverageWaitTime());
            System.out.printf("Consumo Total de Combustível (veículos chegados): %.3f L%n", totalFuelConsumed);
            System.out.printf("Consumo Médio de Combustível por Veículo Chegado: %.3f L%n", getAverageFuelConsumptionPerVehicle());
        } else {
            System.out.println("Nenhum veículo chegou ao destino para calcular médias detalhadas.");
        }
        System.out.printf("Pico de Congestionamento Registrado (Índice Percentual): %.2f%%%n", maxRecordedCongestionRatio);
        // Adiciona a média de congestionamento ao resumo
        System.out.printf("Média de Congestionamento Registrado (Índice Percentual): %.2f%%%n", getAverageCongestionIndex());
        System.out.println("---------------------------\n");
    }
}