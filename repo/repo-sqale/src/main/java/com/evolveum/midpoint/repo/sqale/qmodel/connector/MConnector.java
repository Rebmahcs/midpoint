/*
 * Copyright (C) 2010-2021 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.repo.sqale.qmodel.connector;

import java.util.UUID;

import com.evolveum.midpoint.repo.sqale.qmodel.object.MObject;

/**
 * Querydsl "row bean" type related to {@link QConnector}.
 */
public class MConnector extends MObject {

    public String connectorBundle;
    public String connectorType;
    public String connectorVersion;
    public String framework;
    public UUID connectorHostRefTargetOid;
    public Integer connectorHostRefTargetType;
    public Integer connectorHostRefRelationId;
}
