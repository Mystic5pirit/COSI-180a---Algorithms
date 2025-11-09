/**
 * This class represents a text based Map application.
 * It provides a way to find the shortest/fastest path between two locations on Brandeis campus
 */
package main;

import java.io.File;
import java.io.PrintWriter;
import java.io.FileNotFoundException;
import java.util.Scanner;

import datastructures.HashTable;
import datastructures.MinHeap;
import java.nio.file.attribute.GroupPrincipal;
import java.util.Arrays;

public class Map {
    private final static String vertexPath = "MapDataVertices.txt";
    private final static String edgePath = "MapDataEdges.txt";
    private Graph graph;
    public static void main(String[] args) throws FileNotFoundException {
        new Map().run();
    }

    /**
     * Initializes an instance of Map with a graph representation of all campus locations and paths
     */
    public Map() throws FileNotFoundException {
        Vertex[] vertices = getVertexData();
        Edge[] edges = getEdgeData();  
        this.graph = new Graph(vertices, edges);
    }

    /**
     * Runs the Map application using input from the console
     */
    private void run() throws FileNotFoundException {
        Vertex[] vertices = graph.getVertices();
        ConsoleIO console = new ConsoleIO(mapVertexLabelToIndex(vertices));
        int start = console.getStartVertex();
        int destination;
        boolean optimizingTime, usePrims;
        while (start != -1) {
            destination = console.getDestinationVertex(start);
            optimizingTime = console.getPriorityMetric();
            if (destination == -1) {
                usePrims = console.getPrims();
                Graph minimumSpanningTree = minimumSpanningTree(usePrims, optimizingTime);
                Edge[] tour = preorderTraversal(minimumSpanningTree, start);
                Edge[] tempTour = replaceSuccessiveEdges(tour, optimizingTime);
                while (totalPriority(tempTour, optimizingTime) < totalPriority(tour, optimizingTime)) {
                    tour = tempTour;
                    tempTour = replaceSuccessiveEdges(tour, optimizingTime);
                }
                console.outputTour(tour, minimumSpanningTree, vertices);
                writeOutput(tour);
            } else {
                Edge[] path = dijkstra(start, destination, optimizingTime);
                console.outputPath(path, vertices);
                writeOutput(path);
            }
            start = console.getStartVertex();
        }
    }

    /**
     * @return Vertex array that holds every map location (including map corners and test vertex)
     * @throws FileNotFoundException if there is no file associated with the vertexPath variable
     */
    private Vertex[] getVertexData() throws FileNotFoundException {
        Vertex[] result = new Vertex[10];
        Scanner reader = new Scanner(new File(vertexPath));
        int index = 0;
        String temp;
        while (reader.hasNextLine()) {
            temp = reader.nextLine();
            if (temp.startsWith(String.valueOf(index))) {
                String[] arguments = temp.split(" ", 5);
                int id = Integer.parseInt(arguments[0]);
                String label = arguments[1];
                int x = Integer.parseInt(arguments[2]);
                int y = Integer.parseInt(arguments[3]);
                int nameSize = arguments[4].length();
                String name = arguments[4].substring(1, nameSize-1);

                result[index++] = new Vertex(id, label, x, y, name);
                if (index >= result.length) {
                    Vertex[] tempVertices = result;
                    result = new Vertex[index * 2];
                    for (int i = 0; i < index; i++) {
                        result[i] = tempVertices[i];
                    }
                }
            }
        }
        reader.close();
        if (index == result.length) return result;
        Vertex[] tempVertices = result;
        result = new Vertex[index];
        for (int i = 0; i < index; i++) {
            result[i] = tempVertices[i];
        }
        return result;
    }

