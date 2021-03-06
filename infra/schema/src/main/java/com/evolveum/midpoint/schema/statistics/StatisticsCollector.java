/*
 * Copyright (c) 2010-2015 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */

package com.evolveum.midpoint.schema.statistics;

import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.delta.ChangeType;
import com.evolveum.midpoint.schema.statistics.IterativeTaskInformation.Operation;
import com.evolveum.midpoint.util.annotation.Experimental;
import com.evolveum.midpoint.xml.ns._public.common.common_3.*;

import org.jetbrains.annotations.NotNull;

import javax.xml.namespace.QName;
import java.util.List;

/**
 * An object that receives various statistics and state information, processes them and provides
 * them back to appropriate clients.
 *
 * A bit experimental. We need to think out what kind of statistics and state information we'd like to collect.
 *
 * Currently this functionality is bound to Task interface. However, this may change in the future.
 *
 * @author Pavol Mederly
 */
public interface StatisticsCollector {

    /**
     * Records various kinds of operational information.
     */

    void recordState(String message);

    void recordProvisioningOperation(String resourceOid, String resourceName, QName objectClassName,
            ProvisioningOperation operation, boolean success, int count, long duration);

    void recordNotificationOperation(String transportName, boolean success, long duration);

    void recordMappingOperation(String objectOid, String objectName, String objectTypeName, String mappingName, long duration);

    /**
     * Records the start of iterative operation.
     * The operation end is recorded by calling appropriate method on the returned object.
     */
    @NotNull default Operation recordIterativeOperationStart(PrismObject<? extends ObjectType> object) {
        return recordIterativeOperationStart(new IterationItemInformation(object));
    }

    @NotNull default Operation recordIterativeOperationStart(IterationItemInformation info) {
        return recordIterativeOperationStart(new IterativeOperationStartInfo(info));
    }

    /**
     * Records the start of iterative operation.
     * The operation end is recorded by calling appropriate method on the returned object.
     */
    @NotNull Operation recordIterativeOperationStart(IterativeOperationStartInfo operation);

    /**
     * Records information about repository (focal) events.
     */

    void recordObjectActionExecuted(String objectName, String objectDisplayName, QName objectType, String objectOid, ChangeType changeType, String channel, Throwable exception);

    void recordObjectActionExecuted(PrismObject<? extends ObjectType> object, ChangeType changeType, Throwable exception);

    <T extends ObjectType> void recordObjectActionExecuted(PrismObject<T> object, Class<T> objectTypeClass, String defaultOid, ChangeType changeType, String channel, Throwable exception);

    void markObjectActionExecutedBoundary();

    /**
     * Sets initial values for statistics.
     */

    void resetEnvironmentalPerformanceInformation(EnvironmentalPerformanceInformationType value);

    void resetSynchronizationInformation(SynchronizationInformationType value);

    void resetIterativeTaskInformation(IterativeTaskInformationType value);

    void resetActionsExecutedInformation(ActionsExecutedInformationType value);

    // EXPERIMENTAL - TODO: replace by something more serious
    @NotNull
    @Experimental
    @Deprecated
    List<String> getLastFailures();

}
