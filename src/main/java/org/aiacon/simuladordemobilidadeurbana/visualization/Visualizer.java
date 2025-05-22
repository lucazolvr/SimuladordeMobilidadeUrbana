package org.aiacon.simuladordemobilidadeurbana.visualization;

import org.aiacon.simuladordemobilidadeurbana.model.Edge;
import org.aiacon.simuladordemobilidadeurbana.model.Graph;
import org.aiacon.simuladordemobilidadeurbana.model.Node;
import org.aiacon.simuladordemobilidadeurbana.model.TrafficLight;
import org.aiacon.simuladordemobilidadeurbana.model.Vehicle;
import org.aiacon.simuladordemobilidadeurbana.model.CustomLinkedList;
import org.aiacon.simuladordemobilidadeurbana.model.LightPhase; // Importar o LightPhase
import org.aiacon.simuladordemobilidadeurbana.simulation.Simulator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.Group; // Usar Group para agrupar elementos do semáforo
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.shape.Rectangle; // Para luzes do semáforo
import javafx.scene.text.Text;
import javafx.stage.Stage;
import org.aiacon.simuladordemobilidadeurbana.simulation.Statistics;

import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;

/**
 * Classe responsável por visualizar o grafo e a simulação usando JavaFX.
 */
public class Visualizer extends Application {

    private static final double LARGURA_TELA = 1000;
    private static final double ALTURA_TELA = 750;
    private static final double MARGEM_TELA = 50;
    private static final double ANGULO_ROTACAO_GRAUS = 0;

    private double minLat, maxLat, minLon, maxLon;
    private double centroLat, centroLon;
    private double escalaX, escalaY;
    private boolean transformacaoCalculada = false;
    private Text statsText;
    private Graph graph;
    private Simulator simulator;
    private Text congestionText;

    private Pane pane;
    private Map<String, Group> trafficLightNodeVisuals;
    private Map<String, Circle> regularNodeVisuals;
    private Map<String, Circle> vehicleVisuals;

    private volatile boolean running = true;

    // Classe interna para representar o visual de um semáforo
    private static class TrafficLightDisplay {
        Rectangle nsIndicator; // Indicador para Norte-Sul
        Rectangle ewIndicator; // Indicador para Leste-Oeste
        // Circle baseNodeCircle; // O círculo base do nó já está em trafficLightNodeVisuals ou regularNodeVisuals

        TrafficLightDisplay(Rectangle ns, Rectangle ew) {
            // this.baseNodeCircle = base;
            this.nsIndicator = ns;
            this.ewIndicator = ew;
        }
    }
    private Map<String, TrafficLightDisplay> lightVisualsMap;


    public Visualizer(Graph graph, Simulator simulator) {
        this.graph = graph;
        this.simulator = simulator;
        this.trafficLightNodeVisuals = new HashMap<>();
        this.regularNodeVisuals = new HashMap<>();
        this.lightVisualsMap = new HashMap<>();
        this.vehicleVisuals = new HashMap<>();
    }

    public Visualizer() {
        this.trafficLightNodeVisuals = new HashMap<>();
        this.regularNodeVisuals = new HashMap<>();
        this.lightVisualsMap = new HashMap<>();
        this.vehicleVisuals = new HashMap<>();
    }


