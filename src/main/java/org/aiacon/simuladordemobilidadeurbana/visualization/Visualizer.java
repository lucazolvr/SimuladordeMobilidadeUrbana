package org.aiacon.simuladordemobilidadeurbana.visualization;

import org.aiacon.simuladordemobilidadeurbana.model.Edge;
import org.aiacon.simuladordemobilidadeurbana.model.Graph;
import org.aiacon.simuladordemobilidadeurbana.model.Node;
import org.aiacon.simuladordemobilidadeurbana.model.TrafficLight;
import org.aiacon.simuladordemobilidadeurbana.model.Vehicle;
import org.aiacon.simuladordemobilidadeurbana.model.CustomLinkedList; // Usar sua CustomLinkedList
import org.aiacon.simuladordemobilidadeurbana.simulation.Simulator;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.geometry.Point2D;
import javafx.scene.Scene;
import javafx.scene.layout.Pane;
import javafx.scene.paint.Color;
import javafx.scene.shape.Circle;
import javafx.scene.shape.Line;
import javafx.scene.text.Text;
import javafx.stage.Stage;

import java.util.HashMap;
import java.util.Map;
import java.util.List; // Para conversão temporária se necessário
import java.util.ArrayList; // Para conversão temporária se necessário

/**
 * Classe responsável por visualizar o grafo e a simulação usando JavaFX.
 */
public class Visualizer extends Application {

    // Constantes para configuração da tela e transformação (podem ser ajustadas)
    private static final double LARGURA_TELA = 1000; // Aumentado para melhor visualização
    private static final double ALTURA_TELA = 750;  // Aumentado para melhor visualização
    private static final double MARGEM_TELA = 50;    // Margem para não desenhar nas bordas

    // Parâmetros de transformação de coordenadas (calculados em start())
    private double minLat, maxLat, minLon, maxLon;
    private double centroLat, centroLon;
    private double escalaX, escalaY;
    private boolean transformacaoCalculada = false;

    // Ângulo de rotação (opcional, como em simulador.Main)
    // Se não quiser rotação, defina como 0.
    private static final double ANGULO_ROTACAO_GRAUS = 0; // Ex: 190 para rotacionar, 0 para não rotacionar

    private Graph graph;
    private Simulator simulator;

    private Pane pane;
    private Map<String, Circle> nodeVisuals; // Círculos dos Nós
    private Map<String, Text> nodeIdLabels;   // Labels dos IDs dos Nós
    private Map<String, Circle> vehicleVisuals; // Círculos dos Veículos

    private volatile boolean running = true; // Flag para controlar o loop de atualização

    /**
     * Construtor chamado por HelloApplication.
     * @param graph O grafo carregado.
     * @param simulator O simulador que opera no grafo.
     */
    public Visualizer(Graph graph, Simulator simulator) {
        this.graph = graph;
        this.simulator = simulator;
        this.nodeVisuals = new HashMap<>();
        this.nodeIdLabels = new HashMap<>();
        this.vehicleVisuals = new HashMap<>();
    }

    /**
     * Construtor padrão para JavaFX (caso seja lançado diretamente, o que não é o caso com HelloApplication).
     * Se for usado, graph e simulator precisarão ser injetados de outra forma.
     */
    public Visualizer() {
        // Este construtor seria chamado se Visualizer.launch() fosse usado.
        // HelloApplication usa o construtor com argumentos.
        this.nodeVisuals = new HashMap<>();
        this.nodeIdLabels = new HashMap<>();
        this.vehicleVisuals = new HashMap<>();
    }


