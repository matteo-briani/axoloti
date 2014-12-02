/**
 * Copyright (C) 2013, 2014 Johannes Taelman
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
package axoloti.parameters;

import axoloti.Preset;
import axoloti.datatypes.DataType;
import axoloti.datatypes.Value;
import axoloti.object.AxoObjectInstance;
import axoloti.realunits.NativeToReal;
import axoloti.utils.CharEscape;
import components.AssignPresetMenuItems;
import components.LabelComponent;
import components.control.ACtrlComponent;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.ArrayList;
import javax.swing.BoxLayout;
import javax.swing.JCheckBoxMenuItem;
import javax.swing.JMenu;
import javax.swing.JMenuItem;
import javax.swing.JPanel;
import javax.swing.JPopupMenu;
import javax.swing.UIManager;
import javax.swing.event.MouseInputAdapter;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 *
 * @author Johannes Taelman
 */
@Root(name = "param")
public abstract class ParameterInstance<dt extends DataType> extends JPanel {

    @Attribute
    public String name;
    @Attribute(required = false)
    public Boolean onParent;
    protected int index;
    public Parameter<dt> parameter;
    @ElementList(required = false)
    ArrayList<Preset> presets;
    protected boolean needsTransmit = false;
    public AxoObjectInstance axoObj;
//    JLabel lbl;
    LabelComponent valuelbl = new LabelComponent("123456789");
    NativeToReal convs[];
    int selectedConv = 0;

    public ParameterInstance() {
    }

    public ParameterInstance(Parameter<dt> param, AxoObjectInstance axoObj1) {
        super();
        parameter = param;
        axoObj = axoObj1;
        name = parameter.name;
    }

    void UpdateUnit() {
        if (convs != null) {
            valuelbl.setText(convs[selectedConv].ToReal(getValue()));
        }
    }

    public void CopyValueFrom(ParameterInstance p) {
        onParent = p.onParent;
    }

    public void PostConstructor() {
        removeAll();
        setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));

        JPanel lbls = null;
        if ((((parameter.noLabel == null) || (parameter.noLabel == false))) && (convs != null)) {
            lbls = new JPanel();
            lbls.setLayout(new BoxLayout(lbls, BoxLayout.Y_AXIS));
            this.add(lbls);
        }

        if ((parameter.noLabel == null) || (parameter.noLabel == false)) {
            if (lbls != null) {
                lbls.add(new LabelComponent(parameter.name));
            } else {
                add(new LabelComponent(parameter.name));
            }
        }
        if (convs != null) {
            if (lbls != null) {
                lbls.add(valuelbl);
            } else {
                add(valuelbl);
            }
            Dimension d = new Dimension(50, 10);
            valuelbl.setMinimumSize(d);
            valuelbl.setPreferredSize(d);
            valuelbl.setSize(d);
            valuelbl.setMaximumSize(d);
//            valuelbl.setSize(getWidth(), Constants.font.);
            valuelbl.addMouseListener(new MouseInputAdapter() {
                @Override
                public void mouseClicked(MouseEvent e) {
                    selectedConv = selectedConv + 1;
                    if (selectedConv >= convs.length) {
                        selectedConv = 0;
                    }
                    UpdateUnit();
                }
            });
            UpdateUnit();
        }
