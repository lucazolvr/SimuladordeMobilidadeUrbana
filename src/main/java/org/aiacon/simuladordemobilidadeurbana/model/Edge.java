package org.aiacon.simuladordemobilidadeurbana.model;

public class Edge {
    private String id; // Identificador único (ex.: source-target-key)
    private String source; // ID do nó de origem
    private String target; // ID do nó de destino
    private double length; // Comprimento em metros
    private double travelTime; // Tempo de travessia em segundos
    private boolean oneway; // Mão única (true) ou dupla (false)
    private double maxspeed; // Velocidade máxima em km/h
    private int capacity; // Capacidade de fluxo (veículos)
    public Edge next; // Para lista encadeada

    // Construtor
    public Edge(String id, String source, String target, double length, double travelTime,
                boolean oneway, double maxspeed, int capacity) {
        this.id = id;
        this.source = source;
        this.target = target;
        this.length = length;
        this.travelTime = travelTime;
        this.oneway = oneway;
        this.maxspeed = maxspeed;
        this.capacity = capacity;
        this.next = null;
    }

    // Getters e Setters
    public String getId() {
        return id;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public String getTarget() {
        return target;
    }

    public void setTarget(String target) {
        this.target = target;
    }

    public double getLength() {
        return length;
    }

    public void setLength(double length) {
        this.length = length;
    }

    public double getTravelTime() {
        return travelTime;
    }

    public void setTravelTime(double travelTime) {
        this.travelTime = travelTime;
    }

    public boolean isOneway() {
        return oneway;
    }

    public void setOneway(boolean oneway) {
        this.oneway = oneway;
    }

    public double getMaxspeed() {
        return maxspeed;
    }

    public void setMaxspeed(double maxspeed) {
        this.maxspeed = maxspeed;
    }

    public int getCapacity() {
        return capacity;
    }

    public void setCapacity(int capacity) {
        this.capacity = capacity;
    }

    // Métodos auxiliares
    public double getAverageSpeed() {
        // Calcula a velocidade média em m/s
        return length / travelTime;
    }

    public boolean isBidirectional() {
        // Verifica se a aresta é bidirecional
        return !oneway;
    }

    // Retorna o destino da aresta (ID do nó de destino)
    public String getDestination() {
        return target;
    }
}