package algorithmsAndDataStructures;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.time.Duration;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Deque;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Scanner;
import java.util.concurrent.ThreadLocalRandom;

/**
 * This object is a Graph consisting of Vertices and Edges.
 * 
 * @author Cameron Hudson
 * @date 2018-03-20
 */
public class Graph extends HashMap<Integer, Vertex> {

    // Declare a serialVersionUID to make the compiler happy.
    private static final long serialVersionUID = -3326285451301575854L;

    // Initialize constants.
    private final double MAXPATH = 1000000;

    // Declare private variables.
    private boolean isDirected;
    private Deque<Integer> finishingOrder = new ArrayDeque<>();
    private boolean sccStep1;
    private int currentScc;
    private Map<Integer, Integer> sccGroups = new HashMap<Integer, Integer>();

    /**
     * Constructs a graph without arguments.
     */
    public Graph() {
    }

    /**
     * Constructs a Graph from a text file containing an edge or adjacency list.
     * 
     * @param filename The relative path (including filename and extension) of the text file.
     * @param inputFormat Options = "adjacency list", "edge list", "edge list with header".
     * @param isDirected Whether or not this Graph is a directed graph.
     */
    public Graph(String filename, String inputFormat, boolean isDirected, boolean preReciprocated) {
        System.out.println("Importing graph...");

        this.isDirected = isDirected;

        // Read input from file, otherwise catch IOException.

        try {
            BufferedReader inputBR = new BufferedReader(new FileReader(new File(filename)));
            String line;
            boolean firstline = true;
            while ((line = inputBR.readLine()) != null) {
                switch (inputFormat) {
                case "adjacency list":
                    this.parseAdjacencyListLine(line);
                    break;
                case "edge list with header":
                    if (firstline) {
                        // Ignore the first line.
                        firstline = false;
                        break;
                    }
                    // no break; proceed to input the edge list as normal;
                case "edge list":
                    this.parseEdgeListLine(line);
                    break;
                default:
                    System.out.println("Error: Unknown file format.");
                    System.out.println("Please use either \"adjacency list\" or \"edge list\".");
                }
            }
            inputBR.close();
        } catch (IOException e) {
            System.out.println("ERROR: File not found!");
        }

        // If this Graph is undirected and the Edges are not already reciprocated, reciprocate the
        // Edges.
        if (!isDirected && !preReciprocated) {
            Graph recipGraph = this.copy(true);
            this.mergeGraphs(recipGraph);
        }

        System.out.println("Graph imported successfully.");
        System.out.println("Total vertices = " + this.size());
        System.out.println("Total edges = " + this.getEdgeCount());
    }

    /**
     * Parses adjacency list info and adds it to this Graph.
     * 
     * @param line The adjacency list data (EG, from a text file).
     */
    private void parseAdjacencyListLine(String line) {

        String vertexDelims = "[\t]+";
        String edgeDelims = "[,]+";

        String[] vertexData = line.split(vertexDelims);
        int originId = Integer.parseInt(vertexData[0]);
        this.putIfAbsent(originId, new Vertex(originId));

        int[] edgeData;
        int weight;
        int targetVertexId;
        Vertex targetVertex;
        for (int i = 1; i < vertexData.length; i += 1) {
            edgeData = Arrays.stream(vertexData[i].split(edgeDelims)).mapToInt(Integer::parseInt)
                    .toArray();

            targetVertexId = edgeData[0];
            this.putIfAbsent(targetVertexId, new Vertex(targetVertexId));
            targetVertex = this.get(targetVertexId);

            weight = (edgeData.length > 1) ? edgeData[1] : 1;

            this.get(originId).add(new Edge(targetVertex, weight));
        }
    }

    /**
     * Parses edge list info and adds it to this Graph.
     *
     * @param line The edge list data (EG, from a text file).
     */
    private void parseEdgeListLine(String line) {

        // Convert the string to an array of integers and read them.
        String delims = "[ ]+";
        int[] lineData = Arrays.stream(line.split(delims)).mapToInt(Integer::parseInt).toArray();
        int v1Id = lineData[0];
        int v2Id = lineData[1];
        double edgeWeight = (lineData.length > 2) ? lineData[2] : 1;

        // Make sure that the vertices exist;
        this.putIfAbsent(v1Id, new Vertex(v1Id));
        this.putIfAbsent(v2Id, new Vertex(v2Id));

        // Add the edge.
        Vertex targetVertex = this.get(v2Id);
        this.get(v1Id).add(new Edge(targetVertex, edgeWeight));
    }

