package org.aiacon.simuladordemobilidadeurbana.control;

import org.aiacon.simuladordemobilidadeurbana.model.LightPhase;
import org.aiacon.simuladordemobilidadeurbana.model.TrafficLight;
import org.aiacon.simuladordemobilidadeurbana.simulation.NextPhaseDecision;

public class AdaptiveQueueStrategy implements TrafficLightControlStrategy {
    // Atributos de configuração da estratégia
    private double baseGreenTimeConfig;
    private double yellowTimeConfig;
    private double maxGreenExtensionConfig;
    private int queueThresholdConfig;
    private double minGreenTimeConfig;

    // Construtor parametrizado (bom para flexibilidade)
    public AdaptiveQueueStrategy(double baseGreen, double yellow, double maxExtension, int threshold, double minGreen) {
        this.baseGreenTimeConfig = baseGreen;
        this.yellowTimeConfig = yellow;
        this.maxGreenExtensionConfig = maxExtension;
        this.queueThresholdConfig = threshold;
        this.minGreenTimeConfig = minGreen;
    }

    // Construtor padrão (bom para facilidade de uso com valores default)
    public AdaptiveQueueStrategy() {
        this(10.0, 3.0, 10.0, 3, 5.0); // Chama o construtor parametrizado
    }

    @Override
    public void initialize(TrafficLight light) {
        String initialJsonDir = light.getInitialJsonDirection().toLowerCase();
        LightPhase startPhase = LightPhase.NS_GREEN_EW_RED; // Padrão

        if (initialJsonDir.contains("east") || initialJsonDir.contains("west")) {
            startPhase = LightPhase.NS_RED_EW_GREEN;
        } else if (initialJsonDir.contains("north") || initialJsonDir.contains("south")) {
            startPhase = LightPhase.NS_GREEN_EW_RED;
        }
        // A lógica para "forward" e "backward" pode ser melhorada se necessário,

        // Calcula a duração inicial. Como as filas estão vazias, usará o tempo base ou mínimo.
        double initialDuration = light.isPeakHourEnabled() ? this.baseGreenTimeConfig + 5.0 : this.baseGreenTimeConfig;
        initialDuration = Math.max(initialDuration, this.minGreenTimeConfig);

        light.setCurrentPhase(startPhase, initialDuration);
    }

    @Override
    public NextPhaseDecision decideNextPhase(TrafficLight light, double deltaTime, int[] queueSizes, boolean isPeakHour) {
        LightPhase currentPhase = light.getCurrentPhase();
        LightPhase nextPhaseDetermined;
        double durationDetermined;

        // O parâmetro deltaTime está presente na assinatura da interface,
        // mas esta estratégia específica não o utiliza para determinar a *duração* da próxima fase.
        // Ele é usado pelo Simulator para avançar o tempo global.

        switch (currentPhase) {
            case NS_GREEN_EW_RED:
                nextPhaseDetermined = LightPhase.NS_YELLOW_EW_RED;
                durationDetermined = this.yellowTimeConfig;
                break;
            case NS_YELLOW_EW_RED:
                nextPhaseDetermined = LightPhase.NS_RED_EW_GREEN;
                durationDetermined = calculateAdaptiveGreenTime(light, queueSizes, true, isPeakHour); // true para Leste-Oeste
                break;
            case NS_RED_EW_GREEN:
                nextPhaseDetermined = LightPhase.NS_RED_EW_YELLOW;
                durationDetermined = this.yellowTimeConfig;
                break;
            case NS_RED_EW_YELLOW:
                nextPhaseDetermined = LightPhase.NS_GREEN_EW_RED;
                durationDetermined = calculateAdaptiveGreenTime(light, queueSizes, false, isPeakHour); // false para Norte-Sul (ou seja, é fase N-S)
                break;
            default:
                System.err.println("AdaptiveQueueStrategy: Fase atual desconhecida " + currentPhase + " para nó " + light.getNodeId() +". Resetando para NS_GREEN_EW_RED.");
                nextPhaseDetermined = LightPhase.NS_GREEN_EW_RED;
                durationDetermined = calculateAdaptiveGreenTime(light, queueSizes, false, isPeakHour); // Fallback para N-S verde
                break;
        }
        return new NextPhaseDecision(nextPhaseDetermined, durationDetermined);
    }

