package org.aiacon.simuladordemobilidadeurbana.control;

import org.aiacon.simuladordemobilidadeurbana.model.LightPhase;
import org.aiacon.simuladordemobilidadeurbana.model.TrafficLight;
import org.aiacon.simuladordemobilidadeurbana.simulation.Configuration;
import org.aiacon.simuladordemobilidadeurbana.simulation.NextPhaseDecision;

public class EnergySavingStrategy implements TrafficLightControlStrategy {
    private double strategyBaseGreenDuration;
    private double strategyYellowDuration;
    private double strategyMinGreenDuration;
    private int strategyLowTrafficThreshold;
    private double strategyMaxGreenDuration; // Teto máximo para o verde

    public EnergySavingStrategy(double baseGreen, double yellow, double minGreen, int threshold, double maxGreen) {
        this.strategyBaseGreenDuration = baseGreen;
        this.strategyYellowDuration = yellow;
        this.strategyMinGreenDuration = minGreen;
        this.strategyLowTrafficThreshold = threshold;
        this.strategyMaxGreenDuration = maxGreen; // Armazena o teto
    }

    // Construtor padrão, se ainda necessário, deve usar valores consistentes ou buscar da config
    public EnergySavingStrategy() {
        this(20.0, 3.0, 7.0, 1, 40.0); // Exemplo de valores default
    }

    @Override
    public void initialize(TrafficLight light) {
        String initialJsonDir = light.getInitialJsonDirection().toLowerCase();
        LightPhase startPhase = LightPhase.NS_GREEN_EW_RED;

        if (initialJsonDir.contains("east") || initialJsonDir.contains("west")) {
            startPhase = LightPhase.NS_RED_EW_GREEN;
        }

        double initialDuration = light.isPeakHourEnabled() ? this.strategyBaseGreenDuration + 2.0 : this.strategyBaseGreenDuration; // Exemplo de bônus
        initialDuration = Math.max(initialDuration, this.strategyMinGreenDuration);
        initialDuration = Math.min(initialDuration, this.strategyMaxGreenDuration); // Respeita o teto

        light.setCurrentPhase(startPhase, initialDuration);
    }

    private double calculateEnergySavingGreenTime(TrafficLight light, int[] queueSizes, boolean isEastWestPhase, boolean isPeakHour) {
        double greenTime = isPeakHour ? this.strategyBaseGreenDuration + 2.0 : this.strategyBaseGreenDuration; // Bônus de pico

        Integer relevantIndex1 = isEastWestPhase ? light.getDirectionIndex("east") : light.getDirectionIndex("north");
        Integer relevantIndex2 = isEastWestPhase ? light.getDirectionIndex("west") : light.getDirectionIndex("south");

        int trafficCount = ((relevantIndex1 != null && relevantIndex1 < queueSizes.length) ? queueSizes[relevantIndex1] : 0) +
                ((relevantIndex2 != null && relevantIndex2 < queueSizes.length) ? queueSizes[relevantIndex2] : 0);

        if (trafficCount <= this.strategyLowTrafficThreshold && !isPeakHour) {
            greenTime = this.strategyMinGreenDuration;
        }
        // Aplica o teto máximo para o verde no modo economia
        return Math.min(greenTime, this.strategyMaxGreenDuration);
    }

    @Override
    public NextPhaseDecision decideNextPhase(TrafficLight light, double deltaTime, int[] queueSizes, boolean isPeakHour) {
        LightPhase currentPhase = light.getCurrentPhase();
        LightPhase nextPhase;
        double duration;

        switch (currentPhase) {
            case NS_GREEN_EW_RED:
                nextPhase = LightPhase.NS_YELLOW_EW_RED;
                duration = this.strategyYellowDuration;
                break;
            case NS_YELLOW_EW_RED:
                nextPhase = LightPhase.NS_RED_EW_GREEN;
                duration = calculateEnergySavingGreenTime(light, queueSizes, true, isPeakHour);
                break;
            case NS_RED_EW_GREEN:
                nextPhase = LightPhase.NS_RED_EW_YELLOW;
                duration = this.strategyYellowDuration;
                break;
            case NS_RED_EW_YELLOW:
                nextPhase = LightPhase.NS_GREEN_EW_RED;
                duration = calculateEnergySavingGreenTime(light, queueSizes, false, isPeakHour);
                break;
            default:
                System.err.println("EnergySavingStrategy: Fase atual desconhecida " + currentPhase + " para o nó " + light.getNodeId() + ". Resetando para NS_GREEN_EW_RED.");
                nextPhase = LightPhase.NS_GREEN_EW_RED;
                duration = calculateEnergySavingGreenTime(light, queueSizes, false, isPeakHour);
                break;
        }
        return new NextPhaseDecision(nextPhase, duration);
    }

    @Override
    public String getLightStateForApproach(TrafficLight light, String approachDirection) {
        LightPhase currentPhase = light.getCurrentPhase();
        if (currentPhase == null || approachDirection == null) return "red";
        String dir = approachDirection.toLowerCase();

        switch (currentPhase) {
            case NS_GREEN_EW_RED: return (dir.equals("north") || dir.equals("south")) ? "green" : "red";
            case NS_YELLOW_EW_RED: return (dir.equals("north") || dir.equals("south")) ? "yellow" : "red";
            case NS_RED_EW_GREEN: return (dir.equals("east") || dir.equals("west")) ? "green" : "red";
            case NS_RED_EW_YELLOW: return (dir.equals("east") || dir.equals("west")) ? "yellow" : "red";
            default: return "red";
        }
    }
}