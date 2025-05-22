package org.aiacon.simuladordemobilidadeurbana.model;

import java.util.HashMap; // Importar HashMap
import java.util.Map;    // Importar Map

// Representa o grafo da rede urbana
public class Graph {
    private CustomLinkedList<Node> nodesList; // Usando a CustomLinkedList refatorada
    private CustomLinkedList<Edge> edgesList; // Usando a CustomLinkedList refatorada
    private CustomLinkedList<TrafficLight> trafficLightsList; // Usando a CustomLinkedList refatorada

    // Estrutura auxiliar permitida para busca rápida de nós por ID
    private Map<String, Node> nodeMap;

    public Graph() {
        this.nodesList = new CustomLinkedList<>();
        this.edgesList = new CustomLinkedList<>();
        this.trafficLightsList = new CustomLinkedList<>();
        this.nodeMap = new HashMap<>(); // Inicializar o HashMap
    }

    public void addNode(Node node) {
        if (node != null && node.getId() != null && !node.getId().isEmpty()) {
            if (!this.nodeMap.containsKey(node.getId())) {
                this.nodesList.add(node); // Adiciona à sua lista personalizada
                this.nodeMap.put(node.getId(), node); // Adiciona ao HashMap
                // System.out.println("Nó adicionado ao grafo: " + node.getId()); // Log no JsonParser é melhor
            } else {
                // System.err.println("GRAPH_ADD_NODE: Tentativa de adicionar nó com ID duplicado: " + node.getId());
            }
        } else {
            System.err.println("GRAPH_ADD_NODE: Tentativa de adicionar um nó nulo ou com ID inválido.");
        }
    }

    public CustomLinkedList<Node> getNodes() {
        return this.nodesList;
    }

    // Busca um nó específico no grafo pelo seu ID usando o HashMap
    public Node getNode(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return null;
        }
        return this.nodeMap.get(nodeId); // Busca O(1) em média
    }

    public void addEdge(Edge edge) {
        if (edge != null) {
            this.edgesList.add(edge);
            // System.out.println("Aresta adicionada ao grafo: origem=" + edge.getSource() + ", destino=" + edge.getDestination());
        } else {
            System.err.println("GRAPH_ADD_EDGE: Tentativa de adicionar uma aresta nula.");
        }
    }

    public CustomLinkedList<Edge> getEdges() {
        return this.edgesList;
    }

    public void addTrafficLight(TrafficLight trafficLight) {
        if (trafficLight != null) {
            this.trafficLightsList.add(trafficLight);
            // System.out.println("Semáforo adicionado ao grafo: " + trafficLight);
        } else {
            System.err.println("GRAPH_ADD_TRAFFIC_LIGHT: Tentativa de adicionar um semáforo nulo.");
        }
    }

    public CustomLinkedList<TrafficLight> getTrafficLights() {
        return this.trafficLightsList;
    }

    public boolean containsNode(String nodeId) {
        if (nodeId == null || nodeId.isEmpty()) {
            return false;
        }
        return this.nodeMap.containsKey(nodeId); // O(1) em média
    }

    public boolean containsEdge(String sourceId, String targetId) {
        if (sourceId == null || targetId == null || sourceId.isEmpty() || targetId.isEmpty()) {
            return false;
        }
        for (Edge edge : this.edgesList) {
            if (edge == null) continue;
            // Checagem primária
            if (edge.getSource().equals(sourceId) && edge.getDestination().equals(targetId)) {
                return true;
            }
            // Se a lógica de bidirecionalidade ainda for necessária aqui (idealmente o JsonParser já trata isso
            // criando duas arestas unidirecionais para 'oneway:false')
            // if (!edge.isOneway() && edge.getSource().equals(targetId) && edge.getDestination().equals(sourceId)) {
            // return true;
            // }
        }
        return false;
    }
}