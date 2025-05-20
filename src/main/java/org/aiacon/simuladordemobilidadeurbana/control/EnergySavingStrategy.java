package org.aiacon.simuladordemobilidadeurbana.control;

import org.aiacon.simuladordemobilidadeurbana.model.LightPhase;
import org.aiacon.simuladordemobilidadeurbana.model.TrafficLight;
import org.aiacon.simuladordemobilidadeurbana.simulation.NextPhaseDecision;
// TrafficLightControlStrategy já está no mesmo pacote

public class EnergySavingStrategy implements TrafficLightControlStrategy {
    private double strategyBaseGreenDuration;
    private double strategyYellowDuration;
    private double strategyMinGreenDuration;
    private int strategyLowTrafficThreshold;

    // Construtor que você está chamando de TrafficLight
    public EnergySavingStrategy(double baseGreen, double yellow, double minGreen, int threshold) {
        this.strategyBaseGreenDuration = baseGreen;
        this.strategyYellowDuration = yellow;
        this.strategyMinGreenDuration = minGreen;
        this.strategyLowTrafficThreshold = threshold;
    }

    public EnergySavingStrategy() {
        // Chama o construtor parametrizado com valores default
        this(12.0, 3.0, 5.0, 1);
    }

    @Override
    public void initialize(TrafficLight light) {
        String initialJsonDir = light.getInitialJsonDirection().toLowerCase();
        LightPhase startPhase = LightPhase.NS_GREEN_EW_RED;

        if (initialJsonDir.contains("east") || initialJsonDir.contains("west")) {
            startPhase = LightPhase.NS_RED_EW_GREEN;
        }
        // A duração inicial pode ser simplesmente o tempo base ou o mínimo.
        // Não há filas no início para justificar uma duração menor.
        double initialDuration = light.isPeakHourEnabled() ? this.strategyBaseGreenDuration + 2.0 : this.strategyBaseGreenDuration;
        initialDuration = Math.max(initialDuration, this.strategyMinGreenDuration);

        light.setCurrentPhase(startPhase, initialDuration);
    }

    @Override
    public NextPhaseDecision decideNextPhase(TrafficLight light, double deltaTime, int[] queueSizes, boolean isPeakHour) {
        LightPhase currentPhase = light.getCurrentPhase();
        LightPhase nextPhase;
        double duration;

        double currentCycleGreenTime = isPeakHour ? this.strategyBaseGreenDuration + 2.0 : this.strategyBaseGreenDuration;
        double currentCycleYellowTime = this.strategyYellowDuration;
        double currentCycleMinGreenTime = this.strategyMinGreenDuration;

        switch (currentPhase) {
            case NS_GREEN_EW_RED:
                nextPhase = LightPhase.NS_YELLOW_EW_RED;
                duration = currentCycleYellowTime;
                break;
            case NS_YELLOW_EW_RED:
                nextPhase = LightPhase.NS_RED_EW_GREEN;
                Integer eastIdx = light.getDirectionIndex("east");
                Integer westIdx = light.getDirectionIndex("west");
                int ewTraffic = ((eastIdx != null && eastIdx < queueSizes.length) ? queueSizes[eastIdx] : 0) +
                        ((westIdx != null && westIdx < queueSizes.length) ? queueSizes[westIdx] : 0);
                duration = (ewTraffic <= strategyLowTrafficThreshold && !isPeakHour) ? currentCycleMinGreenTime : currentCycleGreenTime;
                break;
            case NS_RED_EW_GREEN:
                nextPhase = LightPhase.NS_RED_EW_YELLOW;
                duration = currentCycleYellowTime;
                break;
            case NS_RED_EW_YELLOW:
                nextPhase = LightPhase.NS_GREEN_EW_RED;
                Integer northIdx = light.getDirectionIndex("north");
                Integer southIdx = light.getDirectionIndex("south");
                int nsTraffic = ((northIdx != null && northIdx < queueSizes.length) ? queueSizes[northIdx] : 0) +
                        ((southIdx != null && southIdx < queueSizes.length) ? queueSizes[southIdx] : 0);
                duration = (nsTraffic <= strategyLowTrafficThreshold && !isPeakHour) ? currentCycleMinGreenTime : currentCycleGreenTime;
                break;
            default:
                System.err.println("EnergySavingStrategy: Fase atual desconhecida " + currentPhase + " para o nó " + light.getNodeId() + ". Resetando para NS_GREEN_EW_RED.");
                nextPhase = LightPhase.NS_GREEN_EW_RED;
                duration = currentCycleGreenTime;
                break;
        }
        return new NextPhaseDecision(nextPhase, duration);
    }

    @Override
    public String getLightStateForApproach(TrafficLight light, String approachDirection) {
        // Mesma lógica da FixedTimeStrategy, pois o estado da luz depende da fase
        LightPhase currentPhase = light.getCurrentPhase();
        if (currentPhase == null || approachDirection == null) return "red";
        String dir = approachDirection.toLowerCase();

        switch (currentPhase) {
            case NS_GREEN_EW_RED:
                return (dir.equals("north") || dir.equals("south")) ? "green" : "red";
            case NS_YELLOW_EW_RED:
                return (dir.equals("north") || dir.equals("south")) ? "yellow" : "red";
            case NS_RED_EW_GREEN:
                return (dir.equals("east") || dir.equals("west")) ? "green" : "red";
            case NS_RED_EW_YELLOW:
                return (dir.equals("east") || dir.equals("west")) ? "yellow" : "red";
            default:
                return "red";
        }
    }
}