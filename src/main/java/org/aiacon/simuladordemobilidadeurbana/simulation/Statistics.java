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
    private double totalFuelConsumed; // Em litros, por exemplo
    private double currentTime; // Tempo atual da simulação para contextualizar as estatísticas
    private double averageFuelConsumptionPerVehicle;

    // Para congestionamento dinâmico
    private double currentCongestionIndex;

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
        this.averageFuelConsumptionPerVehicle = 0.0;
        this.currentTime = 0.0;
        this.currentCongestionIndex = 0.0; // Inicializa como 0 (sem congestionamento)
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
     * @param waitTime O tempo total que o veículo passou esperando (ex: em semáforos).
     * @param fuelConsumedByVehicle O total de combustível consumido pelo veículo durante sua viagem.
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
     * A métrica atual é uma soma simples do número de veículos ativos e o número total de veículos
     * esperando em filas de semáforos. Esta métrica pode ser refinada para maior precisão.
     *
     * @param activeVehicles Lista de todos os veículos atualmente ativos na simulação.
     * @param graph          O grafo da rede urbana, usado para acessar semáforos e, potencialmente,
     * outras características da rede para métricas mais complexas.
     */
    public synchronized void calculateCurrentCongestion(CustomLinkedList<Vehicle> activeVehicles, Graph graph) {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            this.currentCongestionIndex = 0.0; // Evita divisão por zero se não houver nós
            return;
        }

        int numberOfActiveVehicles = (activeVehicles != null) ? activeVehicles.size() : 0;
        int totalQueuedVehicles = 0;

        if (graph.getTrafficLights() != null) {
            for (TrafficLight tl : graph.getTrafficLights()) {
                if (tl != null) {
                    totalQueuedVehicles += tl.getTotalVehiclesInQueues();
                }
            }
        }
        // Métrica simples: soma de veículos ativos e veículos enfileirados.
        // Valores mais altos indicam maior congestionamento.
        this.currentCongestionIndex = numberOfActiveVehicles + totalQueuedVehicles;
    }

    /**
     * Retorna o índice de congestionamento calculado mais recentemente.
     * @return O índice de congestionamento atual.
     */
    public synchronized double getCurrentCongestionIndex() {
        return this.currentCongestionIndex;
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
     * @return O consumo total de combustível em litros (ou a unidade definida no modelo de veículo).
     */
    public synchronized double getTotalFuelConsumed() {
        return totalFuelConsumed;
    }

    /**
     * Calcula e retorna o consumo médio de combustível por veículo que chegou ao destino.
     * @return O consumo médio de combustível em litros (ou a unidade definida), ou 0.0 se nenhum veículo chegou.
     */
    public synchronized double getAverageFuelConsumptionPerVehicle() {
        if (vehiclesArrived == 0) {
            return 0.0;
        }
        return totalFuelConsumed / vehiclesArrived;
    }

    public double getCurrentTime() {
        return currentTime;
    }

    public int getVehiclesArrived() {
        return vehiclesArrived;
    }

    /**
     * Imprime um resumo das estatísticas da simulação no console.
     * Inclui informações sobre veículos, tempos médios e consumo de combustível.
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
        // Você pode adicionar o congestionamento médio/máximo aqui se implementar esses cálculos.
        // Ex: System.out.printf("Índice de Congestionamento Final (ativos + enfileirados): %.0f%n", currentCongestionIndex);
        System.out.println("---------------------------\n");
    }
}