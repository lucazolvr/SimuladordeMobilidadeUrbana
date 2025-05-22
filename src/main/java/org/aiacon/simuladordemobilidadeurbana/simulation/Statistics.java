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

    private double currentCongestionIndex;
    private double maxRecordedCongestionRatio; // Para guardar o pico de congestionamento normalizado

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
    }

    /**
     * Incrementa o contador de veículos gerados.
     * Deve ser chamado sempre que um novo veículo é introduzido na simulação.
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
     * <p>
     * A métrica de congestionamento é calculada considerando dois componentes principais:
     * <ol>
     * <li><b>Densidade de Veículos:</b> A proporção de veículos ativos em relação ao número total de nós no grafo.
     * Um valor mais alto aqui sugere que a rede está mais "cheia".</li>
     * <li><b>Proporção de Veículos Enfileirados:</b> A porcentagem de veículos ativos que estão atualmente
     * parados em filas de semáforos. Um valor alto aqui indica que muitos veículos estão parados.</li>
     * </ol>
     * Estes dois componentes são combinados, com um peso maior dado à proporção de veículos enfileirados,
     * para gerar um índice de congestionamento normalizado (idealmente entre 0 e 1, mas pode exceder 1
     * em situações extremas se a normalização não for perfeita ou se a rede estiver supersaturada).
     * <p>
     * O método também rastreia o {@code maxRecordedCongestionRatio}.
     *
     * @param activeVehicles Lista de todos os veículos atualmente ativos na simulação.
     * @param graph          O grafo da rede urbana, usado para acessar semáforos e o número total de nós.
     */
    public synchronized void calculateCurrentCongestion(CustomLinkedList<Vehicle> activeVehicles, Graph graph) {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty() || activeVehicles == null) {
            this.currentCongestionIndex = 0.0;
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

        if (totalNodes == 0) { // Evita divisão por zero se, por algum motivo, não houver nós
            this.currentCongestionIndex = numberOfActiveVehicles + totalQueuedVehicles; // Fallback para a métrica antiga
            return;
        }

        // Componente 1: Densidade de veículos na rede (0 a 1, pode ser > 1 se muitos carros por nó)
        double vehicleDensityRatio = (double) numberOfActiveVehicles / totalNodes;

        // Componente 2: Proporção de veículos ativos que estão em filas (0 a 1)
        double queuedVehicleRatio = (numberOfActiveVehicles > 0) ? (double) totalQueuedVehicles / numberOfActiveVehicles : 0.0;

        // Combinação ponderada. Exemplo: 40% da densidade, 60% da proporção em fila.
        // Essa ponderação e a forma de combinar podem ser ajustadas.
        // O objetivo é um índice que "sinta" mais o congestionamento quando muitos estão parados.
        double rawCongestionScore = (0.4 * vehicleDensityRatio) + (0.6 * queuedVehicleRatio);

        // Normalizar para um valor entre 0 e 1 (aproximadamente).
        // Se rawCongestionScore puder, teoricamente, passar de 1 (ex: densidade > 1 e todos em fila),
        // podemos "clipar" em 1 ou usar uma função de escalonamento diferente.
        // Por agora, vamos assumir que rawCongestionScore é uma boa representação entre 0 e 1.
        this.currentCongestionIndex = Math.min(1.0, rawCongestionScore) * 100; // Apresentar como porcentagem

        // Atualiza o pico de congestionamento
        if (this.currentCongestionIndex > this.maxRecordedCongestionRatio) {
            this.maxRecordedCongestionRatio = this.currentCongestionIndex;
        }

        // Para o log, vamos manter a métrica antiga que é mais fácil de interpretar o aumento/diminuição absoluto.
        // A métrica this.currentCongestionIndex (percentual) pode ser usada no visualizer.
        // System.out.println("DEBUG_CONGESTION: Ativos=" + numberOfActiveVehicles + ", Enfileirados=" + totalQueuedVehicles + ", ÍndicePercentual=" + String.format("%.2f", this.currentCongestionIndex) + "%");
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
     * Retorna o número total de veículos gerados durante a simulação.
     * @return O total de veículos gerados.
     */
    public synchronized int getTotalVehiclesGenerated() {
        return vehiclesGenerated;
    }

    /**
     * Retorna o número de veículos que chegaram aos seus destinos.
     * @return O total de veículos que chegaram.
     */
    public synchronized int getArrivedCount() {
        return vehiclesArrived;
    }

    /**
     * Calcula e retorna o tempo médio de viagem para os veículos que chegaram ao destino.
     * @return O tempo médio de viagem em segundos, ou 0.0 se nenhum veículo chegou.
     */
    public synchronized double getAverageTravelTime() {
        if (vehiclesArrived == 0) {
            return 0.0;
        }
        return totalTravelTime / vehiclesArrived;
    }

    /**
     * Calcula e retorna o tempo médio de espera para os veículos que chegaram ao destino.
     * @return O tempo médio de espera em segundos, ou 0.0 se nenhum veículo chegou.
     */
    public synchronized double getAverageWaitTime() {
        if (vehiclesArrived == 0) {
            return 0.0;
        }
        return totalWaitTime / vehiclesArrived;
    }

    /**
     * Retorna o consumo total de combustível acumulado de todos os veículos que chegaram ao destino.
     * @return O consumo total de combustível na unidade definida (ex: litros).
     */
    public synchronized double getTotalFuelConsumed() {
        return totalFuelConsumed;
    }

    /**
     * Calcula e retorna o consumo médio de combustível por veículo que chegou ao destino.
     * @return O consumo médio de combustível (ex: em litros), ou 0.0 se nenhum veículo chegou.
     */
    public synchronized double getAverageFuelConsumptionPerVehicle() {
        if (vehiclesArrived == 0) {
            return 0.0;
        }
        return totalFuelConsumed / vehiclesArrived;
    }

    public int getVehiclesArrived() {
        return vehiclesArrived;
    }

    public double getCurrentTime() {
        return currentTime;
    }

    /**
     * Imprime um resumo das estatísticas da simulação no console.
     * Inclui informações sobre veículos, tempos médios, consumo de combustível e pico de congestionamento.
     */
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
        System.out.println("---------------------------\n");
    }
}