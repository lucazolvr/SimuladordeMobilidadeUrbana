package org.aiacon.simuladordemobilidadeurbana.simulation;

import org.aiacon.simuladordemobilidadeurbana.model.*;
import java.util.HashMap;
import java.util.Map;

public class Simulator implements Runnable {
    private Graph graph;
    private Configuration config;
    private CustomLinkedList<Vehicle> vehicles;
    private Statistics stats;
    private VehicleGenerator generator;
    private double time;
    private volatile boolean running = true;

    public Simulator(Graph graph, Configuration config) {
        this.graph = graph;
        this.config = config;
        this.vehicles = new CustomLinkedList<>();
        this.stats = new Statistics();
        this.generator = new VehicleGenerator(graph, config.getVehicleGenerationRate());
        this.time = 0.0;

        validateGraph();
        if (!isGraphConnected()) {
            throw new IllegalStateException("Erro: O grafo não está totalmente conectado. Nem todos os nós podem ser alcançados.");
        }
    }

    @Override
    public void run() {
        System.out.println("SIMULATOR_RUN: Iniciando loop de simulação. Duração: " + config.getSimulationDuration() + "s");
        double deltaTime = 1.0; // Passo de simulação em segundos

        while (running && time < config.getSimulationDuration()) {
            time += deltaTime;

            if (Thread.currentThread().isInterrupted()) {
                System.out.println("SIMULATOR_RUN: Thread de simulação interrompida, encerrando loop.");
                this.running = false;
                break;
            }

            generateVehicles(deltaTime);
            updateTrafficLights(deltaTime);
            moveVehicles(deltaTime);
            logSimulationState();

            if (running) {
                sleep(deltaTime);
            }
        }
        System.out.println("SIMULATOR_RUN: Loop de simulação terminado. Tempo final: " + String.format("%.2f", time));
        stats.printSummary();
    }

    public void stopSimulation() {
        System.out.println("SIMULATOR_STOPSIMULATION: Sinalizando para parar a simulação.");
        this.running = false;
    }