    /**
     * Returns a deep copy of this Graph, with edges reversed if desired.
     *
     * @param reverse Whether or not the edges should be reversed.
     * @return A deep copy of this Graph.
     */
    public Graph copy(boolean reverse) {

        Vertex v1, v2;
        Vertex v1Copy, v2Copy;
        Graph outputGraph = new Graph();
        double weight;

        Iterator<Entry<Integer, Vertex>> it = this.entrySet().iterator();
        Map.Entry<Integer, Vertex> pair;
        while (it.hasNext()) {
            pair = it.next();
            v1 = pair.getValue();

            outputGraph.putIfAbsent(v1.id, new Vertex(v1.id));
            v1Copy = outputGraph.get(v1.id);
            for (Edge e : v1) {
                v2 = e.dest;
                outputGraph.putIfAbsent(v2.id, new Vertex(v2.id));
                v2Copy = outputGraph.get(v2.id);
                weight = e.weight;
                if (reverse) {
                    v2Copy.add(new Edge(v1Copy, weight));
                } else {
                    v1Copy.add(new Edge(v2Copy, weight));
                }
            }
        }
        return outputGraph;
    }

    /**
     * Merges two vertices.
     *
     * @param v1 The id of the first vertex to merge.
     * @param v2 The id of the second vertex to merge.
     */
    public void mergeVertices(int v1Id, int v2Id) {

        Vertex v1 = this.get(v1Id);
        Vertex v2 = this.get(v2Id);

        // Remove all edges connecting the two vertices.
        this.removeSharedEdges(v1, v2);
        this.removeSharedEdges(v2, v1);

        v2.addAll(v1);

        Vertex currentVertex;

        Iterator<Entry<Integer, Vertex>> it = this.entrySet().iterator();
        Map.Entry<Integer, Vertex> pair;
        while (it.hasNext()) {
            pair = it.next();
            currentVertex = pair.getValue();
            for (Edge e : currentVertex) {
                if (e.dest == v1) {
                    e.dest = v2;
                }
            }
        }
        this.remove(v1Id);
    }

    /**
     * Removes all Edges going from v1 to v2.
     *
     * @param v1 The first of the two Vertices.
     * @param v2 The second of the two Vertices.
     */
    private void removeSharedEdges(Vertex v1, Vertex v2) {
        for (Edge e : v1) {
            if (e.dest == v2) {
                v1.remove(e);
            }
        }
    }

    /**
     * Merges some other graph into this Graph.
     *
     * @param otherGraph The graph to be assimilated.
     */
    public void mergeGraphs(Graph otherGraph) {

        int vertexId;
        Iterator<Entry<Integer, Vertex>> it = otherGraph.entrySet().iterator();
        Map.Entry<Integer, Vertex> pair;
        while (it.hasNext()) {
            pair = it.next();
            vertexId = pair.getKey();
            this.putIfAbsent(vertexId, new Vertex(vertexId));
            this.get(vertexId).addAll(otherGraph.get(vertexId));
        }
    }

    /**
     * Returns the minimum cut of this graph.
     *
     * @return The minimum cut of this graph.
     */
    public int getMinCut() {

        int n = this.size();
        int trials = (int) Math.round(Math.pow(n, 2) * Math.log(n));

        String decision = minCutWarning(trials);
        System.out.println("You entered \"" + decision + "\".");
        if (!(decision.equals("Y") || decision.equals("y"))) {
            return -1;
        }

        Graph g;
        int cut;
        int minCut = this.getEdgeCount();
        int currentProg, progPercent = 0;
        System.out.println(
                "|---------------------------------------------PROGRESS---------------------------------------------|");
        for (int i = 0; i < trials; i += 1) {
            g = this.copy(false);
            while (g.size() > 2) {
                g.contractOnce();
            }
            cut = g.getEdgeCount();
            if (cut < minCut) {
                minCut = cut;
            }
            if (minCut == 0) {
                return minCut;
            }

            currentProg = i * 100 / trials + 1;
            if (currentProg > progPercent) {
                progPercent = currentProg;
                System.out.print("|");
            }
        }
        System.out.println("\n");
        return minCut;
    }

