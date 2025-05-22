package org.aiacon.simuladordemobilidadeurbana.control;

import org.aiacon.simuladordemobilidadeurbana.model.LightPhase;
import org.aiacon.simuladordemobilidadeurbana.model.TrafficLight;
import org.aiacon.simuladordemobilidadeurbana.simulation.Configuration;
import org.aiacon.simuladordemobilidadeurbana.simulation.NextPhaseDecision;

public class FixedTimeStrategy implements TrafficLightControlStrategy {

    private double strategyGreenDuration;
    private double strategyYellowDuration;

    public FixedTimeStrategy(double greenTime, double yellowTime) {
        this.strategyGreenDuration = greenTime;
        this.strategyYellowDuration = yellowTime;
    }

    public FixedTimeStrategy() {
        this.strategyGreenDuration = 15.0; // Default, será sobrescrito se config for usada
        this.strategyYellowDuration = 3.0;  // Default, será sobrescrito se config for usada
    }

    @Override
    public void initialize(TrafficLight light) {
        Configuration config = light.getConfiguration();
        String initialJsonDir = light.getInitialJsonDirection().toLowerCase();
        LightPhase startPhase = LightPhase.NS_GREEN_EW_RED;

        double initialDuration = this.strategyGreenDuration; // Usa o tempo base da configuração

        // Para tornar "pior" no pico, não aplicamos um bônus de tempo verde longo e otimizado.
        // Ela simplesmente usará o strategyGreenDuration (vindo da config.getFixedGreenTime())
        // Se esse valor for baixo (ex: 10-12s), ela será ruim no pico.
        if (light.isPeakHourEnabled()) {
            // Poderíamos até reduzir, mas manter o mesmo já a torna menos eficiente
            // initialDuration = this.strategyGreenDuration * 0.75; // Exemplo de "piorar"
        }


        if (initialJsonDir.contains("east") || initialJsonDir.contains("west")) {
            startPhase = LightPhase.NS_RED_EW_GREEN;
        }
        light.setCurrentPhase(startPhase, initialDuration);
    }

    @Override
    public NextPhaseDecision decideNextPhase(TrafficLight light, double deltaTime, int[] queueSizes, boolean isPeakHour) {
        LightPhase currentPhase = light.getCurrentPhase();
        LightPhase nextPhase;
        double duration;
        Configuration config = light.getConfiguration();

        double activeGreenDuration;

        // Para torná-la "ruim" no horário de pico, vamos usar um tempo verde fixo que
        // não seja otimizado para alta demanda.
        // Poderíamos usar um valor curto fixo, ou o valor da config que pode ser ajustado para ser baixo.
        if (isPeakHour) {
            // Usar um tempo verde fixo e potencialmente subótimo para horário de pico
            // Se config.getFixedGreenTime() for, por exemplo, 10s ou 12s, será ruim para pico.
            activeGreenDuration = this.strategyGreenDuration;
            // Para forçar a ser ruim, você poderia colocar um valor baixo direto aqui:
            // activeGreenDuration = 10.0; // Exemplo: Verde curto e fixo no pico
        } else {
            activeGreenDuration = this.strategyGreenDuration;
        }

        double activeYellowDuration = this.strategyYellowDuration;

        switch (currentPhase) {
            case NS_GREEN_EW_RED:
                nextPhase = LightPhase.NS_YELLOW_EW_RED;
                duration = activeYellowDuration;
                break;
            case NS_YELLOW_EW_RED:
                nextPhase = LightPhase.NS_RED_EW_GREEN;
                duration = activeGreenDuration;
                break;
            case NS_RED_EW_GREEN:
                nextPhase = LightPhase.NS_RED_EW_YELLOW;
                duration = activeYellowDuration;
                break;
            case NS_RED_EW_YELLOW:
                nextPhase = LightPhase.NS_GREEN_EW_RED;
                duration = activeGreenDuration;
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