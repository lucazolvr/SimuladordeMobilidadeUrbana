package org.aiacon.simuladordemobilidadeurbana.control;

import org.aiacon.simuladordemobilidadeurbana.model.LightPhase;
import org.aiacon.simuladordemobilidadeurbana.model.TrafficLight;
import org.aiacon.simuladordemobilidadeurbana.simulation.NextPhaseDecision;
// TrafficLightControlStrategy já está no mesmo pacote

public class FixedTimeStrategy implements TrafficLightControlStrategy {

    private double strategyGreenDuration;
    private double strategyYellowDuration;

    public FixedTimeStrategy(double greenTime, double yellowTime) {
        this.strategyGreenDuration = greenTime;
        this.strategyYellowDuration = yellowTime;
    }

    public FixedTimeStrategy() {
        this.strategyGreenDuration = 15.0;
        this.strategyYellowDuration = 3.0;
    }

    @Override
    public void initialize(TrafficLight light) {
        String initialJsonDir = light.getInitialJsonDirection().toLowerCase();
        LightPhase startPhase = LightPhase.NS_GREEN_EW_RED; // Padrão
        // Ajusta a duração inicial se for horário de pico (usando o getter de TrafficLight)
        double initialDuration = light.isPeakHourEnabled() ? 20.0 : this.strategyGreenDuration;

        if (initialJsonDir.contains("east") || initialJsonDir.contains("west")) {
            startPhase = LightPhase.NS_RED_EW_GREEN;
        }
        // Para "forward" ou "backward", a lógica para determinar a orientação principal
        // (N-S ou L-O) precisaria de mais informações (ex: geometria do grafo).
        // Por enquanto, o fallback é iniciar com NS_GREEN_EW_RED.

        light.setCurrentPhase(startPhase, initialDuration);
    }

    @Override
    public NextPhaseDecision decideNextPhase(TrafficLight light, double deltaTime, int[] queueSizes, boolean isPeakHour) {
        LightPhase currentPhase = light.getCurrentPhase();
        LightPhase nextPhase;
        double duration;

        double activeGreenDuration = isPeakHour ? 20.0 : this.strategyGreenDuration;
        double activeYellowDuration = this.strategyYellowDuration;

        // Lógica de ciclo fixo
        switch (currentPhase) {
            case NS_GREEN_EW_RED:
                nextPhase = LightPhase.NS_YELLOW_EW_RED;
                duration = activeYellowDuration;
                break;
            case NS_YELLOW_EW_RED:
                nextPhase = LightPhase.NS_RED_EW_GREEN;
                duration = activeGreenDuration; // Verde para Leste-Oeste
                break;
            case NS_RED_EW_GREEN:
                nextPhase = LightPhase.NS_RED_EW_YELLOW;
                duration = activeYellowDuration;
                break;
            case NS_RED_EW_YELLOW:
                nextPhase = LightPhase.NS_GREEN_EW_RED;
                duration = activeGreenDuration; // Verde para Norte-Sul
                break;
            default:
                System.err.println("FixedTimeStrategy: Fase atual desconhecida (" + currentPhase + ") para o nó " + light.getNodeId() + ". Resetando para NS_GREEN_EW_RED.");
                nextPhase = LightPhase.NS_GREEN_EW_RED;
                duration = activeGreenDuration;
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