    @Override
    public void start(Stage primaryStage) {
        if (this.graph == null || this.simulator == null) {
            System.err.println("Visualizer não inicializado com Graph e Simulator. Encerrando.");
            // Exibir mensagem de erro na UI
            Pane errorPane = new Pane(new Text("Erro: Visualizer não recebeu dados do Graph ou Simulator."));
            Scene errorScene = new Scene(errorPane, 400, 100);
            primaryStage.setTitle("Erro de Inicialização");
            primaryStage.setScene(errorScene);
            primaryStage.show();
            return;
        }

        this.pane = new Pane();
        pane.setStyle("-fx-background-color: #e0e0e0;"); // Um fundo leve

        calcularParametrosDeTransformacao();
        desenharElementosEstaticos();
        inicializarVeiculosVisuais(); // Criar os shapes dos veículos uma vez

        Scene scene = new Scene(pane, LARGURA_TELA, ALTURA_TELA);
        primaryStage.setScene(scene);
        primaryStage.setTitle("Simulador de Mobilidade Urbana AIACON");
        primaryStage.show();

        primaryStage.setOnCloseRequest(event -> {
            running = false; // Sinaliza para a thread de atualização parar
            System.out.println("Visualizer: Solicitação de fechamento recebida.");
        });

        // Iniciar thread de atualização da visualização
        Thread updateThread = new Thread(() -> {
            while (running) {
                try {
                    Thread.sleep(100); // Taxa de atualização (10 FPS)
                } catch (InterruptedException e) {
                    if (!running) break; // Sai se foi uma interrupção para fechar
                    System.err.println("Visualizer update thread interrupted: " + e.getMessage());
                    Thread.currentThread().interrupt(); // Restaura o status de interrupção
                }
                if (running) { // Verifica novamente antes de executar
                    Platform.runLater(this::atualizarElementosDinamicos);
                }
            }
            System.out.println("Visualizer: Update thread encerrada.");
        });
        updateThread.setDaemon(true); // Permite que a JVM feche mesmo se a thread estiver rodando
        updateThread.start();
    }

    private void calcularParametrosDeTransformacao() {
        if (graph.getNodes() == null || graph.getNodes().isEmpty()) {
            System.err.println("Nenhum nó no grafo para calcular transformação.");
            // Usar valores padrão para evitar NullPointerException, mas a tela ficará vazia/incorreta
            minLat = -90; maxLat = 90; minLon = -180; maxLon = 180;
            transformacaoCalculada = false;
            return;
        }

        minLat = Double.MAX_VALUE;
        maxLat = -Double.MAX_VALUE;
        minLon = Double.MAX_VALUE;
        maxLon = -Double.MAX_VALUE;

        for (Node node : graph.getNodes()) {
            if (node.latitude < minLat) minLat = node.latitude;
            if (node.latitude > maxLat) maxLat = node.latitude;
            if (node.longitude < minLon) minLon = node.longitude;
            if (node.longitude > maxLon) maxLon = node.longitude;
        }

        // Caso haja apenas um nó, ou todos na mesma coordenada
        if (minLat == maxLat) { maxLat += 0.001; minLat -=0.001;}
        if (minLon == maxLon) { maxLon += 0.001; minLon -=0.001;}


        centroLat = (minLat + maxLat) / 2.0;
        centroLon = (minLon + maxLon) / 2.0;

        double deltaLon = maxLon - minLon;
        double deltaLat = maxLat - minLat;

        // Calcular escalas para preencher a tela, mantendo a proporção
        // A área de desenho efetiva é LARGURA_TELA - 2*MARGEM_TELA e ALTURA_TELA - 2*MARGEM_TELA
        double larguraDesenho = LARGURA_TELA - 2 * MARGEM_TELA;
        double alturaDesenho = ALTURA_TELA - 2 * MARGEM_TELA;

        if (deltaLon == 0 || deltaLat == 0) { // Evitar divisão por zero se todos os pontos são colineares
            escalaX = (deltaLon == 0) ? 1 : larguraDesenho / deltaLon;
            escalaY = (deltaLat == 0) ? 1 : alturaDesenho / deltaLat;
        } else {
            double escalaGlobal = Math.min(larguraDesenho / deltaLon, alturaDesenho / deltaLat);
            escalaX = escalaGlobal;
            escalaY = escalaGlobal; // Usar a mesma escala para Y, mas negativo porque Y da tela cresce para baixo
        }

        System.out.printf("Transformação: Lat [%f, %f], Lon [%f, %f]%n", minLat, maxLat, minLon, maxLon);
        System.out.printf("Centro: Lat %f, Lon %f. EscalaX: %f, EscalaY: %f%n", centroLat, centroLon, escalaX, escalaY);
        transformacaoCalculada = true;
    }

