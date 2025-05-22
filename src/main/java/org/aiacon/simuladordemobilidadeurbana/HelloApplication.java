package org.aiacon.simuladordemobilidadeurbana;

import org.aiacon.simuladordemobilidadeurbana.model.Graph;
import org.aiacon.simuladordemobilidadeurbana.simulation.Configuration;
import org.aiacon.simuladordemobilidadeurbana.simulation.Simulator;
import org.aiacon.simuladordemobilidadeurbana.visualization.Visualizer;
import org.aiacon.simuladordemobilidadeurbana.io.JsonParser;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.io.InputStream;
import java.net.URL; // Para obter o caminho do recurso de forma mais robusta

/**
 * Classe principal da aplicação JavaFX para o Simulador de Mobilidade Urbana.
 * Responsável por inicializar o grafo, as configurações da simulação,
 * o simulador e a interface gráfica do visualizador.
 */
public class HelloApplication extends Application {

    private Simulator simulator;
    private Thread simulationThread;

    /**
     * Ponto de entrada principal para a aplicação JavaFX.
     * Este método é chamado após a inicialização do sistema JavaFX.
     *
     * @param primaryStage O palco principal para esta aplicação, sobre o qual
     * a cena da aplicação pode ser definida.
     */
    @Override
    public void start(Stage primaryStage) {
        System.out.println("HELLO_APPLICATION_START: Iniciando a aplicação...");
        Graph graph; // Declarado aqui para estar no escopo do try-catch e após

        try {
            // Carregamento do arquivo JSON do mapa
            String resourcePath = "/mapa/CentroTeresinaPiauiBrazil.json";
            InputStream jsonInputStream = getClass().getResourceAsStream(resourcePath);
            if (jsonInputStream == null) {
                String errorMessage = "Erro Crítico: Não foi possível localizar o arquivo JSON do mapa: " + resourcePath;
                System.err.println(errorMessage);
                mostrarErroFatal(primaryStage, errorMessage);
                return;
            }
            graph = JsonParser.loadGraphFromStream(jsonInputStream); // Método estático parse
            System.out.println("HELLO_APPLICATION_START: Grafo carregado com sucesso.");

        } catch (Exception e) {
            String errorMessage = "Erro Crítico ao carregar o grafo do JSON: " + e.getMessage();
            System.err.println(errorMessage);
            e.printStackTrace();
            mostrarErroFatal(primaryStage, errorMessage);
            return;
        }

        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            String errorMessage = "Erro Crítico: Falha ao carregar o grafo ou o grafo está vazio.";
            System.err.println(errorMessage);
            mostrarErroFatal(primaryStage, errorMessage);
            return;
        }

        // Configurar a simulação
        Configuration config = new Configuration();
        config.setTrafficLightMode(3);           // 1:Fixo, 2:AdaptativoFila, 3:EconomiaEnergia
        config.setVehicleGenerationRate(0.2);    // Veículos por segundo
        config.setRedirectThreshold(10);         // Limiar para redirecionamento
        config.setPeakHour(false);               // Simular horário de pico ou não

        // --- Definição da Duração da Simulação e Parada da Geração de Veículos ---
        double totalSimulationTime = 600.0; // Duração total da simulação em segundos (ex: 5 minutos)
        double stopGeneratingVehiclesAfter = 300.0; // Para de gerar veículos após 100s

        config.setSimulationDuration(totalSimulationTime);
        config.setVehicleGenerationStopTime(stopGeneratingVehiclesAfter);
        // -----------------------------------------------------------------------

        System.out.println("HELLO_APPLICATION_START: Configuração da simulação carregada. Modo Semáforo: " + config.getTrafficLightMode() +
                ", Duração Total: " + totalSimulationTime + "s, Parar Geração em: " + stopGeneratingVehiclesAfter + "s");

        this.simulator = new Simulator(graph, config);
        Visualizer visualizer = new Visualizer(graph, this.simulator);

        try {
            visualizer.start(primaryStage);
            System.out.println("HELLO_APPLICATION_START: Visualizador iniciado.");
        } catch (Exception e) {
            String errorMessage = "Erro Crítico ao iniciar o Visualizer: " + e.getMessage();
            System.err.println(errorMessage);
            e.printStackTrace();
            mostrarErroFatal(primaryStage, errorMessage);
            return;
        }

        simulationThread = new Thread(this.simulator);
        simulationThread.setName("SimulationLoopThread");
        simulationThread.setDaemon(true);
        simulationThread.start();
        System.out.println("HELLO_APPLICATION_START: Thread da simulação iniciada.");
    }

    /**
     * Exibe uma janela de erro fatal e encerra a aplicação.
     * @param stage O palco principal para exibir a cena de erro.
     * @param mensagem A mensagem de erro a ser exibida.
     */
    private void mostrarErroFatal(Stage stage, String mensagem) {
        Pane errorPane = new Pane(new Text(20, 50, mensagem)); // Adiciona margens para o texto
        Scene errorScene = new Scene(errorPane, Math.max(400, mensagem.length() * 7), 100); // Ajusta largura da cena
        stage.setTitle("Erro na Aplicação");
        stage.setScene(errorScene);
        stage.show();
        // Adicionado Platform.exit() para garantir que a aplicação feche após um erro fatal na inicialização
        Platform.exit();
    }

    /**
     * Este método é chamado quando a aplicação JavaFX está sendo encerrada.
     * É usado para parar a thread da simulação de forma graciosa.
     */
    @Override
    public void stop() {
        System.out.println("HELLO_APPLICATION_STOP: Método stop() chamado, encerrando a aplicação...");

        if (simulator != null) {
            System.out.println("HELLO_APPLICATION_STOP: Chamando stopSimulation() do simulador.");
            simulator.stopSimulation();
        }

        if (simulationThread != null && simulationThread.isAlive()) {
            System.out.println("HELLO_APPLICATION_STOP: Tentando interromper a thread da simulação...");
            simulationThread.interrupt();
            try {
                simulationThread.join(1000); // Espera até 1 segundo
                if (simulationThread.isAlive()) {
                    System.err.println("HELLO_APPLICATION_STOP: Thread da simulação ainda está ativa após join().");
                } else {
                    System.out.println("HELLO_APPLICATION_STOP: Thread da simulação terminada.");
                }
            } catch (InterruptedException e) {
                System.err.println("HELLO_APPLICATION_STOP: Thread principal interrompida enquanto esperava pela thread da simulação.");
                Thread.currentThread().interrupt();
            }
        } else {
            System.out.println("HELLO_APPLICATION_STOP: Thread da simulação não estava ativa ou era nula.");
        }

        System.out.println("HELLO_APPLICATION_STOP: Processo de encerramento da aplicação JavaFX concluído.");
        // Platform.exit() e System.exit(0) geralmente não são necessários aqui,
        // pois o JavaFX gerencia o encerramento da JVM quando a última janela é fechada,
        // especialmente se as threads daemon foram configuradas corretamente.
    }

    /**
     * Ponto de entrada principal do programa Java.
     * @param args Argumentos de linha de comando (não utilizados).
     */
    public static void main(String[] args) {
        launch(args);
    }
}