    private double calculateAdaptiveGreenTime(TrafficLight light, int[] queueSizes, boolean isEastWestGreenPhase, boolean isPeakHour) {
        double adaptiveGreenDuration = isPeakHour ? this.baseGreenTimeConfig + 5.0 : this.baseGreenTimeConfig;

        Integer relevantIndex1, relevantIndex2;

        if (isEastWestGreenPhase) { // Se a fase que está para ficar VERDE é Leste-Oeste
            relevantIndex1 = light.getDirectionIndex("east");
            relevantIndex2 = light.getDirectionIndex("west");
        } else { // Se a fase que está para ficar VERDE é Norte-Sul
            relevantIndex1 = light.getDirectionIndex("north");
            relevantIndex2 = light.getDirectionIndex("south");
        }

        // Tratamento seguro para índices, caso getDirectionIndex retorne null
        int relevantQueue1Size = (relevantIndex1 != null && relevantIndex1 >= 0 && relevantIndex1 < queueSizes.length) ? queueSizes[relevantIndex1] : 0;
        int relevantQueue2Size = (relevantIndex2 != null && relevantIndex2 >= 0 && relevantIndex2 < queueSizes.length) ? queueSizes[relevantIndex2] : 0;

        int maxRelevantQueue = Math.max(relevantQueue1Size, relevantQueue2Size);

        if (maxRelevantQueue == 0 && !isPeakHour) {
            // Se não há ninguém na fila e não é horário de pico, reduz o tempo de verde, mas não abaixo do mínimo.
            return Math.max(this.minGreenTimeConfig, adaptiveGreenDuration * 0.66); // Ex: 2/3 do tempo, ou o mínimo.
        }

        if (maxRelevantQueue > this.queueThresholdConfig) {
            // Calcula a extensão baseada nos veículos excedentes.
            double extension = Math.min(
                    (maxRelevantQueue - this.queueThresholdConfig) * 1.0, // 1 segundo por veículo extra (configurável)
                    this.maxGreenExtensionConfig // Limita a extensão total
            );
            adaptiveGreenDuration += extension;
        }

        // Garante que a duração final esteja dentro dos limites configurados.
        double peakBonusForMaxLimit = isPeakHour ? 5.0 : 0.0; // O bônus de pico já foi adicionado a adaptiveGreenDuration
        adaptiveGreenDuration = Math.min(adaptiveGreenDuration, this.baseGreenTimeConfig + peakBonusForMaxLimit + this.maxGreenExtensionConfig);
        adaptiveGreenDuration = Math.max(adaptiveGreenDuration, this.minGreenTimeConfig);

        return adaptiveGreenDuration;
    }

    @Override
    public String getLightStateForApproach(TrafficLight light, String approachDirection) {
        LightPhase currentPhase = light.getCurrentPhase();
        if (currentPhase == null || approachDirection == null) return "red"; // Segurança
        String dir = approachDirection.toLowerCase();

        // A lógica aqui é baseada na fase atual do cruzamento
        switch (currentPhase) {
            case NS_GREEN_EW_RED: // Norte-Sul está verde
                return (dir.equals("north") || dir.equals("south")) ? "green" : "red";
            case NS_YELLOW_EW_RED: // Norte-Sul está amarelo
                return (dir.equals("north") || dir.equals("south")) ? "yellow" : "red";
            case NS_RED_EW_GREEN: // Leste-Oeste está verde
                return (dir.equals("east") || dir.equals("west")) ? "green" : "red";
            case NS_RED_EW_YELLOW: // Leste-Oeste está amarelo
                return (dir.equals("east") || dir.equals("west")) ? "yellow" : "red";
            default:
                return "red"; // Fase desconhecida ou todas vermelhas
        }
    }
}