    private Point2D transformarCoordenadas(double latGeo, double lonGeo) {
        if (!transformacaoCalculada) {
            // Retorna um ponto fora da tela ou no centro se a transformação não pôde ser calculada
            return new Point2D(LARGURA_TELA / 2, ALTURA_TELA / 2);
        }

        // 1. Transladar para a origem (relativo ao centro geográfico)
        double lonRel = lonGeo - centroLon;
        double latRel = latGeo - centroLat;

        // 2. Aplicar Rotação (opcional)
        double lonRot, latRot;
        if (ANGULO_ROTACAO_GRAUS != 0) {
            double anguloRotacaoRad = Math.toRadians(ANGULO_ROTACAO_GRAUS);
            double cosAng = Math.cos(anguloRotacaoRad);
            double sinAng = Math.sin(anguloRotacaoRad);
            lonRot = lonRel * cosAng - latRel * sinAng;
            latRot = lonRel * sinAng + latRel * cosAng;
        } else {
            lonRot = lonRel;
            latRot = latRel;
        }

        // 3. Escalar
        // O Y geográfico (latitude) cresce para cima, o Y da tela cresce para baixo.
        // Por isso, multiplicamos latRot por -escalaY.
        double xTela = lonRot * escalaX;
        double yTela = latRot * (-escalaY); // Negativo para inverter o eixo Y

        // 4. Transladar para o centro da área de desenho + margem
        xTela += LARGURA_TELA / 2;
        yTela += ALTURA_TELA / 2;

        return new Point2D(xTela, yTela);
    }


    private void desenharElementosEstaticos() {
        // Desenhar Arestas primeiro para ficarem abaixo dos nós
        if (graph.getEdges() != null) {
            for (Edge edge : graph.getEdges()) {
                Node sourceNode = graph.getNode(edge.getSource());
                Node targetNode = graph.getNode(edge.getTarget());

                if (sourceNode != null && targetNode != null) {
                    Point2D p1 = transformarCoordenadas(sourceNode.latitude, sourceNode.longitude);
                    Point2D p2 = transformarCoordenadas(targetNode.latitude, targetNode.longitude);

                    Line line = new Line(p1.getX(), p1.getY(), p2.getX(), p2.getY());
                    line.setStroke(Color.DARKGRAY);
                    line.setStrokeWidth(1.5);
                    pane.getChildren().add(line);
                }
            }
        }

        // Desenhar Nós e Labels
        if (graph.getNodes() != null) {
            for (Node node : graph.getNodes()) {
                Point2D p = transformarCoordenadas(node.latitude, node.longitude);

                Circle nodeCircle = new Circle(p.getX(), p.getY(), 5, Color.SLATEBLUE);
                nodeCircle.setStroke(Color.BLACK);
                nodeCircle.setStrokeWidth(0.5);

                nodeVisuals.put(node.id, nodeCircle);
                pane.getChildren().add(nodeCircle);

                Text label = new Text(p.getX() + 7, p.getY() + 4, node.id);
                label.setStyle("-fx-font-size: 8px;");
                nodeIdLabels.put(node.id, label); // Guardar referência se precisar esconder/mostrar
                pane.getChildren().add(label);
            }
        }
    }

    private void inicializarVeiculosVisuais() {
        // Se o simulador já tiver veículos (improvável no início, mas para robustez)
        // ou para preparar um pool de visuals se o número de veículos for fixo/limitado.
        // Por agora, vamos criar/remover dinamicamente em atualizarElementosDinamicos.
        // Esta função pode ser usada para pré-alocar shapes se for uma otimização desejada.
    }


