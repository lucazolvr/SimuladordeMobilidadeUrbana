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
        // Verificar se o grafo contém nós e não está vazio
        if (graph == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            System.err.println("Erro: Grafo está vazio ou não foi inicializado. Não é possível gerar veículo.");
            return null;
        }

        // Criar uma lista com os IDs de todos os nós no grafo
        CustomLinkedList<String> nodeIds = new CustomLinkedList<>();
        for (Node node : graph.getNodes()) {
            nodeIds.add(node.getId());
        }

        int size = nodeIds.size();
        if (size <= 1) {
            System.err.println("Erro: Grafo não possui nós suficientes para origem e destino. Não é possível gerar veículo.");
            return null;
        }

        // Escolher origem e destino aleatórios
        String origin = getRandomNodeId(nodeIds, size);
        String destination = getRandomNodeId(nodeIds, size);

        // Garantir que origem e destino sejam diferentes
        int retries = 0; // Evitar loop infinito
        while (destination.equals(origin) && retries < 100) {
            destination = getRandomNodeId(nodeIds, size);
            retries++;
        }

        // Verificar se origem e destino existem de fato no grafo
        if (!nodeIds.contains(origin) || !nodeIds.contains(destination)) {
            System.err.println("Erro: Nó de origem ou destino não encontrado no grafo.");
            return null; // Ignorar veículo com nó inválido
        }

        // Log para depuração
        System.out.println("Gerando veículo V" + id + " com origem " + origin + " e destino " + destination);

        // Calcular a rota com Dijkstra
        CustomLinkedList<String> route = Dijkstra.calculateRoute(graph, origin, destination);

        // Verificar se a rota foi calculada corretamente
        if (route == null || route.isEmpty()) {
            System.err.println("Erro ao calcular rota para veículo V" + id + ": nenhuma rota encontrada entre " + origin + " e " + destination);
            return null; // Ignorar veículo sem rota válida
        }

        // Criar e retornar o veículo com rota válida
        Vehicle vehicle = new Vehicle("V" + id, origin, destination, route);
        System.out.println("Veículo V" + id + " gerado com sucesso: Rota = " + route);
        return vehicle;
    }

    private String getRandomNodeId(CustomLinkedList<String> nodeIds, int size) {
        if (size <= 0) {
            throw new IllegalArgumentException("Lista de IDs de nós está vazia. Não é possível selecionar um nó aleatório.");
        }

        int index = random.nextInt(size); // Escolhe um índice aleatório
        int currentIndex = 0;

        for (String nodeId : nodeIds) {
            if (currentIndex++ == index) {
                return nodeId;
            }
        }

        throw new IllegalStateException("Erro na seleção de nó aleatório: índice fora do intervalo.");
    }

    public double getGenerationRate() {
        return generationRate;
    }

    public void setGenerationRate(double rate) {
        this.generationRate = rate;
    }
}