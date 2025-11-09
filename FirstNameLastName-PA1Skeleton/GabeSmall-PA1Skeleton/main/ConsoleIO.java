package main;
import java.util.Scanner;

import datastructures.HashTable;

public class ConsoleIO {
    private HashTable<String, Integer> map;
    private Scanner scanner;

    public ConsoleIO(HashTable<String, Integer> map) {
        this.map = map;
        scanner = new Scanner(System.in);
    }

    public int getStartVertex() {
        System.out.println("************* WELCOME TO THE BRANDEIS MAP *************");
        return getVertex("Enter start (return to quit): ");
    }

    public int getDestinationVertex(int startVertexID) {
        int destVertexId = getVertex("Enter finish (or return to do a tour): ");
        while (destVertexId == startVertexID) {
            System.out.println("Destination location must be different from starting location");
            destVertexId = getVertex("Enter finish (or return to do a tour): ");
        }
        return destVertexId;
    }

    public boolean getPriorityMetric() {
        System.out.print("Minimize time (y/n - default=n)? ");
        String input = scanner.nextLine().trim().toLowerCase();
        return input.length() > 0 && input.charAt(0) == 'y';
    }

    public boolean getPrims() {
        System.out.print("Enter one of the tour options:\n"  + 
                        "      0: Prim tree\n" + 
                        "      1: (defualt) Kruskal tree\n" +
                        "      => ");
        String input = scanner.nextLine().trim();
        return input.length() > 0 && input.charAt(0) == '0';
    }

    public void outputTour(Edge[] tour, Graph minimumSpanningTree, Vertex[] vertices) {
        System.out.println();
        int numEdges, totalDistance, numberOfVertices;
        numEdges = totalDistance = numberOfVertices = 0;
        double totalTime = 0;
        boolean[] seenVertices = new boolean[vertices.length];
        for (Edge edge : tour) {
            if (edge != null) {
                if (!seenVertices[edge.getStart()]) numberOfVertices++;
                seenVertices[edge.getStart()] = true;
                if (!seenVertices[edge.getDest()]) numberOfVertices++;
                seenVertices[edge.getDest()] = true;
                System.out.println("From: " + vertices[edge.getStart()]);
                if (edge.getName()!=null && edge.getName().length()>0) {
                    System.out.println("On: " + edge.getName());
                }
                int distance = edge.priority(false).intValue();
                System.out.println(
                    String.format("Walk %s in direction %d degrees %s", 
                                formattedDistance(distance), edge.getAngle(), edge.getDir())
                );
                double time = edge.priority(true).doubleValue();
                System.out.println("To: " + vertices[edge.getDest()]);
                System.out.println("(" + formattedTime(time) + ")\n");
                
                totalDistance += distance;
                totalTime += time;
                numEdges++;
            }
        }

        if (minimumSpanningTree != null) {
            double treeTime = 0;
            double treeDistance = 0;
            int treeEdges = 0;
            for (Vertex v : minimumSpanningTree.getVertices()) {
                for (Edge edge : minimumSpanningTree.getOutGoingEdges(v)) {
                    if (edge != null) {
                        treeTime += edge.priority(true);
                        treeDistance += edge.priority(false);
                        treeEdges++;
                    }
                }
            }
            System.out.println(String.format(
                "Pre-order traversal of tree, %d vertices and %d edges:\n" +
                "      Total distance: %.0f feet (%s)\n" +
                "      Total time: %.0f seconds (%s) (%.2f hours)\n", 
                minimumSpanningTree.getNumberOfVertices(), treeEdges,
                treeDistance, formattedDistance((int)treeDistance), 
                treeTime, formattedTime(treeTime), treeTime/3600)
            );

            System.out.println(String.format(
                "Approximate Hamilton tour, %d vertices and %d edges:\n" + 
                "      Total distance: %d feet (%s)\n" + 
                "      Total time: %.0f seconds (%s) (%.2f hours)\n" + 
                "      *** Distance saved over tree traversal = %.0f feet\n" + 
                "      *** Time saved over tree traversal = %.1f minutes\n",
                numberOfVertices, numEdges, totalDistance, formattedDistance(totalDistance),
                totalTime, formattedTime(totalTime), totalTime/3600, treeDistance - totalDistance,
                (treeTime - totalTime) / 60));
        }

        System.out.println(
            String.format("legs = %d, distance = %s, time = %s\n\n", 
                        numEdges, formattedDistance(totalDistance), formattedTime(totalTime))
        );
    }

    private String formattedTime(double time) {
        if (time >= 60) {
            return String.format("%.1f minutes", time/60);
        } else if (time > 59) {
            return "59 seconds";          
        } else {
            return String.format("%.0f seconds", time);
        }
    }

    private String formattedDistance(int distance) {
        if (distance >= 5280) {
            return String.format("%.1f miles", (double) distance / 5280);
        } else {
            return String.format("%d feet", distance);
        }
    }

    public void outputPath(Edge[] path, Vertex[] vertices) {
        outputTour(path, null, vertices);
    }

    private int getVertex(String prompt) {
        int vertexId = -2;
        boolean validInput = false;
        while (!validInput) {
            System.out.print(prompt);
            String input = scanner.nextLine().trim();
            if (input.isEmpty()) {
                vertexId = -1;
                validInput = true;
            } else if (map.containsKey(input)) {
                vertexId = map.get(input);
                validInput = true;
            } else {
                System.out.println("Location not found. Please try again.");
            }
        }
        return vertexId;

    }
}
