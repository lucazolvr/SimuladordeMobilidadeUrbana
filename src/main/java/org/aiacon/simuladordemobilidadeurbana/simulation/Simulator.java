package org.aiacon.simuladordemobilidadeurbana.simulation;

import org.aiacon.simuladordemobilidadeurbana.model.*;

public class Simulator {
    private Graph graph;                 // O grafo da rede urbana
    private Configuration config;        // Configurações da simulação
    private CustomLinkedList<Vehicle> vehicles; // Lista de veículos ativos na simulação
    private Statistics stats;            // Estatísticas de simulação
    private VehicleGenerator generator;  // Gerador de veículos
    private double time;                 // Tempo atual da simulação

    public Simulator(Graph graph, Configuration config) {
        this.graph = graph;
        this.config = config;
        this.vehicles = new CustomLinkedList<>();
        this.stats = new Statistics();
        this.generator = new VehicleGenerator(graph, config.getVehicleGenerationRate());
        this.time = 0.0;
    }

    public void run() {
        double deltaTime = 1.0; // Passo de simulação em segundos

        while (time < 3600) { // Simular por 1 hora
            time += deltaTime;

            validateGraph();
            // 1. Gerar novos veículos
            generateVehicles(deltaTime);

            // 2. Atualizar semáforos
            updateTrafficLights(deltaTime);

            // 3. Mover veículos
            moveVehicles(deltaTime);

            // 4. Log do estado atual da simulação
            logSimulationState();

            // 5. Espera para próxima iteração
            sleep(deltaTime);
        }

        // Exibir estatísticas finais
        stats.printSummary();
    }

    /**
     * Valida se o grafo foi carregado corretamente antes de iniciar a simulação.
     * Lança uma exceção se o grafo estiver vazio ou não carregado corretamente.
     */
    private void validateGraph() {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            throw new IllegalStateException("Erro: O grafo está vazio ou não foi carregado corretamente.");
        }