//        if (axoObj.patch != null)
//            ShowPreset(axoObj.patch.presetNo);
    }

    public void applyDefaultValue() {
    }

    public boolean GetNeedsTransmit() {
        return needsTransmit;
    }

    abstract public void IncludeInPreset();

    abstract public void ExcludeFromPreset();

    public byte[] TXData() {
        needsTransmit = false;
        byte[] data = new byte[10];
        data[0] = 'A';
        data[1] = 'x';
        data[2] = 'o';
        data[3] = 'P';
        int tvalue = GetValueRaw();
        data[4] = (byte) tvalue;
        data[5] = (byte) (tvalue >> 8);
        data[6] = (byte) (tvalue >> 16);
        data[7] = (byte) (tvalue >> 24);
        data[8] = (byte) (index);
        data[9] = (byte) (index >> 8);
        return data;
    }

    public Preset GetPreset(int i) {
        if (presets == null) {
            return null;
        }
        for (Preset p : presets) {
            if (p.index == i) {
                return p;
            }
        }
        return null;
    }

    public ArrayList<Preset> getPresets() {
        return presets;
    }

    public Preset AddPreset(int index, Value value) {
        Preset p = GetPreset(index);
        if (p != null) {
            p.value = value;
            return p;
        }
        if (presets == null) {
            presets = new ArrayList<Preset>();
        }
        p = new Preset(index, value);
        presets.add(p);
        return p;
    }

    public void RemovePreset(int index) {
        Preset p = GetPreset(index);
        if (p != null) {
            presets.remove(p);
        }
    }

    public abstract Value<dt> getValue();

    public abstract void setValue(Value<dt> value);

    public void SetValueRaw(int v) {
        getValue().setRaw(v);
        updateV();
    }

    public int GetValueRaw() {
        return getValue().getRaw();
    }

    public void updateV() {
        UpdateUnit();
    }

    public String indexName() {
        //return "PEX_" + axoObj.GetCInstanceName()  + "_" + parameter.name;
        return ("" + index);
    }

    public String getLegalName() {
        return CharEscape.CharEscape(name);
    }

    public String KVPName(String vprefix) {
        return "KVP_" + axoObj.getCInstanceName() + "_" + getLegalName();
    }

    public String PExName(String vprefix) {
        return vprefix + "PExch[" + indexName() + "]";
    }

    public String valueName(String vprefix) {
        return PExName(vprefix) + ".value";
    }

    public String ControlOnParentName() {
        if (axoObj.parameterInstances.size() == 1) {
            return axoObj.getInstanceName();
        } else {
            return axoObj.getInstanceName() + ":" + parameter.name;
        }
    }

    public String variableName(String vprefix, boolean enableOnParent) {
        if ((onParent != null) && (onParent) && (enableOnParent)) {
            return "%" + ControlOnParentName() + "%";
        } else {
            return "parent2->" + PExName(vprefix) + ".finalvalue";
        }
    }

    public String signalsName(String vprefix) {
        return PExName(vprefix) + ".signals";
    }

    public String GetPFunction() {
        return "";
    }

    public String GenerateCodeDeclaration(String vprefix) {
        return "";//("#define " + indexName() + " " + index + "\n");
    }

    public abstract String GenerateCodeInit(String vprefix, String StructAccces);

    public abstract String GenerateCodeMidiHandler(String vprefix);

    void SetPresetState(boolean b) { // OBSOLETE
        if (b) {
            setBackground(Color.yellow);
        } //            setBackground(UIManager.getColor ( "Panel.background" ));
        else {
            setBackground(UIManager.getColor("Panel.background"));
        }
    }

    public abstract void ShowPreset(int i);

    public void setIndex(int i) {
        index = i;
    }

    public int getIndex() {
        return index;
    }

    String GenerateMidiCCCodeSub(String vprefix, Integer MidiCC, String value) {
        if (MidiCC != null) {
            return "        if ((status == %midichannel% + MIDI_CONTROL_CHANGE)&&(data1 == " + MidiCC + ")) {\n"
                    + "            PExParameterChange(&parent2->" + PExName(vprefix) + "," + value + ", 0xFFFD);\n"
                    + "        }\n";
        } else {
            return "";
        }
    }

    public Parameter getParameterForParent() {
        Parameter pcopy = parameter.getClone();
        pcopy.name = ControlOnParentName();
        pcopy.noLabel = null;
        return pcopy;
    }

    public boolean isOnParent() {
        if (onParent == null) {
            return false;
        } else {
            return onParent;
        }
    }

    public void setOnParent(boolean b) {
        if (isOnParent() == b) {
            return;
        }
        if (b) {
            onParent = true;
        } else {
            onParent = null;
        }
    }

    public abstract ACtrlComponent CreateControl();

    MouseListener popupMouseListener = new MouseListener() {

        @Override
        public void mouseClicked(MouseEvent e) {
        }

        @Override
        public void mousePressed(MouseEvent e) {
            if (e.isPopupTrigger()) {
                doPopup();
                e.consume();
            }
        }

        @Override
        public void mouseReleased(MouseEvent e) {
            if (e.isPopupTrigger()) {
                doPopup();
                e.consume();
            }
        }

        @Override
        public void mouseEntered(MouseEvent e) {
        }

        @Override
        public void mouseExited(MouseEvent e) {
        }
    };

    public void doPopup() {
        JPopupMenu m = new JPopupMenu();
        populatePopup(m);
        m.show(this, 0, getHeight());
    }

    public void populatePopup(JPopupMenu m) {
        final JCheckBoxMenuItem m_onParent = new JCheckBoxMenuItem("parameter on parent");
        m_onParent.setSelected(isOnParent());
        m.add(m_onParent);
        m_onParent.addActionListener(new ActionListener() {
            @Override
            public void actionPerformed(ActionEvent ae) {
                setOnParent(m_onParent.isSelected());
            }
        });

        JMenu m_preset = new JMenu("Preset");
        new AssignPresetMenuItems(this, m_preset);
        m.add(m_preset);

    }
}
