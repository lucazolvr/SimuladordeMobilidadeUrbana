package org.aiacon.simuladordemobilidadeurbana.model;

import org.aiacon.simuladordemobilidadeurbana.control.AdaptiveQueueStrategy;
import org.aiacon.simuladordemobilidadeurbana.control.EnergySavingStrategy;
import org.aiacon.simuladordemobilidadeurbana.control.FixedTimeStrategy;
import org.aiacon.simuladordemobilidadeurbana.control.TrafficLightControlStrategy;
import org.aiacon.simuladordemobilidadeurbana.simulation.Configuration; // Importe Configuration
import org.aiacon.simuladordemobilidadeurbana.simulation.NextPhaseDecision;

import java.util.HashMap;
import java.util.Map;

public class TrafficLight {
    private String nodeId;
    private int mode;
    private String initialJsonDirection;

    private LightPhase currentPhase;
    private double phaseTimer;

    private Queue[] directionQueues;
    private Map<String, Integer> directionNameToIndexMap;

    private TrafficLightControlStrategy controlStrategy;
    private boolean peakHourStatus = false;
    private Configuration config; // Armazena a referência para a configuração

    public TrafficLight(String nodeId, String jsonOriginalDirection, Configuration config) { // Recebe Configuration
        this.nodeId = nodeId;
        this.initialJsonDirection = jsonOriginalDirection != null ? jsonOriginalDirection.toLowerCase() : "unknown";
        this.config = config;
        this.mode = config.getTrafficLightMode();

        this.directionQueues = new Queue[4];
        for (int i = 0; i < 4; i++) {
            this.directionQueues[i] = new Queue();
        }

        this.directionNameToIndexMap = new HashMap<>();
        directionNameToIndexMap.put("north", 0);
        directionNameToIndexMap.put("east", 1);
        directionNameToIndexMap.put("south", 2);
        directionNameToIndexMap.put("west", 3);

        switch (this.mode) {
            case 1:
                this.controlStrategy = new FixedTimeStrategy(
                        config.getFixedGreenTime(),
                        config.getFixedYellowTime()
                );
                break;
            case 2:
                this.controlStrategy = new AdaptiveQueueStrategy(
                        config.getAdaptiveBaseGreen(),
                        config.getAdaptiveYellowTime(),
                        config.getAdaptiveMaxGreen(),       // Passando o teto MÁXIMO de verde
                        config.getAdaptiveQueueThreshold(),
                        config.getAdaptiveMinGreenTime(),   // Passando o MÍNIMO de verde
                        config.getAdaptiveIncrement()       // Passando o incremento por veículo
                );
                break;
            case 3:
                this.controlStrategy = new EnergySavingStrategy(
                        config.getEnergySavingBaseGreen(),
                        config.getEnergySavingYellowTime(),
                        config.getEnergySavingMinGreen(),
                        config.getEnergySavingThreshold(),
                        config.getEnergySavingMaxGreenTime() // Passando o teto máximo
                );
                break;
            default:
                System.err.println("TRAFFIC_LIGHT_INIT: Modo de semáforo inválido (" + mode + ") para nó " + nodeId + ". Usando FixedTime por padrão.");
                this.controlStrategy = new FixedTimeStrategy(
                        config.getFixedGreenTime(),
                        config.getFixedYellowTime()
                );
                break;
        }

        if (this.controlStrategy != null) {
            this.controlStrategy.initialize(this);
        } else {
            System.err.println("TRAFFIC_LIGHT_INIT_FATAL: controlStrategy não foi instanciada para o nó " + nodeId);
            setCurrentPhase(LightPhase.NS_GREEN_EW_RED, config.getFixedGreenTime());
        }

        if (this.currentPhase == null) {
            // A estratégia DEVE definir a fase inicial. Se não, logar e definir um padrão.
            System.err.println("TRAFFIC_LIGHT_INIT_WARN: Estratégia não definiu fase inicial para o nó " + nodeId + ". Definindo NS_GREEN_EW_RED como padrão.");
            setCurrentPhase(LightPhase.NS_GREEN_EW_RED, config.getFixedGreenTime());
            logPhaseChange(); // Loga a fase de fallback
        }
    }

