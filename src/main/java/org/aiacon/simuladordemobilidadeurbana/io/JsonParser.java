package org.aiacon.simuladordemobilidadeurbana.io;

import org.aiacon.simuladordemobilidadeurbana.model.*;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.*;

/**
 * Parser para carregar o grafo do arquivo JSON.
 */
public class JsonParser {

    /**
     * Método estático para carregar o grafo a partir de um arquivo JSON.
     *
     * @param filename Caminho para o arquivo JSON.
     * @return Um objeto Graph carregado do JSON.
     * @throws IOException Se ocorrer um erro na leitura do arquivo.
     * @throws Exception    Se ocorrer um erro no processamento do JSON.
     */
    public static Graph loadGraph(String filename) throws IOException, Exception {
        Graph graph = new Graph(); // Criar uma nova instância de Graph
        StringBuilder jsonContent = new StringBuilder();

        // Ler o arquivo JSON como texto
        try (BufferedReader reader = new BufferedReader(new FileReader(filename))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
        }

        // Parse do JSON
        JSONObject json = new JSONObject(jsonContent.toString());

        // Carregar nós e outras estruturas do grafo
        processJson(json, graph);

        return graph; // Retorna o grafo carregado
    }

    /**
     * Método estático para carregar o grafo a partir de um InputStream.
     * Esse método é útil para carregar arquivos diretamente do classpath ou recursos.
     *
     * @param inputStream InputStream do arquivo JSON.
     * @return Um objeto Graph carregado do JSON.
     * @throws IOException Se ocorrer um erro na leitura do arquivo.
     * @throws Exception    Se ocorrer um erro no processamento do JSON.
     */
    public static Graph loadGraphFromStream(InputStream inputStream) throws IOException, Exception {
        if (inputStream == null) {
            throw new IOException("InputStream é nulo, não foi possível localizar o arquivo JSON.");
        }

        Graph graph = new Graph(); // Criar uma nova instância de Graph
        StringBuilder jsonContent = new StringBuilder();

        // Ler o InputStream e montar o conteúdo do JSON
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = reader.readLine()) != null) {
                jsonContent.append(line);
            }
        }

        // Parse do JSON
        JSONObject json = new JSONObject(jsonContent.toString());

        // Carregar nós e outras estruturas do grafo
        processJson(json, graph);

        return graph; // Retorna o grafo carregado
    }

    /**
     * Processa o JSON para adicionar nós, arestas e semáforos ao grafo.
     *
     * @param json  Objeto JSON contendo os dados do grafo.
     * @param graph Instância do grafo a ser preenchida.
     */
    private static void processJson(JSONObject json, Graph graph) {
        // Carregar nós no grafo
        JSONArray nodes = json.getJSONArray("nodes");
        for (int i = 0; i < nodes.length(); i++) {
            JSONObject node = nodes.getJSONObject(i);
            graph.addNode(new Node(
                    node.getString("id"),
                    node.getDouble("latitude"),
                    node.getDouble("longitude"),
                    false // Atualiza as informações do semáforo posteriormente
            ));
        }

        // Carregar arestas no grafo
        JSONArray edges = json.getJSONArray("edges");
        for (int i = 0; i < edges.length(); i++) {
            JSONObject edge = edges.getJSONObject(i);

            double maxspeed = edge.getDouble("maxspeed"); // Velocidade máxima
            double length = edge.getDouble("length"); // Comprimento da aresta
            double travelTime = length / (maxspeed * 1000 / 3600); // Converter m/(km/h * 1000/3600) em segundos
            int capacity = (int) (maxspeed / 10); // Cálculo aproximado para a capacidade

            graph.addEdge(new Edge(
                    edge.getString("id"),
                    edge.getString("source"),
                    edge.getString("target"),
                    length,
                    travelTime,
                    edge.getBoolean("oneway"),
                    maxspeed,
                    capacity
            ));
        }

        // Carregar semáforos no grafo
        JSONArray trafficLights = json.getJSONArray("traffic_lights");
        for (int i = 0; i < trafficLights.length(); i++) {
            JSONObject tl = trafficLights.getJSONObject(i);
            JSONObject attributes = tl.getJSONObject("attributes");
            String direction = attributes.getString("traffic_signals:direction");

            graph.addTrafficLight(new TrafficLight(
                    tl.getString("id"),
                    direction,
                    1 // Estado inicial definido como 1, pode ser ajustado pela configuração
            ));
        }
    }
}