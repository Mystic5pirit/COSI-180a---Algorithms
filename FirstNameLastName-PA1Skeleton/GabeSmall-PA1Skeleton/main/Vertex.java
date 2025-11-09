package main;

public class Vertex {
    private int id;
    private String label;
    private int x;
    private int y;
    private String name;
    private Vertex parent;
    private int rank;

    private double key;

    public Vertex(int id, String label, int x, int y, String name) {
        this.id = id;
        this.label = label;
        this.x = x;
        this.y = y;
        this.name = name;
    }

    public void makeSet() {
        this.parent = this;
        this.rank = 0;
    }

    public void union(Vertex y) {
        findSet().link(y.findSet());
    }

    public void link(Vertex y) {
        if (rank > y.rank){
            y.parent = this;
        } else {
            this.parent = y;
            // Only need to check equality if rank is not strictly greater than y.rank
            if (rank == y.rank){
                y.rank++;
            }
        }
    }

    public Vertex findSet() {
        // Set the parent to the representative
        if (parent != this){
            parent = parent.findSet();
        }
        // The parent is now whatever the represntative is
        return parent;
    }

    @Override
    public String toString() {
        return "(" + label + ") " + name;
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Vertex) {
            Vertex other = (Vertex) o;
            return id == other.id;
        }
        return false;
    }

    public String getLabel() {
        return label;
    }

    public int getId() {
        return id;
    }

    public int getX() {
        return x;
    }

    public int getY() {
        return y;
    }

    public Vertex getParent() {
        return parent;
    }

    public void setParent(Vertex parent){
        this.parent = parent;
    }

    public double getKey(){
        return key;
    }

    public void setKey(double key){
        this.key = key;
    }

}
