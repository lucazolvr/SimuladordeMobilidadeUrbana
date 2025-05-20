package org.aiacon.simuladordemobilidadeurbana.simulation;

import org.aiacon.simuladordemobilidadeurbana.model.Graph;
import org.aiacon.simuladordemobilidadeurbana.model.CustomLinkedList;
import org.aiacon.simuladordemobilidadeurbana.model.Node;
import org.aiacon.simuladordemobilidadeurbana.model.Edge;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;

public class Dijkstra {

    public static CustomLinkedList<String> calculateRoute(Graph graph, String originId, String destinationId) {
        if (graph == null || originId == null || destinationId == null || graph.getNodes() == null || graph.getNodes().isEmpty()) {
            System.err.println("DIJKSTRA_ROUTE: Grafo nulo, IDs nulos, ou grafo sem nós.");
            return null;
        }

        Map<String, Integer> distances = new HashMap<>(); // Usar ID do nó (String) como chave
        Map<String, String> previousNodeIds = new HashMap<>(); // Mapear ID do nó para ID do nó anterior
        HashSet<String> visitedNodeIds = new HashSet<>(); // Nós visitados por ID

        // Usar um tipo de "fila de prioridade" simulada para pegar o nó com menor distância
        Map<String, Integer> unvisitedNodesWithDistance = new HashMap<>();

        Node originNode = graph.getNode(originId);
        Node destinationNode = graph.getNode(destinationId);

        if (originNode == null) {
            System.err.println("DIJKSTRA_ROUTE_ERROR: Nó de origem com ID '" + originId + "' não encontrado no grafo.");
            return null;
        }
        if (destinationNode == null) {
            System.err.println("DIJKSTRA_ROUTE_ERROR: Nó de destino com ID '" + destinationId + "' não encontrado no grafo.");
            return null;
        }

        // Inicializar distâncias
        for (Node node : graph.getNodes()) { // Iteração correta
            if (node != null) {
                distances.put(node.getId(), Integer.MAX_VALUE);
                unvisitedNodesWithDistance.put(node.getId(), Integer.MAX_VALUE);
            }
        }

        distances.put(originId, 0);
        unvisitedNodesWithDistance.put(originId, 0);

        while (!unvisitedNodesWithDistance.isEmpty()) {
            String currentNodeId = getClosestUnvisitedNode(unvisitedNodesWithDistance, visitedNodeIds);

            if (currentNodeId == null || distances.get(currentNodeId) == Integer.MAX_VALUE) {
                //System.err.println("DIJKSTRA_ROUTE: Não há mais nós alcançáveis ou nó atual com distância infinita.");
                break; // Nenhum nó restante alcançável ou o restante é infinito
            }

            if (currentNodeId.equals(destinationId)) {
                //System.out.println("DIJKSTRA_ROUTE: Destino " + destinationId + " alcançado.");
                break; // Destino alcançado
            }

            visitedNodeIds.add(currentNodeId);
            unvisitedNodesWithDistance.remove(currentNodeId); // Remover da "fila de prioridade"

            Node currentNodeObject = graph.getNode(currentNodeId);
            if (currentNodeObject == null || currentNodeObject.getEdges() == null) {
                //System.err.println("DIJKSTRA_ROUTE_WARNING: Nó atual " + currentNodeId + " não encontrado ou não tem arestas.");
                continue;
            }

            for (Edge edge : currentNodeObject.getEdges()) { // Iteração correta sobre as arestas do nó atual
                if (edge == null) continue;

                String neighborNodeId = edge.getTarget();
                if (visitedNodeIds.contains(neighborNodeId)) {
                    continue; // Já visitou, pula
                }

                Node neighborNodeObject = graph.getNode(neighborNodeId); // Otimização: poderia já ter o objeto Node
                if (neighborNodeObject == null) {
                    //System.err.println("DIJKSTRA_ROUTE_WARNING: Vizinho " + neighborNodeId + " da aresta " + edge.getId() + " não encontrado.");
                    continue;
                }

                double edgeTravelTime = edge.getTravelTime();
                if (edgeTravelTime <= 0 || edgeTravelTime == Double.POSITIVE_INFINITY) {
                    // System.err.println("DIJKSTRA_ROUTE_WARNING: Aresta " + edge.getId() + " com tempo de viagem inválido: " + edgeTravelTime);
                    continue; // Ignora arestas com tempo de viagem inválido
                }

                // Usar int para distância para simplificar, mas pode truncar. Idealmente, usar double.
                // Multiplicar por 100 para manter alguma precisão se travelTime for pequeno.
                int newDist = distances.get(currentNodeId) + (int) (edgeTravelTime * 100.0);

                if (newDist < distances.getOrDefault(neighborNodeId, Integer.MAX_VALUE)) {
                    distances.put(neighborNodeId, newDist);
                    previousNodeIds.put(neighborNodeId, currentNodeId);
                    unvisitedNodesWithDistance.put(neighborNodeId, newDist); // Atualiza na "fila de prioridade"
                }
            }
        }

        // Se o destino não foi alcançado (não está em 'previousNodeIds' e não é a origem)
        if (!previousNodeIds.containsKey(destinationId) && !originId.equals(destinationId)) {
            System.err.println("DIJKSTRA_ROUTE_ERROR: Caminho para o destino " + destinationId + " não pôde ser construído (não está em 'previousNodeIds').");
            return new CustomLinkedList<>(); // Retorna rota vazia
        }

        return buildPath(previousNodeIds, originId, destinationId);
    }

