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
package axoloti.patch.object.outlet;

import axoloti.object.outlet.Outlet;
import axoloti.patch.PatchModel;
import axoloti.patch.object.IAxoObjectInstance;
import axoloti.patch.object.atom.AtomInstance;
import axoloti.patch.object.iolet.IoletInstance;
import org.simpleframework.xml.Attribute;
import org.simpleframework.xml.Root;

/**
 *
 * @author Johannes Taelman
 */
@Root(name = "source")
public class OutletInstance extends IoletInstance<Outlet> {

    @Attribute(name = "outlet", required = false)
    private String outletname;

    public OutletInstance() {
        super();
    }

    public OutletInstance(String objname, String outletname) {
        super(objname);
        this.outletname = outletname;
    }

    public OutletInstance(Outlet outlet, IAxoObjectInstance axoObj) {
        super(outlet, axoObj);
    }

    @Override
    public String getName() {
        return outletname;
    }

    @Override
    public void setName(String outletname) {
        String preVal = this.outletname;
        this.outletname = outletname;
        firePropertyChange(AtomInstance.NAME, preVal, outletname);
    }

    @Override
    public boolean isSource() {
        return true;
    }

    @Override
    public void dispose() {
        IAxoObjectInstance oi = getParent();
        if (oi == null) {
            System.out.println("AxoObjectInstance : not in an object?");
            return;
        }
        PatchModel pm = oi.getParent();
        if (pm == null) {
            System.out.println("AxoObjectInstance : not in a patch?");
            // it is probably in the objectselector, in which case this is not a problem
            return;
        }
        pm.getController().disconnect(this);
    }

}
