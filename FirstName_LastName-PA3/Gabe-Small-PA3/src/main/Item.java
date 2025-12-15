package main;

public class Item {
    public String name;
    public int left, right;

    public Item() {

    }

    public Item(String name, int left, int right) {
        this.name = name;
        this.left = left;
        this.right = right;
    }

    @Override
    public String toString() {
        return name + " " + left + " " + right;
    }
}