    public String getNodeId() { return nodeId; }
    public LightPhase getCurrentPhase() { return currentPhase; }
    public String getInitialJsonDirection() { return initialJsonDirection; }
    public boolean isPeakHourEnabled() { return peakHourStatus; }
    public Configuration getConfiguration() { return config; }

    public void setCurrentPhase(LightPhase phase, double duration) {
        this.currentPhase = phase;
        this.phaseTimer = duration;
    }

    public Integer getDirectionIndex(String directionName) {
        if (directionName == null) return null;
        return directionNameToIndexMap.get(directionName.toLowerCase());
    }

    public int[] getAllQueueSizes() {
        int[] sizes = new int[4];
        for (int i = 0; i < 4; i++) {
            sizes[i] = (directionQueues[i] != null) ? directionQueues[i].size() : 0;
        }
        return sizes;
    }

    public void addVehicleToQueue(String directionName, Vehicle vehicle) {
        Integer index = getDirectionIndex(directionName);
        if (index != null && index >= 0 && index < directionQueues.length) {
            if (directionQueues[index] == null) {
                directionQueues[index] = new Queue();
            }
            directionQueues[index].enqueue(vehicle);
        } else {
            System.err.println("TrafficLight " + nodeId + ": Não foi possível encontrar índice para direção '" + directionName + "' ao tentar enfileirar veículo " + vehicle.getId());
        }
    }

    public Vehicle popVehicleFromQueue(String directionName) {
        Integer index = getDirectionIndex(directionName);
        if (index != null && index >= 0 && index < directionQueues.length &&
                directionQueues[index] != null && !directionQueues[index].isEmpty()) {
            return directionQueues[index].dequeue();
        }
        return null;
    }

    public void update(double deltaTime, boolean isPeakHour) {
        this.peakHourStatus = isPeakHour;
        this.phaseTimer -= deltaTime;

        if (this.phaseTimer <= 0) {
            if (this.controlStrategy == null) {
                System.err.println("TrafficLight " + nodeId + ": ERRO FATAL - controlStrategy é nula no método update.");
                setCurrentPhase(LightPhase.NS_GREEN_EW_RED, config.getFixedGreenTime());
                logPhaseChange();
                return;
            }
            NextPhaseDecision decision = controlStrategy.decideNextPhase(this, deltaTime, getAllQueueSizes(), this.peakHourStatus);

            if (decision != null && decision.nextPhase != null) {
                setCurrentPhase(decision.nextPhase, decision.duration);
                logPhaseChange();
            } else {
                System.err.println("TrafficLight " + nodeId + ": Estratégia retornou decisão/fase nula. Mantendo fase atual ("+this.currentPhase+") e resetando timer para um valor seguro.");
                this.phaseTimer = config.getFixedGreenTime();
                if (this.currentPhase == null) {
                    setCurrentPhase(LightPhase.NS_GREEN_EW_RED, this.phaseTimer);
                    logPhaseChange();
                }
            }
        }
    }

    public String getLightStateForApproach(String approachDirection) {
        if (controlStrategy == null) {
            System.err.println("TrafficLight " + nodeId + ": Estratégia de controle não inicializada ao chamar getLightStateForApproach.");
            return "red";
        }
        return controlStrategy.getLightStateForApproach(this, approachDirection);
    }

    private void logPhaseChange() {
        String phaseStr = (this.currentPhase != null) ? this.currentPhase.toString() : "INDEFINIDA";
        System.out.println("Semáforo " + nodeId + ": Nova FASE -> " + phaseStr +
                ". Duração programada: " + String.format("%.1f", this.phaseTimer) + "s.");
    }

    public void logCurrentInternalState() {
        String phaseStr = (this.currentPhase != null) ? this.currentPhase.toString() : "INDEFINIDA";
        System.out.printf("Semáforo no nó %s -> Fase: %s, Timer Restante: %.1f%n",
                nodeId, phaseStr, phaseTimer);
    }

    public synchronized int getTotalVehiclesInQueues() {
        int total = 0;
        if (directionQueues != null) {
            for (int i = 0; i < directionQueues.length; i++) {
                if (directionQueues[i] != null) {
                    total += directionQueues[i].size();
                }
            }
        }
        return total;
    }
}