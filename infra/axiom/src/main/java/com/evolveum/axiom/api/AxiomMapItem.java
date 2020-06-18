/*
 * Copyright (C) 2020 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.axiom.api;

import java.util.Collection;
import java.util.Collections;
import java.util.Optional;

import com.evolveum.axiom.api.schema.AxiomItemDefinition;
import com.google.common.collect.Iterables;

public interface AxiomMapItem<V> extends AxiomItem<V> {

    Optional<? extends AxiomValue<V>> value(AxiomValueIdentifier key);

}
