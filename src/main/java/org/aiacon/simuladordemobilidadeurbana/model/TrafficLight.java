package org.aiacon.simuladordemobilidadeurbana.model;

import org.aiacon.simuladordemobilidadeurbana.control.AdaptiveQueueStrategy;
import org.aiacon.simuladordemobilidadeurbana.control.EnergySavingStrategy;
import org.aiacon.simuladordemobilidadeurbana.control.FixedTimeStrategy;
import org.aiacon.simuladordemobilidadeurbana.control.TrafficLightControlStrategy;
import org.aiacon.simuladordemobilidadeurbana.simulation.NextPhaseDecision;


import java.util.HashMap; // Permitido
import java.util.Map;    // Permitido

/**
 * Representa um semáforo em uma interseção (nó) do grafo urbano.
 * O semáforo opera com base em uma máquina de estados finitos (FSM), onde cada estado
 * representa uma configuração de luzes para as vias do cruzamento (fases).
 * O controle da transição entre fases e a duração de cada fase são delegados
 * a uma {@link TrafficLightControlStrategy}.
 */
public class TrafficLight {
    private String nodeId; // ID do nó do grafo ao qual este semáforo está associado
    private int mode; // Modo de operação da estratégia de controle (1: fixo, 2: adaptativo por fila, 3: economia)
    private String initialJsonDirection; // Direção principal ou de referência lida do JSON (ex: "north", "forward")

    // Tempos base que as estratégias podem usar como default ou ponto de partida para seus cálculos
    private int baseGreenTime;
    private int baseYellowTime;

    private LightPhase currentPhase; // A fase atual da máquina de estados do semáforo
    private double phaseTimer;     // Tempo restante (em segundos) na fase atual

    // Array de filas (sua implementação customizada) para veículos aguardando em cada aproximação do cruzamento.
    private Queue[] directionQueues;
    private Map<String, Integer> directionNameToIndexMap; // Mapeia "north"->0, "east"->1, etc.

    private TrafficLightControlStrategy controlStrategy; // A estratégia de controle atual do semáforo
    private boolean peakHourStatus = false; // Indica se a simulação está em horário de pico

    /**
     * Construtor para um semáforo.
     *
     * @param nodeId O ID do nó do grafo onde este semáforo está localizado.
     * @param jsonOriginalDirection A string de direção lida do atributo "traffic_signals:direction" do JSON.
     * Usada para ajudar a estratégia a inferir a orientação principal do semáforo.
     * @param mode O modo de operação que determina qual {@link TrafficLightControlStrategy} será usada.
     * 1 para {@link FixedTimeStrategy},
     * 2 para {@link AdaptiveQueueStrategy},
     * 3 para {@link EnergySavingStrategy}.
     */
    public TrafficLight(String nodeId, String jsonOriginalDirection, int mode) {
        this.nodeId = nodeId;
        this.initialJsonDirection = jsonOriginalDirection != null ? jsonOriginalDirection.toLowerCase() : "unknown";
        this.mode = mode;

        this.baseGreenTime = 15;
        this.baseYellowTime = 3;

        this.directionQueues = new Queue[4];
        for (int i = 0; i < 4; i++) {
            this.directionQueues[i] = new Queue(); // Sua classe Queue
        }

        this.directionNameToIndexMap = new HashMap<>();
        directionNameToIndexMap.put("north", 0);
        directionNameToIndexMap.put("east", 1);
        directionNameToIndexMap.put("south", 2);
        directionNameToIndexMap.put("west", 3);

        switch (this.mode) {
            case 1:
                this.controlStrategy = new FixedTimeStrategy((double)this.baseGreenTime, (double)this.baseYellowTime);
                break;
            case 2:
                this.controlStrategy = new AdaptiveQueueStrategy((double)this.baseGreenTime, (double)this.baseYellowTime, 25.0, 3, 5.0);
                break;
            case 3:
                this.controlStrategy = new EnergySavingStrategy((double)this.baseGreenTime, (double)this.baseYellowTime, 5.0, 1);
                break;
            default:
                System.err.println("TRAFFIC_LIGHT_INIT: Modo de semáforo inválido (" + mode + ") para nó " + nodeId + ". Usando FixedTime por padrão.");
                this.controlStrategy = new FixedTimeStrategy((double)this.baseGreenTime, (double)this.baseYellowTime);
                break;
        }

        if (this.controlStrategy != null) {
            this.controlStrategy.initialize(this);
        } else {
            System.err.println("TRAFFIC_LIGHT_INIT_FATAL: controlStrategy não foi instanciada para o nó " + nodeId);
            // Lidar com este erro grave, talvez lançando uma exceção ou definindo um comportamento padrão seguro.
            setCurrentPhase(LightPhase.NS_GREEN_EW_RED, this.baseGreenTime); // Fallback extremo
        }


        if (this.currentPhase == null) {
            System.err.println("TRAFFIC_LIGHT_INIT_WARN: Estratégia não definiu fase inicial para o nó " + nodeId + ". Definindo NS_GREEN_EW_RED como padrão.");
            setCurrentPhase(LightPhase.NS_GREEN_EW_RED, this.baseGreenTime);
        }
        // logPhaseChange(); // A estratégia deve chamar logPhaseChange através de setCurrentPhase se desejar.
    }

