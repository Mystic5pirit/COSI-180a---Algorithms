package main;
/**
 * This class represents a directed graph using an adjacency list to store edges 
 * 
 * <p> Assumes every vertex in the vertices array is indexed according to their vertexId
 * and that every edge has both endpoints in the vertices array
 */
public class Graph {
    private Vertex[] vertices;
    private int numberOfVertices;
    private Edge[][] adjList;
    private int[] outDegree;
    private boolean[] seenVertices;
    private static final int DEFAULT_OUT_DEGREE = 2;

    public Graph(Vertex[] vertices) {
        this.vertices = vertices;
        this.numberOfVertices = 0;
        this.seenVertices = new boolean[vertices.length];
        this.outDegree = new int[vertices.length];
        this.adjList = new Edge[vertices.length][DEFAULT_OUT_DEGREE];
    }

    public Graph(Vertex[] vertices, Edge[] edges) {
        this(vertices);
        addEdges(edges);
    }

    public void addEdge(Edge edge) {
        if (edge != null) {
            if (!seenVertices[edge.getStart()]) numberOfVertices++;
            seenVertices[edge.getStart()] = true;
            if (!seenVertices[edge.getDest()]) numberOfVertices++;
            seenVertices[edge.getDest()] = true;
            int start = edge.getStart();
            if (outDegree[start] >= adjList[start].length) {
                resizeEdgeList(start);
            }
            adjList[start][outDegree[start]++] = edge;
        }
    }

    public void addEdges(Edge[] edges) {
        if (edges != null) for (Edge edge : edges) addEdge(edge);
    }

    public Vertex getVertex(int vertexId) {
        return vertices[vertexId];
    }

    public Edge[] getOutGoingEdges(Vertex v) {
        return getOutGoingEdges(v.getId());
    }

    public Edge[] getOutGoingEdges(int vertexId) {
        if (outDegree[vertexId] < adjList[vertexId].length) {
            Edge[] result = new Edge[outDegree[vertexId]];
            for (int i=0; i<outDegree[vertexId]; i++) {
                result[i] = adjList[vertexId][i];
            }
            return result;
        }
        return adjList[vertexId];
    }

    public Vertex[] getVertices() {
        return vertices;
    }

    public Edge getEdge(int startId, int destId) {
        for (int i=0; i < outDegree[startId]; i++) {
            Edge edge = adjList[startId][i];
            if (edge.getDest() == destId) {
                return edge;
            }
        }
        return null;
    }

    public Edge getOppositeEdge(Edge edge) {
        return getEdge(edge.getDest(), edge.getStart());
    }

    public int getNumberOfVertices() {
        return numberOfVertices;
    }

    private void resizeEdgeList(int start) {
        Edge[] temp = new Edge[outDegree[start] * 2];
        for (int i = 0; i < outDegree[start]; i++) {
            temp[i] = adjList[start][i];
        }
        adjList[start] = temp;
    }
}
