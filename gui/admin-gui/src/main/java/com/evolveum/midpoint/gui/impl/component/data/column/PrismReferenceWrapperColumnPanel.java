/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.gui.impl.component.data.column;

import com.evolveum.midpoint.web.component.data.column.AjaxLinkPanel;

import org.apache.wicket.ajax.AjaxRequestTarget;
import org.apache.wicket.markup.html.panel.Panel;
import org.apache.wicket.model.IModel;

import com.evolveum.midpoint.gui.api.util.WebComponentUtil;
import com.evolveum.midpoint.gui.impl.component.data.column.AbstractItemWrapperColumn.ColumnType;
import com.evolveum.midpoint.gui.impl.error.ErrorPanel;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismContainerValueWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismReferenceWrapper;
import com.evolveum.midpoint.gui.api.prism.wrapper.PrismValueWrapper;
import com.evolveum.midpoint.prism.Referencable;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;

/**
 * @author katka
 *
 */
public class PrismReferenceWrapperColumnPanel<R extends Referencable> extends AbstractItemWrapperColumnPanel<PrismReferenceWrapper<R>, PrismValueWrapper<R>> {

    private static final long serialVersionUID = 1L;
    private static final Trace LOGGER = TraceManager.getTrace(PrismReferenceWrapperColumnPanel.class);

    PrismReferenceWrapperColumnPanel(String id, IModel<PrismReferenceWrapper<R>> model, ColumnType columnType) {
        super(id, model, columnType);
    }

    @Override
    protected String createLabel(PrismValueWrapper<R> object) {
        return WebComponentUtil.getReferencedObjectDisplayNamesAndNames(object.getRealValue(), false, true);
    }

    @Override
    protected Panel createValuePanel(String id, IModel<PrismReferenceWrapper<R>> model, PrismValueWrapper<R> object) {

        Panel panel;
        try {
            panel = getPageBase().initItemPanel(id, model.getObject().getTypeName(), model, null);
        } catch (SchemaException e) {
            LOGGER.error("Cannot create panel for {}", model.getObject());
            getSession().error("Cannot create panel for: " + model.getObject());
            return new ErrorPanel(id, createStringResource("PropertyPropertyWrapperColumnPanel.cannot.create.panel"));
        }

        return panel;
    }

    @Override
    protected Panel createLink(String id, IModel<PrismValueWrapper<R>> object) {
        AjaxLinkPanel ajaxLinkPanel = new AjaxLinkPanel(id, new IModel<String>() {

            @Override
            public String getObject() {
                return createLabel(object.getObject());
            }

        }) {
            private static final long serialVersionUID = 1L;

            @Override
            public void onClick(AjaxRequestTarget target) {
                PrismReferenceWrapperColumnPanel.this.onClick(target, getModelObject().getParent());
            }

        };
        return ajaxLinkPanel;
    }

    protected void onClick(AjaxRequestTarget target, PrismContainerValueWrapper<?> rowModel) {
    }
}
