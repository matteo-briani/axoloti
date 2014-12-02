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

import axoloti.Modulation;
import axoloti.Modulator;
import axoloti.Preset;
import axoloti.datatypes.Frac32;
import axoloti.datatypes.Value;
import axoloti.datatypes.ValueFrac32;
import axoloti.object.AxoObjectInstance;
import java.util.ArrayList;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.ElementList;

/**
 *
 * @author Johannes Taelman
 */
public abstract class ParameterInstanceFrac32 extends ParameterInstance<Frac32> {

    @Attribute(name = "value", required = false)
    public double getValuex() {
        return value.getDouble();
    }
    @ElementList(required = false)
    ArrayList<Modulation> modulators;

    final ValueFrac32 value = new ValueFrac32();
    int presetEditActive = 0;

    public ParameterInstanceFrac32(@Attribute(name = "value") double v) {
        value.setDouble(v);
    }

    public ParameterInstanceFrac32() {
        //value = new ValueFrac32();
    }

    abstract double getMin();

    abstract double getMax();

    abstract double getTick();

    public ParameterInstanceFrac32(Parameter<Frac32> param, AxoObjectInstance axoObj1) {
        super(param, axoObj1);
        //value = new ValueFrac32();
    }

    @Override
    public void PostConstructor() {
        super.PostConstructor();
        if (modulators != null) {
            for (Modulation m : modulators) {
                System.out.println("mod amount " + m.getValue().getDouble());
                m.PostConstructor(this);
            }
        }
    }

    @Override
    public Value<Frac32> getValue() {
        return value;
    }

    @Override
    public void setValue(Value<Frac32> value) {
        this.value.setDouble(value.getDouble());
        updateV();
    }

    @Override
    public void applyDefaultValue() {
        if (((ParameterFrac32) parameter).DefaultValue != null) {
            value.setRaw(((ParameterFrac32) parameter).DefaultValue.getRaw());
        }
    }

    @Override
    public void IncludeInPreset() {
        if (presetEditActive > 0) {
            Preset p = GetPreset(presetEditActive);
            if (p != null) {
                return;
            }
            if (presets == null) {
                presets = new ArrayList<Preset>();
            }
            p = new Preset(presetEditActive, value);
            presets.add(p);
        }
        ShowPreset(presetEditActive);
    }

    @Override
    public void ExcludeFromPreset() {
        if (presetEditActive > 0) {
            Preset p = GetPreset(presetEditActive);
            if (p != null) {
                presets.remove(p);
                if (presets.isEmpty()) {
                    presets = null;
                }
            }
        }
        ShowPreset(presetEditActive);
    }

    public void updateModulation(int index, double amount) {
        // existing modulation
        if (modulators == null) {
            modulators = new ArrayList<Modulation>();
        }

        Modulator modulator = axoObj.patch.Modulators.get(index);
        Modulation n = null;
        for (Modulation m : modulators) {
            if (m.source == modulator.objinst) {
                if ((modulator.name == null) || (modulator.name.isEmpty())) {
                    n = m;
                } else {
                    if (modulator.name.equals(m.modName)) {
                        n = m;
                    }
                }
            }
        }
        if (n == null) {
            n = new Modulation();
            modulators.add(n);
        }
        n.source = modulator.objinst;
        n.sourceName = modulator.objinst.getInstanceName();
        n.modName = modulator.name;
        n.getValue().setDouble(amount);
        n.destination = this;
        axoObj.patch.updateModulation(n);
    }

    public ArrayList<Modulation> getModulators() {
        return modulators;
    }

    @Override
    public Parameter getParameterForParent() {
        Parameter p = super.getParameterForParent();
        ((ParameterFrac32) p).DefaultValue = value;
        return p;
    }

    @Override
    public void CopyValueFrom(ParameterInstance p) {
        super.CopyValueFrom(p);
        if (p instanceof ParameterInstanceFrac32) {
            ParameterInstanceFrac32 p1 = (ParameterInstanceFrac32) p;
            modulators = p1.getModulators();
            presets = p1.presets;
            value.setRaw(p1.value.getRaw());
            updateV();
        }
    }

}
