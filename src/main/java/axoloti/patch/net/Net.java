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
package axoloti.patch.net;

import axoloti.datatypes.DataType;
import axoloti.mvc.AbstractController;
import axoloti.mvc.AbstractModel;
import axoloti.patch.PatchModel;
import axoloti.patch.object.IAxoObjectInstance;
import axoloti.patch.object.inlet.InletInstance;
import axoloti.patch.object.outlet.OutletInstance;
import axoloti.preferences.Theme;
import axoloti.property.DestinationArrayProperty;
import axoloti.property.Property;
import axoloti.property.SourceArrayProperty;
import java.awt.Color;
import java.util.ArrayList;
import java.util.Arrays;
import static java.util.Arrays.asList;
import java.util.List;
import org.simpleframework.xml.ElementList;
import org.simpleframework.xml.Root;

/**
 *
 * @author Johannes Taelman
 */
@Root(name = "net")
public class Net extends AbstractModel {

    private OutletInstance[] sources;
    private InletInstance[] dests;
    boolean selected = false;
    
    PatchModel parent;

    public Net(
            @ElementList(name = "source", inline = true, required = false) List<OutletInstance> source,
            @ElementList(name = "dest", inline = true, required = false) List<InletInstance> dest
    ) {
        if (source == null) {
            this.sources = new OutletInstance[]{};
        } else {
            this.sources = source.toArray(new OutletInstance[]{});
        }
        if (dest == null) {
            this.dests = new InletInstance[]{};
        } else {
            this.dests = dest.toArray(new InletInstance[]{});
        }

        for (OutletInstance o : sources) {
            o.setConnected(true);
        }
        for (InletInstance i : dests) {
            i.setConnected(true);
        }
    }

    @ElementList(name = "source", inline = true, required = false)
    public List<OutletInstance> getSourceList() {
        if (sources == null) {
            return null;
        }
        if (sources.length == 0) {
            return null;
        }
        return asList(sources);
    }

    @ElementList(name = "dest", inline = true, required = false)
    public List<InletInstance> getDestList() {
        if (dests == null) {
            return null;
        }
        if (dests.length == 0) {
            return null;
        }
        return asList(dests);
    }

    public Net() {
        this.sources = new OutletInstance[]{};
        this.dests = new InletInstance[]{};
    }

    public Net(PatchModel parent, OutletInstance[] sources, InletInstance[] dests) {
        this.parent = parent;
        setDocumentRoot(parent.getDocumentRoot());
        this.sources = sources;
        this.dests = dests;
    }

    public void validate() {
        if (sources == null) {
            throw new Error("source is null, empty array required");
        }
        if (dests == null) {
            throw new Error("dest is null, empty array required");
        }
        if (dests.length + sources.length < 2) {
            throw new Error("less than 2 iolets connected, should not exist");
        }
        for (int j = 0; j < dests.length; j++) {
            InletInstance i = dests[j];
            IAxoObjectInstance o = i.getParent();
            if (o == null) continue;
            if (!o.getInletInstances().contains(i)) {
                String inletName = i.getName();
                InletInstance i2 = o.findInletInstance(inletName);
                if (i2 == null) {
                    throw new Error("detached net");
                }
                dests[j] = i2;
            }
        }
        for (int j = 0; j < sources.length; j++) {
            OutletInstance i = sources[j];
            IAxoObjectInstance o = i.getParent();
            if (o == null) continue;
            if (!o.getOutletInstances().contains(i)) {
                String outletName = i.getName();
                OutletInstance i2 = o.findOutletInstance(outletName);
                if (i2 == null) {
                    throw new Error("detached net");
                }
                sources[j] = i2;
            }
        }
    }

    public boolean isValidNet() {
        if (sources == null) {
            return false;
        }
        if (sources.length != 1) {
            return false;
        }
        if (dests == null) {
            return false;
        }
        if (dests.length == 0) {
            return false;
        }
        for (InletInstance s : dests) {
            if (!getDataType().IsConvertableToType(s.getDataType())) {
                return false;
            }
        }
        return true;
    }

    Color GetColor() {
        Color c = getDataType().GetColor();
        if (c == null) {
            c = Theme.getCurrentTheme().Cable_Default;
        }
        return c;
    }

    public DataType getDataType() {
        if (sources == null) {
            return null;
        }
        if (sources.length == 0) {
            return null;
        }
        if (sources.length == 1) {
            return sources[0].getDataType();
        }
        OutletInstance first_outlet = java.util.Collections.min(Arrays.asList(sources));
        DataType t = first_outlet.getDataType();
        return t;
    }

    public String CType() {
        DataType d = getDataType();
        if (d != null) {
            return d.CType();
        } else {
            return null;
        }
    }

    // TODO: migrate NET_SOURCES to ListProperty
    public final static Property NET_SOURCES = new SourceArrayProperty("Sources", Net.class);
    // TODO: migrate NET_DESTINATIONS to ListProperty
    public final static Property NET_DESTINATIONS = new DestinationArrayProperty("Destinations", Net.class);

    public OutletInstance[] getSources() {
        return sources;
    }

    public void setSources(OutletInstance[] sources) {
        OutletInstance[] old_value = this.sources;
        this.sources = sources;
        validate();
        for(OutletInstance o : sources) {
            o.getControllerFromModel().changeConnected(true);
        }
        firePropertyChange(
                NET_SOURCES,
                old_value, sources);
    }

    public InletInstance[] getDestinations() {
        return dests;
    }

    public void setDestinations(InletInstance[] dests) {
        InletInstance[] old_value = this.dests;
        List<InletInstance> dests_list = Arrays.asList(dests);
        if (old_value != null) {
            for (InletInstance i : old_value) {
                if (!dests_list.contains(i)) {
                    i.getControllerFromModel().changeConnected(false);
                }
            }
        }
        this.dests = dests;
        validate();
        for(InletInstance i : dests) {
            i.getControllerFromModel().changeConnected(true);
        }
        firePropertyChange(
                NET_DESTINATIONS,
                old_value, dests);
    }

    @Override
    public List<Property> getProperties() {
        List<Property> l = new ArrayList<>();
        l.add(NET_SOURCES);
        l.add(NET_DESTINATIONS);
        return l;
    }

    @Override
    protected AbstractController createController() {
        return new NetController(this);
    }

    @Override
    public NetController getControllerFromModel() {
        return (NetController) super.getControllerFromModel();
    }

    @Override
    public PatchModel getParent() {
        return parent;
    }

    public void setParent(PatchModel patchModel) {
        parent = patchModel;
    }

}