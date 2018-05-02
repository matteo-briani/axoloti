/**
 * Copyright (C) 2013 - 2016 Johannes Taelman
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
package axoloti.swingui.objecteditor;

import axoloti.mvc.FocusEdit;
import axoloti.mvc.IView;
import axoloti.object.AxoObject;
import axoloti.object.ObjectController;
import axoloti.object.atom.AtomDefinition;
import axoloti.object.atom.AtomDefinitionController;
import axoloti.property.ListProperty;
import axoloti.property.Property;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.beans.PropertyChangeEvent;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.swing.BoxLayout;
import javax.swing.DefaultCellEditor;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTable;
import javax.swing.ListSelectionModel;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;
import javax.swing.table.AbstractTableModel;

/**
 *
 * @author jtaelman
 * @param <T>
 */
abstract class AtomDefinitionsEditor<T extends AtomDefinition> implements IView {

    private final T[] AtomDefinitionsList;
    private AtomDefinitionController o;
    private final ListProperty prop;
    private final ObjectController objcontroller;
    private final AxoObjectEditor editor;
    private JPanel parentPanel;

    public AtomDefinitionsEditor(ObjectController objcontroller, ListProperty atomfield, T[] atomDefinitionsList, AxoObjectEditor editor) {
        this.objcontroller = objcontroller;
        this.prop = atomfield;
        this.AtomDefinitionsList = atomDefinitionsList;
        this.editor = editor;
    }

    @Override
    public ObjectController getController() {
        return objcontroller;
    }

    abstract String getDefaultName();

    abstract String getAtomTypeName();

    private List<IView> AtomViews = new ArrayList<>();

    private ActionListener actionListenerMoveUp = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            int row = jTable1.getSelectedRow();
            if (row < 1) {
                return;
            }
            FocusEdit focusEdit = new FocusEdit() {

                @Override
                protected void focus() {
                    editor.switchToTab(parentPanel);
                    jTable1.setRowSelectionInterval(row, row);
                }

            };
            getController().addMetaUndo("move " + getAtomTypeName(), focusEdit);

            ArrayList<T> n = new ArrayList<>((List<T>) objcontroller.getModelProperty(prop));
            T elem = n.get(row);
            n.remove(row);
            n.add(row - 1, elem);
            objcontroller.generic_setModelUndoableProperty(prop, n);

