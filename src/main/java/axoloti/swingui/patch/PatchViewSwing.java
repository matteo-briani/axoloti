/**
 * Copyright (C) 2013, 2014, 2015 Johannes Taelman
 *
 * This file is part of Axoloti.
 *
 * Axoloti is free software: you can redistribute it and/or modify it under the
 * terms of the GNU General Public License as published by the Free Software
 * Foundation, either version 3 of the License, or (at your option) any later
 * version.
 *
 * Axoloti is distributed in the hope that it will be useful, but WITHOUT ANY
 * WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR
 * A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License along with
 * Axoloti. If not, see <http://www.gnu.org/licenses/>.
 */
package axoloti.swingui.patch;

import axoloti.abstractui.IAxoObjectInstanceView;
import axoloti.abstractui.IAxoObjectInstanceViewFactory;
import axoloti.abstractui.INetView;
import axoloti.abstractui.PatchView;
import axoloti.abstractui.PatchViewportView;
import axoloti.object.AxoObjects;
import axoloti.patch.PatchController;
import axoloti.patch.PatchModel;
import axoloti.patch.net.NetController;
import axoloti.patch.object.IAxoObjectInstance;
import axoloti.patch.object.ObjectInstanceController;
import axoloti.preferences.Theme;
import axoloti.swingui.patch.net.NetView;
import axoloti.swingui.patch.object.AxoObjectInstanceViewAbstract;
import axoloti.swingui.patch.object.AxoObjectInstanceViewFactory;
import axoloti.utils.Constants;
import axoloti.utils.KeyUtils;
import java.awt.Component;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.ClipboardOwner;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.event.KeyAdapter;
import java.awt.event.KeyEvent;
import java.awt.event.MouseAdapter;
import java.awt.event.MouseEvent;
import java.awt.event.MouseMotionAdapter;
import java.beans.PropertyChangeEvent;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.Action;
import javax.swing.ActionMap;
import javax.swing.InputMap;
import javax.swing.JComponent;
import javax.swing.JPanel;
import javax.swing.KeyStroke;
import javax.swing.TransferHandler;
import org.simpleframework.xml.Root;
import org.simpleframework.xml.Serializer;
import org.simpleframework.xml.core.Persister;

/**
 *
 * @author Johannes Taelman
 */
@Root(name = "patch-1.0")
public class PatchViewSwing extends PatchView {

    class JPanelAbsoluteLayout extends JPanel {

        JPanelAbsoluteLayout() {
            super(null);
        }

        @Override
        public void remove(Component comp) {
            // a null layout does not cause a repaint when removing a component?
            super.remove(comp);
            repaint(comp.getBounds());
        }
    }

    public PatchLayeredPane Layers = new PatchLayeredPane();

    public JPanel objectLayerPanel = new JPanelAbsoluteLayout();
    public JPanel draggedObjectLayerPanel = new JPanelAbsoluteLayout();
    public JPanel netLayerPanel = new JPanelAbsoluteLayout();
    public JPanel selectionRectLayerPanel = new JPanelAbsoluteLayout();

    SelectionRectangle selectionrectangle = new SelectionRectangle();
    Point selectionRectStart;
    Point panOrigin;