    /**
     * Notifies the user of how long the minimum cut subroutine may take.
     *
     * @param trials The number of random cut trials to perform.
     * @return The user's response to confirmation.
     */
    private static String minCutWarning(int trials) {

        LocalTime t1 = LocalTime.parse("13:43", DateTimeFormatter.ofPattern("HH:mm"));
        LocalTime t2 = LocalTime.parse("14:06", DateTimeFormatter.ofPattern("HH:mm"));
        Duration benchmarkTime = Duration.between(t1, t2);
        int benchmarkSize = 200;
        long benchmarkOperations = Math.round(Math.pow(benchmarkSize, 2) * Math.log(benchmarkSize));
        Duration expectedTime = benchmarkTime.multipliedBy(trials / benchmarkOperations);
        String expectedTimeHms = String.format("%d hours, %d minutes, %d seconds",
                expectedTime.toHoursPart(), expectedTime.toMinutesPart(),
                expectedTime.toSecondsPart());

        Scanner kbd = new Scanner(System.in);
        System.out.println("Warning: This process is expected to take approximately "
                + expectedTimeHms.toString() + ".");
        System.out.println("Enter \"Y\" to proceed.");
        String decision = kbd.nextLine();
        kbd.close();

        return decision;
    }

    /**
     * Contracts one randomly selected edge in this Graph.
     */
    private void contractOnce() {

        // Randomly select an edge to contract.
        int randomEdge = ThreadLocalRandom.current().nextInt(1, this.getEdgeCount() + 1);
        Vertex upcomingVertex = null;
        int upcomingEdges = 0;
        Iterator<Entry<Integer, Vertex>> it = this.entrySet().iterator();
        Map.Entry<Integer, Vertex> pair = null;
        while (it.hasNext() && randomEdge > -1) {
            pair = it.next();
            upcomingVertex = pair.getValue();
            upcomingEdges = upcomingVertex.size();
            randomEdge -= upcomingEdges;
        }
        randomEdge += upcomingEdges;

        Vertex vertexToDelete = upcomingVertex;

        int i = 0;
        Vertex vertexToMerge = null;
        for (Edge e : vertexToDelete) {
            if (i == randomEdge)
                vertexToMerge = e.dest;
            i++;
        }

        this.mergeVertices(vertexToDelete.id, vertexToMerge.id);
    }

    /**
     * Returns the total number of edges in this Graph.
     *
     * @return The number of edges in this Graph.
     */
    public int getEdgeCount() {

        int total;
        total = this.values().stream().mapToInt(value -> value.size()).sum();
        return (!this.isDirected) ? total / 2 : total;
    }

    /**
     * Returns the total weight of all of the edges in this Graph.
     * 
     * @return The total weight of all of the edges in this Graph.
     */
    public double getEdgeWeight() {

        double total = this.entrySet().stream()
                .mapToDouble(
                        entry -> entry.getValue().stream().mapToDouble(edge -> edge.weight).sum())
                .sum();
        return (!this.isDirected) ? total / 2 : total;
    }

    /**
     * Prints this Graph in adjacency list format.
     */
    public void print() {
        Iterator<Entry<Integer, Vertex>> it = this.entrySet().iterator();
        Map.Entry<Integer, Vertex> pair;
        while (it.hasNext()) {
            pair = it.next();
            pair.getValue().print();
        }
    }

    /**
     * Returns the number of SCCs in this Graph.
     *
     * @return The number of SCCs in this Graph.
     */
    public int getSccCount() {
        this.findSccs();
        return this.sccGroups.size();
    }

    /**
     * Finds the SCCs in this Graph.
     */
    public void findSccs() {

        if (!this.isDirected) {
            System.out.println(
                    "ERROR: SCC groups are only defined for directed graphs, and this graph is not directed.");
            return;
        }

        // Create the reverse graph.
        System.out.println("Creating reversed graph...");
        boolean reverse = true;
        Graph reversed = this.copy(reverse);

        // Recurse through the reversed graph to create a Stack of Vertices in the descending order
        // of their finishing times.
        System.out.println("Recursing through reversed graph...");
        reversed.resetSeen();
        int id;
        finishingOrder.clear();
        reversed.sccStep1 = true;
        Iterator<Entry<Integer, Vertex>> it = reversed.entrySet().iterator();
        Map.Entry<Integer, Vertex> pair;
        while (it.hasNext()) {
            pair = it.next();
            id = pair.getKey();
            if (!reversed.get(id).seen) {
                reversed.sccRecurse(id);
            }
        }
        this.sccStep1 = false;
        this.finishingOrder = reversed.finishingOrder;

        // Now we recurse through the original (unreversed) graph in the order specified by the
        // Stack in order to determine SCC to which each vertex belongs.
        System.out.println("Recursing through original graph...");
        this.resetSeen();
        this.sccGroups.clear();
        while (finishingOrder.peekFirst() != null) {
            id = finishingOrder.removeFirst();
            if (!this.get(id).seen) {
                this.currentScc = id;
                this.sccRecurse(id);
            }
        }
    }

