package org.aiacon.simuladordemobilidadeurbana.model;

public class Vehicle {
    private String id; // Identificador único
    private String origin; // Nó de origem
    private String destination; // Nó de destino
    private CustomLinkedList<String> route; // Rota calculada (lista de nós)
    private String currentNode; // Nó atual
    private double travelTime; // Tempo total de viagem (s)
    private double waitTime; // Tempo total de espera (s)
    private double position; // Posição na aresta atual (0 a 1)
    public Vehicle next; // Para lista encadeada
    private double fuelConsumed = 0.0;
    private double fuelConsumptionRateMoving = 0.0005; // L/s em movimento
    private double fuelConsumptionRateIdle = 0.0002;   // L/s em marcha lenta
    // Construtor
    public Vehicle(String id, String origin, String destination, CustomLinkedList<String> route) {
        this.id = id;
        this.origin = origin;
        this.destination = destination;
        this.route = (route != null) ? route : new CustomLinkedList<>(); // Atribuir rota válida
        this.currentNode = origin;
        this.travelTime = 0.0;
        this.waitTime = 0.0;
        this.position = 0.0;
        this.next = null;
    }


    // Getters e Setters
    public String getId() {
        return id;
    }

    public String getOrigin() {
        return origin;
    }

    public String getDestination() {
        return destination;
    }

    public CustomLinkedList<String> getRoute() {
        return route;
    }

    public void setRoute(CustomLinkedList<String> route) {
        if (route == null) {
            System.err.println("Atribuição de rota nula para o veículo " + id);
            this.route = new CustomLinkedList<>(); // Substitui por uma rota vazia
        } else {
            this.route = route;
        }
    }

    public String getCurrentNode() {
        return currentNode;
    }

    public void setCurrentNode(String currentNode) {
        this.currentNode = currentNode;
    }

    public double getTravelTime() {
        return travelTime;
    }

    public void incrementTravelTime(double deltaTime) {
        this.travelTime += deltaTime;
    }

    public double getWaitTime() {
        return waitTime;
    }

    public void incrementWaitTime(double deltaTime) {
        this.waitTime += deltaTime;
    }

    public double getPosition() {
        return position;
    }

    public void setPosition(double position) {
        this.position = position;
    }

    public double getFuelConsumed() {
        return fuelConsumed;
    }

    public void incrementFuelConsumption(double consumption) {
        this.fuelConsumed += consumption;
    }

    public double getFuelConsumptionRateMoving() {
        return fuelConsumptionRateMoving;
    }

    public double getFuelConsumptionRateIdle() {
        return fuelConsumptionRateIdle;
    }


}