    public PatchViewSwing(PatchController patchController) {
        super(patchController);

        Layers.setLayout(null);
        Layers.setSize(Constants.PATCH_SIZE, Constants.PATCH_SIZE);
        Layers.setLocation(0, 0);

        JComponent[] layerComponents = {
            objectLayerPanel, draggedObjectLayerPanel, netLayerPanel,
            selectionRectLayerPanel};
        for (JComponent c : layerComponents) {
            c.setLayout(null);
            c.setSize(Constants.PATCH_SIZE, Constants.PATCH_SIZE);
            c.setLocation(0, 0);
            c.setOpaque(false);
        }

        Layers.add(objectLayerPanel, new Integer(1));
        Layers.add(netLayerPanel, new Integer(2));
        Layers.add(draggedObjectLayerPanel, new Integer(3));
        Layers.add(selectionRectLayerPanel, new Integer(4));

        netLayerPanel.setName("netLayerPanel");
        selectionRectLayerPanel.setName("selectionRectLayerPanel");

        objectLayerPanel.setName(Constants.OBJECT_LAYER_PANEL);
        draggedObjectLayerPanel.setName(Constants.DRAGGED_OBJECT_LAYER_PANEL);

        selectionRectLayerPanel.add(selectionrectangle);
        selectionrectangle.setLocation(100, 100);
        selectionrectangle.setSize(100, 100);
        selectionrectangle.setOpaque(false);
        selectionrectangle.setVisible(false);

        Layers.setSize(Constants.PATCH_SIZE, Constants.PATCH_SIZE);
        Layers.setVisible(true);
        Layers.setBackground(Theme.getCurrentTheme().Patch_Unlocked_Background);
        Layers.setOpaque(true);
        Layers.revalidate();

        Layers.setTransferHandler(TH);

        InputMap inputMap = Layers.getInputMap(JComponent.WHEN_IN_FOCUSED_WINDOW);
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_X,
                KeyUtils.CONTROL_OR_CMD_MASK), "cut");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_C,
                KeyUtils.CONTROL_OR_CMD_MASK), "copy");
        inputMap.put(KeyStroke.getKeyStroke(KeyEvent.VK_V,
                KeyUtils.CONTROL_OR_CMD_MASK), "paste");

        ActionMap map = Layers.getActionMap();
        map.put(TransferHandler.getCutAction().getValue(Action.NAME),
                TransferHandler.getCutAction());
        map.put(TransferHandler.getCopyAction().getValue(Action.NAME),
                TransferHandler.getCopyAction());
        map.put(TransferHandler.getPasteAction().getValue(Action.NAME),
                TransferHandler.getPasteAction());

        Layers.setEnabled(true);
        Layers.setFocusable(true);
        Layers.setFocusCycleRoot(true);
        Layers.addKeyListener(new KeyAdapter() {

            @Override
            public void keyPressed(KeyEvent ke) {
                int xsteps = 1;
                int ysteps = 1;
                if (!ke.isShiftDown()) {
                    xsteps = Constants.X_GRID;
                    ysteps = Constants.Y_GRID;
                }
                if ((ke.getKeyCode() == KeyEvent.VK_SPACE)
                        || ((ke.getKeyCode() == KeyEvent.VK_N) && !KeyUtils.isControlOrCommandDown(ke))
                        || ((ke.getKeyCode() == KeyEvent.VK_1) && KeyUtils.isControlOrCommandDown(ke))) {
                    Point p = Layers.getMousePosition();
                    ke.consume();
                    if (p != null) {
                        ShowClassSelector(p, null, null);
                    }
                } else if (((ke.getKeyCode() == KeyEvent.VK_C) && !KeyUtils.isControlOrCommandDown(ke))
                        || ((ke.getKeyCode() == KeyEvent.VK_5) && KeyUtils.isControlOrCommandDown(ke))) {
                    getController().addObjectInstance(AxoObjects.getAxoObjects().GetAxoObjectFromName(patchComment, null).get(0), Layers.getMousePosition());
                    ke.consume();
                } else if ((ke.getKeyCode() == KeyEvent.VK_I) && !KeyUtils.isControlOrCommandDown(ke)) {
                    Point p = Layers.getMousePosition();
                    ke.consume();
                    if (p != null) {
                        ShowClassSelector(p, null, patchInlet);
                    }
                } else if ((ke.getKeyCode() == KeyEvent.VK_O) && !KeyUtils.isControlOrCommandDown(ke)) {
                    Point p = Layers.getMousePosition();
                    ke.consume();
                    if (p != null) {
                        ShowClassSelector(p, null, patchOutlet);
                    }
                } else if ((ke.getKeyCode() == KeyEvent.VK_D) && !KeyUtils.isControlOrCommandDown(ke)) {
                    Point p = Layers.getMousePosition();
                    ke.consume();
                    if (p != null) {
                        ShowClassSelector(p, null, patchDisplay);
                    }
                } else if ((ke.getKeyCode() == KeyEvent.VK_M) && !KeyUtils.isControlOrCommandDown(ke)) {
                    Point p = Layers.getMousePosition();
                    ke.consume();
                    if (p != null) {
                        if (ke.isShiftDown()) {
                            ShowClassSelector(p, null, patchMidiKey);
                        } else {
                            ShowClassSelector(p, null, patchMidi);
                        }
                    }
                } else if ((ke.getKeyCode() == KeyEvent.VK_A) && !KeyUtils.isControlOrCommandDown(ke)) {
                    Point p = Layers.getMousePosition();
                    ke.consume();
                    if (p != null) {
                        if (ke.isShiftDown()) {
                            ShowClassSelector(p, null, patchAudioOut);
                        } else {
                            ShowClassSelector(p, null, patchAudio);
                        }
                    }
                } else if ((ke.getKeyCode() == KeyEvent.VK_DELETE) || (ke.getKeyCode() == KeyEvent.VK_BACK_SPACE)) {
                    List<ObjectInstanceController> selected = getController().getSelectedObjects();
                    if (!selected.isEmpty()) {
                        getController().addMetaUndo("delete objects");
                        for (ObjectInstanceController o : selected) {
                            getController().delete(o.getModel());
                        }
                    }
                    ke.consume();
                } else if (ke.getKeyCode() == KeyEvent.VK_UP) {
                    getController().addMetaUndo("move up");
                    MoveSelectedAxoObjInstances(Direction.UP, xsteps, ysteps);
                    ke.consume();
                } else if (ke.getKeyCode() == KeyEvent.VK_DOWN) {
                    getController().addMetaUndo("move down");
                    MoveSelectedAxoObjInstances(Direction.DOWN, xsteps, ysteps);
                    ke.consume();
                } else if (ke.getKeyCode() == KeyEvent.VK_RIGHT) {
                    getController().addMetaUndo("move right");
                    MoveSelectedAxoObjInstances(Direction.RIGHT, xsteps, ysteps);
                    ke.consume();
                } else if (ke.getKeyCode() == KeyEvent.VK_LEFT) {
                    getController().addMetaUndo("move left");
                    MoveSelectedAxoObjInstances(Direction.LEFT, xsteps, ysteps);
                    ke.consume();
                }
            }

        });

        Layers.addMouseListener(new MouseAdapter() {
            @Override
            public void mouseClicked(MouseEvent me) {
                if (me.getButton() == MouseEvent.BUTTON1) {
                    getController().SelectNone();
                    if (me.getClickCount() == 2) {
                        ShowClassSelector(me.getPoint(), null, null);
                    } else {
                        if ((osf != null) && osf.isVisible()) {
                            osf.Accept();
                        }
                        Layers.requestFocusInWindow();
                    }
                    me.consume();
                } else {
                    if ((osf != null) && osf.isVisible()) {
                        osf.Cancel();
                    }
                    Layers.requestFocusInWindow();
                    me.consume();
                }
            }

            @Override
            public void mousePressed(MouseEvent me) {
                if (me.getButton() == MouseEvent.BUTTON1) {
                    selectionRectStart = me.getPoint();
                    selectionrectangle.setBounds(me.getX(), me.getY(), 1, 1);
                    selectionrectangle.setVisible(true);

                    Layers.requestFocusInWindow();
                    me.consume();
                } else {
                }
            }

            @Override
            public void mouseReleased(MouseEvent me) {
                if (selectionrectangle.isVisible() || me.getButton() == MouseEvent.BUTTON1) {
                    Rectangle r = selectionrectangle.getBounds();
                    for (IAxoObjectInstanceView o : objectInstanceViews) {
                        Rectangle bounds = new Rectangle(o.getLocation().x, o.getLocation().y, o.getSize().width, o.getSize().height);
                        o.getController().changeSelected(bounds.intersects(r));
                    }
                    selectionrectangle.setVisible(false);
                    me.consume();
                }
            }

        });

        Layers.setVisible(true);

        Layers.addMouseMotionListener(new MouseMotionAdapter() {
            @Override
            public void mouseDragged(MouseEvent ev) {
                if (selectionrectangle.isVisible()) {
                    int x1 = selectionRectStart.x;
                    int y1 = selectionRectStart.y;
                    int x2 = ev.getX();
                    int y2 = ev.getY();
                    int xmin = x1 < x2 ? x1 : x2;
                    int xmax = x1 > x2 ? x1 : x2;
                    int ymin = y1 < y2 ? y1 : y2;
                    int ymax = y1 > y2 ? y1 : y2;
                    int width = xmax - xmin;
                    int height = ymax - ymin;
                    selectionrectangle.setBounds(xmin, ymin, width, height);
                    selectionrectangle.setVisible(true);
                    ev.consume();
                }
            }
        });

        Layers.setDropTarget(dt);
        Layers.setVisible(true);
    }

    public void scrollTo(Rectangle rect) {
        getViewportView().getComponent().scrollRectToVisible(rect);
    }

    TransferHandler TH = new TransferHandler() {
        @Override
        public int getSourceActions(JComponent c) {
            return COPY_OR_MOVE;
        }

        @Override
        public void exportToClipboard(JComponent comp, Clipboard clip, int action) throws IllegalStateException {
            PatchModel p = getSelectedObjects();
            if (p.getObjectInstances().isEmpty()) {
                clip.setContents(new StringSelection(""), null);
                return;
            }
            Serializer serializer = new Persister();
            try {
                ByteArrayOutputStream baos = new ByteArrayOutputStream();
                serializer.write(p, baos);
                StringSelection s = new StringSelection(baos.toString());
                clip.setContents(s, (ClipboardOwner) null);
            } catch (Exception ex) {
                Logger.getLogger(AxoObjects.class.getName()).log(Level.SEVERE, null, ex);
            }
            if (action == MOVE) {
                getController().addMetaUndo("cut");
                for (IAxoObjectInstance o : p.getObjectInstances()) {
                    getController().delete(o);
                }
            }
        }

        @Override
        public boolean importData(TransferHandler.TransferSupport support) {
            return super.importData(support);
        }

        @Override
        public boolean importData(JComponent comp, Transferable t) {
            try {
                if (!isLocked()) {
                    if (t.isDataFlavorSupported(DataFlavor.stringFlavor)) {

                        paste((String) t.getTransferData(DataFlavor.stringFlavor), comp.getMousePosition(), false);
                    }
                }
            } catch (UnsupportedFlavorException ex) {
                Logger.getLogger(PatchViewSwing.class.getName()).log(Level.SEVERE, "paste", ex);
            } catch (IOException ex) {
                Logger.getLogger(PatchViewSwing.class.getName()).log(Level.SEVERE, "paste", ex);
            }
            return true;
        }

        @Override
        protected Transferable createTransferable(JComponent c) {
            return new StringSelection("copy");
        }

        @Override
        public boolean canImport(TransferHandler.TransferSupport support) {
            boolean r = super.canImport(support);
            return r;
        }

    };

    @Override
    public void PostConstructor() {
        Layers.setPreferredSize(new Dimension(Constants.PATCH_SIZE, Constants.PATCH_SIZE));
        ShowPreset(0);
    }

    @Override
    public void setCordsInBackground(boolean b) {
        if (b) {
            Layers.removeAll();
            Layers.add(netLayerPanel, new Integer(1));
            Layers.add(objectLayerPanel, new Integer(2));
            Layers.add(draggedObjectLayerPanel, new Integer(3));
            Layers.add(selectionRectLayerPanel, new Integer(4));
        } else {
            Layers.removeAll();
            Layers.add(objectLayerPanel, new Integer(1));
            Layers.add(netLayerPanel, new Integer(2));
            Layers.add(draggedObjectLayerPanel, new Integer(3));
            Layers.add(selectionRectLayerPanel, new Integer(4));
        }
    }

    void clampLayerSize(Dimension s) {
        if (Layers.getParent() != null) {
            if (s.width < Layers.getParent().getWidth()) {
                s.width = Layers.getParent().getWidth();
            }
            if (s.height < Layers.getParent().getHeight()) {
                s.height = Layers.getParent().getHeight();
            }
        }
    }

    @Override
    public void updateSize() {
        int maxX = 0;
        int maxY = 0;
        for (Component c : objectLayerPanel.getComponents()) {
            Rectangle r = c.getBounds();
            int x = r.x + r.width;
            if (x > maxX) {
                maxX = x;
            }
            int y = r.y + r.height;
            if (y > maxY) {
                maxY = y;
            }
        }
        Dimension s = new Dimension(maxX, maxY);
        clampLayerSize(s);
        if (!Layers.getSize().equals(s)) {
            Layers.setSize(s);
        }
        if (!Layers.getPreferredSize().equals(s)) {
            Layers.setPreferredSize(s);
        }
    }

    @Override
    public void modelPropertyChange(PropertyChangeEvent evt) {
        super.modelPropertyChange(evt);
        if (PatchModel.PATCH_LOCKED.is(evt)) {
            if ((Boolean)evt.getNewValue() == false) {
                Layers.setBackground(Theme.getCurrentTheme().Patch_Unlocked_Background);
            } else {
                Layers.setBackground(Theme.getCurrentTheme().Patch_Locked_Background);
            }
        }
    }

    @Override
    public PatchViewportView getViewportView() {
        return Layers;
    }

    @Override
    public Point getLocationOnScreen() {
        return objectLayerPanel.getLocationOnScreen();
    }

    @Override
    public void requestFocus() {
        Layers.requestFocus();
    }

    @Override
    public void remove(IAxoObjectInstanceView v) {
        objectLayerPanel.remove((AxoObjectInstanceViewAbstract) v);
        objectLayerPanel.repaint(((AxoObjectInstanceViewAbstract) v).getBounds());
    }

    @Override
    public void remove(INetView view) {
        netLayerPanel.remove((NetView) view);
        netLayerPanel.repaint(((NetView) view).getBounds());
    }

    @Override
    public void add(IAxoObjectInstanceView v) {
        if (objectLayerPanel != null) {
            objectLayerPanel.add((AxoObjectInstanceViewAbstract) v);
            v.resizeToGrid();
            updateSize();
            v.repaint();
        }
    }

    @Override
    public void removeAllObjectViews() {
        if (objectLayerPanel != null) {
            objectLayerPanel.removeAll();
        }
    }

    @Override
    public void removeAllNetViews() {
        if (netLayerPanel != null) {
            netLayerPanel.removeAll();
        }
    }

    @Override
    public void add(INetView view) {
        if (netLayerPanel != null) {
            netLayerPanel.add((NetView) view);
        }
    }

    @Override
    public void dispose() {
        super.dispose();
    }

    @Override
    public IAxoObjectInstanceViewFactory getAxoObjectInstanceViewFactory() {
        return AxoObjectInstanceViewFactory.getInstance();
    }

    @Override
    public INetView createNetView(NetController controller, PatchView patchView) {
        return new NetView(controller, (PatchViewSwing) patchView);
    }
}