    /**
     * @return Edge array that holds every path between two map locations (including map boundaries)
     * @throws FileNotFoundException if there is no file associated with the edgePath variable
     */
    private Edge[] getEdgeData() throws FileNotFoundException {
        Edge[] edges = new Edge[10];
        Scanner reader = new Scanner(new File(edgePath));
        int index = 0;
        String temp;
        while (reader.hasNextLine()) {
            temp = reader.nextLine();
            if (temp.startsWith(String.valueOf(index))) {
                String[] arguments = temp.split(" ", 10);
                int id  = Integer.parseInt(arguments[0]);
                String startVertexLabel = arguments[1];
                String destVertexLabel = arguments[2];
                int startVertexIndex = Integer.parseInt(arguments[3]);
                int destVertexIndex = Integer.parseInt(arguments[4]);
                int length = Integer.parseInt(arguments[5]);
                int angle = Integer.parseInt(arguments[6]);
                String direction = arguments[7];
                String code = arguments[8].substring(1, 2);
                int nameSize = arguments[9].length();
                String name = arguments[9].substring(1, nameSize-1);
                edges[index++] = new Edge(id, startVertexLabel, destVertexLabel, startVertexIndex, destVertexIndex, length, angle, direction, code, name);
                if (index >= edges.length) {
                    Edge[] tempEdges = edges;
                    edges = new Edge[index * 2];
                    for (int i = 0; i < index; i++) {
                        edges[i] = tempEdges[i];
                    }
                }
            }
        }
        reader.close();
        return edges;
    }

    /**
     * @param vertices a Vertex array of every possible map location
     * @return a HashTable that maps vertex labels to their associated id
     */
    private HashTable<String, Integer> mapVertexLabelToIndex(Vertex[] vertices) {
        HashTable<String, Integer> result = new HashTable<String, Integer>();
        for (Vertex v : vertices) {
            result.put(v.getLabel(), v.getId());
        }
        return result;
    }

    /**
     * @param route an Edge array representing a route on the campus
     * @param optimizingTime whether to use time or distance
     * @return the total time/distance of the route
     */
    private double totalPriority(Edge[] route, boolean optimizingTime) {
        double totalPriority = 0;
        for (Edge edge : route) {
            if (edge != null) totalPriority += edge.priority(optimizingTime);
        }
        return totalPriority;
    }

    /**
     * @param route a path/tour of the brandeis campus
     * Writes output to Route.txt, RouteCropped.txt, and RouteEdges.txt
     * @throws FileNotFoundException if any of the three output files do not support a PrintWriter
     */
    public void writeOutput(Edge[] route) throws FileNotFoundException {
        Vertex[] vertices = graph.getVertices();
        int MapWidthFeet = 5521;
        int MapHeightFeet = 4369;
        int MapWidthPixels = 2528;
        int MapHeightPixels = 2000;
        int CropLeft = 150;
        int CropDown = 125;
        PrintWriter routeWriter = new PrintWriter("Route.txt");
        PrintWriter routeCroppedWriter = new PrintWriter("RouteCropped.txt");
        PrintWriter routeEdgesWriter = new PrintWriter("RouteEdges.txt");
        for (Edge edge : route) {
            int a = vertices[edge.getStart()].getX() * MapHeightPixels / MapHeightFeet;
            int b = vertices[edge.getStart()].getY() * MapWidthPixels / MapWidthFeet;
            int c = vertices[edge.getDest()].getX() * MapHeightPixels / MapHeightFeet;
            int d = vertices[edge.getDest()].getY() * MapWidthPixels / MapWidthFeet;
            routeWriter.printf("%d %d %d %d%n", a, b, c, d);
            a -= CropLeft;
            b -= CropDown;
            c -= CropLeft;
            d -= CropDown;
            routeCroppedWriter.printf("%d %d %d %d%n", a, b, c, d);
            routeEdgesWriter.println(edge);
        }
        routeWriter.close();
        routeCroppedWriter.close();
        routeEdgesWriter.close();
    }

    /**
     * @param usePrims whether to use Prims or Kruskals MST algorithm
     * @param optimizingTime whether the MST is minimal with respect to time or distance
     * @return
     */
    public Graph minimumSpanningTree(boolean usePrims, boolean optimizingTime) {
        if (usePrims) {
            return minimumSpanningTreePrims(optimizingTime);
        } 
        return minimumSpanningTreeKruskals(optimizingTime);
    }

