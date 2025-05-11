package org.aiacon.simuladordemobilidadeurbana.model;

// Representa uma interseção na rede urbana
public class Node {
    public String id; // Identificador único (do OSM)
    public double latitude; // Coordenada latitudinal
    public double longitude; // Coordenada longitudinal
    public boolean isTrafficLight; // Indica se tem semáforo
    public Node next; // Para lista encadeada

    public Node(String id, double latitude, double longitude, boolean isTrafficLight) {
        this.id = id;
        this.latitude = latitude;
        this.longitude = longitude;
        this.isTrafficLight = isTrafficLight;
        this.next = null;
    }

    // Getter para o campo id
    public String getId() {
        return id;
    }
}