    private static String getClosestUnvisitedNode(Map<String, Integer> unvisitedNodesWithDistance, HashSet<String> visitedNodeIds) {
        String closestNodeId = null;
        int minDistance = Integer.MAX_VALUE;

        for (Map.Entry<String, Integer> entry : unvisitedNodesWithDistance.entrySet()) {
            String nodeId = entry.getKey();
            int distance = entry.getValue();
            if (!visitedNodeIds.contains(nodeId) && distance < minDistance) {
                minDistance = distance;
                closestNodeId = nodeId;
            }
        }
        return closestNodeId;
    }

    private static CustomLinkedList<String> buildPath(Map<String, String> previousNodeIds, String originId, String destinationId) {
        CustomLinkedList<String> path = new CustomLinkedList<>();
        String currentNodeId = destinationId;

        // Se o destino não tem antecessor e não é a origem, não há caminho.
        if (!previousNodeIds.containsKey(currentNodeId) && !currentNodeId.equals(originId)) {
            //System.err.println("DIJKSTRA_BUILD_PATH: Destino " + destinationId + " não é alcançável (não está em previousNodeIds).");
            return path; // Retorna caminho vazio
        }

        while (currentNodeId != null) {
            path.addFirst(currentNodeId);
            if (currentNodeId.equals(originId)) {
                break; // Chegou na origem
            }
            currentNodeId = previousNodeIds.get(currentNodeId); // Move para o nó anterior
            if (currentNodeId == null && !path.getFirst().equals(originId)) {
                //System.err.println("DIJKSTRA_BUILD_PATH: Caminho interrompido antes de alcançar a origem.");
                return new CustomLinkedList<>(); // Caminho quebrado
            }
        }

        // Validação final: o caminho deve começar na origem e terminar no destino.
        // A primeira validação é se o primeiro elemento é a origem, se o path não estiver vazio.
        if (path.isEmpty() || !path.getFirst().equals(originId)) {
            if (!originId.equals(destinationId)) { // Se origem e destino são iguais, um path com um nó é válido.
                // System.err.println("DIJKSTRA_BUILD_PATH: Caminho construído não começa na origem " + originId + " ou está vazio. Primeiro no path: " + (path.isEmpty() ? "VAZIO" : path.getFirst()));
                return new CustomLinkedList<>(); // Retorna caminho vazio
            }
        }

        return path;
    }
}