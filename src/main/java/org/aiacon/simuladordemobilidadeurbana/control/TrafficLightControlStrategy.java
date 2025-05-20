package org.aiacon.simuladordemobilidadeurbana.control;

import org.aiacon.simuladordemobilidadeurbana.model.TrafficLight; // Supondo que TrafficLight ainda exista
import org.aiacon.simuladordemobilidadeurbana.simulation.NextPhaseDecision;

public interface TrafficLightControlStrategy {
    /**
     * Decide a próxima fase do semáforo e sua duração.
     * Este método seria chamado pelo TrafficLight quando seu timer de fase atual expira.
     *
     * @param light O semáforo sendo controlado.
     * @param deltaTime O passo de tempo da simulação (pode não ser usado por todas as estratégias).
     * @param queueSizes Um array ou Map contendo o tamanho das filas para as direções relevantes do cruzamento.
     * Por exemplo, [norte, leste, sul, oeste] ou um Map com chaves de direção.
     * @param isPeakHour Indica se é horário de pico.
     * @return Um objeto contendo a próxima fase e a duração calculada para essa fase.
     */
    NextPhaseDecision decideNextPhase(TrafficLight light, double deltaTime, int[] queueSizes, boolean isPeakHour);

    /**
     * Inicializa a estratégia para um semáforo específico, se necessário.
     * Pode ser usado para definir a primeira fase e o timer inicial.
     * @param light O semáforo.
     */
    void initialize(TrafficLight light);

    // Poderia haver outros métodos, como para obter o estado atual de uma via específica
    String getLightStateForApproach(TrafficLight light, String approachDirection);
}