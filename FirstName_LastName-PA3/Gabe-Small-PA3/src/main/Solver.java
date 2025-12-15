package main;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.PrintWriter;
import java.util.HashMap;
import java.util.Scanner; // To prevent checking against the header for every index, still possible without this


public class Solver {
    private Item[] itemsList;
    private Node[] nodesList; 
    private int[] solution; 
    private PrintWriter output;
    private final String OUTPUTFILEPATH = "data/solution.txt";

    private HashMap<String,Integer> labelMap;


    public static void main(String[] args) {
        String inputFilePath = "data/exactCover.txt";
        try {
            Solver solver = new Solver();
            solver.readInput(inputFilePath);
            solver.solve(10);
        } catch (FileNotFoundException e) {
			System.err.println(e.getMessage());
        }
        
    }

    /**
     * Initializes a Solver instance. 
     */
    public Solver() {
    }

    /**
     * Reads in a txt file containing a properly formatted exact cover problem and sets up U and F appropriately
     * @param inputFilePath
     * @throws FileNotFoundException
     */
    public void readInput(String inputFilePath) throws FileNotFoundException {
        Scanner scan = new Scanner(new File(inputFilePath));

        // First line - Labels filling in U
        String[] labels = scan.nextLine().split(" ");
        labelMap = new HashMap<>(labels.length);
        itemsList = new Item[labels.length + 1];
        itemsList[0] = new Item(null, labels.length, 1);
        for (int i = 1 ; i <= labels.length ; i++){
            itemsList[i] = new Item(labels[i-1], (i-1), (i+1)%(labels.length+1));
            labelMap.put(labels[i-1], i);
        }

        // Filling in first line of F
        nodesList = new Node[labels.length + 1];
        nodesList[0] = null;
        int FLength = 0;
        for (int i = 1; i <= labels.length; i++){
            nodesList[i] = new Node(0, i, i);
            FLength++;
        }

        // The number of options
        int optionCount = 0;

        // The index of the first OptionNode in the option before the spacerNode
        int lastFirst = -1;
        // Goes through each row
        while(scan.hasNext()){
            optionCount++;

            String[] options = scan.nextLine().split(" ");

            // Adjust size as needed
            if (nodesList.length <= FLength + options.length + 1){
                Node[] temp = new Node[nodesList.length * 2];
                System.arraycopy(nodesList, 0, temp, 0, FLength + 1);
                nodesList = temp;
            }

            // Insert SpacerNode
            FLength++;
            nodesList[FLength] = new Node(-1, lastFirst, FLength + options.length);
            lastFirst = FLength + 1;

            // Insert each OptionNode
            for (String label : options){
                FLength++;
                int top = labelMap.get(label);
                nodesList[FLength] = new Node(top, nodesList[top].up, top);

                // Adjust adjacent nodes
                nodesList[nodesList[top].up].down = FLength;
                nodesList[top].up = FLength;
                nodesList[top].top++;
            }
        }
        // Insert final spacer node (the in between are already taaken care of)
        if (nodesList.length <= FLength + 1){
                Node[] temp = new Node[nodesList.length + 1];
                System.arraycopy(nodesList, 0, temp, 0, FLength+1);
                nodesList = temp;
            }
            FLength++;
            nodesList[FLength] = new Node(-1, lastFirst, -1);


        solution = new int[optionCount];
        // Close because good practice/To stop VSCode from yelling at me
        scan.close();   
    }

	/**
     * Solves the exact cover problem and writes the covers to designated output file
	 * @param maxSolutions - number of covers written to output file, if negative it writes all covers
	 */
    public void solve(int maxSolutions) throws FileNotFoundException {

        output = new PrintWriter(OUTPUTFILEPATH);
        
        if (maxSolutions > 0){
            this.maxSolutions = maxSolutions;
        } else {
            this.maxSolutions = Integer.MAX_VALUE;
        }
        this.foundSolutions = 0;
        findCovers(0);  
        
        output.close();
    }

    private int maxSolutions;
    private int foundSolutions;
    /**
     * Solves the exact cover problem and writes all covers to designated output file
     * @param level - the depth of the recursion
     */
    private void findCovers(int level) {
        // 1. If already found maxSolution covers then return
        if (foundSolutions > maxSolutions){
            return;
        }       

        // 2. If U is empty
        // 3.   outputSolution(level)
        if (itemsList[0].right == 0){ // Do not need to check right and left, as if one is to itself then the other is too
            foundSolutions++;
            outputSolution(level);
        }

        // 4. i = item from U with minimum options associated with it
        int minItem = mrv();

        // 5. If i has zero options
        // 6.   return
        if (minItem == 0 || nodesList[minItem].top == 0){
            return;
        }

        // 7. cover(i)
        cover(minItem);

        // 8. For each option in F that has i
        int currNode = nodesList[minItem].down;
        while (currNode != minItem){
            // 9.   For each item j in option (where j != i)
            // 10.      cover(j)
            int q = currNode;
            // Go back until the start of the option
            while (!nodesList[q].isSpacer()) {
                q--;
            }
            q++;
            // Go to the end of the option
            while (!nodesList[q].isSpacer()) {
                if (q != currNode){
                    cover(nodesList[q].top);
                }
                q++;
            }
            
            // 11.  Solution[level] = index of some node in current option
            solution[level] = --q;

            // 12.  solve(level + 1)
            findCovers(level + 1);

            // 13.  For each item j in option (where j != i) // Note this is in reverse order to line 6.
            // 14.      uncover(j)
            // We are already at the end op the option so we don't need to move to the end
            while (!nodesList[q].isSpacer()){
                if (q != currNode){
                    uncover(nodesList[q].top);
                }                q--;
            }

            currNode = nodesList[currNode].down;
        }

        // 15. uncover(i)
        uncover(minItem);
    }
    