    /**
     * @param optimizingTime whether the MST is minimal with respect to time or distance
     * @return a Graph representing an undirected MST of the campus graph (excluding map corners and test vertex)
     */
    private Graph minimumSpanningTreeKruskals(boolean optimizingTime) {
        Vertex[] vertices = graph.getVertices();
        Graph output = new Graph(vertices);
        for (Vertex vertex : vertices){
            vertex.makeSet();
        }
        MinHeap<Edge> minHeap = new MinHeap<>();
        for (Vertex vertex : Arrays.copyOfRange(vertices, 5, vertices.length)){
            for (Edge edge : graph.getOutGoingEdges(vertex)){
                // First 20 edges connect to the corners
                if (edge.getId() < 20){
                    continue;
                }
                // Prevents adding each edge twice, we do not care which side connects
                if (edge.getStart() < edge.getDest()){ 
                    minHeap.add(edge, edge.priority(optimizingTime) + graph.getOppositeEdge(edge).priority(optimizingTime));
                }
            }
        }

        while (minHeap.size() > 0){
            Edge lightEdge = minHeap.poll().getKey();
            Vertex x = graph.getVertex(lightEdge.getStart());
            Vertex y = graph.getVertex(lightEdge.getDest());
            if (x.findSet() != y.findSet()){
                output.addEdge(lightEdge);
                output.addEdge(graph.getOppositeEdge(lightEdge));
                x.union(y);
            }
        }

        return output;
    }

    /**
     * @param optimizingTime whether the MST is minimal with respect to time or distance
     * @return a Graph representing an undirected MST of the campus graph (excluding map corners and test vertex)
     */
    private Graph minimumSpanningTreePrims(boolean optimizingTime) {
        // Ignores the first five elements (black hole and the corners)
        Vertex[] vertices = graph.getVertices();
        Graph output = new Graph(vertices);
        for (Vertex vertex : vertices){
            vertex.setKey(Double.POSITIVE_INFINITY);
            vertex.setParent(null);
        }
        Vertex start = vertices[5]; // Starts with the first vertex after the corners and test vertex
        start.setKey(0);
        MinHeap<Vertex> minHeap = new MinHeap<>();
        for (Vertex vertex : vertices){
            minHeap.add(vertex, vertex.getKey());
        }
        while (minHeap.size() > 0){
            Vertex u = minHeap.poll().getKey();
            if (u.getParent() != null){
                output.addEdge(graph.getEdge(u.getId(), u.getParent().getId()));
                output.addEdge(graph.getEdge(u.getParent().getId(), u.getId()));
            }
            Edge[] edges = graph.getOutGoingEdges(u);
            for (Edge e : edges){
                // First 20 edges connect to the corners
                if (e.getId() < 20){
                    continue;
                }
                Vertex v = graph.getVertex(e.getDest());
                double edgeWeight = e.priority(optimizingTime) + graph.getOppositeEdge(e).priority(optimizingTime);
                if (minHeap.containsKey(v) && edgeWeight < v.getKey()){
                    v.setParent(u);
                    v.setKey(edgeWeight);
                    minHeap.update(v, v.getKey());
                }
            }
        }
        return output;
    }

    /**
     * @param tree a Graph representing a MST of the campus graph
     * @param startVertexID the root of the tree
     * @return an Edge array representing a preorder traversal of tree starting at startVertexID
     */
    public Edge[] preorderTraversal(Graph tree, int startVertexID) {
        Edge[] edges = new Edge[2* (tree.getNumberOfVertices() - 1)];
        preorderHelper(tree, startVertexID, -1, edges, 0);
        return edges;
    }

    /**
     * 
     * @param tree a Graph representing a MST of the campus graph
     * @param vertex the vertex to branch out from
     * @param lastVertex the vertex to not branch back to
     * @param edges the container for the data
     * @param index the index to iterate through the edges
     * @return the next index
     */
    private int preorderHelper(Graph tree, int vertex, int lastVertex, Edge[] edges, int index){
        // Exit condition is only one outgoing edge which goes back to the last vertex
        for (Edge e : tree.getOutGoingEdges(vertex)){
            if (lastVertex == -1 || e.getDest() != lastVertex){
                edges[index] = e;
                index++;
                index = preorderHelper(tree, e.getDest(), vertex, edges, index);
                edges[index] = tree.getOppositeEdge(e);
                index++;
            }
        }
        return index;
    } 

