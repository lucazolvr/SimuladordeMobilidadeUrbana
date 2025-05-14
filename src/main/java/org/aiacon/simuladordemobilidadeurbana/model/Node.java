package org.aiacon.simuladordemobilidadeurbana.model;

// Representa uma interseção na rede urbana
public class Node {
    public String id; // Identificador único (do OSM)
    public double latitude; // Coordenada latitudinal
    public double longitude; // Coordenada longitudinal
    public boolean isTrafficLight; // Indica se tem semáforo
    public Node next; // Para lista encadeada

    private CustomLinkedList<Edge> edges; // Lista de arestas conectadas ao nó (implementação personalizada)

    // Construtor
    public Node(String id, double latitude, double longitude, boolean isTrafficLight) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isTrafficLight = isTrafficLight;
        this.next = null;
        this.edges = new CustomLinkedList<>(); // Inicializa a lista de arestas com a sua implementação personalizada
    }

    // Getter para o campo id
    public String getId() {
        return id;
    }

    // Adiciona uma aresta conectada ao nó
    public void addEdge(Edge edge) {
        if (edge != null) {
            edges.add(edge); // Metodo add da CustomLinkedList adiciona a aresta
        }
    }

    // Retorna a lista de arestas conectadas ao nó
    public CustomLinkedList<Edge> getEdges() {
        return edges; // Retorna a referência da CustomLinkedList
    }
}