/*
 * Copyright 2018 Andrew Hoffman This source code file is part of HalfNES and is
 * Licensed under the GNU GPL Version 3. See LICENSE file for more detail
 *
 * February 15 2021 - Modified by Parker TenBroeck
 */
package retrosoundplugin.ui;

import javax.swing.*;
import java.awt.*;
import java.awt.image.BufferedImage;

public class DebugUI extends JFrame {
    // StrokeInformer aStrokeInformer = new StrokeInformer();

    private DebugUI.ShowFrame fbuf;
    private int xsize, ysize;
    private DebugUI.Repainter painter = new DebugUI.Repainter();

    public DebugUI(int height, int width) {
        this.xsize = height;
        this.ysize = width;
        fbuf = new DebugUI.ShowFrame();
        fbuf.setIgnoreRepaint(true);
    }

    public void run() {
        this.setTitle("Osc");
        this.setResizable(false);
        this.setDefaultCloseOperation(HIDE_ON_CLOSE);
        this.setContentPane(fbuf);
        this.pack();
        this.setVisible(true);
    }

    public void messageBox(String s) {
        JOptionPane.showMessageDialog(fbuf, s);
    }

    public void setFrame(BufferedImage b) {
        fbuf.nextFrame = b;

        java.awt.EventQueue.invokeLater(painter);
        //do the actual screen update on the event thread, basically all this does is blit the new frame
    }

    public class Repainter implements Runnable {

        public void run() {
            fbuf.repaint();
        }
    }

    public class ShowFrame extends JPanel {

        public BufferedImage nextFrame;

        /**
         *
         */
        public ShowFrame() {
            this.setBounds(0, 0, xsize, ysize);
            this.setPreferredSize(new Dimension(xsize, ysize));
        }

        @Override
        public void paint(final Graphics g) {
            g.drawImage(nextFrame, 0, 0, xsize, ysize, null);
        }
    }
}
