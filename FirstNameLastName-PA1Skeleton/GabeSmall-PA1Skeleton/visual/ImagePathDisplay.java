package visual;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.RenderingHints;
import java.awt.Toolkit;
import java.awt.image.BufferedImage;
import java.awt.BorderLayout;
import javax.imageio.ImageIO;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Scanner;

public class ImagePathDisplay extends JPanel{
    private String BrandeisMapImagePath = "BrandeisMapLabeled.jpg";
    private String BrandeisMapCroppedImagePath = "BrandeisMapLabeledCropped.jpg";
    private String BrandeisRoutePath = "Route.txt";
    private String BrandeisCroppedRoutePath = "RouteCropped.txt";
    private BufferedImage[] images;
    private int[][][] pathPoints;
    private int isCropped;

    public ImagePathDisplay() {
        images = new BufferedImage[2];
        isCropped = 0;
        try {
            images = new BufferedImage[]{
                ImageIO.read(new File(BrandeisMapImagePath)),
                ImageIO.read(new File(BrandeisMapCroppedImagePath))};
            pathPoints = new int[][][] {
                readFile(BrandeisRoutePath),
                readFile(BrandeisCroppedRoutePath)};
        } catch (FileNotFoundException e) {
            System.out.println("Failed to load text file: " + e.getMessage());
            return;
        } catch (IOException e) {
            System.out.println("Failed to load image: " + e.getMessage());
            return;
        } catch (IllegalArgumentException e) {
            System.out.println(e.getMessage());
            return;
        }
        // zoomedScale = 0;
    }

    private int[][] readFile(String filename) throws FileNotFoundException {
        int linecount = 0;
        try (Scanner s = new Scanner(new File(filename))) {
            while (s.hasNextLine()) {
                String line = s.nextLine().trim();
                if (line.isEmpty()) continue; 
                String coordinates[] = line.split("\\s+");
                if (coordinates.length != 4) {
                    throw new IllegalArgumentException(
                        "Invalid line format: Expected 4 values but got " + coordinates.length 
                        + " from " + line);
                }
                for (String coordinate : coordinates) {
                    try {
                        Integer.parseInt(coordinate);
                    } catch (NumberFormatException e) {
                        throw new IllegalArgumentException(
                            "Invalid integer value: '" + coordinate + "' in line: " + line);
                    }
                }
                linecount++;
            }
        }

        int[][] result = new int[linecount][4];
        try (Scanner s = new Scanner(new File(filename))) {
            int i = 0;
            while (s.hasNextLine()) {
                String line = s.nextLine().trim();
                if (line.isEmpty()) continue;
                String[] coordinates = line.split("\\s+");
                for (int j=0; j<4; j++) {
                    result[i][j] = Integer.parseInt(coordinates[j]);
                }
                i++;
            }
        }
        return result;
    }

    @Override
    public Dimension getPreferredSize() {
        Dimension screenSize = Toolkit.getDefaultToolkit().getScreenSize();
        int preferredWidth = screenSize.width * 3 / 4; 
        int preferredHeight = screenSize.height * 3 / 4;
        return new Dimension(preferredWidth, preferredHeight);
    }

    @Override
    protected void paintComponent(Graphics g) {
        super.paintComponent(g);
        if (images == null) return;

        Graphics2D g2 = (Graphics2D) g;
        g2.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);

        int panelWidth = getWidth();
        int panelHeight = getHeight();
        int imageWidth = images[isCropped].getWidth();
        int imageHeight = images[isCropped].getHeight();

        double scaleX = (double) panelWidth / imageWidth;
        double scaleY = (double) panelHeight / imageHeight;
        double scale = Math.min(scaleX, scaleY); 

        int scaledWidth = (int) (imageWidth * scale);
        int scaledHeight = (int) (imageHeight * scale);
        int xOffset = (panelWidth - scaledWidth) / 2;
        int yOffset = (panelHeight - scaledHeight) / 2;

        g2.drawImage(images[isCropped], xOffset, yOffset, scaledWidth, scaledHeight, this);
        g2.setColor(Color.magenta);
        g2.setStroke(new BasicStroke(4));

        for (int i = 0; i < pathPoints[isCropped].length - 1; i++) {
            int x1 = (int) (pathPoints[isCropped][i][0] * scale) + xOffset;
            int y1 = (int) (pathPoints[isCropped][i][1] * scale) + yOffset;
            int x2 = (int) (pathPoints[isCropped][i][2] * scale) + xOffset;
            int y2 = (int) (pathPoints[isCropped][i][3] * scale) + yOffset;
            g2.drawLine(x1, y1, x2, y2);
        }
    }

    public static void main(String[] args) {
        JFrame frame = new JFrame();
        ImagePathDisplay panel = new ImagePathDisplay();
        JButton toggleButton = new JButton("Toggle Map");
        toggleButton.addActionListener(e -> {
            panel.isCropped = 1 - panel.isCropped;
            panel.repaint();
        });
        frame.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        frame.add(panel);
        frame.add(toggleButton, BorderLayout.SOUTH);
        frame.pack();  
        frame.setLocationRelativeTo(null); 
        frame.setVisible(true);
    }
}