    // --- Getters ---

    /** @return O ID do nó associado a este semáforo. */
    public String getNodeId() { return nodeId; }

    /** @return A fase atual do semáforo (ex: NS_GREEN_EW_RED). */
    public LightPhase getCurrentPhase() { return currentPhase; }

    /** @return A string de direção original lida do JSON para este semáforo. */
    public String getInitialJsonDirection() { return initialJsonDirection; }

    /** @return Verdadeiro se a simulação estiver atualmente em horário de pico, falso caso contrário. */
    public boolean isPeakHourEnabled() { return peakHourStatus; }

    // --- Setters e Métodos para a Estratégia de Controle ---

    /**
     * Define a fase atual do semáforo e a duração dessa fase.
     * Este método é tipicamente chamado pela {@link TrafficLightControlStrategy} durante a inicialização ou transição de fase.
     * @param phase A nova {@link LightPhase} para o semáforo.
     * @param duration A duração (em segundos) da nova fase.
     */
    public void setCurrentPhase(LightPhase phase, double duration) {
        this.currentPhase = phase;
        this.phaseTimer = duration;
    }

    /**
     * Retorna o índice do array de filas (`directionQueues`) correspondente
     * a um nome de direção (ex: "north" retorna 0).
     * Usado pelas estratégias para acessar as filas corretas.
     * @param directionName O nome da direção ("north", "east", "south", "west").
     * @return O índice correspondente, ou null se a direção não for mapeada.
     */
    public Integer getDirectionIndex(String directionName) {
        if (directionName == null) return null;
        return directionNameToIndexMap.get(directionName.toLowerCase());
    }

    // --- Métodos de Gerenciamento de Fila ---

    /**
     * Retorna um array com os tamanhos das filas para cada uma das 4 direções principais (N, E, S, O).
     * A ordem no array é consistente com o mapeamento em `directionNameToIndexMap` (0:N, 1:E, 2:S, 3:W).
     * @return Array de inteiros representando os tamanhos das filas.
     */
    public int[] getAllQueueSizes() {
        int[] sizes = new int[4];
        for (int i = 0; i < 4; i++) {
            sizes[i] = (directionQueues[i] != null) ? directionQueues[i].size() : 0;
        }
        return sizes;
    }

    /**
     * Adiciona um veículo à fila da direção especificada pelo nome.
     * @param directionName Nome da direção (ex: "north", "east", "south", "west").
     * @param vehicle O veículo a ser adicionado.
     */
    public void addVehicleToQueue(String directionName, Vehicle vehicle) {
        Integer index = getDirectionIndex(directionName);
        if (index != null && index >= 0 && index < directionQueues.length) {
            if (directionQueues[index] == null) { // Segurança, não deveria acontecer se o construtor inicializou todas
                directionQueues[index] = new Queue();
            }
            directionQueues[index].enqueue(vehicle);
        } else {
            System.err.println("TrafficLight " + nodeId + ": Não foi possível encontrar índice para direção '" + directionName + "' ao tentar enfileirar veículo " + vehicle.getId());
        }
    }

    /**
     * Remove e retorna o primeiro veículo da fila da direção especificada pelo nome.
     * @param directionName Nome da direção (ex: "north").
     * @return O veículo removido, ou null se a fila estiver vazia ou a direção for inválida.
     */
    public Vehicle popVehicleFromQueue(String directionName) {
        Integer index = getDirectionIndex(directionName);
        if (index != null && index >= 0 && index < directionQueues.length &&
                directionQueues[index] != null && !directionQueues[index].isEmpty()) {
            return directionQueues[index].dequeue();
        }
        return null;
    }