            jTable1.setRowSelectionInterval(row - 1, row - 1);
        }
    };

    private ActionListener actionListenerMoveDown = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            int row = jTable1.getSelectedRow();

            FocusEdit focusEdit = new FocusEdit() {

                @Override
                protected void focus() {
                    editor.switchToTab(parentPanel);
                    jTable1.setRowSelectionInterval(row, row);
                }

            };

            getController().addMetaUndo("move " + getAtomTypeName(), focusEdit);

            if (row < 0) {
                return;
            }
            ArrayList<T> n = new ArrayList<>((List<T>) objcontroller.getModelProperty(prop));
            if (row > (n.size() - 1)) {
                return;
            }
            T o = n.remove(row);
            n.add(row + 1, o);
            objcontroller.generic_setModelUndoableProperty(prop, n);

            jTable1.setRowSelectionInterval(row + 1, row + 1);
        }
    };

    private ActionListener actionListenerRemove = new ActionListener() {
        @Override
        public void actionPerformed(ActionEvent e) {
            int row = jTable1.getSelectedRow();
            if (row < 0) {
                return;
            }
            if (jTable1.getRowCount() >= row) {

                FocusEdit focusEdit = new FocusEdit() {

                    @Override
                    protected void focus() {
                        editor.switchToTab(parentPanel);
                        jTable1.setRowSelectionInterval(row, row);
                    }

                };
                getController().addMetaUndo("remove " + getAtomTypeName(), focusEdit);
                ArrayList<T> n = new ArrayList<>((List<T>) objcontroller.getModelProperty(prop));
                n.remove(row);
                objcontroller.generic_setModelUndoableProperty(prop, n);
            }
            if (row > 0) {
                jTable1.setRowSelectionInterval(row - 1, row - 1);
            }
            UpdateTable2();
            parentPanel.revalidate();
        }
    };

    private ActionListener actionListenerAdd = new ActionListener() {

        @Override
        public void actionPerformed(ActionEvent e) {
            try {
                T o2 = (T) AtomDefinitionsList[0].getClass().newInstance();
                int i = 0;
                while (true) {
                    i++;
                    boolean free = true;
                    for (T a : (List<T>) objcontroller.getModelProperty(prop)) {
                        if (a.getName().equals(getDefaultName() + i)) {
                            free = false;
                        }
                    }
                    if (free == true) {
                        break;
                    }
                }
                o2.setName(getDefaultName() + i);
                o2.setParent((AxoObject) objcontroller.getModel());
                FocusEdit focusEdit = new FocusEdit() {

                    @Override
                    protected void focus() {
                        editor.switchToTab(parentPanel);
                        int row = jTable1.getRowCount() - 1;
                        jTable1.setRowSelectionInterval(row, row);
                    }

                };
                getController().addMetaUndo("add " + getAtomTypeName(), focusEdit);
                objcontroller.generic_addUndoableElementToList(prop, o2);
                UpdateTable2();
            } catch (InstantiationException ex) {
                Logger.getLogger(AtomDefinitionsEditor.class.getName()).log(Level.SEVERE, null, ex);
            } catch (IllegalAccessException ex) {
                Logger.getLogger(AtomDefinitionsEditor.class.getName()).log(Level.SEVERE, null, ex);
            }
        }
    };

    void initComponents(JPanel parentPanel) {
        this.parentPanel = parentPanel;
//        this.obj = obj;
        jScrollPane1 = new JScrollPane();
        jTable1 = new JTable();
        jTable1.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jTable1.setVisible(true);
        parentPanel.setLayout(new BoxLayout(parentPanel, BoxLayout.Y_AXIS));
        jScrollPane1.add(jTable1);
        parentPanel.add(jScrollPane1);

        jPanel1 = new JPanel();
        jPanel1.setLayout(new BoxLayout(jPanel1, BoxLayout.X_AXIS));
        parentPanel.add(jPanel1);

        jScrollPane2 = new JScrollPane();
        jTable2 = new JTable();
        jTable2.getSelectionModel().setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        jTable2.setVisible(true);
        parentPanel.setLayout(new BoxLayout(parentPanel, BoxLayout.Y_AXIS));
        jScrollPane2.add(jTable2);
        parentPanel.add(jScrollPane2);

        jButtonAdd = new JButton("Add");
        jButtonAdd.addActionListener(actionListenerAdd);
        jPanel1.add(jButtonAdd);
        jButtonRemove = new JButton("Remove");
        jButtonRemove.addActionListener(actionListenerRemove);
        jPanel1.add(jButtonRemove);
        jButtonMoveUp = new JButton("Move up");
        jButtonMoveUp.addActionListener(actionListenerMoveUp);
        jPanel1.add(jButtonMoveUp);
        jButtonMoveDown = new JButton("Move down");
        jButtonMoveDown.addActionListener(actionListenerMoveDown);
        jPanel1.add(jButtonMoveDown);
        jScrollPane1.setVisible(true);
        jTable1.setModel(new AbstractTableModel() {
            private final String[] columnNames = {"Name", "Type", "Description"};

            @Override
            public int getColumnCount() {
                return columnNames.length;
            }

            @Override
            public String getColumnName(int column) {
                return columnNames[column];
            }

            @Override
            public Class getColumnClass(int column) {
                switch (column) {
                    case 0:
                        return String.class;
                    case 1:
                        return getValueAt(0, column).getClass();
                    case 2:
                        return String.class;
                }
                return null;
            }

            @Override
            public int getRowCount() {
                return ((List<T>) objcontroller.getModelProperty(prop)).size();
            }

            @Override
            public void setValueAt(Object value, int rowIndex, int columnIndex) {
                List<T> list = (List<T>) objcontroller.getModelProperty(prop);
                T atomDefinitionController = list.get(rowIndex);
                if (atomDefinitionController == null) {
                    return;
                }
                FocusEdit focusEdit = new FocusEdit() {

                    @Override
                    protected void focus() {
                        editor.switchToTab(parentPanel);
                        jTable1.setRowSelectionInterval(rowIndex, rowIndex);
                    }

                };
                switch (columnIndex) {
                    case 0: {
                        assert (value instanceof String);
                        getController().addMetaUndo("edit " + getAtomTypeName() + " name", focusEdit);
                        List atomDefinitions = (List) objcontroller.getModelProperty(prop);
                        AtomDefinition m = (AtomDefinition) atomDefinitions.get(rowIndex);
                        m.getControllerFromModel().changeName((String) value);
                    }
                    break;
                    case 1:
                        try {
                            T j = (T) value.getClass().newInstance();
                            j.setName(GetAtomDefinition(rowIndex).getName());
                            j.setDescription(GetAtomDefinition(rowIndex).getDescription());
                            j.setParent((AxoObject) objcontroller.getModel());
                            getController().addMetaUndo("change " + getAtomTypeName() + " type", focusEdit);
                            List<T> n = new ArrayList<>((List<T>) objcontroller.getModelProperty(prop));
                            n.set(rowIndex, j);
                            objcontroller.generic_setModelUndoableProperty(prop, n);
                            UpdateTable2();
                        } catch (InstantiationException ex) {
                            Logger.getLogger(AxoObjectEditor.class.getName()).log(Level.SEVERE, null, ex);
                        } catch (IllegalAccessException ex) {
                            Logger.getLogger(AxoObjectEditor.class.getName()).log(Level.SEVERE, null, ex);
                        }
                        break;
                    case 2: {
                        assert (value instanceof String);
                        getController().addMetaUndo("edit " + getAtomTypeName() + " description", focusEdit);

                        List atomDefinitions = (List) objcontroller.getModelProperty(prop);
                        AtomDefinition m = (AtomDefinition) atomDefinitions.get(rowIndex);
                        m.getControllerFromModel().changeDescription((String) value);
                    }
                    break;
                }
            }

            T GetAtomDefinition(int rowIndex) {
                List<T> list = (List<T>) objcontroller.getModelProperty(prop);
                if (rowIndex < list.size()) {
                    return (T) list.get(rowIndex);
                } else {
                    return null;
                }
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                Object returnValue = null;
                List<T> list = (List<T>) objcontroller.getModelProperty(prop);

                switch (columnIndex) {
                    case 0:
                        returnValue = list.get(rowIndex).getName();
                        break;
                    case 1:
                        returnValue = list.get(rowIndex).getTypeName();
                        break;
                    case 2:
                        returnValue = list.get(rowIndex).getDescription();
                        break;
                }
                return returnValue;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return true;
            }

        });
        JComboBox jComboBoxAtomDefinitionsList = new JComboBox(AtomDefinitionsList);
        jTable1.getColumnModel().getColumn(1).setCellEditor(new DefaultCellEditor(jComboBoxAtomDefinitionsList));
        jTable1.getSelectionModel().addListSelectionListener(new ListSelectionListener() {
            @Override
            public void valueChanged(ListSelectionEvent e) {
                int row = jTable1.getSelectedRow();
                if (row < 0) {
                    jButtonMoveUp.setEnabled(false);
                    jButtonMoveDown.setEnabled(false);
                    jButtonRemove.setEnabled(false);
                    jTable2.removeEditor();
                    UpdateTable2();
                } else {
                    jButtonMoveUp.setEnabled(row > 0);
                    jButtonMoveDown.setEnabled(row < ((List<T>) objcontroller.getModelProperty(prop)).size() - 1);
                    jButtonRemove.setEnabled(true);
                    jTable2.removeEditor();
                    UpdateTable2();
                }
            }
        });

        jTable2.setModel(new AbstractTableModel() {
            private String[] columnNames = {"Property", "Value"};

            @Override
            public int getColumnCount() {
                return columnNames.length;
            }

            @Override
            public String getColumnName(int column) {
                return columnNames[column];
            }

            @Override
            public Class getColumnClass(int column) {
                switch (column) {
                    case 0:
                        return String.class;
                    case 1:
                        return String.class;
                }
                return null;
            }

            @Override
            public int getRowCount() {
                if (properties == null) {
                    return 0;
                }
                return properties.size();
            }

            @Override
            public void setValueAt(Object value, int rowIndex, int columnIndex) {
                if (properties == null) {
                    return;
                }
                if (o == null) {
                    return;
                }
                Property property = properties.get(rowIndex);
                switch (columnIndex) {
                    case 0:
                        assert (false);
                        break;
                    case 1:
                        //Field f = getController().get(rowIndex).fields.get(rowIndex);
                        Class c = property.getType();
                        String svalue = (String) value;
                        Object newValue = property.StringToObj(svalue);
                        int table1row = jTable1.getSelectedRow();
                        o.addMetaUndo("change " + property.getFriendlyName(), new FocusEdit() {

                            @Override
                            protected void focus() {
                                editor.switchToTab(parentPanel);
                                jTable1.setRowSelectionInterval(table1row, table1row);
                                jTable2.setRowSelectionInterval(rowIndex, rowIndex);
                            }
                        });
                        o.generic_setModelUndoableProperty(property, newValue);
                        break;
                }
            }

            @Override
            public Object getValueAt(int rowIndex, int columnIndex) {
                if (properties == null) {
                    return null;
                }
                if (o == null) {
                    return null;
                }
                Property prop = properties.get(rowIndex);
                Object returnValue = null;
                switch (columnIndex) {
                    case 0:
                        returnValue = prop.getFriendlyName();
                        break;
                    case 1: {
                        try {
                            returnValue = prop.getAsString(o.getModel());
                        } catch (IllegalArgumentException ex) {
                            return "illegal argument";
                        }
                    }
                    break;
                }

                return returnValue;
            }

            @Override
            public boolean isCellEditable(int rowIndex, int columnIndex) {
                return (columnIndex == 1);
            }

        });

        jScrollPane1.setViewportView(jTable1);
        jScrollPane2.setViewportView(jTable2);
    }

    private List<Property> properties;

    void UpdateTable2() {
        jButtonRemove.setEnabled(jTable1.getRowCount() > 0);
        List<T> list = (List<T>) objcontroller.getModelProperty(prop);
        int row = jTable1.getSelectedRow();
        if (row != -1 && (row < list.size())) {
            AtomDefinition m = (AtomDefinition) list.get(row);
            o = (AtomDefinitionController) m.getControllerFromModel();
            properties = o.getModel().getEditableFields();
        } else {
            properties = null;
        }
        ((AbstractTableModel) jTable2.getModel()).fireTableDataChanged();
    }

    public void setEditable(boolean editable) {
        jTable1.setEnabled(editable);
        jTable2.setEnabled(editable);
        jButtonMoveUp.setEnabled(editable);
        jButtonMoveDown.setEnabled(editable);
        jButtonRemove.setEnabled(editable);
        jButtonAdd.setEnabled(editable);
    }

    private JScrollPane jScrollPane1;
    private JScrollPane jScrollPane2;
    private JTable jTable1;
    private JTable jTable2;
    private JButton jButtonMoveUp;
    private JButton jButtonMoveDown;
    private JButton jButtonRemove;
    private JButton jButtonAdd;
    private JPanel jPanel1;

    class IView1 implements IView<AtomDefinitionController> {

        final AtomDefinitionController c;

        public IView1(AtomDefinitionController c) {
            this.c = c;
        }

        @Override
        public void modelPropertyChange(PropertyChangeEvent evt) {
            // System.out.println("editor: changed " + evt.getPropertyName());
            if (jTable1 == null) {
                return;
            }
            AbstractTableModel tm = (AbstractTableModel) jTable1.getModel();
            if (tm == null) {
                return;
            }
            tm.fireTableDataChanged();
            UpdateTable2();
        }

        @Override
        public AtomDefinitionController getController() {
            return c;
        }

        @Override
        public void dispose() {
        }

    }

    @Override
    public void modelPropertyChange(PropertyChangeEvent evt) {
        if (prop.is(evt)) {
            for (IView v : AtomViews) {
                v.getController().removeView(v);
            }
            AtomViews.clear();
            List<T> list = (List<T>) evt.getNewValue();
            for (T t : list) {
                IView1 v = new IView1(t.getControllerFromModel());
                t.getControllerFromModel().addView(v);
                AtomViews.add(v);
            }
            if (jTable1 == null) {
                return;
            }
            ((AbstractTableModel) jTable1.getModel()).fireTableDataChanged();
            UpdateTable2();
        }
    }

    @Override
    public void dispose() {
    }

}