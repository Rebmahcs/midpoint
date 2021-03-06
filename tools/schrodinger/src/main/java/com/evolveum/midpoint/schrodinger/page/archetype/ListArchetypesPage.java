/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.schrodinger.page.archetype;

import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListPage;
import com.evolveum.midpoint.schrodinger.component.assignmentholder.AssignmentHolderObjectListTable;
import com.evolveum.midpoint.schrodinger.component.common.table.TableWithPageRedirect;
import com.evolveum.midpoint.schrodinger.component.table.TableHeaderDropDownMenu;

/**
 * Created by Viliam Repan (lazyman).
 */
public class ListArchetypesPage extends AssignmentHolderObjectListPage<AssignmentHolderObjectListTable, ArchetypePage> {

    @Override
    public AssignmentHolderObjectListTable<ListArchetypesPage, ArchetypePage> table() {
        return new AssignmentHolderObjectListTable<ListArchetypesPage, ArchetypePage>(this, getTableBoxElement()) {
            @Override
            protected  <P extends TableWithPageRedirect<ListArchetypesPage>> TableHeaderDropDownMenu<P> clickHeaderActionDropDown() {
                return null;
            }

            @Override
            public ArchetypePage getObjectDetailsPage() {
                return new ArchetypePage();
            }
        };
    }

    @Override
    public ArchetypePage getObjectDetailsPage() {
        return new ArchetypePage();
    }
}