    // --- Lógica Principal de Update ---

    /**
     * Atualiza o estado do semáforo com base no tempo decorrido e na estratégia de controle.
     * Decrementa o timer da fase atual. Se o timer expirar, consulta a
     * {@link TrafficLightControlStrategy} para decidir a próxima fase e sua duração.
     * @param deltaTime O tempo (em segundos) que passou desde a última atualização.
     * @param isPeakHour Verdadeiro se a simulação estiver em horário de pico, falso caso contrário.
     */
    public void update(double deltaTime, boolean isPeakHour) {
        this.peakHourStatus = isPeakHour;
        this.phaseTimer -= deltaTime;

        if (this.phaseTimer <= 0) {
            if (this.controlStrategy == null) {
                System.err.println("TrafficLight " + nodeId + ": ERRO FATAL - controlStrategy é nula no método update.");
                setCurrentPhase(LightPhase.NS_GREEN_EW_RED, 60.0); // Estado de emergência seguro
                logPhaseChange(); // Log da fase de emergência
                return;
            }

            NextPhaseDecision decision = controlStrategy.decideNextPhase(this, deltaTime, getAllQueueSizes(), this.peakHourStatus);

            if (decision != null && decision.nextPhase != null) {
                setCurrentPhase(decision.nextPhase, decision.duration);
                logPhaseChange();
            } else {
                System.err.println("TrafficLight " + nodeId + ": Estratégia retornou decisão/fase nula. Mantendo fase atual ("+this.currentPhase+") e resetando timer para um valor seguro.");
                this.phaseTimer = this.baseGreenTime > 0 ? this.baseGreenTime : 15.0;
                if (this.currentPhase == null) {
                    setCurrentPhase(LightPhase.NS_GREEN_EW_RED, this.phaseTimer);
                    logPhaseChange(); // Log da fase de fallback
                }
            }
        }
    }

    /**
     * Usado pelo {@link org.aiacon.simuladordemobilidadeurbana.simulation.Simulator} para verificar o estado da luz
     * para uma determinada direção de aproximação ao cruzamento.
     * A 'approachDirection' deve ser uma das direções cardeais ("north", "south", "east", "west")
     * relativa à configuração do cruzamento, determinada pelo Simulator.
     * @param approachDirection A direção de aproximação do veículo (ex: "north").
     * @return O estado da luz como uma String ("green", "yellow", "red").
     */
    public String getLightStateForApproach(String approachDirection) {
        if (controlStrategy == null) {
            System.err.println("TrafficLight " + nodeId + ": Estratégia de controle não inicializada ao chamar getLightStateForApproach.");
            return "red"; // Estado seguro padrão
        }
        return controlStrategy.getLightStateForApproach(this, approachDirection);
    }

    /**
     * Registra a mudança de fase do semáforo no console.
     */
    private void logPhaseChange() {
        // Adiciona uma verificação para currentPhase para evitar NullPointerException se a inicialização falhar completamente
        String phaseStr = (this.currentPhase != null) ? this.currentPhase.toString() : "INDEFINIDA";
        System.out.println("Semáforo " + nodeId + ": Nova FASE -> " + phaseStr +
                ". Duração programada: " + String.format("%.1f", this.phaseTimer) + "s.");
    }

    /**
     * Registra o estado interno atual do semáforo (fase e tempo restante) no console.
     * Pode ser útil para debugging.
     */
    public void logCurrentInternalState() {
        String phaseStr = (this.currentPhase != null) ? this.currentPhase.toString() : "INDEFINIDA";
        System.out.printf("Semáforo no nó %s -> Fase: %s, Timer Restante: %.1f%n",
                nodeId, phaseStr, phaseTimer);
    }

    /**
     * Retorna o número total de veículos esperando em todas as filas deste semáforo.
     * Assumindo que directionQueues é um array de 4 filas (N, E, S, O).
     * @return O número total de veículos enfileirados.
     */
    public synchronized int getTotalVehiclesInQueues() {
        int total = 0;
        if (directionQueues != null) { // Verifica se o array de filas não é nulo
            for (int i = 0; i < directionQueues.length; i++) { // Itera pelo comprimento real do array
                if (directionQueues[i] != null) {
                    total += directionQueues[i].size();
                }
            }
        }
        return total;
    }
}