    @Override
    public void start(Stage primaryStage) {
        if (this.graph == null || this.simulator == null) {
            System.err.println("Visualizer: Graph ou Simulator não inicializado. Encerrando UI.");
            Pane errorPane = new Pane(new Text("Erro Crítico: Dados da simulação não carregados para o Visualizer."));
            Scene errorScene = new Scene(errorPane, 450, 100);
            primaryStage.setTitle("Erro de Inicialização do Visualizer");
            primaryStage.setScene(errorScene);
            primaryStage.show();
            return;
        }

        this.pane = new Pane();
        pane.setStyle("-fx-background-color: #D3D3D3;");

        statsText = new Text(10, ALTURA_TELA - 10, "Estatísticas: Carregando...");
        pane.getChildren().add(statsText);
        // Adicionar o texto para o congestionamento
        congestionText = new Text(10, ALTURA_TELA - 10, "Congestionamento: N/A"); // Posição de exemplo
        pane.getChildren().add(congestionText);

        calcularParametrosDeTransformacao();
        desenharElementosEstaticos();

        Scene scene = new Scene(pane, LARGURA_TELA, ALTURA_TELA);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Simulador de Mobilidade Urbana AIACON");
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            running = false;
            System.out.println("Visualizer: Solicitação de fechamento recebida.");
            if (simulator != null) {
                simulator.stopSimulation();
            }
        });

        Thread updateThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    if (!running) break;
                    System.err.println("Visualizer: update thread interrompida: " + e.getMessage());
                    Thread.currentThread().interrupt();
                }
                if (running) {
                    Platform.runLater(this::atualizarElementosDinamicos);
                }
            }
            System.out.println("Visualizer: Update thread finalizada.");
        });
        updateThread.setDaemon(true);
        updateThread.setName("VisualizerUpdateThread");
        updateThread.start();
    }

    private void calcularParametrosDeTransformacao() {
        if (graph.getNodes() == null || graph.getNodes().isEmpty()) {
            System.err.println("Visualizer: Nenhum nó no grafo para calcular transformação. Usando defaults.");
            minLat = -5.12; maxLat = -5.06; minLon = -42.84; maxLon = -42.78;
        } else {
            minLat = Double.MAX_VALUE; maxLat = -Double.MAX_VALUE;
            minLon = Double.MAX_VALUE; maxLon = -Double.MAX_VALUE;
            for (Node node : graph.getNodes()) {
                if (node == null) continue;
                if (node.getLatitude() < minLat) minLat = node.getLatitude();
                if (node.getLatitude() > maxLat) maxLat = node.getLatitude();
                if (node.getLongitude() < minLon) minLon = node.getLongitude();
                if (node.getLongitude() > maxLon) maxLon = node.getLongitude();
            }
        }
        if (Math.abs(maxLat - minLat) < 0.00001) { maxLat = minLat + 0.001; minLat = minLat - 0.001;}
        if (Math.abs(maxLon - minLon) < 0.00001) { maxLon = minLon + 0.001; minLon = minLon - 0.001;}

        centroLat = (minLat + maxLat) / 2.0;
        centroLon = (minLon + maxLon) / 2.0;
        double deltaLonGeo = maxLon - minLon;
        double deltaLatGeo = maxLat - minLat;
        double larguraDesenhoUtil = LARGURA_TELA - 2 * MARGEM_TELA;
        double alturaDesenhoUtil = ALTURA_TELA - 2 * MARGEM_TELA;
        double escalaPotencialX = (deltaLonGeo == 0) ? larguraDesenhoUtil : larguraDesenhoUtil / deltaLonGeo;
        double escalaPotencialY = (deltaLatGeo == 0) ? alturaDesenhoUtil : alturaDesenhoUtil / deltaLatGeo;
        escalaX = Math.min(escalaPotencialX, escalaPotencialY);
        escalaY = escalaX;
        transformacaoCalculada = true;
    }

    private Point2D transformarCoordenadas(double latGeo, double lonGeo) {
        if (!transformacaoCalculada) {
            calcularParametrosDeTransformacao();
            if(!transformacaoCalculada) {
                return new Point2D(LARGURA_TELA / 2, ALTURA_TELA / 2);
            }
        }
        double lonRel = lonGeo - centroLon;
        double latRel = latGeo - centroLat;
        double xTela = lonRel * escalaX + LARGURA_TELA / 2;
        double yTela = latRel * (-escalaY) + ALTURA_TELA / 2;
        return new Point2D(xTela, yTela);
    }

    private void desenharElementosEstaticos() {
        pane.getChildren().clear();
        trafficLightNodeVisuals.clear();
        regularNodeVisuals.clear();
        lightVisualsMap.clear();

        if (graph.getEdges() != null) {
            for (Edge edge : graph.getEdges()) {
                if (edge == null) continue;
                Node sourceNode = graph.getNode(edge.getSource());
                Node targetNode = graph.getNode(edge.getDestination());
                if (sourceNode != null && targetNode != null) {
                    Point2D p1 = transformarCoordenadas(sourceNode.getLatitude(), sourceNode.getLongitude());
                    Point2D p2 = transformarCoordenadas(targetNode.getLatitude(), targetNode.getLongitude());
                    Line line = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                    line.setStroke(Color.rgb(100, 100, 100, 0.8));
                    line.setStrokeWidth(1.8);
                    pane.getChildren().add(line);
                }
            }
        }

        if (graph.getNodes() != null) {
            for (Node node : graph.getNodes()) {
                if (node == null) continue;
                Point2D p = transformarCoordenadas(node.getLatitude(), node.getLongitude());
                TrafficLight tl = simulator.getTrafficLight(node.getId());

                if (tl != null) {
                    Group trafficLightGroup = new Group(); // Agrupa todos os elementos do semáforo

                    Circle baseCircle = new Circle(p.getX(), p.getY(), 5, Color.DARKSLATEGRAY);
                    baseCircle.setStroke(Color.BLACK);
                    baseCircle.setStrokeWidth(0.5);
                    trafficLightGroup.getChildren().add(baseCircle);

                    // Indicador Vertical (N-S) - um pouco mais longo
                    Rectangle nsIndicator = new Rectangle(p.getX() - 2, p.getY() - 8, 4, 16);
                    nsIndicator.setFill(Color.GRAY); // Cor inicial
                    nsIndicator.setStroke(Color.BLACK);
                    nsIndicator.setStrokeWidth(0.4);

                    // Indicador Horizontal (L-O) - um pouco mais longo
                    Rectangle ewIndicator = new Rectangle(p.getX() - 8, p.getY() - 2, 16, 4);
                    ewIndicator.setFill(Color.GRAY); // Cor inicial
                    ewIndicator.setStroke(Color.BLACK);
                    ewIndicator.setStrokeWidth(0.4);

                    trafficLightGroup.getChildren().addAll(nsIndicator, ewIndicator);
                    pane.getChildren().add(trafficLightGroup);

                    // Armazenar os componentes para atualização
                    lightVisualsMap.put(node.getId(), new TrafficLightDisplay(nsIndicator, ewIndicator));
                    // Não precisa mais do trafficLightNodeVisuals se lightVisualsMap guarda os componentes
                } else {
                    Circle nodeCircle = new Circle(p.getX(), p.getY(), 3, Color.ROYALBLUE);
                    nodeCircle.setStroke(Color.NAVY);
                    nodeCircle.setStrokeWidth(0.5);
                    regularNodeVisuals.put(node.getId(), nodeCircle);
                    pane.getChildren().add(nodeCircle);
                }
            }
        }
    }

    private void atualizarElementosDinamicos() {
        if (pane == null || graph == null || simulator == null || !transformacaoCalculada) return;

        // 1. Atualizar Cores dos Semáforos
        if (graph.getTrafficLights() != null) {
            for (TrafficLight tl : graph.getTrafficLights()) {
                if (tl == null) continue;
                TrafficLightDisplay display = lightVisualsMap.get(tl.getNodeId());
                if (display != null) {
                    LightPhase phase = tl.getCurrentPhase();
                    Color nsColor = Color.DARKRED; // Vermelho padrão
                    Color ewColor = Color.DARKRED; // Vermelho padrão

                    if (phase != null) {
                        switch (phase) {
                            case NS_GREEN_EW_RED:
                                nsColor = Color.LIMEGREEN;
                                ewColor = Color.INDIANRED;
                                break;
                            case NS_YELLOW_EW_RED:
                                nsColor = Color.GOLD;
                                ewColor = Color.INDIANRED;
                                break;
                            case NS_RED_EW_GREEN:
                                nsColor = Color.INDIANRED;
                                ewColor = Color.LIMEGREEN;
                                break;
                            case NS_RED_EW_YELLOW:
                                nsColor = Color.INDIANRED;
                                ewColor = Color.GOLD;
                                break;
                            // Caso não haja default, ambas ficam vermelhas (já setado)
                        }
                    }
                    display.nsIndicator.setFill(nsColor);
                    display.ewIndicator.setFill(ewColor);
                }
            }
        }

        // 2. Atualizar Posições dos Veículos
        CustomLinkedList<Vehicle> currentVehicles = simulator.getVehicles();
        if (currentVehicles == null) return;

        Map<String, Circle> newVehicleVisualsMap = new HashMap<>();
        List<javafx.scene.Node> childrenToAdd = new ArrayList<>();
        List<javafx.scene.Node> childrenToRemove = new ArrayList<>();

        for (Vehicle vehicle : currentVehicles) {
            if (vehicle == null || vehicle.getCurrentNode() == null) continue;

            Point2D vehiclePos;
            Node currentNodeObject = graph.getNode(vehicle.getCurrentNode());
            if (currentNodeObject == null) continue;

            if (vehicle.getPosition() == 0.0 || vehicle.getRoute() == null || vehicle.getRoute().isEmpty()) {
                vehiclePos = transformarCoordenadas(currentNodeObject.getLatitude(), currentNodeObject.getLongitude());
            } else {
                String nextNodeId = getNextNodeIdInRoute(vehicle, currentNodeObject.getId());
                if (nextNodeId == null) {
                    vehiclePos = transformarCoordenadas(currentNodeObject.getLatitude(), currentNodeObject.getLongitude());
                } else {
                    Node nextNodeObject = graph.getNode(nextNodeId);
                    if (nextNodeObject == null) {
                        vehiclePos = transformarCoordenadas(currentNodeObject.getLatitude(), currentNodeObject.getLongitude());
                    } else {
                        Point2D startScreenPos = transformarCoordenadas(currentNodeObject.getLatitude(), currentNodeObject.getLongitude());
                        Point2D endScreenPos = transformarCoordenadas(nextNodeObject.getLatitude(), nextNodeObject.getLongitude());
                        double interpolatedX = startScreenPos.getX() + vehicle.getPosition() * (endScreenPos.getX() - startScreenPos.getX());
                        double interpolatedY = startScreenPos.getY() + vehicle.getPosition() * (endScreenPos.getY() - startScreenPos.getY());
                        vehiclePos = new Point2D(interpolatedX, interpolatedY);
                    }
                }
            }

            Circle vehicleCircle = vehicleVisuals.get(vehicle.getId());
            if (vehicleCircle == null) {
                vehicleCircle = new Circle(3.5, Color.DEEPSKYBLUE);
                vehicleCircle.setStroke(Color.BLACK);
                vehicleCircle.setStrokeWidth(0.6);
                childrenToAdd.add(vehicleCircle);
            }
            vehicleCircle.setCenterX(vehiclePos.getX());
            vehicleCircle.setCenterY(vehiclePos.getY());
            newVehicleVisualsMap.put(vehicle.getId(), vehicleCircle);
        }

        for (String existingId : vehicleVisuals.keySet()) {
            if (!newVehicleVisualsMap.containsKey(existingId)) {
                childrenToRemove.add(vehicleVisuals.get(existingId));
            }
        }

        pane.getChildren().removeAll(childrenToRemove);
        pane.getChildren().addAll(childrenToAdd);
        vehicleVisuals = newVehicleVisualsMap;



        // 3. Atualizar Texto de Estatísticas
        if (simulator != null && simulator.getStats() != null && statsText != null) {
            Statistics currentStats = simulator.getStats();
            String statsDisplay = String.format(
                    "Tempo: %.0fs | Veículos Ativos: %d | Congest.: %.0f\n" +
                            "Chegadas: %d | T Médio Viagem: %.1fs | T Médio Espera: %.1fs\n" +
                            "Comb. Total: %.2f L | Comb. Médio/Veículo: %.3f L",
                    currentStats.getCurrentTime(), // Adicionar currentTime em Statistics ou pegar do simulator.time
                    (simulator.getVehicles() != null ? simulator.getVehicles().size() : 0),
                    currentStats.getCurrentCongestionIndex(),
                    currentStats.getVehiclesArrived(),
                    currentStats.getAverageTravelTime(),
                    currentStats.getAverageWaitTime(),
                    currentStats.getTotalFuelConsumed(),
                    currentStats.getAverageFuelConsumptionPerVehicle()
            );
            statsText.setText(statsDisplay);
        }
    }

    private String getNextNodeIdInRoute(Vehicle vehicle, String currentVehicleNodeId) {
        CustomLinkedList<String> route = vehicle.getRoute();
        if (route == null || route.isEmpty()) return null;

        int currentIndex = route.indexOf(currentVehicleNodeId);
        if (currentIndex != -1 && currentIndex + 1 < route.size()) {
            return route.get(currentIndex + 1);
        }
        return null;
    }
}