    private void atualizarElementosDinamicos() {
        if (pane == null || graph == null || simulator == null) return;

        // 1. Atualizar Cores dos Semáforos
        if (graph.getTrafficLights() != null) {
            for (TrafficLight tl : graph.getTrafficLights()) {
                Circle nodeCircle = nodeVisuals.get(tl.getNodeId());
                if (nodeCircle != null) {
                    switch (tl.getState().toLowerCase()) {
                        case "green":
                            nodeCircle.setFill(Color.LIMEGREEN);
                            break;
                        case "yellow":
                            nodeCircle.setFill(Color.GOLD);
                            break;
                        case "red":
                            nodeCircle.setFill(Color.INDIANRED);
                            break;
                        default:
                            nodeCircle.setFill(Color.LIGHTGRAY); // Estado desconhecido
                            break;
                    }
                }
            }
        }

        // 2. Atualizar Posições dos Veículos
        CustomLinkedList<Vehicle> currentVehicles = simulator.getVehicles(); // Precisa de getVehicles() em Simulator
        if (currentVehicles == null) return;

        // Remover visuals de veículos que não existem mais (se necessário, depende da estratégia do getVehicles)
        // Estratégia simples: remover todos e redesenhar. Para muitos veículos, seria melhor atualizar existentes.
        List<String> activeVehicleIds = new ArrayList<>();
        for (Vehicle v : currentVehicles) {
            activeVehicleIds.add(v.getId());
        }

        // Remover shapes de veículos que não estão mais na lista do simulador
        List<String> idsToRemove = new ArrayList<>();
        for (String vehicleId : vehicleVisuals.keySet()) {
            if (!activeVehicleIds.contains(vehicleId)) {
                pane.getChildren().remove(vehicleVisuals.get(vehicleId));
                idsToRemove.add(vehicleId);
            }
        }
        for (String id : idsToRemove) {
            vehicleVisuals.remove(id);
        }

        for (Vehicle vehicle : currentVehicles) {
            Point2D vehiclePos;
            Node currentNode = graph.getNode(vehicle.getCurrentNode());

            if (currentNode == null) { // Veículo com nó atual inválido
                System.err.println("Veículo " + vehicle.getId() + " com nó atual inválido: " + vehicle.getCurrentNode());
                continue;
            }

            if (vehicle.getPosition() == 0.0 || vehicle.getRoute() == null || vehicle.getRoute().isEmpty()) {
                // Veículo está em um nó (ou rota terminou)
                vehiclePos = transformarCoordenadas(currentNode.latitude, currentNode.longitude);
            } else {
                // Veículo está em uma aresta
                String nextNodeId = getNextNodeIdInRoute(vehicle, currentNode.id);
                if (nextNodeId == null) { // Fim da rota, mas ainda com position > 0? Considerar no nó atual.
                    vehiclePos = transformarCoordenadas(currentNode.latitude, currentNode.longitude);
                } else {
                    Node nextNode = graph.getNode(nextNodeId);
                    if (nextNode == null) { // Próximo nó inválido
                        System.err.println("Veículo " + vehicle.getId() + " com próximo nó inválido na rota: " + nextNodeId);
                        vehiclePos = transformarCoordenadas(currentNode.latitude, currentNode.longitude); // Coloca no atual
                    } else {
                        Point2D startScreenPos = transformarCoordenadas(currentNode.latitude, currentNode.longitude);
                        Point2D endScreenPos = transformarCoordenadas(nextNode.latitude, nextNode.longitude);

                        double screenX = startScreenPos.getX() + vehicle.getPosition() * (endScreenPos.getX() - startScreenPos.getX());
                        double screenY = startScreenPos.getY() + vehicle.getPosition() * (endScreenPos.getY() - startScreenPos.getY());
                        vehiclePos = new Point2D(screenX, screenY);
                    }
                }
            }

            Circle vehicleCircle = vehicleVisuals.get(vehicle.getId());
            if (vehicleCircle == null) { // Novo veículo
                vehicleCircle = new Circle(vehiclePos.getX(), vehiclePos.getY(), 3, Color.DEEPSKYBLUE);
                vehicleCircle.setStroke(Color.BLACK);
                vehicleCircle.setStrokeWidth(0.5);
                vehicleVisuals.put(vehicle.getId(), vehicleCircle);
                pane.getChildren().add(vehicleCircle);
            } else { // Veículo existente, apenas atualiza posição
                vehicleCircle.setCenterX(vehiclePos.getX());
                vehicleCircle.setCenterY(vehiclePos.getY());
            }
        }
    }

    private String getNextNodeIdInRoute(Vehicle vehicle, String currentVehicleNodeId) {
        CustomLinkedList<String> route = vehicle.getRoute();
        if (route == null || route.isEmpty()) return null;

        int currentIndex = -1;
        for(int i = 0; i < route.size(); i++) {
            if(route.get(i).equals(currentVehicleNodeId)){
                currentIndex = i;
                break;
            }
        }

        if (currentIndex != -1 && currentIndex + 1 < route.size()) {
            return route.get(currentIndex + 1);
        }
        return null; // Fim da rota ou nó atual não encontrado na rota
    }

    /**
     * Método main para permitir que Visualizer seja lançado independentemente para testes,
     * mas HelloApplication é o ponto de entrada principal da aplicação.
     */
    public static void main(String[] args) {
        // Para testes, você precisaria carregar um Graph e um Simulator mockados ou reais aqui.
        // Exemplo:
        // Graph testGraph = // carregar ou criar grafo de teste
        // Simulator testSimulator = new Simulator(testGraph, new Configuration());
        // Visualizer.staticGraph = testGraph; // Se usar a abordagem estática
        // Visualizer.staticSimulator = testSimulator;
        // launch(args);

        System.out.println("Visualizer.main() chamado. Para rodar a aplicação completa, use HelloApplication.");
        // Ou, se quiser permitir que Visualizer seja seu próprio ponto de entrada simples (sem dados):
        // Application.launch(args); // Isso chamaria o construtor padrão
    }
}