    /**
     * @param tour a tour of the campus that visits every vertex at least once
     * @param optimizingTime whether the tour minimizing time or distance
     * @return a tour in which successive edges are greedily replaced with a single edge 
     * if it is faster/shorter and still reaches every vertex at least once
     */
    public Edge[] replaceSuccessiveEdges(Edge[] tour, boolean optimizingTime) {
        Edge[] tempTour = tour.clone();
        HashTable<Vertex, Integer> visitCounts = new HashTable<>();
        for (Edge e : tempTour){
            Vertex dest = graph.getVertex(e.getDest());
            if (visitCounts.containsKey(dest)){
                visitCounts.put(dest, visitCounts.get(dest) + 1);
            } else {
                visitCounts.put(dest, 1);
            }
        }
        
        int length = tempTour.length;

        for(int i = 0; i < tempTour.length - 1; i++){
            if (tempTour[i] == null){
                continue;
            }
            int next = i + 1;
            while (next < tempTour.length && tempTour[next] == null){
                next++;
            }
            if (next >= tempTour.length){
                break;
            }


            if (graph.getEdge(tempTour[i].getStart(), tempTour[next].getDest()) != null && tempTour[i].priority(optimizingTime) + tempTour[next].priority(optimizingTime) > graph.getEdge(tempTour[i].getStart(), tempTour[next].getDest()).priority(optimizingTime)){
                Vertex v = graph.getVertex(tempTour[i].getDest());
                if (visitCounts.get(v) > 1){
                    visitCounts.put(v, visitCounts.get(v) - 1);
                    tempTour[i] = graph.getEdge(tempTour[i].getStart(), tempTour[next].getDest());
                    tempTour[next] = null;
                    length--;
                }
            }
            
        }
        
        Edge[] output = new Edge[length];
        int index = 0;
        for (int i = 0; i < tempTour.length; i++){
            if (tempTour[i] != null){
                output[index] = tempTour[i];
                index++;
            }
        }
        return output;
    }

    /**
     * @param start the vertexId associated with the starting vertex
     * @param destination the vertexId associated with the destination vertex
     * @param optimizingTime whether the path is minimizing time or distance
     * @return an Edge array representing the fastest/shortest path to get from start to destination
     */
    public Edge[] dijkstra(int start, int destination, boolean optimizingTime) {
        Vertex[] vertices = graph.getVertices();
        MinHeap<Vertex> heap = new MinHeap<>();
        for (Vertex vertex : vertices){
            vertex.setKey(Double.POSITIVE_INFINITY);
            vertex.setParent(null);
            heap.add(vertex, vertex.getKey());
        }
        graph.getVertex(start).setKey(0);
        heap.update(graph.getVertex(start), 0.0);
        while (heap.size() > 0){
            Vertex vertex = heap.poll().getKey();
            if (vertex.getId() == destination){
                return backtrack(destination);
            }
            for (Edge e : graph.getOutGoingEdges(vertex)){
                Vertex dest = graph.getVertex(e.getDest());
                if (dest.getKey() > vertex.getKey() + e.priority(optimizingTime)){
                    dest.setKey(vertex.getKey() + e.priority(optimizingTime));
                    dest.setParent(vertex);
                    heap.update(dest, dest.getKey());
                }
            }
        }
      
        return new Edge[0];
    }

    private Edge[] backtrack(int destination){
        int counter = 0;
        Vertex tracker = graph.getVertex(destination);
        while (tracker.getParent() != null){
            counter++;
            tracker = tracker.getParent();
        }
        Edge[] output = new Edge[counter];
        tracker = graph.getVertex(destination);
        while (tracker.getParent() != null){
            counter--;
            output[counter] = graph.getEdge(tracker.getParent().getId(), tracker.getId());
            tracker = tracker.getParent();
        }
        return output;
    }
}