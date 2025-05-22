package org.aiacon.simuladordemobilidadeurbana.simulation;

/**
 * Configurações da simulação, como taxas de geração de veículos,
 * durações de semáforos e modos de operação.
 */
public class Configuration {
    private double vehicleGenerationRate; // Veículos por segundo
    private double simulationDuration;    // Duração total da simulação em segundos
    private int trafficLightMode;         // 1: Fixo, 2: Adaptativo por Fila, 3: Economia
    private int redirectThreshold;        // Número de veículos na fila para considerar redirecionamento
    private boolean peakHour;             // Indica se é horário de pico

    // Novos tempos para FixedTimeStrategy (exemplos)
    private double fixedGreenTime = 15.0;
    private double fixedYellowTime = 3.0;

    // Novos tempos para AdaptiveQueueStrategy (exemplos)
    private double adaptiveBaseGreen = 10.0;
    private double adaptiveYellowTime = 3.0;
    private double adaptiveMaxGreen = 25.0;
    private double adaptiveIncrement = 2.0; // Incremento por veículo acima do threshold
    private int adaptiveQueueThreshold = 2; // Threshold de fila para começar a adaptar

    // Novos tempos para EnergySavingStrategy (exemplos)
    private double energySavingBaseGreen = 12.0;
    private double energySavingYellowTime = 3.0;
    private double energySavingMinGreen = 5.0;
    private int energySavingThreshold = 1;  // Se <= 1 veículo, usa minGreen fora do pico

    // NOVO ATRIBUTO: Tempo limite para geração de veículos
    private double vehicleGenerationStopTime; // Em segundos. Após este tempo, não gera mais veículos.

    /**
     * Construtor padrão para configurações.
     * Inicializa com valores default.
     */
    public Configuration() {
        this.vehicleGenerationRate = 0.5; // Default: 1 veículo a cada 2 segundos
        this.simulationDuration = 3600.0; // Default: 1 hora
        this.trafficLightMode = 2;        // Default: Adaptativo por Fila
        this.redirectThreshold = 10;      // Default: Redirecionar se fila > 10
        this.peakHour = false;            // Default: Não é horário de pico
        this.vehicleGenerationStopTime = 100.0; // Default: Gera veículos apenas nos primeiros 100 segundos
    }

    // Getters e Setters existentes...
    public double getVehicleGenerationRate() { return vehicleGenerationRate; }
    public void setVehicleGenerationRate(double rate) { this.vehicleGenerationRate = rate; }

    public double getSimulationDuration() { return simulationDuration; }
    public void setSimulationDuration(double duration) { this.simulationDuration = duration; }

    public int getTrafficLightMode() { return trafficLightMode; }
    public void setTrafficLightMode(int mode) { this.trafficLightMode = mode; }

    public int getRedirectThreshold() { return redirectThreshold; }
    public void setRedirectThreshold(int threshold) { this.redirectThreshold = threshold; }

    public boolean isPeakHour() { return peakHour; }
    public void setPeakHour(boolean peakHour) { this.peakHour = peakHour; }

    // Getters e Setters para os tempos dos semáforos
    public double getFixedGreenTime() { return fixedGreenTime; }
    public void setFixedGreenTime(double fixedGreenTime) { this.fixedGreenTime = fixedGreenTime; }
    public double getFixedYellowTime() { return fixedYellowTime; }
    public void setFixedYellowTime(double fixedYellowTime) { this.fixedYellowTime = fixedYellowTime; }

    public double getAdaptiveBaseGreen() { return adaptiveBaseGreen; }
    public void setAdaptiveBaseGreen(double adaptiveBaseGreen) { this.adaptiveBaseGreen = adaptiveBaseGreen; }
    public double getAdaptiveYellowTime() { return adaptiveYellowTime; }
    public void setAdaptiveYellowTime(double adaptiveYellowTime) { this.adaptiveYellowTime = adaptiveYellowTime; }
    public double getAdaptiveMaxGreen() { return adaptiveMaxGreen; }
    public void setAdaptiveMaxGreen(double adaptiveMaxGreen) { this.adaptiveMaxGreen = adaptiveMaxGreen; }
    public double getAdaptiveIncrement() { return adaptiveIncrement; }
    public void setAdaptiveIncrement(double adaptiveIncrement) { this.adaptiveIncrement = adaptiveIncrement; }
    public int getAdaptiveQueueThreshold() { return adaptiveQueueThreshold; }
    public void setAdaptiveQueueThreshold(int adaptiveQueueThreshold) { this.adaptiveQueueThreshold = adaptiveQueueThreshold; }


    public double getEnergySavingBaseGreen() { return energySavingBaseGreen; }
    public void setEnergySavingBaseGreen(double energySavingBaseGreen) { this.energySavingBaseGreen = energySavingBaseGreen; }
    public double getEnergySavingYellowTime() { return energySavingYellowTime; }
    public void setEnergySavingYellowTime(double energySavingYellowTime) { this.energySavingYellowTime = energySavingYellowTime; }
    public double getEnergySavingMinGreen() { return energySavingMinGreen; }
    public void setEnergySavingMinGreen(double energySavingMinGreen) { this.energySavingMinGreen = energySavingMinGreen; }
    public int getEnergySavingThreshold() { return energySavingThreshold; }
    public void setEnergySavingThreshold(int energySavingThreshold) { this.energySavingThreshold = energySavingThreshold; }


    /**
     * Retorna o tempo (em segundos de simulação) após o qual nenhum novo veículo será gerado.
     * @return O tempo limite para geração de veículos.
     */
    public double getVehicleGenerationStopTime() {
        return vehicleGenerationStopTime;
    }

    /**
     * Define o tempo (em segundos de simulação) após o qual nenhum novo veículo será gerado.
     * Se o tempo atual da simulação exceder este valor, a geração de veículos para.
     * Para gerar veículos durante toda a simulação, defina este valor igual ou maior que {@code simulationDuration}.
     * @param vehicleGenerationStopTime O tempo limite para geração de veículos.
     */
    public void setVehicleGenerationStopTime(double vehicleGenerationStopTime) {
        this.vehicleGenerationStopTime = vehicleGenerationStopTime;
    }
}