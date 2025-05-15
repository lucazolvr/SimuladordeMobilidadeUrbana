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

        // Validar o grafo ao inicializar
        validateGraph();

        // Verificar conectividade do grafo
        if (!isGraphConnected()) { // ESTA LINHA CHAMA O MÉTODO ATUALIZADO ABAIXO
            throw new IllegalStateException("Erro: O grafo não está totalmente conectado. Nem todos os nós podem ser alcançados.");
        }
    }



    public void run() {
        double deltaTime = 1.0; // Passo de simulação em segundos
        // validateGraph(); // Já foi validado no construtor, pode ser redundante aqui a menos que o grafo possa mudar
        while (time < 3600) { // Simular por 1 hora
            time += deltaTime;

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
    /**
     * Valida se o grafo foi carregado corretamente antes de iniciar a simulação.
     * Lança uma exceção se o grafo estiver vazio ou não carregado corretamente.
     */
    private void validateGraph() {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            throw new IllegalStateException("Erro: O grafo está vazio ou não foi carregado corretamente.");
        }

        System.out.println("Grafo validado com sucesso! Nós carregados: " + graph.getNodes().size());

        // Opcional: Reduzir a quantidade de logs aqui se estiver muito verboso
        // System.out.println("IDs dos nós carregados no grafo:");
        // for (Node node : graph.getNodes()) {
        //     System.out.println("Nó ID: " + node.getId());
        // }
    }

    // MÉTODO ATUALIZADO COM LOGS DE DEPURAÇÃO:
    private boolean isGraphConnected() {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            System.out.println("BFS: Grafo nulo ou sem nós.");
            return false;
        }

        CustomLinkedList<String> visited = new CustomLinkedList<>();
        CustomLinkedList<String> queue = new CustomLinkedList<>();

        Node startNode = graph.getNodes().get(0);
        if (startNode == null) {
            System.out.println("BFS: Primeiro nó do grafo é nulo! (graph.getNodes().get(0) retornou null)");
            return false;
        }
        System.out.println("BFS: Iniciando com o nó: " + startNode.getId());
        queue.add(startNode.getId());

        int iterations = 0;
        int maxIterations = graph.getNodes().size() * graph.getNodes().size();

        while (!queue.isEmpty() && iterations < maxIterations) {
            iterations++;
            String currentNodeId = queue.removeFirst();

            if (!visited.contains(currentNodeId)) {
                visited.add(currentNodeId);
                Node currentNode = graph.getNode(currentNodeId);
                if (currentNode != null) {
                    if (currentNode.getEdges() == null) {
                        System.out.println("BFS: Explorando nó " + currentNodeId + ". A lista de arestas saindo deste nó é NULA.");
                    } else {
                        // System.out.println("BFS: Explorando nó " + currentNodeId + ". Arestas saindo deste nó: " + currentNode.getEdges().size()); // Log verboso, já vimos que funciona
                        for (Edge edge : currentNode.getEdges()) {
                            if (edge == null) {
                                System.out.println("BFS WARNING: Encontrada aresta NULA saindo do nó " + currentNodeId);
                                continue;
                            }
                            String neighborId = edge.getDestination();
                            if (neighborId == null || neighborId.isEmpty()) {
                                System.out.println("BFS WARNING: Aresta " + edge.getId() + " tem destino NULO ou VAZIO saindo do nó " + currentNodeId);
                                continue;
                            }
                            if (!visited.contains(neighborId)) {
                                queue.add(neighborId);
                            }
                        }
                    }
                } else {
                    System.out.println("BFS WARNING: Nó com ID " + currentNodeId + " (obtido da fila) não encontrado no grafo ao tentar obter o objeto Node!");
                }
            }
        }

        boolean connected = visited.size() == graph.getNodes().size();
        System.out.println("BFS RESULTADO: Nós visitados: " + visited.size() + " de " + graph.getNodes().size() + ". Grafo conectado: " + connected);

        // ***** INÍCIO DA MODIFICAÇÃO PARA LISTAR NÓS NÃO VISITADOS *****
        if (!connected) {
            System.out.println("--- LISTA DE NÓS NÃO VISITADOS ---");
            int unvisitedCount = 0;
            if (graph.getNodes() != null) {
                for (Node nodeFromGraph : graph.getNodes()) {
                    if (nodeFromGraph != null && !visited.contains(nodeFromGraph.getId())) {
                        System.out.println("NÓ NÃO VISITADO: " + nodeFromGraph.getId());
                        unvisitedCount++;
                        // Opcional: Imprimir detalhes sobre este nó não visitado
                        Node unvisitedNodeObject = graph.getNode(nodeFromGraph.getId());
                        if (unvisitedNodeObject != null) {
                            if (unvisitedNodeObject.getEdges() != null && !unvisitedNodeObject.getEdges().isEmpty()) {
                                System.out.println("  -> Arestas DESTE nó ("+ nodeFromGraph.getId() +"): " + unvisitedNodeObject.getEdges().size());
                                for(Edge e : unvisitedNodeObject.getEdges()){
                                    System.out.println("    -> Vai para: " + e.getDestination() + " (Este destino foi visitado? " + visited.contains(e.getDestination()) + ")");
                                }
                            } else {
                                System.out.println("  -> Este nó ("+ nodeFromGraph.getId() +") NÃO possui arestas de saída em sua lista interna (Node.edges).");
                            }
                            // Verificar arestas GLOBAIS que CHEGAM a este nó
                            int incomingGlobalEdges = 0;
                            if(graph.getEdges() != null){
                                for(Edge globalEdge : graph.getEdges()){
                                    if(globalEdge != null && globalEdge.getTarget() != null && globalEdge.getTarget().equals(nodeFromGraph.getId())){
                                        System.out.println("  -> Aresta GLOBAL " + globalEdge.getId() + " (de " + globalEdge.getSource() + ") APONTA PARA este nó. (Origem " + globalEdge.getSource() + " foi visitada? " + visited.contains(globalEdge.getSource()) + ")");
                                        incomingGlobalEdges++;
                                    }
                                }
                            }
                            if(incomingGlobalEdges == 0) {
                                System.out.println("  -> Nenhuma aresta na lista GLOBAL do grafo aponta para este nó não visitado.");
                            }
                        }
                    }
                }
            }
            if (unvisitedCount == 0 && graph.getNodes() != null && visited.size() != graph.getNodes().size()){
                System.out.println("AVISO: Contagem de nós não visitados foi 0, mas visited.size() != graph.getNodes().size(). Verifique a lógica de CustomLinkedList.contains ou IDs duplicados/nulos.");
            }
            System.out.println("------------------------------------");
        }
        // ***** FIM DA MODIFICAÇÃO *****


        if (iterations >= maxIterations && !queue.isEmpty()) {
            System.out.println("BFS WARNING: Limite de iterações (" + maxIterations + ") atingido, possível problema na lógica da fila, visitação, ou estrutura do grafo.");
        }
        return connected;
    }


    /**
     * Gera novos veículos com base na taxa de geração configurada.
     * Garante que apenas veículos com rotas válidas sejam adicionados à simulação.
     */
    /**
     * Gera novos veículos com base na taxa de geração configurada.
     * Garante que apenas veículos com rotas válidas sejam adicionados à simulação.
     */
    /**
     * Gera novos veículos com base na taxa de geração configurada.
     * Garante que apenas veículos com rotas válidas sejam adicionados à simulação.
     */
    private void generateVehicles(double deltaTime) {
        int numVehiclesToGenerate = (int) (deltaTime * config.getVehicleGenerationRate());
        for (int i = 0; i < numVehiclesToGenerate; i++) {
            System.out.println("Tentando gerar veículo #" + (vehicles.size() + 1));
            Vehicle vehicle = generator.generateVehicle(vehicles.size() + 1);

            if (vehicle != null) {
                vehicles.add(vehicle);
                System.out.println("Veículo V" + vehicle.getId() + " adicionado à simulação com rota valida.");
            } else {
                System.out.println("Falha ao gerar veículo #" + (vehicles.size() + 1) + ". Ignorando...");
            }
        }
    }


    private void checkNodeConnections() { // Este método não parece estar sendo chamado. Se precisar, chame-o.
        System.out.println("Verificando arestas dos nós (chamada de checkNodeConnections):");
        if (graph == null || graph.getNodes() == null) {
            System.out.println("CHECK_CON: Grafo ou lista de nós nula.");
            return;
        }
        for (Node node : graph.getNodes()) {
            if (node == null) {
                System.err.println("CHECK_CON: Encontrado nó NULO na lista de nós do grafo.");
                continue;
            }
            if (node.getEdges() == null) {
                System.err.println("CHECK_CON: Nó " + node.getId() + " possui lista de arestas NULA.");
            } else if (node.getEdges().isEmpty()) {
                System.err.println("CHECK_CON: Nó " + node.getId() + " não possui arestas de saída (lista vazia).");
            } else {
                System.out.println("CHECK_CON: Nó " + node.getId() + " tem " + node.getEdges().size() + " aresta(s) de saída.");
            }
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

        if (vehicle.getPosition() == 0.0) { // Veículo está em um nó, tentando iniciar em uma nova aresta
            String nextNodeIdInRoute = getNextNodeInRoute(vehicle);
            if (nextNodeIdInRoute == null) { // Chegou ao destino final ou não há mais rota
                // System.out.println("UPDATE_VEHICLE: Veículo " + vehicle.getId() + " chegou ao destino ou fim da rota em " + vehicle.getCurrentNode());
                return;
            }

            String currentVehicleNodeId = vehicle.getCurrentNode();

            // Lógica de Semáforo (simplificada para focar no erro de movimento)
            TrafficLight tl = getTrafficLight(currentVehicleNodeId);
            if (tl != null && tl.getState() != null && !tl.getState().equals("green")) {
                vehicle.incrementWaitTime(deltaTime);
                // A lógica de adicionar à fila do semáforo pode precisar ser mais robusta aqui.
                return;
            }

            Edge edgeToTraverse = findEdge(currentVehicleNodeId, nextNodeIdInRoute);
            if (edgeToTraverse == null) {
                System.err.println("UPDATE_VEHICLE (EM NÓ): Veículo " + vehicle.getId() + " no nó " + currentVehicleNodeId +
                        ". Não foi possível encontrar a aresta para o PRÓXIMO nó da rota: " + nextNodeIdInRoute +
                        ". Rota: " + (vehicle.getRoute() != null ? vehicle.getRoute().toString() : "NULA"));
                // Detalhar arestas saindo do currentVehicleNodeId
                Node sourceNodeObj = graph.getNode(currentVehicleNodeId);
                if (sourceNodeObj != null && sourceNodeObj.getEdges() != null) {
                    System.err.println("    -> Arestas realmente saindo de " + currentVehicleNodeId + " (total " + sourceNodeObj.getEdges().size() + "):");
                    for (Edge e : sourceNodeObj.getEdges()) {
                        System.err.println("      -> para " + e.getTarget() + " (ID: " + e.getId() + ")");
                    }
                }
                return;
            }
            double edgeTravelTime = edgeToTraverse.getTravelTime();

            if (edgeTravelTime <= 0 || Double.isInfinite(edgeTravelTime) || Double.isNaN(edgeTravelTime)) {
                System.err.println("UPDATE_VEHICLE (EM NÓ): Veículo " + vehicle.getId() + ". Tempo de viagem da aresta " + edgeToTraverse.getId() +
                        " (de " + currentVehicleNodeId + " para " + nextNodeIdInRoute + ") é inválido: " + edgeTravelTime);
                return;
            }

            vehicle.setPosition(deltaTime / edgeTravelTime);

            if (vehicle.getPosition() >= 1.0) {
                vehicle.setCurrentNode(nextNodeIdInRoute);
                vehicle.setPosition(0.0);
                //System.out.println("UPDATE_VEHICLE: Veículo " + vehicle.getId() + " moveu-se para o nó " + nextNodeIdInRoute);
            } else {
                //System.out.println("UPDATE_VEHICLE: Veículo " + vehicle.getId() + " iniciou movimento na aresta " + edgeToTraverse.getId() + ", posição: " + vehicle.getPosition());
            }

        } else { // Veículo já está em uma aresta, movendo-se
            System.out.println("\nDEBUG_UPDATE_VEHICLE_ON_EDGE: Veículo " + vehicle.getId() +
                    " está na posição " + String.format("%.2f", vehicle.getPosition()) +
                    " da aresta que começou em " + vehicle.getCurrentNode() + // Este DEVERIA ser o nó de início da aresta atual
                    ". Rota: " + (vehicle.getRoute() != null ? vehicle.getRoute().toString() : "NULA"));

            String actualSourceNodeOfCurrentSegment = vehicle.getCurrentNode(); // Nó onde o veículo começou a atravessar esta aresta
            String targetNodeOfCurrentSegment = getNextNodeInRoute(vehicle);

            System.out.println("  DEBUG_ON_EDGE: Nó atual (origem da aresta): " + actualSourceNodeOfCurrentSegment +
                    ", Próximo nó na rota (destino desta aresta): " + targetNodeOfCurrentSegment);

            if (targetNodeOfCurrentSegment == null) {
                System.err.println("  -> ERRO_ON_EDGE: Veículo " + vehicle.getId() + " está numa aresta (posição > 0) mas getNextNodeInRoute é nulo. Nó atual: " + actualSourceNodeOfCurrentSegment);
                // Isso implicaria que o veículo completou a rota mas position não foi resetada, ou a rota é inválida.
                // Se o veículo chegou, sua posição deveria ser 0 e ele estaria no nó de destino.
                // Vamos resetar a posição e assumir que chegou, para evitar loop de erro.
                // No entanto, isso pode esconder um problema mais profundo na lógica de chegada.
                // vehicle.setPosition(0.0);
                return; // Melhor parar aqui para investigar
            }

            Edge currentEdge = findEdge(actualSourceNodeOfCurrentSegment, targetNodeOfCurrentSegment);

            if (currentEdge == null) {
                System.err.println("UPDATE_VEHICLE (EM ARESTA): Veículo " + vehicle.getId() +
                        ". Não foi possível encontrar a aresta entre " + actualSourceNodeOfCurrentSegment + " e " + targetNodeOfCurrentSegment +
                        ". Rota: " + (vehicle.getRoute() != null ? vehicle.getRoute().toString() : "NULA"));
                Node sourceNodeObj = graph.getNode(actualSourceNodeOfCurrentSegment);
                if (sourceNodeObj != null && sourceNodeObj.getEdges() != null) {
                    System.err.println("    -> Arestas realmente saindo de " + actualSourceNodeOfCurrentSegment + " (total " + sourceNodeObj.getEdges().size() + "):");
                    for (Edge e : sourceNodeObj.getEdges()) {
                        System.err.println("      -> para " + e.getTarget() + " (ID: " + e.getId() + ")");
                    }
                } else if (sourceNodeObj == null) {
                    System.err.println("    -> Objeto Node para sourceNodeId " + actualSourceNodeOfCurrentSegment + " é NULO no grafo.");
                } else { // sourceNodeObj.getEdges() == null
                    System.err.println("    -> Lista de arestas para sourceNodeId " + actualSourceNodeOfCurrentSegment + " é NULA.");
                }
                return;
            }
            double edgeTravelTime = currentEdge.getTravelTime();

            if (edgeTravelTime <= 0 || Double.isInfinite(edgeTravelTime) || Double.isNaN(edgeTravelTime)) {
                System.err.println("UPDATE_VEHICLE (EM ARESTA): Veículo " + vehicle.getId() + ". Tempo de viagem da aresta " + currentEdge.getId() +
                        " (de " + actualSourceNodeOfCurrentSegment + " para " + targetNodeOfCurrentSegment + ") é inválido: " + edgeTravelTime);
                return;
            }

            vehicle.setPosition(vehicle.getPosition() + (deltaTime / edgeTravelTime));
            //System.out.println("  DEBUG_ON_EDGE: Veículo " + vehicle.getId() + " nova posição: " + String.format("%.2f", vehicle.getPosition()));


            if (vehicle.getPosition() >= 1.0) {
                //System.out.println("  DEBUG_ON_EDGE: Veículo " + vehicle.getId() + " chegou ao nó " + targetNodeOfCurrentSegment);
                vehicle.setCurrentNode(targetNodeOfCurrentSegment);
                vehicle.setPosition(0.0);
            }
        }
    }

    // Novo método auxiliar para encontrar uma aresta específica (usado em updateVehicle)
    private Edge findEdge(String sourceNodeId, String targetNodeId) {
        if (sourceNodeId == null || targetNodeId == null) return null;
        Node sourceNode = graph.getNode(sourceNodeId);
        if (sourceNode == null || sourceNode.getEdges() == null) return null;

        for (Edge edge : sourceNode.getEdges()) {
            if (edge.getTarget().equals(targetNodeId)) { // Verifica se esta aresta vai para o nó de destino desejado
                return edge;
            }
        }
        return null; // Nenhuma aresta encontrada do source para o target especificado
    }

    /**
     * Calcula o nível de congestionamento na rede urbana.
     * O congestionamento pode ser medido como a razão entre o número de veículos ativos
     * e o número total de nós no grafo.
     *
     * @return O nível de congestionamento (ex: 0.0 = sem veículos, 1.0 = congestionamento máximo)
     */
    private double calculateCongestion() {
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            // throw new IllegalStateException("Erro: O grafo está vazio ou não inicializado.");
            return 0.0; // Retorna 0 se o grafo não estiver pronto
        }
        if (vehicles == null) return 0.0;

        int numVehicles = vehicles.size();
        int totalNodes = graph.getNodes().size();
        if (totalNodes == 0) return 0.0; // Evitar divisão por zero

        double congestion = (double) numVehicles / totalNodes;
        return Math.min(congestion, 1.0);
    }



    /**
     * Log do estado atual da simulação.
     */
    private void logSimulationState() {
        System.out.println("Tempo: " + String.format("%.2f", time) + "s, Veículos: " + (vehicles != null ? vehicles.size() : 0) + ", Congestionamento: " + String.format("%.2f", calculateCongestion()));
    }


    /**
     * Aguarda por um tempo equivalente a `deltaTime` em milissegundos.
     */
    private void sleep(double deltaTime) {
        try {
            Thread.sleep((long) (deltaTime * 1000));
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
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
        if (graph == null || graph.getTrafficLights() == null || nodeId == null) return null;
        for (TrafficLight tl : graph.getTrafficLights()) {
            if (tl != null && nodeId.equals(tl.getNodeId())) {
                return tl;
            }
        }
        return null;
    }

    /**
     * Obtém o próximo nó na rota do veículo.
     */
    private String getNextNodeInRoute(Vehicle vehicle) {
        if (vehicle == null || vehicle.getRoute() == null || vehicle.getRoute().isEmpty()) {
            // System.err.println("Erro: veículo ou rota nula/vazia em getNextNodeInRoute."); // Log verboso
            return null;
        }

        String currentNode = vehicle.getCurrentNode();
        if (currentNode == null) {
            // System.err.println("Erro: nó atual do veículo é nulo em getNextNodeInRoute."); // Log verboso
            return null;
        }

        int currentIndex = vehicle.getRoute().indexOf(currentNode);

        if (currentIndex == -1 || currentIndex + 1 >= vehicle.getRoute().size()) {
            // System.err.println("Erro: nó atual " + currentNode + " do veículo " + vehicle.getId() + " não encontrado na rota ou é o último."); // Log verboso
            return null;
        }
        return vehicle.getRoute().get(currentIndex + 1);
    }

    /**
     * Obtém o nó anterior na rota do veículo.
     */
    //Implementação do getPreviousNodeInRoute - pode precisar ser mais robusta
    // ou o veículo pode precisar armazenar sua aresta/segmento de rota atual.
    private String getPreviousNodeInRoute(Vehicle vehicle) {
        if (vehicle == null || vehicle.getRoute() == null || vehicle.getRoute().isEmpty() || vehicle.getCurrentNode() == null) {
            // System.out.println("GET_PREVIOUS: Condição inicial nula para veículo " + (vehicle != null ? vehicle.getId() : "ID NULO"));
            return null;
        }
        String currentVehicleNodeId = vehicle.getCurrentNode();
        int currentIndex = vehicle.getRoute().indexOf(currentVehicleNodeId);

        // System.out.println("GET_PREVIOUS: Veículo " + vehicle.getId() + ", Nó Atual: " + currentVehicleNodeId + ", Índice na Rota: " + currentIndex);

        if (currentIndex > 0) {
            return vehicle.getRoute().get(currentIndex - 1);
        }
        // Retorna null se for o primeiro nó da rota (currentIndex == 0) ou se não encontrado (currentIndex == -1)
        return null;
    }


    /**
     * Calcula o tempo de viagem de uma aresta específica.
     */
    // Este método não é mais ideal, pois o grafo agora tem arestas bidirecionais representadas como duas de mão única.
    // O método findEdge(source, target) é mais apropriado para a lógica de movimentação.
    private double getEdgeTravelTime(String source, String target) {
        Edge edge = findEdge(source, target);
        if (edge != null) {
            return edge.getTravelTime();
        }
        // System.err.println("GET_EDGE_TRAVEL_TIME: Aresta não encontrada entre " + source + " e " + target + " usando findEdge. Tentando busca global (legado)...");
        // Legado: busca global (menos eficiente e pode não respeitar a estrutura correta do nó)
        // if (graph == null || graph.getEdges() == null) return Double.MAX_VALUE;
        // for (Edge legacyEdge : graph.getEdges()) {
        //     if (legacyEdge != null && legacyEdge.getSource().equals(source) && legacyEdge.getTarget().equals(target)) {
        //         return legacyEdge.getTravelTime();
        //     }
        // }
        return Double.MAX_VALUE; // Inacessível
    }

    /**
     * Obtém o índice da direção (norte, sul, leste, oeste).
     */
    private int getDirectionIndex(String direction) {
        // Esta lógica de direção (north, south, east, west) pode não corresponder
        // diretamente aos IDs dos nós. Precisaria de um mapeamento ou uma forma diferente
        // de determinar a direção de aproximação de um veículo a um semáforo.
        // Por exemplo, baseado no ângulo entre o nó anterior, o nó atual (com semáforo), e os eixos.
        if (direction == null) return -1;
        switch (direction.toLowerCase()) { // Adicionado toLowerCase para robustez
            case "north": return 0;
            case "south": return 1;
            case "east": return 2;
            case "west": return 3;
            default: return -1; // Direção desconhecida
        }
    }
}