        System.out.println("Grafo validado com sucesso! Nós carregados: " + graph.getNodes().size());
    }

    /**
     * Gera novos veículos com base na taxa de geração configurada.
     * Garante que apenas veículos com rotas válidas sejam adicionados à simulação.
     */
    private void generateVehicles(double deltaTime) {
        int numVehiclesToGenerate = (int) (config.getVehicleGenerationRate() * deltaTime);

        for (int i = 0; i < numVehiclesToGenerate; i++) {
            System.out.println("Tentando gerar veículo #" + (vehicles.size() + 1));

            Vehicle vehicle = generator.generateVehicle(vehicles.size() + 1);

            if (vehicle == null) {
                System.err.println("Falha ao gerar veículo #" + (vehicles.size() + 1) + ". Ignorando...");
                continue;
            }

            vehicles.add(vehicle);
            System.out.println("Veículo gerado com sucesso: ID = " + vehicle.getId() +
                    ", Origem = " + vehicle.getOrigin() +
                    ", Destino = " + vehicle.getDestination());
        }
    }


    /**
     * Atualiza o estado dos semáforos na simulação.
     */
    private void updateTrafficLights(double deltaTime) {
        for (TrafficLight tl : graph.getTrafficLights()) {
            int[] queueSizes = new int[4];
            for (int i = 0; i < 4; i++) {
                queueSizes[i] = tl.getDirectionQueueSize(i);
            }
            tl.update(deltaTime, queueSizes[0], queueSizes[1], queueSizes[2], queueSizes[3], config.isPeakHour());
        }
    }

    /**
     * Move os veículos na simulação, atualizando suas posições.
     */
    private void moveVehicles(double deltaTime) {
        CustomLinkedList<Vehicle> updatedVehicles = new CustomLinkedList<>();
        for (Vehicle vehicle : vehicles) {
            updateVehicle(vehicle, deltaTime);
            if (!vehicle.getCurrentNode().equals(vehicle.getDestination())) {
                updatedVehicles.add(vehicle); // Adiciona veículos que não chegaram ao destino
            } else {
                stats.vehicleArrived(vehicle.getTravelTime(), vehicle.getWaitTime());
            }
        }
        vehicles = updatedVehicles; // Atualiza a lista de veículos ativos
    }

    /**
     * Atualiza a posição de um veículo na simulação.
     */
    private void updateVehicle(Vehicle vehicle, double deltaTime) {
        vehicle.incrementTravelTime(deltaTime);

        // Se o veículo estiver em um nó
        if (vehicle.getPosition() == 0.0) {
            String nextNode = getNextNodeInRoute(vehicle);
            if (nextNode == null) return; // Chegou ao destino

            // Verificar semáforo no nó atual
            TrafficLight tl = getTrafficLight(vehicle.getCurrentNode());
            if (tl != null && !tl.getState().equals("green")) {
                vehicle.incrementWaitTime(deltaTime);
                int dirIndex = getDirectionIndex(getPreviousNodeInRoute(vehicle));
                tl.addVehicleToQueue(dirIndex, vehicle);
                return;
            }

            // Mover para a próxima aresta
            double travelTime = getEdgeTravelTime(vehicle.getCurrentNode(), nextNode);
            vehicle.setPosition(deltaTime / travelTime);
            if (vehicle.getPosition() >= 1.0) {
                vehicle.setCurrentNode(nextNode);
                vehicle.setPosition(0.0);
            }
        } else {
            // Avançar na aresta
            String nextNode = getNextNodeInRoute(vehicle);
            double travelTime = getEdgeTravelTime(vehicle.getCurrentNode(), nextNode);
            vehicle.setPosition(vehicle.getPosition() + deltaTime / travelTime);
            if (vehicle.getPosition() >= 1.0) {
                vehicle.setCurrentNode(nextNode);
                vehicle.setPosition(0.0);
            }
        }
    }

    /**
     * Log do estado atual da simulação.
     */
    private void logSimulationState() {
        System.out.printf("Tempo: %.1fs, Veículos: %d, Congestionamento: %d%n",
                time, vehicles.size(), stats.getCongestion());
    }

    /**
     * Aguarda por um tempo equivalente a `deltaTime` em milissegundos.
     */
    private void sleep(double deltaTime) {
        try {
            Thread.sleep((long) (deltaTime * 1000));
        } catch (InterruptedException e) {
            e.printStackTrace();
        }
    }

    /**
     * Redireciona um veículo para uma direção alternativa.
     *
     * @param vehicle O veículo a ser redirecionado.
     * @param alternativeDirection A nova direção para o veículo.
     */
    private void redirectVehicle(Vehicle vehicle, String alternativeDirection) {
        if (vehicle == null || alternativeDirection == null || alternativeDirection.isEmpty()) {
            throw new IllegalArgumentException("Veículo ou direção alternativa inválida.");
        }

        CustomLinkedList<String> route = vehicle.getRoute();
        if (route == null || route.isEmpty()) {
            throw new IllegalStateException("A rota do veículo está vazia.");
        }

        // Adiciona a direção alternativa ao começo ou modifica a rota do veículo
        route.addFirst(alternativeDirection);
        System.out.printf("Veículo %s redirecionado para %s.%n", vehicle.getId(), alternativeDirection);
    }


    /**
     * Redireciona um veículo para uma rota alternativa se necessário.
     */
    private void redirectIfNeeded(Vehicle vehicle, TrafficLight tl) {
        if (vehicle == null || tl == null) {
            throw new IllegalArgumentException("Vehicle or TrafficLight cannot be null.");
        }

        final int DIRECTIONS = 4;
        int[] queueSizes = new int[DIRECTIONS];
        String[] directions = {"north", "south", "east", "west"};
        if (queueSizes.length != directions.length) {
            throw new IllegalStateException("Mismatch between queue sizes and directions.");
        }

        // Obter tamanhos das filas
        for (int i = 0; i < DIRECTIONS; i++) {
            queueSizes[i] = tl.getDirectionQueueSize(i);
        }

        // Identificar maior fila
        int maxQueue = queueSizes[0];
        for (int q : queueSizes) {
            if (q > maxQueue) {
                maxQueue = q;
            }
        }

        // Processar redirecionamento
        final int REDIRECT_THRESHOLD = 5; // Configurável
        if (maxQueue > REDIRECT_THRESHOLD) {
            int minIndex = 0;
            for (int i = 1; i < DIRECTIONS; i++) {
                if (queueSizes[i] < queueSizes[minIndex]) {
                    minIndex = i;
                }
            }

            String alternativeDirection = directions[minIndex];
            if (!vehicle.getRoute().contains(alternativeDirection)) {
                throw new IllegalStateException("Alternative direction is not valid for this vehicle.");
            }

            // Redirecionar veículo
            redirectVehicle(vehicle, alternativeDirection);
            System.out.printf("Redirecting vehicle %s to %s due to traffic.%n",
                    vehicle.getId(), alternativeDirection);
        }
    }

    /**
     * Obtem o semáforo associado a um nó específico.
     */
    private TrafficLight getTrafficLight(String nodeId) {
        for (TrafficLight tl : graph.getTrafficLights()) {
            if (tl.getNodeId().equals(nodeId)) {
                return tl;
            }
        }
        return null;
    }

    /**
     * Obtém o próximo nó na rota do veículo.
     */
    private String getNextNodeInRoute(Vehicle vehicle) {
        CustomLinkedList<String> route = vehicle.getRoute();

        if (route == null || route.isEmpty()) {
            System.err.println("Erro: veículo " + vehicle.getId() + " possui rota nula ou vazia.");
            return null; // Retorna nulo ou define um comportamento padrão
        }

        String currentNode = vehicle.getCurrentNode();
        int currentIndex = route.indexOf(currentNode);

        if (currentIndex == -1 || currentIndex + 1 >= route.size()) {
            System.err.println("Erro: nó atual " + currentNode + " do veículo " + vehicle.getId() + " não encontrado ou é o último.");
            return null;
        }

        return route.get(currentIndex + 1);
    }

    /**
     * Obtém o nó anterior na rota do veículo.
     */
    private String getPreviousNodeInRoute(Vehicle vehicle) {
        String previous = null;
        for (String node : vehicle.getRoute()) {
            if (node.equals(vehicle.getCurrentNode())) {
                return previous;
            }
            previous = node;
        }
        return null;
    }

    /**
     * Calcula o tempo de viagem de uma aresta específica.
     */
    private double getEdgeTravelTime(String source, String target) {
        for (Edge edge : graph.getEdges()) {
            if (edge.getSource().equals(source) && edge.getTarget().equals(target)) {
                return edge.getTravelTime();
            }
        }
        return Double.MAX_VALUE; // Inacessível
    }

    /**
     * Obtém o índice da direção (norte, sul, leste, oeste).
     */
    private int getDirectionIndex(String direction) {
        switch (direction) {
            case "north": return 0;
            case "south": return 1;
            case "east": return 2;
            case "west": return 3;
            default: return -1;
        }
    }
}