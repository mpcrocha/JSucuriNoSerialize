/**
 * inputsSobel/$filename outputsSobel/pixalSobel 26 4920
 */
package examples.sobel;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;

public class SobelSerialWitoutGray
{
    // chronoJudge.jpg chronoJudgeSobelado.jpg 10
    // /home/marcos/Dropbox/Mestrado/tese/experimentoSobel/inputsSobel/$filename Outputs/OutputsSobelSerial 26 50
    public static void main(String args[]) throws Exception
    {

        if(args.length < 4)
        {
            System.out.println("usage: java Sobel <input> <output> <startFrame> <finalFrame>");
            return;
        }

        String file_in = args[0];
        String file_out = args[1];

        //float[] pixel;
        //float[] imagem = new float[bi_original.getWidth() * bi_original.getHeight() * 3];
        int startFrame = new Integer(args[2]);
        int finalFrame = new Integer(args[3]);

        for(int i = startFrame; i <= finalFrame; i++) {
            String addNumberbefore = i < 100? "00":i<1000?"0":"";
            String completeFileName = file_in+addNumberbefore+i+".bmp";
            BufferedImage bi_original = ImageIO.read(new File(completeFileName));

            BufferedImage bi_sobel = new BufferedImage(bi_original.getWidth(), bi_original.getHeight(),bi_original.getType());

            //apply sobel

            final int sobel_x[][] = {{-1, 0, 1}, {-2, 0, 2}, {-1, 0, 1}};
            final int sobel_y[][] = {{1, 2, 1}, {0, 0, 0}, {-1, -2, -1}};

            for (int x = 1; x < bi_original.getWidth() - 1; x++) {
                for (int y = 1; y < bi_original.getHeight() - 1; y++) {
                    float c00 = new Color(bi_original.getRGB(x - 1, y - 1)).getComponents(null)[0];
                    float c01 = new Color(bi_original.getRGB(x, y - 1)).getComponents(null)[0];
                    float c02 = new Color(bi_original.getRGB(x + 1, y - 1)).getComponents(null)[0];

                    float c10 = new Color(bi_original.getRGB(x - 1, y)).getComponents(null)[0];
                    float c11 = new Color(bi_original.getRGB(x, y)).getComponents(null)[0];
                    float c12 = new Color(bi_original.getRGB(x + 1, y)).getComponents(null)[0];

                    float c20 = new Color(bi_original.getRGB(x - 1, y + 1)).getComponents(null)[0];
                    float c21 = new Color(bi_original.getRGB(x, y + 1)).getComponents(null)[0];
                    float c22 = new Color(bi_original.getRGB(x + 1, y + 1)).getComponents(null)[0];

                    float g_x = (sobel_x[0][0] * c00) + (sobel_x[0][1] * c01) + (sobel_x[0][2] * c02) + (sobel_x[1][0] * c10) + (sobel_x[1][1] * c11) + (sobel_x[1][2] * c12) + (sobel_x[2][0] * c20) + (sobel_x[2][1] * c21) + (sobel_x[2][2] * c22);

                    float g_y = (sobel_y[0][0] * c00) + (sobel_y[0][1] * c01) + (sobel_y[0][2] * c02) + (sobel_y[1][0] * c10) + (sobel_y[1][1] * c11) + (sobel_y[1][2] * c12) + (sobel_y[2][0] * c20) + (sobel_y[2][1] * c21) + (sobel_y[2][2] * c22);

                    float val = (float) Math.sqrt(g_x * g_x + g_y * g_y);

                    if (val > 0.8f)
                        val = 1.0f;
                    else if (val < 0.2f)
                        val = 0.0f;

                    bi_sobel.setRGB(x, y, new Color(val, val, val).getRGB());
                }
            }

            //write image to disk
            ImageIO.write(bi_sobel, "png", new File(file_out +"_" + i + "_sobel.png"));
        }


    }
}
