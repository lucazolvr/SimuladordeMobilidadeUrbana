package org.aiacon.simuladordemobilidadeurbana.visualization;

import org.aiacon.simuladordemobilidadeurbana.model.*;
import org.aiacon.simuladordemobilidadeurbana.simulation.Simulator;
import javafx.application.Application;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;

/**
 * Classe responsável por visualizar o grafo e a simulação usando JavaFX
 */
public class Visualizer extends Application {
    private Graph graph;
    private Simulator simulator;
    private Map<String, Circle> nodeCircles; // Map para armazenar os nós visualizados

    /**
     * Construtor da classe Visualizer.
     *
     * @param graph      O grafo carregado.
     * @param simulator  O simulador que opera no grafo.
     */
    public Visualizer(Graph graph, Simulator simulator) {
        this.graph = graph;
        this.simulator = simulator;
        this.nodeCircles = new HashMap<>();
    }

    /**
     * Inicializa a interface gráfica para a simulação.
     *
     * @param primaryStage O estágio principal do JavaFX.
     */
    @Override
    public void start(Stage primaryStage) {
        Pane pane = new Pane();
        drawGraph(pane); // Desenhando o grafo no painel

        // Configurar a cena (interface gráfica)
        Scene scene = new Scene(pane, 800, 600);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Simulador de Mobilidade Urbana");
        primaryStage.show();

        // Atualizar a visualização da simulação em um ciclo
        new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(100); // Atualizar a cada 100ms
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                // Atualizar os semáforos e itens visuais
                javafx.application.Platform.runLater(() -> updateVisualization(pane));
            }
        }).start();
    }

    /**
     * Desenha o grafo na interface gráfica.
     *
     * @param pane O painel onde o grafo será desenhado.
     */
    private void drawGraph(Pane pane) {
        // Desenhar os nós do grafo
        for (Node node : graph.getNodes()) {
            Circle circle = new Circle(
                    normalize(node.latitude, -90, 90, 50, 750),
                    normalize(node.longitude, -180, 180, 50, 550),
                    5, Color.BLUE
            );
            nodeCircles.put(node.id, circle); // Mapear o círculo pelo ID do nó
            pane.getChildren().add(circle);
        }

        // Desenhar as arestas do grafo
        for (Edge edge : graph.getEdges()) {
            Node sourceNode = getNode(edge.getSource());
            Node targetNode = getNode(edge.getTarget());
            if (sourceNode != null && targetNode != null) {
                Line line = new Line(
                        normalize(sourceNode.latitude, -90, 90, 50, 750),
                        normalize(sourceNode.longitude, -180, 180, 50, 550),
                        normalize(targetNode.latitude, -90, 90, 50, 750),
                        normalize(targetNode.longitude, -180, 180, 50, 550)
                );
                line.setStrokeWidth(2);
                pane.getChildren().add(line);
            }
        }
    }

    /**
     * Atualiza a visualização gráfica com base no estado atual dos semáforos.
     *
     * @param pane O painel que será atualizado.
     */
    private void updateVisualization(Pane pane) {
        for (TrafficLight trafficLight : graph.getTrafficLights()) {
            Node trafficNode = getNode(trafficLight.getNodeId());
            if (trafficNode != null) {
                Circle light = nodeCircles.get(trafficNode.id);
                if (light != null) {
                    if (trafficLight.getState().equals("GREEN")) {
                        light.setFill(Color.GREEN);
                    } else {
                        light.setFill(Color.RED);
                    }
                }
            }
        }
    }

    /**
     * Retorna o nó correspondente ao ID fornecido.
     *
     * @param id O ID do nó desejado.
     * @return O nó correspondente ou null se não encontrado.
     */
    private Node getNode(String id) {
        for (Node node : graph.getNodes()) {
            if (node.id.equals(id)) {
                return node;
            }
        }
        return null;
    }

    /**
     * Normaliza valores (por exemplo, latitude e longitude) para coordenadas na tela.
     *
     * @param value   O valor original.
     * @param min     O valor mínimo da escala original.
     * @param max     O valor máximo da escala original.
     * @param newMin  O novo valor mínimo.
     * @param newMax  O novo valor máximo.
     * @return O valor normalizado.
     */
    private double normalize(double value, double min, double max, double newMin, double newMax) {
        return newMin + (value - min) * (newMax - newMin) / (max - min);
    }

    /**
     * Método principal para iniciar a aplicação JavaFX.
     *
     * @param args Argumentos de linha de comando.
     */
    public static void main(String[] args) {
        launch(args);
    }
}