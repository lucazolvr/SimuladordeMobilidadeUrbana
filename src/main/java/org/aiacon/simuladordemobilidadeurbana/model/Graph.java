package org.aiacon.simuladordemobilidadeurbana.model;

// Representa o grafo da rede urbana
public class Graph {
    private CustomLinkedList<Node> nodes;               // Lista dos nós (cruzamentos da rede)
    private CustomLinkedList<Edge> edges;               // Lista das arestas (ruas da rede)
    private CustomLinkedList<TrafficLight> trafficLights; // Lista dos semáforos

    public Graph() {
        nodes = new CustomLinkedList<>();
        edges = new CustomLinkedList<>();
        trafficLights = new CustomLinkedList<>();
    }

    // Adiciona um nó ao grafo
    public void addNode(Node node) {
        nodes.add(node);
    }

    // Retorna todos os nós do grafo
    public CustomLinkedList<Node> getNodes() {
        return nodes;
    }

    // Adiciona uma aresta (rua) ao grafo
    public void addEdge(Edge edge) {
        edges.add(edge);
    }

    // Retorna todas as arestas do grafo
    public CustomLinkedList<Edge> getEdges() {
        return edges;
    }

    // Adiciona um semáforo ao grafo
    public void addTrafficLight(TrafficLight trafficLight) {
        trafficLights.add(trafficLight);
        // Marcar nó correspondente como semáforo
        for (Node node : nodes) { // Usar iteração segura
            if (node.id.equals(trafficLight.getNodeId())) {
                node.isTrafficLight = true;
                break;
            }
        }
    }

    // Retorna todos os semáforos do grafo
    public CustomLinkedList<TrafficLight> getTrafficLights() {
        return trafficLights;
    }
}