    /**
     * Resets the "seen" booleans for all Vertices in this Graph.
     */
    private void resetSeen() {
        Iterator<Entry<Integer, Vertex>> it = this.entrySet().iterator();
        Map.Entry<Integer, Vertex> pair;
        while (it.hasNext()) {
            pair = it.next();
            pair.getValue().seen = false;
        }
    }

    /**
     * Recurses depth-first through the graph on which it is called, first populating a Deque of
     * completed vertices, then assigning each to an SCC.
     * 
     * @param startVId The vertex at which to begin depth-first recursion.
     */
    private void sccRecurse(int startVId) {
        Vertex currentVertex = this.get(startVId);
        // System.out.println("Checking vertex " + currentVertex.id + " of " + this.size() + " in
        // inner loop...");
        currentVertex.seen = true;
        Vertex nextV = null;
        for (Edge e : currentVertex) {
            nextV = e.dest;
            if (!nextV.seen) {
                this.sccRecurse(nextV.id);
            }
        }
        if (this.sccStep1) {
            // Add the current Vertex to the Stack of completed vertices.
            this.finishingOrder.addFirst(currentVertex.id);
        } else {
            // Add this Vertex to the current SCC.
            this.sccGroups.putIfAbsent(this.currentScc, 0);
            this.sccGroups.merge(this.currentScc, 1, Integer::sum);

            this.get(currentVertex.id).sccGroup = this.currentScc;
        }
    }

    /**
     * Finds the length of the shortest path from a starting Vertex to each other Vertex.
     *
     * @param startVId The id of the Vertex at which to start.
     * @return The length of the shortest path to each other Vertex.
     */
    public Map<Integer, Double> getShortestPaths(int startVId) {

        Map<Integer, Double> shortestPaths = new HashMap<Integer, Double>();

        Comparator<Vertex> comparator = new CompareVertexByDijkstraScore();
        int initialCapacity = (int) Math.round(Math.log(this.size()));
        PriorityQueue<Vertex> frontier = new PriorityQueue<>(initialCapacity, comparator);

        // Initialize all dijkstra scores to the maximum path length.
        for (Map.Entry<Integer, Vertex> entry : this.entrySet()) {
            entry.getValue().dijkstraScore = this.MAXPATH;
        }

        int currentVertexId;
        Vertex currentVertex;
        Vertex startV = this.get(startVId);

        // Give startV a dijkstra score of 0 and add it to the frontier.
        startV.dijkstraScore = 0;
        frontier.add(startV);

        double tempScore;
        // While there are still Vertices on the frontier,
        while (frontier.size() > 0) {

            // Pull the nearest Vertex from the frontier and record its final score.
            currentVertex = frontier.poll();
            shortestPaths.put(currentVertex.id, currentVertex.dijkstraScore);

            // For each Edge attached to the current Vertex,
            for (Edge e : currentVertex) {

                // If the destination Vertex hasn't already been logged into shortestPaths,
                if (shortestPaths.get(e.dest.id) == null) {

                    // Update the dijkstra score of the destination Vertex if the score has dropped.
                    tempScore = currentVertex.dijkstraScore + e.weight;
                    if (e.dest.dijkstraScore > tempScore) {
                        frontier.remove(e.dest);
                        e.dest.dijkstraScore = tempScore;
                        frontier.add(e.dest);
                    }
                }
            }
        }

        // For each Vertex in the graph,
        for (Map.Entry<Integer, Vertex> entry : this.entrySet()) {
            currentVertexId = entry.getKey();
            currentVertex = entry.getValue();

            // If the Vertex exists in this graph but not in shortestPaths,
            if (shortestPaths.get(currentVertexId) == null) {

                // Create an entry in shortestPaths (with the previously assigned value of MAXPATH).
                shortestPaths.put(currentVertexId, currentVertex.dijkstraScore);
            }
        }
        return shortestPaths;
    }

    /**
     * A comparator that selects the Vertex with the lower dijkstra score.
     * 
     * @author Cameron Hudson
     * @date 2018-03-17
     */
    private class CompareVertexByDijkstraScore implements Comparator<Vertex> {

        /**
         * Returns the difference between the dijkstra scores of two Vertices.
         */
        @Override
        public int compare(Vertex v1, Vertex v2) {
            return (int) Math.round(v1.dijkstraScore - v2.dijkstraScore);
        }
    }

