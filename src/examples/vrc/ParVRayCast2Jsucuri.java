package examples.vrc;

import java.awt.image.BufferedImage;
import java.io.File;
import java.util.ArrayList;
import java.util.List;
import jsucuriinoserialize.*;
import javax.imageio.ImageIO;

/**
 * Created by alexandrenery on 10/26/16. volumes
 * <numWorker> <numrayCastNodes> <imageFile> <imWidth> <imHeight> <numSamples>
 *  8 8 foot.raw 5120 2160 2
 */
public class ParVRayCast2Jsucuri {

    public static void main(String args[]) throws Exception {
        int nx = 256;
        int ny = 256;
        int nz = 256;

        // String filepath = "foot.raw";
        int numWorkers = new Integer(args[0]);
        int numRayCastNodes = new Integer(args[1]);
        String filepath = args[2];
        int imWidth = new Integer(args[3]);
        int imHeight = new Integer(args[4]);
        int samples = new Integer(args[5]).intValue();

        //imWidth = 1280;
        //imHeight = 720;

        System.out.println("numWorkers:" + numWorkers);
        System.out.println("numRayCastNodes:" + numRayCastNodes);
        System.out.println("imWidth:" + imWidth);
        System.out.println("imHeight:" + imHeight);
        System.out.println("Samples:" + samples);

        Point3d eye = new Point3d(-2000.0f, -2000.0f, 2000.0f);
        Point3d lookat = new Point3d(0.0f, -100.0f, 0.0f);
        Point3d min = new Point3d(-1.0f, -1.0f, -1.0f);
        Point3d max = new Point3d(1.0f, 1.0f, 1.0f);

        min.scale(200.0f);
        max.scale(200.0f);

        Camera cam = new Camera(imWidth, imHeight, eye, lookat);

        Grid grid = new Grid(min, max, nx, ny, nz);

        NodeFunction rayCast = new NodeFunction() {
            @Override
            public Object f(Object[] inputs) {
                int threadId = (Integer) inputs[0];
                float[] data = (float[]) inputs[1];
                Camera camera = (Camera) inputs[2];
                int samplesRayCast = (Integer) inputs[3];
                Grid gridRayCast = (Grid) inputs[4];
                int numRayCastNodesRayCast = (Integer) inputs[5];

                float rcp_samples = 1.0f / (float) samplesRayCast;

                float chunckSize = (camera.getWidth() / new Float(numRayCastNodesRayCast));
                int chunck = (int) Math.ceil(chunckSize);

                List<java.awt.Color> colorList = new ArrayList<java.awt.Color>();
                //System.out.println("chunck: " +chunck);
                int start = threadId * chunck;
                int end = start + chunck < camera.getWidth() ? start + chunck : camera.getWidth();
                //System.out.println("threadId: "+threadId + " start: "+start +" end: " + end);
                int numberPixels = chunck * camera.getHeight();

                //System.out.println("Number of pixels: " + numberPixels);
                for (int i = start; i < end; i++) {
                    for (int j = 0; j < camera.getHeight(); j++) {
                        float r, g, b;
                        r = g = b = 0.0f;

                        for (int s = 0; s < samplesRayCast; s++) {
                            Ray ray = camera.get_primary_ray(i, j, samplesRayCast);
                            // Ray ray = get_primary_ray(cam, i, j, samples);

                            // System.out.println("ray: " + ray);

                            Color c = gridRayCast.intersectGrid(ray, data, 1.0f);

                            r += c.r;
                            g += c.g;
                            b += c.b;

                        }

                        r = r * rcp_samples;
                        g = g * rcp_samples;
                        b = b * rcp_samples;

                        float maxColor = Math.max(Math.max(r, g), b);
                        if (maxColor > 1.0f) {
                            r = r / maxColor;
                            g = g / maxColor;
                            b = b / maxColor;
                        }

                        // System.out.println("rgb = " + r + "," + g + "," + b);

                        java.awt.Color color = new java.awt.Color(r, g, b);
                        //System.out.println("size"+ colorList.size());
                        colorList.add(color);

                        //im.setRGB(i, j, c.getRGB());
                    }
                }
                return colorList;
            }
        };

        NodeFunction readImage = new NodeFunction() {
            @Override
            public Object f(Object[] inputs) {
                float[] data = null;
                String filePath = (String) inputs[0];
                int[] dimensions = (int[]) inputs[1];

                int nX = dimensions[0];
                int nY = dimensions[1];
                int nZ = dimensions[2];

                try {
                    data = Util.loadRawFileFloats(filePath, nX * nY * nZ);
                } catch (Exception e) {
                    // TODO Auto-generated catch block
                    e.printStackTrace();
                }
                if (data == null) {
                    return null;
                }
                return data;
            }
        };

        NodeFunction writeImage = new NodeFunction() {
            @Override
            public Object f(Object[] inputs) {
                // System.out.println("prices " + inputs[0]);
                Camera camera = (Camera) inputs[inputs.length - 2];
                int numberRayCastNodes = (Integer) inputs[inputs.length - 1];
                BufferedImage im = new BufferedImage(camera.getWidth(), camera.getHeight(), BufferedImage.TYPE_3BYTE_BGR);
                // (RenderedImage) inputs[0];
                File outputfile = new File("output.png");
                float chunckSize = (camera.getWidth() / new Float(numberRayCastNodes));
                int chunck = (int) Math.ceil(chunckSize);

                int height = camera.getHeight();
                for (int indexNodes = 0; indexNodes < numberRayCastNodes; indexNodes++) {
                    int start = indexNodes * chunck;
                    int end = start + chunck < camera.getWidth() ? start + chunck : camera.getWidth();
                    List<java.awt.Color> colorListPartial = (ArrayList<java.awt.Color>) inputs[indexNodes];
                    //System.out.println("Node: "+ indexNodes + " Start: " + start + " End: " + end);
                    //System.out.println("colorListPartialSize: "+ colorListPartial.size());
                    for (int indexWidth = start, indexColorList = 0; indexWidth < end; indexWidth++, indexColorList++) {
                        for (int indexHeight = 0; indexHeight < height; indexHeight++) {

                            //BufferedImage imPartial = (BufferedImage)inputs[indexNodes];

                        /*int colorInt = imPartial.getRGB(indexHeight, indexWidth);

                        int  r   = (colorInt & 0x00ff0000) >> 16;
                        int  g = (colorInt & 0x0000ff00) >> 8;
                        int  b  =  colorInt & 0x000000ff;
                        java.awt.Color c = new java.awt.Color(r, g, b);*/
                            int index = indexHeight + (height * indexColorList);
                            //System.out.println("index: " +index);
                            java.awt.Color c = colorListPartial.get(index);
                            //System.out.println(indexWidth +" "+ indexHeight);
                            im.setRGB(indexWidth, indexHeight, c.getRGB());
                        }
                    }

                    //System.out.println("chunck: " +chunck);

                }
                //RenderedImage image = im;
                try {
                    ImageIO.write(im, "png", outputfile);
                } catch (Exception e) {
                    e.printStackTrace();
                }

                //ImageIcon imageIcon = new ImageIcon(im);

                //JOptionPane.showMessageDialog(null, imageIcon, "Output image", JOptionPane.PLAIN_MESSAGE);
                return 0;
            }

        };

        DFGraph graph = new DFGraph();
        Scheduler sched = new Scheduler(graph, numWorkers, false);

		/*
		 * BufferedReader fp = null; try { fp = new BufferedReader(new
		 * InputStreamReader(new FileInputStream("text.txt"))); } catch
		 * (FileNotFoundException e) { e.printStackTrace(); }
		 */

        // FilterTagged filter = new FilterTagged(filterPrices, 1);
        Feeder filePathFeeder = new Feeder(filepath);
        int[] dimensions = new int[]{nx, ny, nz};
        Feeder dimensionsFeeder = new Feeder(dimensions);
        Node readImageNode = new Node(readImage, 2);

        Node out = new Node(writeImage, numRayCastNodes + 2);

        graph.add(filePathFeeder);
        graph.add(dimensionsFeeder);
        graph.add(readImageNode);

        graph.add(out);

        filePathFeeder.add_edge(readImageNode, 0);
        dimensionsFeeder.add_edge(readImageNode, 1);

        Feeder cameraFeeder = new Feeder(cam);
        graph.add(cameraFeeder);
        cameraFeeder.add_edge(out, numRayCastNodes);

        Feeder samplesFeeder = new Feeder(samples);
        graph.add(samplesFeeder);

        Feeder gridFeeder = new Feeder(grid);
        graph.add(gridFeeder);

        Feeder numRayCastNodesFeeder = new Feeder(numRayCastNodes);
        graph.add(numRayCastNodesFeeder);
        numRayCastNodesFeeder.add_edge(out, numRayCastNodes + 1);

        List<Node> rayCastNodesList = new ArrayList<Node>();
        List<Feeder> feederNodesList = new ArrayList<Feeder>();
        for (int i = 0; i < numRayCastNodes; i++) {
            feederNodesList.add(new Feeder(i));
            graph.add(feederNodesList.get(i));
            rayCastNodesList.add(new Node(rayCast, 6));
            graph.add(rayCastNodesList.get(i));

            feederNodesList.get(i).add_edge(rayCastNodesList.get(i), 0);
            readImageNode.add_edge(rayCastNodesList.get(i), 1);
            cameraFeeder.add_edge(rayCastNodesList.get(i), 2);
            samplesFeeder.add_edge(rayCastNodesList.get(i), 3);
            gridFeeder.add_edge(rayCastNodesList.get(i), 4);
            numRayCastNodesFeeder.add_edge(rayCastNodesList.get(i), 5);

            rayCastNodesList.get(i).add_edge(out, i);
        }


        System.out.println("Tracing...");
        long time1 = System.currentTimeMillis();

        sched.start();

        long time2 = System.currentTimeMillis();
        System.out.println("Time: " + (time2 - time1) + " ms");
        System.out.println("Time: " + (time2 - time1) / 1000 + " s");
    }
}