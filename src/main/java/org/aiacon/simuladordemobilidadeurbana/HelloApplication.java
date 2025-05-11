package org.aiacon.simuladordemobilidadeurbana;

import org.aiacon.simuladordemobilidadeurbana.model.Graph;
import org.aiacon.simuladordemobilidadeurbana.simulation.Configuration;
import org.aiacon.simuladordemobilidadeurbana.simulation.Simulator;
import org.aiacon.simuladordemobilidadeurbana.visualization.Visualizer;
import org.aiacon.simuladordemobilidadeurbana.io.JsonParser;
import javafx.application.Application;
import javafx.stage.Stage;

import java.io.InputStream;

public class HelloApplication extends Application {

    @Override
    public void start(Stage primaryStage) throws Exception {
        // Carregar o grafo diretamente dos recursos
        InputStream jsonInputStream = getClass().getResourceAsStream("/mapa/CentroTeresinaPiauiBrazil.json");
        if (jsonInputStream == null) {
            System.err.println("Erro: Não foi possível localizar o arquivo JSON em /mapa/CentroTeresinaPiauiBrazil.json");
            return;
        }

        // Passar o InputStream para o JsonParser
        Graph graph = JsonParser.loadGraphFromStream(jsonInputStream);

        if (graph == null) {
            System.err.println("Falha ao carregar o grafo.");
            return;
        }

        // Configurar a simulação
        Configuration config = new Configuration();
        config.setTrafficLightMode(2); // Usar o Modelo 2 de semáforos
        config.setVehicleGenerationRate(0.5); // Gerar veículos a cada 2 segundos, em média

        // Criar o simulador e visualizador
        Simulator simulator = new Simulator(graph, config);
        Visualizer visualizer = new Visualizer(graph, simulator);

        // Configurar o visualizador no palco principal do JavaFX
        visualizer.start(primaryStage);

        // Iniciar a simulação em uma nova thread
        new Thread(simulator::run).start();
    }

    public static void main(String[] args) {
        launch(args); // Iniciar a aplicação JavaFX
    }
}