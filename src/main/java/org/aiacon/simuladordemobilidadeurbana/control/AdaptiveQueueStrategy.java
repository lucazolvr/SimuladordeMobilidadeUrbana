package org.aiacon.simuladordemobilidadeurbana.control;

import org.aiacon.simuladordemobilidadeurbana.model.LightPhase;
import org.aiacon.simuladordemobilidadeurbana.model.TrafficLight;
import org.aiacon.simuladordemobilidadeurbana.simulation.Configuration;
import org.aiacon.simuladordemobilidadeurbana.simulation.NextPhaseDecision;

public class AdaptiveQueueStrategy implements TrafficLightControlStrategy {
    private double baseGreenTimeParam;
    private double yellowTimeParam;
    private double maxGreenTimeParam;      // Teto máximo absoluto para o verde
    private int queueThresholdParam;
    private double minGreenTimeParam;
    private double incrementPerVehicleParam;

    public AdaptiveQueueStrategy(double baseGreen, double yellow, double maxGreen,
                                 int threshold, double minGreen, double incrementPerVehicle) {
        this.baseGreenTimeParam = baseGreen;
        this.yellowTimeParam = yellow;
        this.maxGreenTimeParam = maxGreen; // Teto máximo para o tempo verde total
        this.queueThresholdParam = threshold;
        this.minGreenTimeParam = minGreen;
        this.incrementPerVehicleParam = incrementPerVehicle;
    }

    @Override
    public void initialize(TrafficLight light) {
        Configuration config = light.getConfiguration(); // Acessa a configuração
        String initialJsonDir = light.getInitialJsonDirection().toLowerCase();
        LightPhase startPhase = LightPhase.NS_GREEN_EW_RED;

        if (initialJsonDir.contains("east") || initialJsonDir.contains("west")) {
            startPhase = LightPhase.NS_RED_EW_GREEN;
        }

        double initialDuration = light.isPeakHourEnabled() ? this.baseGreenTimeParam + 5.0 : this.baseGreenTimeParam; // Bônus de pico
        initialDuration = Math.max(initialDuration, this.minGreenTimeParam);
        initialDuration = Math.min(initialDuration, this.maxGreenTimeParam); // Não exceder o teto máximo

        light.setCurrentPhase(startPhase, initialDuration);
        // Não precisa logar a mudança de fase aqui, o TrafficLight.update fará isso.
    }

    @Override
    public NextPhaseDecision decideNextPhase(TrafficLight light, double deltaTime, int[] queueSizes, boolean isPeakHour) {
        LightPhase currentPhase = light.getCurrentPhase();
        LightPhase nextPhaseDetermined;
        double durationDetermined;

        switch (currentPhase) {
            case NS_GREEN_EW_RED:
                nextPhaseDetermined = LightPhase.NS_YELLOW_EW_RED;
                durationDetermined = this.yellowTimeParam;
                break;
            case NS_YELLOW_EW_RED:
                nextPhaseDetermined = LightPhase.NS_RED_EW_GREEN;
                durationDetermined = calculateAdaptiveGreenTime(light, queueSizes, true, isPeakHour); // Verde para Leste-Oeste
                break;
            case NS_RED_EW_GREEN:
                nextPhaseDetermined = LightPhase.NS_RED_EW_YELLOW;
                durationDetermined = this.yellowTimeParam;
                break;
            case NS_RED_EW_YELLOW:
                nextPhaseDetermined = LightPhase.NS_GREEN_EW_RED;
                durationDetermined = calculateAdaptiveGreenTime(light, queueSizes, false, isPeakHour); // Verde para Norte-Sul
                break;
            default:
                System.err.println("AdaptiveQueueStrategy: Fase atual desconhecida " + currentPhase + " para nó " + light.getNodeId() +". Resetando para NS_GREEN_EW_RED.");
                nextPhaseDetermined = LightPhase.NS_GREEN_EW_RED;
                durationDetermined = calculateAdaptiveGreenTime(light, queueSizes, false, isPeakHour);
                break;
        }
        return new NextPhaseDecision(nextPhaseDetermined, durationDetermined);
    }

    private double calculateAdaptiveGreenTime(TrafficLight light, int[] queueSizes, boolean isEastWestGreenPhase, boolean isPeakHour) {
        double adaptiveGreenDuration = isPeakHour ? this.baseGreenTimeParam + 5.0 : this.baseGreenTimeParam;

        Integer relevantIndex1 = isEastWestGreenPhase ? light.getDirectionIndex("east") : light.getDirectionIndex("north");
        Integer relevantIndex2 = isEastWestGreenPhase ? light.getDirectionIndex("west") : light.getDirectionIndex("south");

        int relevantQueue1Size = (relevantIndex1 != null && relevantIndex1 >= 0 && relevantIndex1 < queueSizes.length) ? queueSizes[relevantIndex1] : 0;
        int relevantQueue2Size = (relevantIndex2 != null && relevantIndex2 >= 0 && relevantIndex2 < queueSizes.length) ? queueSizes[relevantIndex2] : 0;
        int maxRelevantQueue = Math.max(relevantQueue1Size, relevantQueue2Size);

        if (maxRelevantQueue == 0 && !isPeakHour) {
            return Math.max(this.minGreenTimeParam, adaptiveGreenDuration * 0.66);
        }

        if (maxRelevantQueue > this.queueThresholdParam) {
            double extension = (maxRelevantQueue - this.queueThresholdParam) * this.incrementPerVehicleParam;
            adaptiveGreenDuration += extension;
        }

        adaptiveGreenDuration = Math.min(adaptiveGreenDuration, this.maxGreenTimeParam);
        adaptiveGreenDuration = Math.max(adaptiveGreenDuration, this.minGreenTimeParam);

        return adaptiveGreenDuration;
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