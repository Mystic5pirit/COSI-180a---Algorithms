package main;

public class Edge {
    private int id;
    private String startVertexLabel;
    private String destVertexLabel;
    private int startVertexID;
    private int destVertexID;
    private int length;
    private int angle;
    private String direction;
    private String code;
    private String name;

    private double factor;

    public Edge(int id, String startVertexLabel, String destVertexLabel, int startVertexID, int destVertexID, int length, int angle, String direction, String code, String name) {
        this.id = id;
        this.startVertexLabel = startVertexLabel;
        this.destVertexLabel = destVertexLabel;
        this.startVertexID = startVertexID;
        this.destVertexID = destVertexID;
        this.length = length;
        this.angle = angle;
        this.direction = direction;
        this.code = code;
        this.name = name;

        switch(this.code){
            case "f":
                this.factor = 1.0;
            case "u":
                this.factor = 0.9;
            case "d":
                this.factor = 1.1;
            case "s":
                this.factor = 0.5;
            case "t":
                this.factor = 0.9;
            case "b":
                this.factor = 1.0;
            case "x":
                this.factor = 1.0;
        }


    }

    public Double priority(boolean optimizingTime) {
        if (!optimizingTime){
            return (double)length;
        } else {
            double WalkSpeed = 272;
            return (length / (WalkSpeed * factor)) * 60;
        }
    }

    public int getStart() {
        return this.startVertexID;
    }

    public int getDest() {
        return this.destVertexID;
    }

    public int getAngle() {
        return this.angle;
    }

    public String getDir() {
        return this.direction;
    }

    public String getName() {
        return this.name;
    }

    public int getId() {
        return id;
    }

    @Override
    public String toString() {
        return String.format(
            "%d - (%d,%d) [%s -> %s] %s", 
            id, startVertexID, destVertexID, 
            startVertexLabel, destVertexLabel, name
        );
    }

    @Override
    public boolean equals(Object o) {
        if (o instanceof Edge) {
            Edge other = (Edge) o;
            return id == other.id;
        }
        return false;
    }
}
