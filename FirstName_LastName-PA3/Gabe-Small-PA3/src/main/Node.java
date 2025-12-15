package main;

public class Node {
    public int top;
    public int up, down;

    public Node() {

    }

    public Node(int top, int up, int down) {
        this.top = top;
        this.up = up;
        this.down = down;
    }

    @Override
    public String toString() {
        return String.format("%d %d %d", top, up, down);
    }

    /**
     * @return whether the current Node is a SpacerNode
     */
    public boolean isSpacer() {
        return top == -1;
    }
}
