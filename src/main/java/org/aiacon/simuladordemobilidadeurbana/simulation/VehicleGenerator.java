package org.aiacon.simuladordemobilidadeurbana.simulation;

import org.aiacon.simuladordemobilidadeurbana.model.Graph;
import org.aiacon.simuladordemobilidadeurbana.model.CustomLinkedList;
import org.aiacon.simuladordemobilidadeurbana.model.Node;
import org.aiacon.simuladordemobilidadeurbana.model.Vehicle;

import java.util.Random;

// Gera veículos aleatoriamente
public class VehicleGenerator {
    private Graph graph;
    private double generationRate; // Veículos por segundo
    private Random random;

    public VehicleGenerator(Graph graph, double generationRate) {
        this.graph = graph;
        this.generationRate = generationRate;
        this.random = new Random();
    }



    public Vehicle generateVehicle(int id) {
        // Escolher origem e destino aleatórios
        CustomLinkedList<String> nodeIds = new CustomLinkedList<>();
        for (Node node : graph.getNodes()) {
            nodeIds.add(node.getId());
        }

        int size = nodeIds.size();
        String origin = getRandomNodeId(nodeIds, size);
        String destination = getRandomNodeId(nodeIds, size);

        // Garantir que origem e destino são diferentes
        while (destination.equals(origin)) {
            destination = getRandomNodeId(nodeIds, size);
        }

        // Calcular a rota com Dijkstra
        CustomLinkedList<String> route = Dijkstra.calculateRoute(graph, origin, destination);

        // Verificar se a rota foi calculada corretamente
        if (route == null || route.isEmpty()) {
            System.err.println("Erro ao calcular rota para veículo V" + id + ": nenhuma rota encontrada entre " + origin + " e " + destination);
            return null; // Ignorar veículo sem rota
        }

        // Criar e retornar veículo com rota válida
        return new Vehicle("V" + id, origin, destination, route);
    }

    private String getRandomNodeId(CustomLinkedList<String> nodeIds, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Lista de nós está vazia, não é possível selecionar um nó aleatório");
        }

        int index = random.nextInt(size);
        int currentIndex = 0;

        for (String nodeId : nodeIds) {
            if (currentIndex++ == index) {
                return nodeId;
            }
        }

        throw new IllegalStateException("Erro na seleção de nó aleatório: índice fora do intervalo");
    }

    public double getGenerationRate() {
        return generationRate;
    }

    public void setGenerationRate(double rate) {
        this.generationRate = rate;
    }
}