    /**
     * @return the index of an uncovered Item in U with the minimum number of options
     */
    public int mrv() {
        int minItem = itemsList[0].right;
        int currItem = itemsList[0].right;
        while (currItem != 0){
            int currIndex = labelMap.get(itemsList[currItem].name);
            if (nodesList[currIndex].top < nodesList[minItem].top){
                minItem = currIndex;
            }
            currItem = itemsList[currItem].right;
        }
        return minItem;
    }

    /**
     * Hides all options that contain the item, then removes the item from U
     * (note because the item is removed it doesn't affect nodes in the item's vertical list)
     * @param item - index of the item to be covered
     */
    public void cover(int item) {
        int p = nodesList[item].down;
        while (p != item) { // Follow up note, basically we never hide(item)
            hide(p);
            p = nodesList[p].down;
        }
        itemDelete(item);
    }

    /**
     * Restores the item back into U, then unhides all options that contain the item
     * (similar to cover, the nodes in the item's vertical list are unchanged)
     * @param item - index of the item to be uncovered
     */
    public void uncover(int item) {
        itemInsert(item);
        int p = nodesList[item].up;
        while (p != item) { // Follow up note, basically we never unhide(item)
            unhide(p);
            p = nodesList[p].up;
        }
    }

    /**
     * Removes all nodes associated with node p from their respective vertical lists 
     * (consequently reducing the length associated with the item node) 
     * Note that it doesn't remove p from its vertical list as a consequence of how cover is implemented
     * @param p - index of the OptionNode to hide
     */
    public void hide(int p) {
        int q = p + 1;
        while (q != p) {
            if (nodesList[q].isSpacer()) {
                q = nodesList[q].up;
            } else {
                delete(q);
                q++;
            }
        }
    }

    /**
     * Inserts back all nodes associated with node p from their respective vertical lists 
     * (consequently increasing the length associated with the item node) 
     * Note that the node at index p is unchanged
     * @param p - index of the OptionNode to unhide
     */
    public void unhide(int p) {
        int q = p - 1;
        while (q != p) {
            if (nodesList[q].isSpacer()) {
                q = nodesList[q].down;
            } else {
                insert(q);
                q--;
            }
        }
    }

	/**
	 * Removes the item from its horizontal doubly linked list
	 * @param item
	 */
    public void itemDelete(int item) {
        int l = itemsList[item].left;
        int r = itemsList[item].right;
        itemsList[l].right = r;
        itemsList[r].left = l;
    }

	/**
	 * Inserts the item back into its horizontal doubly linked list (undoes the effect of itemDelete)
	 * @param item
	 */
    public void itemInsert(int item) {
        int l = itemsList[item].left, r = itemsList[item].right;
        itemsList[r].left = itemsList[l].right = item;
    }

	/**
	 * Deletes the OptionNode from its vertical doubly linked list, 
	 * subsequently decrementing the number of options in its ItemHeaderNode
	 * @param node
	 */
    public void delete(int node) {
        int x = nodesList[node].top;
        int u = nodesList[node].up;
        int d = nodesList[node].down;
        nodesList[u].down = d;
        nodesList[d].up = u;
        nodesList[x].top--;
    }

	/**
	 * Inserts the OptionNode back into its vertical doubly linked list,
	 * subsequently incrementing the number of options in its ItemHeaderNode
	 * @param node
	 */
    public void insert(int node) {
        int x = nodesList[node].top;
        int u = nodesList[node].up;
        int d = nodesList[node].down;
        nodesList[u].down = node;
        nodesList[d].up = node;
        nodesList[x].top++;
    }

    /**
     * Writes formatted solution to designated output file
     * @param level - number of options in current solution
     */
    public void outputSolution(int level){
        output.println("Solution #" + foundSolutions);
        for (int i = 0 ; i < level ; i++){
            String outputStr = "";
            int q = solution[i];
            while (!nodesList[q].isSpacer()) {
                outputStr += itemsList[nodesList[q].top].name;
                outputStr += " ";
                q--;
            }
            output.println(outputStr.substring(0, outputStr.length() - 1));
        }        
    }
}