    private void validateGraph() {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            throw new IllegalStateException("Erro: O grafo está vazio ou não foi carregado corretamente.");
        }
        System.out.println("Grafo validado com sucesso! Nós carregados: " + graph.getNodes().size());
    }

    private boolean isGraphConnected() {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            System.out.println("BFS: Grafo nulo ou sem nós.");
            return false;
        }
        CustomLinkedList<String> visited = new CustomLinkedList<>();
        CustomLinkedList<String> queue = new CustomLinkedList<>(); // Sua CustomLinkedList

        Node startNode = graph.getNodes().get(0); // Assumindo que get(0) é seguro
        if (startNode == null) {
            System.out.println("BFS: Primeiro nó do grafo é nulo!");
            return false;
        }

        queue.add(startNode.getId());
        visited.add(startNode.getId()); // Marcar como visitado ao enfileirar

        int iterations = 0;
        int maxIterations = graph.getNodes().size(); // BFS visita cada nó no máximo uma vez em grafo conectado

        while (!queue.isEmpty() && iterations < maxIterations) { // Adicionada checagem de iterações para segurança
            iterations++;
            String currentNodeId = queue.removeFirst();
            Node currentNodeObject = graph.getNode(currentNodeId); // Otimizado com HashMap no Graph

            if (currentNodeObject != null && currentNodeObject.getEdges() != null) {
                for (Edge edge : currentNodeObject.getEdges()) {
                    if (edge == null) continue;
                    String neighborId = edge.getDestination();
                    if (neighborId != null && !neighborId.isEmpty() && !visited.contains(neighborId)) {
                        visited.add(neighborId);
                        queue.add(neighborId);
                    }
                }
            }
        }
        boolean connected = visited.size() == graph.getNodes().size();
        System.out.println("BFS RESULTADO: Nós visitados: " + visited.size() + " de " + graph.getNodes().size() + ". Grafo conectado: " + connected);
        return connected;
    }

    private void generateVehicles(double deltaTime) {
        double numExpectedVehicles = deltaTime * config.getVehicleGenerationRate();
        int numToGenerate = (int) numExpectedVehicles;
        if (Math.random() < (numExpectedVehicles - numToGenerate)) {
            numToGenerate++;
        }

        for (int i = 0; i < numToGenerate; i++) {
            int vehicleId = stats.getTotalVehiclesGenerated() + 1;
            Vehicle vehicle = generator.generateVehicle(vehicleId);

            if (vehicle != null) {
                vehicles.add(vehicle);
                stats.vehicleGenerated();
            }
        }
    }

    private void updateTrafficLights(double deltaTime) {
        if (graph.getTrafficLights() == null) return;
        for (TrafficLight tl : graph.getTrafficLights()) {
            if (tl != null) {
                tl.update(deltaTime, config.isPeakHour()); // Assinatura correta
            }
        }
    }

    private void moveVehicles(double deltaTime) {
        CustomLinkedList<Vehicle> vehiclesStillActive = new CustomLinkedList<>();
        for (Vehicle vehicle : vehicles) {
            if (vehicle == null) continue;
            updateVehicle(vehicle, deltaTime);

            if (running && vehicle.getCurrentNode().equals(vehicle.getDestination()) && vehicle.getPosition() == 0.0) {
                stats.vehicleArrived(vehicle.getTravelTime(), vehicle.getWaitTime(), vehicle.getFuelConsumed());
            } else if (running) { // Só adiciona de volta se a simulação não parou por erro em updateVehicle
                vehiclesStillActive.add(vehicle);
            }
        }
        if (running) { // Só atualiza a lista se a simulação ainda estiver rodando
            vehicles = vehiclesStillActive;
        }
    }

    /**
     * Determina a direção cardeal APROXIMADA do movimento DE um nó PARA outro.
     * Usado para determinar de onde um veículo se aproxima de um semáforo,
     * ou para qual direção ele pretende sair de um cruzamento.
     * @param fromNodeId ID do nó de origem.
     * @param toNodeId ID do nó de destino.
     * @return String como "north", "south", "east", "west", ou "unknown".
     */
    private String determineCardinalDirection(String fromNodeId, String toNodeId) {
        if (fromNodeId == null || toNodeId == null || fromNodeId.equals(toNodeId)) {
            return "unknown";
        }
        Node fromNode = graph.getNode(fromNodeId);
        Node toNode = graph.getNode(toNodeId);
        if (fromNode == null || toNode == null) {
            return "unknown";
        }

        double deltaLat = toNode.getLatitude() - fromNode.getLatitude();
        double deltaLon = toNode.getLongitude() - fromNode.getLongitude();
        double absDeltaLat = Math.abs(deltaLat);
        double absDeltaLon = Math.abs(deltaLon);
        double threshold = 0.000001; // Pequeno limiar para movimento significativo

        if (absDeltaLat > absDeltaLon + threshold) {
            return (deltaLat > 0) ? "north" : "south"; // Movimento para Norte (lat aumenta) ou Sul (lat diminui)
        } else if (absDeltaLon > absDeltaLat + threshold) {
            return (deltaLon > 0) ? "east" : "west"; // Movimento para Leste (lon aumenta) ou Oeste (lon diminui)
        }
        return "unknown"; // Diagonal ou muito próximo
    }

    private void updateVehicle(Vehicle vehicle, double deltaTime) {
        if (!running) return; // Verifica se a simulação deve continuar

        vehicle.incrementTravelTime(deltaTime);

        if (vehicle.getPosition() == 0.0) { // Veículo está em um nó
            String currentVehicleNodeId = vehicle.getCurrentNode();
            String previousNodeIdInRoute = getPreviousNodeInRoute(vehicle);

            TrafficLight tl = getTrafficLight(currentVehicleNodeId);

            if (tl != null && config.getRedirectThreshold() > 0) { // Só tenta redirecionar se o threshold for positivo
                redirectIfNeeded(vehicle, currentVehicleNodeId, graph);
            }

            String nextNodeIdInRoute = getNextNodeInRoute(vehicle);

            if (nextNodeIdInRoute == null) {
                if(!currentVehicleNodeId.equals(vehicle.getDestination())){
                    System.err.println("UPDATE_VEHICLE: Veículo " + vehicle.getId() + " em " + currentVehicleNodeId + " sem próximo nó, mas não está no destino " + vehicle.getDestination() + ". Rota: " + vehicle.getRoute());
                }
                return;
            }

            if (tl != null) {
                String approachToLightDirection = "unknown";
                if (previousNodeIdInRoute != null) { // Veículo vindo de algum lugar
                    approachToLightDirection = determineCardinalDirection(previousNodeIdInRoute, currentVehicleNodeId);
                } else { // Veículo começando a rota no nó do semáforo
                    approachToLightDirection = determineCardinalDirection(currentVehicleNodeId, nextNodeIdInRoute); // Direção de saída pretendida
                }

                String lightState = tl.getLightStateForApproach(approachToLightDirection);

                if (!"green".equalsIgnoreCase(lightState)) {
                    vehicle.incrementWaitTime(deltaTime);
                    // Enfileira na direção de aproximação que está esperando
                    tl.addVehicleToQueue(approachToLightDirection, vehicle);
                    return;
                }
            }

            Edge edgeToTraverse = findEdge(currentVehicleNodeId, nextNodeIdInRoute);
            if (edgeToTraverse == null) {
                System.err.println("UPDATE_VEHICLE (EM NÓ): Veículo " + vehicle.getId() + " no nó " + currentVehicleNodeId +
                        ". Não foi possível encontrar a aresta para o PRÓXIMO nó da rota: " + nextNodeIdInRoute +
                        ". Rota: " + (vehicle.getRoute() != null ? vehicle.getRoute().toString() : "NULA"));
                this.running = false;
                return;
            }
            double edgeTravelTime = edgeToTraverse.getTravelTime();
            if (edgeTravelTime <= 0) edgeTravelTime = deltaTime; // Evitar divisão por zero ou tempo negativo

            vehicle.setPosition(deltaTime / edgeTravelTime);

            if (vehicle.getPosition() >= 1.0) {
                vehicle.setCurrentNode(nextNodeIdInRoute);
                vehicle.setPosition(0.0);
            }

        } else { // Veículo já está em uma aresta
            String sourceNodeOfCurrentSegment = vehicle.getCurrentNode();
            String targetNodeOfCurrentSegment = getNextNodeInRoute(vehicle);

            if (targetNodeOfCurrentSegment == null) {
                System.err.println("UPDATE_VEHICLE (EM ARESTA): Veículo " + vehicle.getId() + " na aresta de " + sourceNodeOfCurrentSegment +
                        " mas getNextNodeInRoute é nulo. Posição: " + String.format("%.2f",vehicle.getPosition()));
                vehicle.setPosition(0.0); // Força chegada no sourceNodeOfCurrentSegment
                if (!sourceNodeOfCurrentSegment.equals(vehicle.getDestination())) {
                    System.err.println("    Veículo " + vehicle.getId() + " parou em " + sourceNodeOfCurrentSegment + " pois a rota terminou inesperadamente.");
                }
                return;
            }

            Edge currentEdge = findEdge(sourceNodeOfCurrentSegment, targetNodeOfCurrentSegment);
            if (currentEdge == null) {
                System.err.println("UPDATE_VEHICLE (EM ARESTA): Veículo " + vehicle.getId() +
                        ". Não foi possível encontrar a aresta entre " + sourceNodeOfCurrentSegment + " e " + targetNodeOfCurrentSegment);
                this.running = false;
                return;
            }
            double edgeTravelTime = currentEdge.getTravelTime();
            if (edgeTravelTime <= 0) edgeTravelTime = deltaTime;

            vehicle.setPosition(vehicle.getPosition() + (deltaTime / edgeTravelTime));

            if (vehicle.getPosition() >= 1.0) {
                vehicle.setCurrentNode(targetNodeOfCurrentSegment);
                vehicle.setPosition(0.0);
            }
        }
    }

    private Edge findEdge(String sourceNodeId, String targetNodeId) {
        if (sourceNodeId == null || targetNodeId == null) return null;
        Node sourceNode = graph.getNode(sourceNodeId);
        if (sourceNode == null || sourceNode.getEdges() == null) return null;
        for (Edge edge : sourceNode.getEdges()) {
            if (edge != null && edge.getDestination() != null && edge.getDestination().equals(targetNodeId)) {
                return edge;
            }
        }
        return null;
    }

    private void logSimulationState() {
        // O método calculateCongestion foi removido do Simulator.
        System.out.println("Tempo: " + String.format("%.2f", time) + "s, Veículos: " + (vehicles != null ? vehicles.size() : 0));
    }

    private void sleep(double deltaTime) {
        try {
            Thread.sleep((long) (deltaTime * 1000));
        } catch (InterruptedException e) {
            System.out.println("SIMULATOR_SLEEP: A thread foi interrompida durante o sleep.");
            this.running = false;
            Thread.currentThread().interrupt();
        }
    }

    private void redirectIfNeeded(Vehicle vehicle, String trafficLightNodeId, Graph graph) {
        if (vehicle == null || trafficLightNodeId == null || graph == null || config == null || config.getRedirectThreshold() <= 0) {
            return; // Não redireciona se o threshold for não positivo ou parâmetros nulos
        }

        TrafficLight tl = getTrafficLight(trafficLightNodeId);
        if (tl == null) return;

        String nextNodeInOriginalRoute = getNextNodeInRoute(vehicle);
        if (nextNodeInOriginalRoute == null) return;

        String currentRouteOutgoingDirection = determineCardinalDirection(trafficLightNodeId, nextNodeInOriginalRoute);
        if ("unknown".equals(currentRouteOutgoingDirection)) return;

        Integer currentDirectionIndex = tl.getDirectionIndex(currentRouteOutgoingDirection);
        int[] queueSizes = tl.getAllQueueSizes(); // Obtém os tamanhos das filas do semáforo

        if (currentDirectionIndex != null && currentDirectionIndex >= 0 && currentDirectionIndex < queueSizes.length &&
                queueSizes[currentDirectionIndex] > config.getRedirectThreshold()) {

            System.out.println("REDIRECT_IF_NEEDED: Veículo " + vehicle.getId() + " no nó " + trafficLightNodeId +
                    ". Rota atual via " + currentRouteOutgoingDirection + " (nó " + nextNodeInOriginalRoute + ") congestionada (fila: " + queueSizes[currentDirectionIndex] + "). Procurando alternativa...");

            String bestAlternativeOutgoingDirection = null;
            int minQueueSizeForAlternative = queueSizes[currentDirectionIndex]; // Inicia com a fila atual para garantir melhora
            Node bestAlternativeNextNode = null;
            String oppositeOfCurrentOutgoing = getOppositeDirection(currentRouteOutgoingDirection);

            String[] tryDirections = {"north", "east", "south", "west"};

            for (String potentialOutgoingDir : tryDirections) {
                if (potentialOutgoingDir.equalsIgnoreCase(currentRouteOutgoingDirection) ||
                        (oppositeOfCurrentOutgoing != null && potentialOutgoingDir.equalsIgnoreCase(oppositeOfCurrentOutgoing))) {
                    continue;
                }
                Node potentialNextNode = findNeighborInDirection(trafficLightNodeId, potentialOutgoingDir, graph);
                if (potentialNextNode != null && (getPreviousNodeInRoute(vehicle) == null || !potentialNextNode.getId().equals(getPreviousNodeInRoute(vehicle)))) {
                    Integer potentialDirIndex = tl.getDirectionIndex(potentialOutgoingDir);
                    if (potentialDirIndex != null && potentialDirIndex >= 0 && potentialDirIndex < queueSizes.length &&
                            queueSizes[potentialDirIndex] < minQueueSizeForAlternative) {
                        minQueueSizeForAlternative = queueSizes[potentialDirIndex];
                        bestAlternativeOutgoingDirection = potentialOutgoingDir;
                        bestAlternativeNextNode = potentialNextNode;
                    }
                }
            }

            if (bestAlternativeNextNode != null) { // Se uma alternativa válida foi encontrada (mesmo que a fila não seja estritamente menor, mas diferente)
                System.out.println("  -> Redirecionando Veículo " + vehicle.getId() + " para direção '" + bestAlternativeOutgoingDirection +
                        "' (nó: " + bestAlternativeNextNode.getId() + ") com fila: " + minQueueSizeForAlternative);

                CustomLinkedList<String> newRouteFromAlternative = Dijkstra.calculateRoute(graph, bestAlternativeNextNode.getId(), vehicle.getDestination());

                if (newRouteFromAlternative != null && !newRouteFromAlternative.isEmpty()) {
                    CustomLinkedList<String> finalNewRoute = new CustomLinkedList<>();
                    finalNewRoute.add(trafficLightNodeId);
                    finalNewRoute.add(bestAlternativeNextNode.getId());

                    // Adiciona o restante da rota, garantindo que não haja duplicação do bestAlternativeNextNode
                    boolean firstNodeSkipped = false;
                    for(String routeNode : newRouteFromAlternative) {
                        if (!firstNodeSkipped && routeNode.equals(bestAlternativeNextNode.getId())) {
                            firstNodeSkipped = true;
                            continue;
                        }
                        finalNewRoute.add(routeNode);
                    }
                    vehicle.setRoute(finalNewRoute);
                    System.out.println("  -> Nova rota para V" + vehicle.getId() + ": " + finalNewRoute.toString());
                } else {
                    System.out.println("  -> Não foi possível calcular rota alternativa para Veículo " + vehicle.getId() + " via " + bestAlternativeNextNode.getId());
                }
            } else {
                System.out.println("  -> Nenhuma direção alternativa viável encontrada para Veículo " + vehicle.getId());
            }
        }
    }

    private String getOppositeDirection(String direction) {
        if (direction == null) return "unknown"; // Retorna unknown para consistência
        switch (direction.toLowerCase()) {
            case "north": return "south";
            case "south": return "north";
            case "east": return "west";
            case "west": return "east";
            default: return "unknown";
        }
    }

    public TrafficLight getTrafficLight(String nodeId) {
        if (graph == null || graph.getTrafficLights() == null || nodeId == null) return null;
        for (TrafficLight tl : graph.getTrafficLights()) {
            if (tl != null && nodeId.equals(tl.getNodeId())) {
                return tl;
            }
        }
        return null;
    }

    private String getNextNodeInRoute(Vehicle vehicle) {
        if (vehicle == null || vehicle.getRoute() == null || vehicle.getRoute().isEmpty()) return null;
        String currentNode = vehicle.getCurrentNode();
        if (currentNode == null) return null;
        int currentIndex = vehicle.getRoute().indexOf(currentNode); // O(N) para CustomLinkedList
        if (currentIndex == -1 || currentIndex + 1 >= vehicle.getRoute().size()) return null;
        return vehicle.getRoute().get(currentIndex + 1); // O(N) para CustomLinkedList
    }

    private String getPreviousNodeInRoute(Vehicle vehicle) {
        if (vehicle == null || vehicle.getRoute() == null || vehicle.getRoute().isEmpty() || vehicle.getCurrentNode() == null) return null;
        String currentVehicleNodeId = vehicle.getCurrentNode();
        int currentIndex = vehicle.getRoute().indexOf(currentVehicleNodeId); // O(N) para CustomLinkedList
        if (currentIndex > 0) {
            return vehicle.getRoute().get(currentIndex - 1); // O(N) para CustomLinkedList
        }
        return null;
    }

    /**
     * Encontra um nó vizinho que melhor corresponde à direção cardeal desejada a partir de um nó de origem.
     * A "melhor" correspondência é baseada em qual vizinho tem a maior componente vetorial na direção desejada,
     * minimizando o componente perpendicular.
     */
    private Node findNeighborInDirection(String sourceNodeId, String targetDirection, Graph graph) {
        Node sourceNode = graph.getNode(sourceNodeId);
        if (sourceNode == null || sourceNode.getEdges() == null || targetDirection == null || targetDirection.equals("unknown")) {
            return null;
        }
        Node bestNeighbor = null;
        double bestScore = -Double.MAX_VALUE; // Queremos maximizar o "alinhamento"

        for (Edge edge : sourceNode.getEdges()) {
            if (edge == null) continue;
            Node neighbor = graph.getNode(edge.getDestination());
            if (neighbor == null) continue;

            double deltaLat = neighbor.getLatitude() - sourceNode.getLatitude();
            double deltaLon = neighbor.getLongitude() - sourceNode.getLongitude();
            double score = 0.0;

            // Calcula um score de alinhamento. Um valor positivo maior indica melhor alinhamento.
            // Penaliza desvios da direção principal.
            switch (targetDirection.toLowerCase()) {
                case "north":
                    if (deltaLat > 0) score = deltaLat - Math.abs(deltaLon); // Prioriza deltaLat positivo
                    break;
                case "south":
                    if (deltaLat < 0) score = -deltaLat - Math.abs(deltaLon); // Prioriza deltaLat negativo (maior -deltaLat)
                    break;
                case "east":
                    if (deltaLon > 0) score = deltaLon - Math.abs(deltaLat); // Prioriza deltaLon positivo
                    break;
                case "west":
                    if (deltaLon < 0) score = -deltaLon - Math.abs(deltaLat); // Prioriza deltaLon negativo (maior -deltaLon)
                    break;
                default: continue; // Direção inválida
            }

            if (score > 0 && score > bestScore) { // Apenas considera se o score for positivo e melhor que o anterior
                bestScore = score;
                bestNeighbor = neighbor;
            }
        }
        // if (bestNeighbor == null) {
        //     System.out.println("FIND_NEIGHBOR: Nenhum vizinho encontrado de " + sourceNodeId + " na direção " + targetDirection);
        // }
        return bestNeighbor;
    }

    public CustomLinkedList<Vehicle> getVehicles() {
        return this.vehicles;
    }

}