/*
 * Copyright (c) 2010-2019 Evolveum
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.evolveum.midpoint.wf.impl.engine;

import com.evolveum.midpoint.casemgmt.api.CaseEventDispatcher;
import com.evolveum.midpoint.casemgmt.api.CaseEventListener;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.CaseType;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

/**
 * Listener that accepts case creation events generated by lower layers (UCF).
 * In the future it could pass them to WorkflowEngine to be processed
 *
 * @author mederly
 */
@Component
public class CaseManagementListener  {

    private static final Trace LOGGER = TraceManager.getTrace(CaseManagementListener.class);

    //private static final String DOT_CLASS = WorkflowListener.class.getName() + ".";


//	@Override
//	public void onWorkItemCreation(CaseWorkItemType workItem, CaseType aCase, Task task, OperationResult result) {
//		for (ObjectReferenceType assigneeRef : workItem.getAssigneeRef()) {
//			WorkItemEvent event = new WorkItemEvent(identifierGenerator, ChangeType.ADD, workItem,
//					SimpleObjectRefImpl.create(functions, assigneeRef), aCase);
//			processEvent(event, result);
//		}
//	}
//
//	private void processEvent(CaseWorkItemEvent event, OperationResult result) {
//        try {
//            notificationManager.processEvent(event);
//        } catch (RuntimeException e) {
//            result.recordFatalError("An unexpected exception occurred when preparing and sending notifications: " + e.getMessage(), e);
//            LoggingUtils.logUnexpectedException(LOGGER, "An unexpected exception occurred when preparing and sending notifications: " + e.getMessage(), e);
//        }
//
//        // todo work correctly with operationResult (in whole notification module)
//        if (result.isUnknown()) {
//            result.computeStatus();
//        }
//        result.recordSuccessIfUnknown();
//    }


}