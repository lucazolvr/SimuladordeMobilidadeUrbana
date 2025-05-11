package org.aiacon.simuladordemobilidadeurbana.model;

import java.util.LinkedList;
import java.util.Queue;

public class TrafficLight {
    private String nodeId;       // ID do nó associado ao semáforo
    private int mode;            // 1: fixo, 2: espera, 3: economia de energia
    private String direction;    // Direção principal (ex.: north, south, east, west)
    private int greenTime;       // Tempo do sinal verde (segundos)
    private int yellowTime;      // Tempo do sinal amarelo (segundos)
    private int redTime;         // Tempo do sinal vermelho (segundos)
    private String state;        // Estado atual (green, yellow, red)
    private double timeInCycle;  // Tempo atual dentro do ciclo
    private Queue<Vehicle>[] directionQueues; // Filas de veículos por direção (north, south, east, west)

    // Construtor
    public TrafficLight(String nodeId, String direction, int mode) {
        this.nodeId = nodeId;
        this.direction = direction;
        this.mode = mode;

        // Ciclo padrão do semáforo
        this.greenTime = 15; // Tempo padrão do sinal verde
        this.yellowTime = 3; // Tempo padrão do sinal amarelo
        this.redTime = 15;   // Tempo padrão do sinal vermelho
        this.state = "red";  // Estado inicial
        this.timeInCycle = 0.0;

        // Inicializar filas para as quatro direções
        directionQueues = new LinkedList[4];
        for (int i = 0; i < 4; i++) {
            directionQueues[i] = new LinkedList<>();
        }
    }

    // Getters e Setters
    public String getNodeId() {
        return nodeId;
    }

    public int getMode() {
        return mode;
    }

    public void setMode(int mode) {
        this.mode = mode;
    }

    public String getState() {
        return state;
    }

    public void setCycleTimes(int greenTime, int yellowTime, int redTime) {
        this.greenTime = greenTime;
        this.yellowTime = yellowTime;
        this.redTime = redTime;
    }

    /**
     * Retorna o tamanho da fila de uma direção específica.
     */
    public int getDirectionQueueSize(int directionIndex) {
        if (directionIndex < 0 || directionIndex >= directionQueues.length) {
            return 0; // Direção inválida
        }
        return directionQueues[directionIndex].size();
    }

    /**
     * Adiciona um veículo à fila de uma direção específica.
     */
    public void addVehicleToQueue(int directionIndex, Vehicle vehicle) {
        if (directionIndex >= 0 && directionIndex < directionQueues.length) {
            directionQueues[directionIndex].add(vehicle);
        }
    }

    /**
     * Atualiza o estado do semáforo, as filas e gerencia o ciclo atual.
     */
    public void update(double deltaTime, int queueSizeNorth, int queueSizeSouth, int queueSizeEast, int queueSizeWest, boolean isPeakHour) {
        // Atualizar tempos do ciclo com base no horário de pico
        if (isPeakHour) {
            this.greenTime = 20;
            this.redTime = 20;
        } else {
            this.greenTime = 15;
            this.redTime = 15;
        }

        // Atualiza o tempo dentro do ciclo
        timeInCycle += deltaTime;

        // Alterna o estado do semáforo com base no ciclo
        double cycleDuration = greenTime + yellowTime + redTime;

        if (timeInCycle <= greenTime) {
            state = "green";
        } else if (timeInCycle <= greenTime + yellowTime) {
            state = "yellow";
        } else if (timeInCycle <= cycleDuration) {
            state = "red";
        } else {
            timeInCycle = 0.0; // Reinicia o ciclo
        }
    }

    /**
     * Remove o primeiro veículo da fila de uma direção específica (se possível).
     */
    public Vehicle popVehicleFromQueue(int directionIndex) {
        if (directionIndex >= 0 && directionIndex < directionQueues.length && !directionQueues[directionIndex].isEmpty()) {
            return directionQueues[directionIndex].poll(); // Remove e retorna o primeiro veículo da fila
        }
        return null;
    }

    /**
     * Imprime o estado atual do semáforo para fins de depuração.
     */
    public void logState() {
        System.out.printf("Semáforo no nó %s -> Estado: %s, Tempo no Ciclo: %.1f%n",
                nodeId, state, timeInCycle);
    }
}