    /**
     * A comparator that selects the Vertex with the lower minimum edge weight.
     * 
     * @author Cameron Hudson
     * @date 2018-03-31
     */
    private class CompareVertexByMstScore implements Comparator<Vertex> {
        /**
         * Returns the difference between MST scores of two Vertices.
         */
        @Override
        public int compare(Vertex v1, Vertex v2) {
            return (int) Math.round(v1.mstScore - v2.mstScore);
        }
    }

    /**
     * Prints the most populated n SCCs, in descending order of population.
     *
     * @param n The number of SCCs to print.
     */
    public void printSccGroups(int n) {

        this.findSccs();
        this.sccGroups.entrySet().stream()
                .sorted(Map.Entry.<Integer, Integer>comparingByValue().reversed()).limit(n)
                .forEach(System.out::println);
    }

    /**
     * Returns the Minimum Spanning Tree (MST) of this Graph (returns null if there isn't one).
     *
     * @return The MST of this Graph.
     */
    public Graph getMST() {

        // The MST will contain a copy of each vertex, but with only the edges that belong to the
        // MST.
        Graph mst = new Graph();

        Comparator<Vertex> comparator = new CompareVertexByMstScore();
        int initialCapacity = (int) Math.round(Math.log(this.size()));
        PriorityQueue<Vertex> frontier = new PriorityQueue<>(initialCapacity, comparator);

        // Initialize all mst scores to the maximum path length.
        for (Map.Entry<Integer, Vertex> entry : this.entrySet()) {
            entry.getValue().mstScore = this.MAXPATH;
        }

        Vertex startV = this.values().stream().findFirst().get();
        mst.put(startV.id, new Vertex(startV.id));

        for (Edge e : startV) {
            /*
             * After much bug diagnosing, I have found that we must store pointers to the Vertices
             * of the *original* Graph ("this" Graph), not to the Vertices of the MST Graph. I tried
             * to do it in terms of the MST graph, but Java began losing Edges from Vertices after
             * about three layers of Vertex -> Edge -> Vertex -> Edge pointer bouncing. Instead, we
             * will always refer to the original Graph to prevent "going deep". This is why we only
             * store pointers to the Vertices of the original Graph in frontier, and why we only
             * store our mstScores in the Vertices of the original Graph.
             */
            if (this.get(e.dest.id).mstScore > e.weight) {
                this.get(e.dest.id).mstScore = e.weight;
            }

            frontier.add(this.get(e.dest.id));
        }

        Vertex currentVertex;
        Edge relevantEdge = null;

        // While there are still Vertices on the frontier,
        while (frontier.size() > 0) {

            // Pull the nearest Vertex from the frontier and add it to the MST.
            currentVertex = frontier.poll();

            relevantEdge = null;
            for (Edge e : currentVertex) {
                if (e.weight == this.get(currentVertex.id).mstScore && mst.containsKey(e.dest.id)) {
                    relevantEdge = e;
                    break;
                }
            }

            mst.put(currentVertex.id, new Vertex(currentVertex.id));

            int sourceVId = relevantEdge.dest.id;
            int destVId = currentVertex.id;

            // Add the edge to the MST. It connects to 2 vertices.
            mst.get(sourceVId).add(new Edge(mst.get(destVId), relevantEdge.weight));
            mst.get(destVId).add(new Edge(mst.get(sourceVId), relevantEdge.weight));

            // For each Edge attached to the current Vertex,
            for (Edge currentEdge : currentVertex) {

                // If the destination Vertex hasn't already been logged into the MST,
                if (!mst.containsKey(currentEdge.dest.id)) {

                    // Attempt to remove the destination Vertex. (This prevents duplicates.)
                    frontier.remove(this.get(currentEdge.dest.id));

                    // If necessary, update the MST score of the destination Vertex if the score has
                    // dropped.
                    if (this.get(currentEdge.dest.id).mstScore > currentEdge.weight) {
                        this.get(currentEdge.dest.id).mstScore = currentEdge.weight;
                    }

                    // Now put the Vertex back into the frontier.
                    frontier.add(this.get(currentEdge.dest.id));
                }
            }
        }

        // For each Vertex in this Graph,
        for (Map.Entry<Integer, Vertex> entry : this.entrySet()) {
            currentVertex = entry.getValue();

            // If the Vertex exists in this graph but not in the MST,
            if (mst.get(currentVertex.id) == null) {

                // Then this Graph has no MST.
                return null;
            }
        }
        return mst;
    }
}
