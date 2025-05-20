package org.aiacon.simuladordemobilidadeurbana;

import org.aiacon.simuladordemobilidadeurbana.model.Graph;
import org.aiacon.simuladordemobilidadeurbana.simulation.Configuration;
import org.aiacon.simuladordemobilidadeurbana.simulation.Simulator;
import org.aiacon.simuladordemobilidadeurbana.visualization.Visualizer;
import org.aiacon.simuladordemobilidadeurbana.io.JsonParser;
import javafx.application.Application;
import javafx.stage.Stage;
import javafx.application.Platform; // Import para Platform.exit()
import javafx.scene.Scene;        // Para cenas de erro, se necessário
import javafx.scene.layout.Pane;   // Para cenas de erro
import javafx.scene.text.Text;    // Para cenas de erro

import java.io.InputStream;

public class HelloApplication extends Application {

    private Simulator simulator; // Tornar o simulador uma variável de instância
    private Thread simulationThread; // Para controlar a thread da simulação

    @Override
    public void start(Stage primaryStage) { // Removido 'throws Exception' desnecessário se tratado
        System.out.println("HELLO_APPLICATION_START: Iniciando a aplicação...");
        Graph graph = null;
        try {
            InputStream jsonInputStream = getClass().getResourceAsStream("/mapa/CentroTeresinaPiauiBrazil.json");
            if (jsonInputStream == null) {
                System.err.println("Erro Crítico: Não foi possível localizar o arquivo JSON do mapa: /mapa/CentroTeresinaPiauiBrazil.json");
                mostrarErroFatal(primaryStage, "Erro Crítico: Arquivo de mapa não encontrado.");
                return;
            }
            graph = JsonParser.loadGraphFromStream(jsonInputStream);
        } catch (Exception e) {
            System.err.println("Erro Crítico ao carregar o grafo do JSON: " + e.getMessage());
            e.printStackTrace();
            mostrarErroFatal(primaryStage, "Erro Crítico ao carregar o mapa: " + e.getMessage());
            return;
        }


        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            System.err.println("Erro Crítico: Falha ao carregar o grafo ou grafo está vazio.");
            mostrarErroFatal(primaryStage, "Erro Crítico: Falha ao carregar dados do mapa.");
            return;
        }
        System.out.println("HELLO_APPLICATION_START: Grafo carregado com sucesso.");

        // Configurar a simulação
        Configuration config = new Configuration();
        config.setTrafficLightMode(2); // Ex: 1 para FixedTime, 2 para AdaptiveQueue, 3 para EnergySaving
        config.setVehicleGenerationRate(0.5); // Ex: 0.5 veículos por segundo (1 a cada 2 segundos)
        config.setSimulationDuration(3600); // Ex: 2 horas de simulação
        config.setRedirectThreshold(5);     // Ex: Limiar para redirecionamento

        System.out.println("HELLO_APPLICATION_START: Configuração da simulação carregada. Modo Semáforo: " + config.getTrafficLightMode());

        // Criar o simulador e visualizador
        // 'simulator' agora é uma variável de instância
        this.simulator = new Simulator(graph, config);
        Visualizer visualizer = new Visualizer(graph, this.simulator); // Passa a instância do simulador

        // Configurar o visualizador no palco principal do JavaFX
        try {
            visualizer.start(primaryStage); // O método start do Visualizer configurará a cena
            System.out.println("HELLO_APPLICATION_START: Visualizador iniciado.");
        } catch (Exception e) {
            System.err.println("Erro Crítico ao iniciar o Visualizer: " + e.getMessage());
            e.printStackTrace();
            mostrarErroFatal(primaryStage, "Erro Crítico ao iniciar a visualização: " + e.getMessage());
            return;
        }


        // Iniciar a simulação em uma nova thread
        simulationThread = new Thread(this.simulator); // Simulator agora é Runnable
        simulationThread.setName("SimulationThread");
        simulationThread.setDaemon(true); // Importante para que a JVM feche quando a UI fechar
        simulationThread.start();
        System.out.println("HELLO_APPLICATION_START: Thread da simulação iniciada.");
    }

    private void mostrarErroFatal(Stage stage, String mensagem) {
        Pane errorPane = new Pane(new Text(mensagem));
        Scene errorScene = new Scene(errorPane, 400, 100);
        stage.setTitle("Erro na Aplicação");
        stage.setScene(errorScene);
        stage.show();
        // Considerar Platform.exit() aqui também se o erro impedir a continuação
    }

    @Override
    public void stop() throws Exception {
        System.out.println("HELLO_APPLICATION_STOP: Método stop() chamado, encerrando a aplicação...");

        //running = false; // Sinaliza para a thread de atualização do Visualizer parar (se o Visualizer tiver essa flag)

        if (simulator != null) {
            System.out.println("HELLO_APPLICATION_STOP: Chamando stopSimulation() do simulador.");
            simulator.stopSimulation(); // Sinaliza para a thread do simulador parar seu loop
        }

        if (simulationThread != null && simulationThread.isAlive()) {
            System.out.println("HELLO_APPLICATION_STOP: Tentando interromper a thread da simulação...");
            simulationThread.interrupt(); // Interrompe a thread se ela estiver em sleep/wait
            try {
                simulationThread.join(1000); // Espera até 1 segundo pela thread terminar
                if (simulationThread.isAlive()) {
                    System.err.println("HELLO_APPLICATION_STOP: Thread da simulação ainda está ativa após join().");
                } else {
                    System.out.println("HELLO_APPLICATION_STOP: Thread da simulação terminada.");
                }
            } catch (InterruptedException e) {
                System.err.println("HELLO_APPLICATION_STOP: Thread principal interrompida enquanto esperava pela thread da simulação.");
                Thread.currentThread().interrupt(); // Restaura o status de interrupção
            }
        } else {
            System.out.println("HELLO_APPLICATION_STOP: Thread da simulação não estava ativa ou era nula.");
        }

        System.out.println("HELLO_APPLICATION_STOP: Chamando super.stop().");
        super.stop(); // Chama o método stop da classe pai (Application)
        System.out.println("HELLO_APPLICATION_STOP: Processo de encerramento da aplicação JavaFX (super.stop) concluído.");
        // Platform.exit(); // Garante que a aplicação JavaFX termine.
        // System.exit(0); // Força o encerramento da JVM se necessário.
    }

    public static void main(String[] args) {
        launch(args); // Iniciar a aplicação JavaFX
    }
}