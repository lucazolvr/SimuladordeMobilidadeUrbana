package org.aiacon.simuladordemobilidadeurbana.simulation;

import org.aiacon.simuladordemobilidadeurbana.model.Graph;
import org.aiacon.simuladordemobilidadeurbana.model.CustomLinkedList;
import org.aiacon.simuladordemobilidadeurbana.model.Node;
import org.aiacon.simuladordemobilidadeurbana.model.Edge;

import java.util.HashMap;
import java.util.HashSet;

// Implementação do algoritmo de Dijkstra
public class Dijkstra {

    // Encontra o caminho mais curto entre origem e destino
    public static CustomLinkedList<String> calculateRoute(Graph graph, String originId, String destinationId) {
        // Verificação inicial: grafo e entradas válidas
        if (graph == null || originId == null || destinationId == null) {
            System.err.println("Erro: grafo ou IDs de origem/destino são nulos.");
            return null;
        }

        HashMap<Node, Integer> distances = new HashMap<>();
        HashMap<Node, Node> previous = new HashMap<>();
        HashSet<Node> visited = new HashSet<>();

        // Encontrar nós de origem e destino no grafo
        Node origin = null, destination = null;

        for (Node node = graph.getNodes().getFirst(); node != null; node = node.next) {
            distances.put(node, Integer.MAX_VALUE);
            if (node.id.equals(originId)) origin = node;
            if (node.id.equals(destinationId)) destination = node;
        }

        // Verificar se origem e destino existem
        if (origin == null || destination == null) {
            System.err.println("Erro: Nó de origem ou destino não encontrado no grafo.");
            return null;
        }

        // Inicializar a distância da origem como 0
        distances.put(origin, 0);

        // Algoritmo principal de Dijkstra
        while (visited.size() < distances.size()) {
            // Encontra o nó não visitado com a menor distância
            Node current = findMinDistanceNode(distances, visited);
            if (current == null) break; // Nenhum nó restante alcançável (grafo desconexo)

            visited.add(current);

            // Atualizar distâncias para os vizinhos do nó atual
            for (Edge edge = graph.getEdges().getFirst(); edge != null; edge = edge.next) {
                if (edge.getSource().equals(current.id)) { // Verifica se a aresta parte do "current"
                    Node neighbor = getNodeById(graph.getNodes(), edge.getTarget());

                    // Verificar se o nó de destino da aresta é válido
                    if (neighbor == null) {
                        System.err.println("Erro: A aresta aponta para um nó inexistente. Aresta de: "
                                + current.id + " para " + edge.getTarget());
                        continue;
                    }

                    // Validar o tempo de viagem da aresta
                    if (edge.getTravelTime() <= 0) {
                        System.err.println("Erro: Tempo de viagem inválido na aresta de "
                                + current.id + " para " + edge.getTarget());
                        continue;
                    }

                    // Atualização da distância
                    int newDist = distances.get(current) + (int) (edge.getTravelTime() * 1000);
                    if (newDist < distances.get(neighbor)) {
                        distances.put(neighbor, newDist);
                        previous.put(neighbor, current);
                    }
                }
            }
        }

        // Construir o caminho do destino até a origem
        return buildPath(previous, origin, destination);
    }



    // Encontra o nó com a menor distância que ainda não foi visitado
    private static Node findMinDistanceNode(HashMap<Node, Integer> distances, HashSet<Node> visited) {
        Node minNode = null;
        int minDistance = Integer.MAX_VALUE;

        for (Node node : distances.keySet()) {
            int distance = distances.get(node);
            if (!visited.contains(node) && distance < minDistance) {
                minDistance = distance;
                minNode = node;
            }
        }

        return minNode; // Retorna o nó com a menor distância
    }

    // Retorna o nó pelo ID
    private static Node getNodeById(CustomLinkedList<Node> nodes, String id) {
        for (Node node = nodes.getFirst(); node != null; node = node.next) {
            if (node.id.equals(id)) {
                return node;
            }
        }
        return null; // Retorna null se o nó não for encontrado
    }


    // Constrói o caminho a partir dos nós anteriores
    // Constrói o caminho do destino até a origem usando o mapa "previous"
    private static CustomLinkedList<String> buildPath(HashMap<Node, Node> previous, Node origin, Node destination) {
        CustomLinkedList<String> path = new CustomLinkedList<>();

        // Caminha pelo mapa de "previous" partindo da origem até o destino
        for (Node node = destination; node != null; node = previous.get(node)) {
            path.addFirst(node.id); // Adiciona no início para obter a ordem inversa
            if (node.equals(origin)) break; // Para ao voltar até a origem
        }

        // Validar se o caminho começa na origem
        if (!path.getFirst().equals(origin.id)) {
            System.err.println("Erro: Caminho não começa no nó de origem. Retornando lista vazia.");
            return new CustomLinkedList<>(); // Retorna lista vazia se o caminho não for válido
        }

        return path; // A lista já está na ordem correta
    }
}