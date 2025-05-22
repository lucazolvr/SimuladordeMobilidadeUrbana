package org.aiacon.simuladordemobilidadeurbana.simulation; // Ou .control

import org.aiacon.simuladordemobilidadeurbana.model.LightPhase; // Supondo que LightPhase está em TrafficLight

public class NextPhaseDecision {
    public final LightPhase nextPhase;
    public final double duration;

    public NextPhaseDecision(LightPhase nextPhase, double duration) {
        this.nextPhase = nextPhase;
        this.duration = duration;
    }
}