package org.aiacon.simuladordemobilidadeurbana.model;

// Representa o grafo da rede urbana
public class Graph {
    private CustomLinkedList<Node> nodes;               // Lista dos nós (cruzamentos da rede)
    private CustomLinkedList<Edge> edges;               // Lista das arestas (ruas da rede)
    private CustomLinkedList<TrafficLight> trafficLights; // Lista dos semáforos

    public Graph() {
        this.nodes = new CustomLinkedList<>();
        this.edges = new CustomLinkedList<>();
        this.trafficLights = new CustomLinkedList<>();
    }

    // Adiciona um nó ao grafo
    public void addNode(Node node) {
        if (node != null) {
            nodes.add(node); // Adiciona o nó à CustomLinkedList
            System.out.println("Nó adicionado ao grafo: " + node.getId());
        } else {
            System.err.println("Tentativa de adicionar um nó nulo ao grafo.");
        }
    }

    // Retorna todos os nós do grafo
    public CustomLinkedList<Node> getNodes() {
        return nodes; // Retorna a CustomLinkedList com os nós
    }

    // Adiciona uma aresta (rua) ao grafo
    public void addEdge(Edge edge) {
        if (edge != null) {
            edges.add(edge); // Adiciona a aresta à CustomLinkedList
            System.out.println("Aresta adicionada ao grafo: origem=" + edge.getSource() + ", destino=" + edge.getDestination());
        } else {
            System.err.println("Tentativa de adicionar uma aresta nula ao grafo.");
        }
    }

    // Retorna todas as arestas do grafo
    public CustomLinkedList<Edge> getEdges() {
        return edges; // Retorna a CustomLinkedList com as arestas
    }

    // Adiciona um semáforo ao grafo
    public void addTrafficLight(TrafficLight trafficLight) {
        if (trafficLight != null) {
            trafficLights.add(trafficLight); // Adiciona o semáforo
            System.out.println("Semáforo adicionado ao grafo: " + trafficLight);
        } else {
            System.err.println("Tentativa de adicionar um semáforo nulo ao grafo.");
        }
    }

    // Retorna todos os semáforos do grafo
    public CustomLinkedList<TrafficLight> getTrafficLights() {
        return trafficLights; // Retorna a CustomLinkedList com os semáforos
    }

    // Busca um nó específico no grafo pelo seu ID
    public Node getNode(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            System.err.println("ID do nó inválido.");
            return null; // Retorna null para nodeId inválido
        }

        // Itera diretamente pela CustomLinkedList para encontrar o nó
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i); // Acessa o nó pelo índice
            if (node.getId().equals(nodeId)) {
                return node; // Retorna o nó se o ID for correspondente
            }
        }

        System.err.println("Nó com ID " + nodeId + " não encontrado.");
        return null; // Retorna null se não encontrar
    }

    // Verifica se o grafo contém um nó com o ID especificado
    public boolean containsNode(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return false; // Retorna false para IDs inválidos
        }

        // Itera pelos nós e verifica a existência
        for (int i = 0; i < nodes.size(); i++) {
            Node node = nodes.get(i);
            if (node.getId().equals(nodeId)) {
                return true; // Retorna true se o nó existir
            }
        }

        return false; // Caso nenhum nó corresponda ao ID
    }

    // Verifica se o grafo contém uma aresta entre dois nós
    public boolean containsEdge(String sourceId, String targetId) {
        if (sourceId == null || targetId == null || sourceId.isEmpty() || targetId.isEmpty()) {
            return false; // IDs inválidos não podem ter conexão
        }

        for (int i = 0; i < edges.size(); i++) {
            Edge edge = edges.get(i);

            // Verifica conexões unidirecionais e bidirecionais
            if (edge.getSource().equals(sourceId) && edge.getDestination().equals(targetId)) {
                return true;
            }
            if (!edge.isOneway() && edge.getSource().equals(targetId) && edge.getDestination().equals(sourceId)) {
                return true; // Conexão bidirecional
            }
        }

        return false; // Caso nenhuma aresta atenda aos critérios
    }
}