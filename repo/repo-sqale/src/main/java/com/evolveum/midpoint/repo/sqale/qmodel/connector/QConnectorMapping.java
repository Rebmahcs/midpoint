/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.connector;

import static com.evolveum.midpoint.repo.sqlbase.mapping.item.SimpleItemFilterProcessor.stringMapper;
import static com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType.*;

import com.evolveum.midpoint.repo.sqale.RefItemFilterProcessor;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObject;
import com.evolveum.midpoint.repo.sqale.qmodel.object.QObjectMapping;
import com.evolveum.midpoint.repo.sqlbase.SqlTransformerContext;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ConnectorType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;

/**
 * Mapping between {@link QObject} and {@link ObjectType}.
 */
public class QConnectorMapping
        extends QObjectMapping<ConnectorType, QConnector, MConnector> {

    public static final String DEFAULT_ALIAS_NAME = "con";

    public static final QConnectorMapping INSTANCE = new QConnectorMapping();

    private QConnectorMapping() {
        super(QConnector.TABLE_NAME, DEFAULT_ALIAS_NAME, ConnectorType.class, QConnector.class);

        addItemMapping(F_CONNECTOR_BUNDLE, stringMapper(path(q -> q.connectorBundle)));
        addItemMapping(F_CONNECTOR_TYPE, stringMapper(path(q -> q.connectorType)));
        addItemMapping(F_CONNECTOR_VERSION, stringMapper(path(q -> q.connectorVersion)));
        addItemMapping(F_FRAMEWORK, stringMapper(path(q -> q.framework)));
        addItemMapping(F_CONNECTOR_HOST_REF, RefItemFilterProcessor.mapper(
                path(q -> q.connectorHostRefTargetOid),
                path(q -> q.connectorHostRefTargetType),
                path(q -> q.connectorHostRefRelationId)));

        // TODO mapping for List<String> F_TARGET_SYSTEM_TYPE
    }

    @Override
    protected QConnector newAliasInstance(String alias) {
        return new QConnector(alias);
    }

    @Override
    public ConnectorSqlTransformer createTransformer(
            SqlTransformerContext transformerContext) {
        return new ConnectorSqlTransformer(transformerContext, this);
    }

    @Override
    public MConnector newRowObject() {
        return new MConnector();
    }
}
