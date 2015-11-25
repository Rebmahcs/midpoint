/*
 * Copyright (c) 2010-2014 Evolveum
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

package com.evolveum.midpoint.repo.sql;

import com.evolveum.midpoint.common.InternalsConfig;
import com.evolveum.midpoint.common.crypto.CryptoUtil;
import com.evolveum.midpoint.prism.ConsistencyCheckScope;
import com.evolveum.midpoint.prism.Containerable;
import com.evolveum.midpoint.prism.Item;
import com.evolveum.midpoint.prism.PrismContainer;
import com.evolveum.midpoint.prism.PrismContainerValue;
import com.evolveum.midpoint.prism.PrismContext;
import com.evolveum.midpoint.prism.PrismObject;
import com.evolveum.midpoint.prism.PrismObjectDefinition;
import com.evolveum.midpoint.prism.PrismProperty;
import com.evolveum.midpoint.prism.PrismPropertyDefinition;
import com.evolveum.midpoint.prism.PrismPropertyValue;
import com.evolveum.midpoint.prism.PrismReference;
import com.evolveum.midpoint.prism.PrismReferenceDefinition;
import com.evolveum.midpoint.prism.PrismReferenceValue;
import com.evolveum.midpoint.prism.Visitable;
import com.evolveum.midpoint.prism.Visitor;
import com.evolveum.midpoint.prism.delta.ContainerDelta;
import com.evolveum.midpoint.prism.delta.ItemDelta;
import com.evolveum.midpoint.prism.delta.ObjectDelta;
import com.evolveum.midpoint.prism.delta.PropertyDelta;
import com.evolveum.midpoint.prism.delta.ReferenceDelta;
import com.evolveum.midpoint.prism.parser.XNodeProcessorEvaluationMode;
import com.evolveum.midpoint.prism.path.ItemPath;
import com.evolveum.midpoint.prism.path.NameItemPathSegment;
import com.evolveum.midpoint.prism.polystring.PolyString;
import com.evolveum.midpoint.prism.polystring.PrismDefaultPolyStringNormalizer;
import com.evolveum.midpoint.prism.query.AllFilter;
import com.evolveum.midpoint.prism.query.NoneFilter;
import com.evolveum.midpoint.prism.query.ObjectFilter;
import com.evolveum.midpoint.prism.query.ObjectPaging;
import com.evolveum.midpoint.prism.query.ObjectQuery;
import com.evolveum.midpoint.repo.api.RepoAddOptions;
import com.evolveum.midpoint.repo.api.RepositoryService;
import com.evolveum.midpoint.repo.sql.data.common.RAccessCertificationCampaign;
import com.evolveum.midpoint.repo.sql.data.common.RLookupTable;
import com.evolveum.midpoint.repo.sql.data.common.RObject;
import com.evolveum.midpoint.repo.sql.data.common.any.RAnyValue;
import com.evolveum.midpoint.repo.sql.data.common.any.RValueType;
import com.evolveum.midpoint.repo.sql.data.common.container.RAccessCertificationCase;
import com.evolveum.midpoint.repo.sql.data.common.embedded.RPolyString;
import com.evolveum.midpoint.repo.sql.data.common.other.RLookupTableRow;
import com.evolveum.midpoint.repo.sql.data.common.type.RObjectExtensionType;
import com.evolveum.midpoint.repo.sql.query.QueryEngine;
import com.evolveum.midpoint.repo.sql.query.QueryException;
import com.evolveum.midpoint.repo.sql.query.RQuery;
import com.evolveum.midpoint.repo.sql.query2.QueryEngine2;
import com.evolveum.midpoint.repo.sql.util.ClassMapper;
import com.evolveum.midpoint.repo.sql.util.DtoTranslationException;
import com.evolveum.midpoint.repo.sql.util.GetObjectResult;
import com.evolveum.midpoint.repo.sql.util.IdGeneratorResult;
import com.evolveum.midpoint.repo.sql.util.PrismIdentifierGenerator;
import com.evolveum.midpoint.repo.sql.util.RUtil;
import com.evolveum.midpoint.repo.sql.util.ScrollableResultsIterator;
import com.evolveum.midpoint.schema.GetOperationOptions;
import com.evolveum.midpoint.schema.LabeledString;
import com.evolveum.midpoint.schema.ObjectSelector;
import com.evolveum.midpoint.schema.RelationalValueSearchQuery;
import com.evolveum.midpoint.schema.RepositoryDiag;
import com.evolveum.midpoint.schema.ResultHandler;
import com.evolveum.midpoint.schema.RetrieveOption;
import com.evolveum.midpoint.schema.SearchResultList;
import com.evolveum.midpoint.schema.SearchResultMetadata;
import com.evolveum.midpoint.schema.SelectorOptions;
import com.evolveum.midpoint.schema.result.OperationResult;
import com.evolveum.midpoint.schema.result.OperationResultStatus;
import com.evolveum.midpoint.schema.util.ObjectQueryUtil;
import com.evolveum.midpoint.util.DebugUtil;
import com.evolveum.midpoint.util.exception.ObjectAlreadyExistsException;
import com.evolveum.midpoint.util.exception.ObjectNotFoundException;
import com.evolveum.midpoint.util.exception.SchemaException;
import com.evolveum.midpoint.util.exception.SystemException;
import com.evolveum.midpoint.util.logging.Trace;
import com.evolveum.midpoint.util.logging.TraceManager;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCampaignType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.AccessCertificationCaseType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.FocusType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableRowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.LookupTableType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ObjectType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.SequenceType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.ShadowType;
import com.evolveum.midpoint.xml.ns._public.common.common_3.UserType;
import com.evolveum.prism.xml.ns._public.types_3.PolyStringType;
import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.Validate;
import org.hibernate.Criteria;
import org.hibernate.LockMode;
import org.hibernate.LockOptions;
import org.hibernate.Query;
import org.hibernate.SQLQuery;
import org.hibernate.ScrollMode;
import org.hibernate.ScrollableResults;
import org.hibernate.Session;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.hibernate.exception.ConstraintViolationException;
import org.hibernate.internal.SessionFactoryImpl;
import org.hibernate.jdbc.Work;
import org.hibernate.transform.Transformers;
import org.springframework.stereotype.Repository;

import javax.annotation.PostConstruct;
import javax.xml.namespace.QName;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.Driver;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Properties;


/**
 * @author lazyman
 */
@Repository
public class SqlRepositoryServiceImpl extends SqlBaseService implements RepositoryService {

    public static final String PERFORMANCE_LOG_NAME = SqlRepositoryServiceImpl.class.getName() + ".performance";

    private static final Trace LOGGER = TraceManager.getTrace(SqlRepositoryServiceImpl.class);
    private static final Trace LOGGER_PERFORMANCE = TraceManager.getTrace(PERFORMANCE_LOG_NAME);

    private static final int MAX_CONSTRAINT_NAME_LENGTH = 40;
    private static final String IMPLEMENTATION_SHORT_NAME = "SQL";
    private static final String IMPLEMENTATION_DESCRIPTION = "Implementation that stores data in generic relational" +
            " (SQL) databases. It is using ORM (hibernate) on top of JDBC to access the database.";
    private static final String DETAILS_TRANSACTION_ISOLATION = "transactionIsolation";
    private static final String DETAILS_CLIENT_INFO = "clientInfo.";
    private static final String DETAILS_DATA_SOURCE = "dataSource";
    private static final String DETAILS_HIBERNATE_DIALECT = "hibernateDialect";
    private static final String DETAILS_HIBERNATE_HBM_2_DDL = "hibernateHbm2ddl";

    private OrgClosureManager closureManager;

    public SqlRepositoryServiceImpl(SqlRepositoryFactory repositoryFactory) {
        super(repositoryFactory);
    }

    // public because of testing
    public OrgClosureManager getClosureManager() {
        if (closureManager == null) {
            closureManager = new OrgClosureManager(getConfiguration());
        }
        return closureManager;
    }

    private <T extends ObjectType> PrismObject<T> getObject(Session session, Class<T> type, String oid,
                                                            Collection<SelectorOptions<GetOperationOptions>> options,
                                                            boolean lockForUpdate)
            throws ObjectNotFoundException, SchemaException, DtoTranslationException, QueryException {

        boolean lockedForUpdateViaHibernate = false;
        boolean lockedForUpdateViaSql = false;

        LockOptions lockOptions = new LockOptions();
        //todo fix lock for update!!!!!
        if (lockForUpdate) {
            if (getConfiguration().isLockForUpdateViaHibernate()) {
                lockOptions.setLockMode(LockMode.PESSIMISTIC_WRITE);
                lockedForUpdateViaHibernate = true;
            } else if (getConfiguration().isLockForUpdateViaSql()) {
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Trying to lock object " + oid + " for update (via SQL)");
                }
                long time = System.currentTimeMillis();
                SQLQuery q = session.createSQLQuery("select oid from m_object where oid = ? for update");
                q.setString(0, oid);
                Object result = q.uniqueResult();
                if (result == null) {
                    return throwObjectNotFoundException(type, oid);
                }
                if (LOGGER.isTraceEnabled()) {
                    LOGGER.trace("Locked via SQL (in " + (System.currentTimeMillis() - time) + " ms)");
                }
                lockedForUpdateViaSql = true;
            }
        }

        if (LOGGER.isTraceEnabled()) {
            if (lockedForUpdateViaHibernate) {
                LOGGER.trace("Getting object " + oid + " with locking for update (via hibernate)");
            } else if (lockedForUpdateViaSql) {
                LOGGER.trace("Getting object " + oid + ", already locked for update (via SQL)");
            } else {
                LOGGER.trace("Getting object " + oid + " without locking for update");
            }
        }

        GetObjectResult fullObject = null;
        if (!lockForUpdate) {
            Query query = session.getNamedQuery("get.object");
            query.setString("oid", oid);
            query.setResultTransformer(GetObjectResult.RESULT_TRANSFORMER);
            query.setLockOptions(lockOptions);

            fullObject = (GetObjectResult) query.uniqueResult();
        } else {
            // we're doing update after this get, therefore we load full object right now
            // (it would be loaded during merge anyway)
            // this just loads object to hibernate session, probably will be removed later. Merge after this get
            // will be faster. Read and use object only from fullObject column.
            // todo remove this later [lazyman]
            Criteria criteria = session.createCriteria(ClassMapper.getHQLTypeClass(type));
            criteria.add(Restrictions.eq("oid", oid));

            criteria.setLockMode(lockOptions.getLockMode());
            RObject obj = (RObject) criteria.uniqueResult();

            if (obj != null) {
                obj.toJAXB(getPrismContext(), null).asPrismObject();
                fullObject = new GetObjectResult(obj.getFullObject(), obj.getStringsCount(), obj.getLongsCount(),
                        obj.getDatesCount(), obj.getReferencesCount(), obj.getPolysCount(), obj.getBooleansCount());
            }
        }

        LOGGER.trace("Got it.");
        if (fullObject == null) {
            throwObjectNotFoundException(type, oid);
        }
        
       
        LOGGER.trace("Transforming data to JAXB type.");
        PrismObject<T> prismObject = updateLoadedObject(fullObject, type, options, session);
        validateObjectType(prismObject, type);

        return prismObject;
    }

    private <T extends ObjectType> PrismObject<T> throwObjectNotFoundException(Class<T> type, String oid)
            throws ObjectNotFoundException {
        throw new ObjectNotFoundException("Object of type '" + type.getSimpleName() + "' with oid '" + oid
                + "' was not found.", null, oid);
    }

    @Override
    public <T extends ObjectType> PrismObject<T> getObject(Class<T> type, String oid,
                                                           Collection<SelectorOptions<GetOperationOptions>> options,
                                                           OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Getting object '{}' with oid '{}'.", new Object[]{type.getSimpleName(), oid});

        final String operation = "getting";
        int attempt = 1;

        OperationResult subResult = result.createMinorSubresult(GET_OBJECT);
        subResult.addParam("type", type.getName());
        subResult.addParam("oid", oid);

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("getObject");

        try {
            while (true) {
                try {
                    return getObjectAttempt(type, oid, options, subResult);
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(oid, operation, attempt, ex, subResult);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private <T extends ObjectType> PrismObject<T> getObjectAttempt(Class<T> type, String oid,
                                                                   Collection<SelectorOptions<GetOperationOptions>> options,
                                                                   OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        LOGGER_PERFORMANCE.debug("> get object {}, oid={}", type.getSimpleName(), oid);
        PrismObject<T> objectType = null;

        Session session = null;
        try {
            session = beginReadOnlyTransaction();

            objectType = getObject(session, type, oid, options, false);

            session.getTransaction().commit();
        } catch (ObjectNotFoundException ex) {
            GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
            rollbackTransaction(session, ex, result, !GetOperationOptions.isAllowNotFound(rootOptions));
            throw ex;
        } catch (SchemaException ex) {
            rollbackTransaction(session, ex, "Schema error while getting object with oid: "
                    + oid + ". Reason: " + ex.getMessage(), result, true);
            throw ex;
        } catch (QueryException | DtoTranslationException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Get object:\n{}", new Object[]{(objectType != null ? objectType.debugDump(3) : null)});
        }

        return objectType;
    }

    private Session beginReadOnlyTransaction() {
        return beginTransaction(getConfiguration().isUseReadOnlyTransactions());
    }

    @Override
    public <F extends FocusType> PrismObject<F> searchShadowOwner(String shadowOid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws ObjectNotFoundException {
        Validate.notEmpty(shadowOid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Searching shadow owner for {}", shadowOid);

        final String operation = "searching shadow owner";
        int attempt = 1;

        OperationResult subResult = result.createSubresult(SEARCH_SHADOW_OWNER);
        subResult.addParam("shadowOid", shadowOid);

        while (true) {
            try {
                return searchShadowOwnerAttempt(shadowOid, options, subResult);
            } catch (RuntimeException ex) {
                attempt = logOperationAttempt(shadowOid, operation, attempt, ex, subResult);
            }
        }
    }

    private <F extends FocusType> PrismObject<F> searchShadowOwnerAttempt(String shadowOid, Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws ObjectNotFoundException {
        LOGGER_PERFORMANCE.debug("> search shadow owner for oid={}", shadowOid);
        PrismObject<F> owner = null;
        Session session = null;
        try {
            session = beginReadOnlyTransaction();
            Query query = session.getNamedQuery("searchShadowOwner.getShadow");
            query.setString("oid", shadowOid);
            if (query.uniqueResult() == null) {
                throw new ObjectNotFoundException("Shadow with oid '" + shadowOid + "' doesn't exist.");
            }

            LOGGER.trace("Selecting account shadow owner for account {}.", new Object[]{shadowOid});
            query = session.getNamedQuery("searchShadowOwner.getOwner");
            query.setString("oid", shadowOid);
            query.setResultTransformer(GetObjectResult.RESULT_TRANSFORMER);

            List<GetObjectResult> focuses = query.list();
            LOGGER.trace("Found {} focuses, transforming data to JAXB types.",
                    new Object[]{(focuses != null ? focuses.size() : 0)});

            if (focuses == null || focuses.isEmpty()) {
                // account shadow owner was not found
                return null;
            }

            if (focuses.size() > 1) {
                LOGGER.warn("Found {} owners for shadow oid {}, returning first owner.",
                        new Object[]{focuses.size(), shadowOid});
            }

            GetObjectResult focus = focuses.get(0);
            owner = updateLoadedObject(focus, (Class<F>) FocusType.class, options, session);

            session.getTransaction().commit();
        } catch (ObjectNotFoundException ex) {
            GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
            rollbackTransaction(session, ex, result, !GetOperationOptions.isAllowNotFound(rootOptions));
            throw ex;
        } catch (SchemaException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        return owner;
    }

    @Override
    @Deprecated
    public PrismObject<UserType> listAccountShadowOwner(String accountOid, OperationResult result)
            throws ObjectNotFoundException {
        Validate.notEmpty(accountOid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Selecting account shadow owner for account {}.", new Object[]{accountOid});

        final String operation = "listing account shadow owner";
        int attempt = 1;

        OperationResult subResult = result.createSubresult(LIST_ACCOUNT_SHADOW);
        subResult.addParam("accountOid", accountOid);

        while (true) {
            try {
                return listAccountShadowOwnerAttempt(accountOid, subResult);
            } catch (RuntimeException ex) {
                attempt = logOperationAttempt(accountOid, operation, attempt, ex, subResult);
            }
        }
    }

    private PrismObject<UserType> listAccountShadowOwnerAttempt(String accountOid, OperationResult result)
            throws ObjectNotFoundException {
        LOGGER_PERFORMANCE.debug("> list account shadow owner oid={}", accountOid);
        PrismObject<UserType> userType = null;
        Session session = null;
        try {
            session = beginReadOnlyTransaction();
            Query query = session.getNamedQuery("listAccountShadowOwner.getUser");
            query.setString("oid", accountOid);
            query.setResultTransformer(GetObjectResult.RESULT_TRANSFORMER);

            List<GetObjectResult> users = query.list();
            LOGGER.trace("Found {} users, transforming data to JAXB types.",
                    new Object[]{(users != null ? users.size() : 0)});

            if (users == null || users.isEmpty()) {
                // account shadow owner was not found
                return null;
            }

            if (users.size() > 1) {
                LOGGER.warn("Found {} users for account oid {}, returning first user. [interface change needed]",
                        new Object[]{users.size(), accountOid});
            }

            GetObjectResult user = users.get(0);
            userType = updateLoadedObject(user, UserType.class, null, session);

            session.getTransaction().commit();
        } catch (SchemaException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        return userType;
    }

    private void validateName(PrismObject object) throws SchemaException {
        PrismProperty name = object.findProperty(ObjectType.F_NAME);
        if (name == null || ((PolyString) name.getRealValue()).isEmpty()) {
            throw new SchemaException("Attempt to add object without name.");
        }
    }

    @Override
    public <T extends ObjectType> String addObject(PrismObject<T> object, RepoAddOptions options, OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException {
        Validate.notNull(object, "Object must not be null.");
        validateName(object);
        Validate.notNull(result, "Operation result must not be null.");

        if (options == null) {
            options = new RepoAddOptions();
        }

        LOGGER.debug("Adding object type '{}', overwrite={}, allowUnencryptedValues={}",
                new Object[]{object.getCompileTimeClass().getSimpleName(), options.isOverwrite(),
                        options.isAllowUnencryptedValues()}
        );

        if (InternalsConfig.encryptionChecks && !RepoAddOptions.isAllowUnencryptedValues(options)) {
            CryptoUtil.checkEncrypted(object);
        }

        if (InternalsConfig.consistencyChecks) {
            object.checkConsistence(ConsistencyCheckScope.THOROUGH);
        } else {
            object.checkConsistence(ConsistencyCheckScope.MANDATORY_CHECKS_ONLY);
        }

        if (LOGGER.isTraceEnabled()) {
            // Explicitly log name
            PolyStringType namePolyType = object.asObjectable().getName();
            LOGGER.trace("NAME: {} - {}", namePolyType.getOrig(), namePolyType.getNorm());
        }

        OperationResult subResult = result.createSubresult(ADD_OBJECT);
        subResult.addParam("object", object);
        subResult.addParam("options", options);

        final String operation = "adding";
        int attempt = 1;

        String oid = object.getOid();
        while (true) {
            try {
                return addObjectAttempt(object, options, subResult);
            } catch (RuntimeException ex) {
                attempt = logOperationAttempt(oid, operation, attempt, ex, subResult);
            }
        }
    }

    private <T extends ObjectType> String addObjectAttempt(PrismObject<T> object, RepoAddOptions options,
                                                           OperationResult result)
            throws ObjectAlreadyExistsException, SchemaException {
        LOGGER_PERFORMANCE.debug("> add object {}, oid={}, overwrite={}",
                new Object[]{object.getCompileTimeClass().getSimpleName(), object.getOid(), options.isOverwrite()});
        String oid = null;
        Session session = null;
        OrgClosureManager.Context closureContext = null;
        // it is needed to keep the original oid for example for import options. if we do not keep it
        // and it was null it can bring some error because the oid is set when the object contains orgRef
        // or it is org. and by the import we do not know it so it will be trying to delete non-existing object
        String originalOid = object.getOid();
        try {
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Object\n{}", new Object[]{object.debugDump()});
            }

            LOGGER.trace("Translating JAXB to data type.");
            PrismIdentifierGenerator.Operation operation = options.isOverwrite() ?
                    PrismIdentifierGenerator.Operation.ADD_WITH_OVERWRITE :
                    PrismIdentifierGenerator.Operation.ADD;

            RObject rObject = createDataObjectFromJAXB(object, operation);

            session = beginTransaction();

            closureContext = getClosureManager().onBeginTransactionAdd(session, object, options.isOverwrite());

            if (options.isOverwrite()) {
                oid = overwriteAddObjectAttempt(object, rObject, originalOid, session, closureContext);
            } else {
                oid = nonOverwriteAddObjectAttempt(object, rObject, originalOid, session, closureContext);
            }
            session.getTransaction().commit();

            LOGGER.trace("Saved object '{}' with oid '{}'", new Object[]{
                    object.getCompileTimeClass().getSimpleName(), oid});

            object.setOid(oid);
        } catch (ConstraintViolationException ex) {
            handleConstraintViolationException(session, ex, result);
            rollbackTransaction(session, ex, result, true);

            LOGGER.debug("Constraint violation occurred (will be rethrown as ObjectAlreadyExistsException).", ex);
            // we don't know if it's only name uniqueness violation, or something else,
            // therefore we're throwing it always as ObjectAlreadyExistsException revert
            // to the original oid and prevent of unexpected behaviour (e.g. by import with overwrite option)
            if (StringUtils.isEmpty(originalOid)) {
                object.setOid(null);
            }
            String constraintName = ex.getConstraintName();
            // Breaker to avoid long unreadable messages
            if (constraintName != null && constraintName.length() > MAX_CONSTRAINT_NAME_LENGTH) {
                constraintName = null;
            }
            throw new ObjectAlreadyExistsException("Conflicting object already exists"
                    + (constraintName == null ? "" : " (violated constraint '" + constraintName + "')"), ex);
        } catch (ObjectAlreadyExistsException | SchemaException ex) {
            rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (DtoTranslationException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupClosureAndSessionAndResult(closureContext, session, result);
        }

        return oid;
    }

    private <T extends ObjectType> String overwriteAddObjectAttempt(PrismObject<T> object, RObject rObject,
                                                                    String originalOid, Session session, OrgClosureManager.Context closureContext)
            throws ObjectAlreadyExistsException, SchemaException, DtoTranslationException {

        PrismObject<T> oldObject = null;

        //check if object already exists, find differences and increment version if necessary
        Collection<? extends ItemDelta> modifications = null;
        if (originalOid != null) {
            try {
                oldObject = getObject(session, object.getCompileTimeClass(), originalOid, null, true);
                ObjectDelta<T> delta = object.diff(oldObject);
                modifications = delta.getModifications();

                //we found existing object which will be overwritten, therefore we increment version
                Integer version = RUtil.getIntegerFromString(oldObject.getVersion());
                version = (version == null) ? 0 : ++version;

                rObject.setVersion(version);
            } catch (QueryException ex) {
                handleGeneralCheckedException(ex, session, null);
            } catch (ObjectNotFoundException ex) {
                //it's ok that object was not found, therefore we won't be overwriting it
            }
        }

        updateFullObject(rObject, object);
        RObject merged = (RObject) session.merge(rObject);
        addLookupTableRows(session, rObject, modifications != null);
        addCertificationCampaignCases(session, rObject, modifications != null);

        if (getClosureManager().isEnabled()) {
            OrgClosureManager.Operation operation;
            if (modifications == null) {
                operation = OrgClosureManager.Operation.ADD;
                modifications = createAddParentRefDelta(object);
            } else {
                operation = OrgClosureManager.Operation.MODIFY;
            }
            getClosureManager().updateOrgClosure(oldObject, modifications, session, merged.getOid(), object.getCompileTimeClass(),
                    operation, closureContext);
        }
        return merged.getOid();
    }

    private <T extends ObjectType> void updateFullObject(RObject object, PrismObject<T> savedObject)
            throws DtoTranslationException, SchemaException {
        LOGGER.debug("Updating full object xml column start.");
        savedObject.setVersion(Integer.toString(object.getVersion()));

        if (FocusType.class.isAssignableFrom(savedObject.getCompileTimeClass())) {
            savedObject.removeProperty(FocusType.F_JPEG_PHOTO);
        } else if (LookupTableType.class.equals(savedObject.getCompileTimeClass())) {
            PrismContainer table = savedObject.findContainer(LookupTableType.F_ROW);
            savedObject.remove(table);
        } else if (AccessCertificationCampaignType.class.equals(savedObject.getCompileTimeClass())) {
            PrismContainer caseContainer = savedObject.findContainer(AccessCertificationCampaignType.F_CASE);
            savedObject.remove(caseContainer);
        }

        String xml = getPrismContext().serializeObjectToString(savedObject, PrismContext.LANG_XML);
        byte[] fullObject = RUtil.getByteArrayFromXml(xml, getConfiguration().isUseZip());

        if (LOGGER.isTraceEnabled()) LOGGER.trace("Storing full object\n{}", xml);

        object.setFullObject(fullObject);

        LOGGER.debug("Updating full object xml column finish.");
    }

    private <T extends ObjectType> String nonOverwriteAddObjectAttempt(PrismObject<T> object, RObject rObject,
                                                                       String originalOid, Session session, OrgClosureManager.Context closureContext)
            throws ObjectAlreadyExistsException, SchemaException, DtoTranslationException {

        // check name uniqueness (by type)
        if (StringUtils.isNotEmpty(originalOid)) {
            LOGGER.trace("Checking oid uniqueness.");
            //todo improve this table name bullshit
            Class hqlType = ClassMapper.getHQLTypeClass(object.getCompileTimeClass());
            SQLQuery query = session.createSQLQuery("select count(*) from " + RUtil.getTableName(hqlType)
                    + " where oid=:oid");
            query.setString("oid", object.getOid());

            Number count = (Number) query.uniqueResult();
            if (count != null && count.longValue() > 0) {
                throw new ObjectAlreadyExistsException("Object '" + object.getCompileTimeClass().getSimpleName()
                        + "' with oid '" + object.getOid() + "' already exists.");
            }
        }

        updateFullObject(rObject, object);

        LOGGER.trace("Saving object (non overwrite).");
        String oid = (String) session.save(rObject);
        addLookupTableRows(session, rObject, false);
        addCertificationCampaignCases(session, rObject, false);

        if (getClosureManager().isEnabled()) {
            Collection<ReferenceDelta> modifications = createAddParentRefDelta(object);
            getClosureManager().updateOrgClosure(null, modifications, session, oid, object.getCompileTimeClass(),
                    OrgClosureManager.Operation.ADD, closureContext);
        }

        return oid;
    }

    @Override
    public <T extends ObjectType> void deleteObject(Class<T> type, String oid, OperationResult result)
            throws ObjectNotFoundException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notEmpty(oid, "Oid must not be null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Deleting object type '{}' with oid '{}'", new Object[]{type.getSimpleName(), oid});

        final String operation = "deleting";
        int attempt = 1;

        OperationResult subResult = result.createSubresult(DELETE_OBJECT);
        subResult.addParam("type", type.getName());
        subResult.addParam("oid", oid);

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("deleteObject");

        try {
            while (true) {
                try {
                    deleteObjectAttempt(type, oid, subResult);
                    return;
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(oid, operation, attempt, ex, subResult);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private <T extends ObjectType> List<ReferenceDelta> createAddParentRefDelta(PrismObject<T> object) {
        PrismReference parentOrgRef = object.findReference(ObjectType.F_PARENT_ORG_REF);
        if (parentOrgRef == null || parentOrgRef.isEmpty()) {
            return new ArrayList<>();
        }

        PrismObjectDefinition def = object.getDefinition();
        ReferenceDelta delta = ReferenceDelta.createModificationAdd(new ItemPath(ObjectType.F_PARENT_ORG_REF),
                def, parentOrgRef.getClonedValues());

        return Arrays.asList(delta);
    }

    private <T extends ObjectType> void deleteObjectAttempt(Class<T> type, String oid, OperationResult result)
            throws ObjectNotFoundException {
        LOGGER_PERFORMANCE.debug("> delete object {}, oid={}", new Object[]{type.getSimpleName(), oid});
        Session session = null;
        OrgClosureManager.Context closureContext = null;
        try {
            session = beginTransaction();

            closureContext = getClosureManager().onBeginTransactionDelete(session, type, oid);

            Criteria query = session.createCriteria(ClassMapper.getHQLTypeClass(type));
            query.add(Restrictions.eq("oid", oid));
            RObject object = (RObject) query.uniqueResult();
            if (object == null) {
                throw new ObjectNotFoundException("Object of type '" + type.getSimpleName() + "' with oid '" + oid
                        + "' was not found.", null, oid);
            }

            getClosureManager().updateOrgClosure(null, null, session, oid, type, OrgClosureManager.Operation.DELETE, closureContext);

            session.delete(object);
            if (LookupTableType.class.equals(type)) {
                deleteLookupTableRows(session, oid);
            }
            if (AccessCertificationCampaignType.class.equals(type)) {
                deleteCertificationCampaignCases(session, oid);
            }

            session.getTransaction().commit();
        } catch (ObjectNotFoundException ex) {
            rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupClosureAndSessionAndResult(closureContext, session, result);
        }
    }

    /**
     * This method removes all lookup table rows for object defined by oid
     */
    private void deleteLookupTableRows(Session session, String oid) {
        Query query = session.getNamedQuery("delete.lookupTableData");
        query.setParameter("oid", oid);

        query.executeUpdate();
    }

    private void addLookupTableRows(Session session, RObject object, boolean merge) {
        if (!(object instanceof RLookupTable)) {
            return;
        }
        RLookupTable table = (RLookupTable) object;

        if (merge) {
            deleteLookupTableRows(session, table.getOid());
        }
        if (table.getRows() != null) {
            for (RLookupTableRow row : table.getRows()) {
                session.save(row);
            }
        }
    }

    private void deleteCertificationCampaignCases(Session session, String oid) {
        Query query = session.getNamedQuery("delete.campaignCases");
        query.setParameter("oid", oid);

        query.executeUpdate();
    }

    private void addCertificationCampaignCases(Session session, RObject object, boolean merge) {
        if (!(object instanceof RAccessCertificationCampaign)) {
            return;
        }
        RAccessCertificationCampaign campaign = (RAccessCertificationCampaign) object;

        if (merge) {
            deleteCertificationCampaignCases(session, campaign.getOid());
        }
        if (campaign.getCases() != null) {
            for (RAccessCertificationCase aCase : campaign.getCases()) {
                session.save(aCase);
            }
        }
    }

    private void addLookupTableRows(Session session, String tableOid, Collection<PrismContainerValue> values, int currentId) {
        for (PrismContainerValue value : values) {
            LookupTableRowType rowType = new LookupTableRowType();
            rowType.setupContainerValue(value);

            RLookupTableRow row = RLookupTableRow.toRepo(tableOid, rowType);
            row.setId(currentId);
            currentId++;
            session.save(row);
        }
    }

    private void addCertificationCampaignCases(Session session, String campaignOid, Collection<PrismContainerValue> values, int currentId) {
        for (PrismContainerValue value : values) {
            AccessCertificationCaseType caseType = new AccessCertificationCaseType();
            caseType.setupContainerValue(value);

            RAccessCertificationCase row = RAccessCertificationCase.toRepo(campaignOid, caseType, getPrismContext());
            row.setId(currentId);
            currentId++;
            session.save(row);
        }
    }

    private void updateLookupTableData(Session session, RObject object, Collection<? extends ItemDelta> modifications) {
        if (modifications.isEmpty()) {
            return;
        }

        if (!(object instanceof RLookupTable)) {
            throw new IllegalStateException("Object being modified is not a LookupTable; it is " + object.getClass());
        }
        final RLookupTable rLookupTable = (RLookupTable) object;
        final String tableOid = object.getOid();

        for (ItemDelta delta : modifications) {
            if (!(delta instanceof ContainerDelta) || delta.getPath().size() != 1) {
                throw new IllegalStateException("Wrong table delta sneaked into updateLookupTableData: class=" + delta.getClass() + ", path=" + delta.getPath());
            }
            // one "table" container modification
            ContainerDelta containerDelta = (ContainerDelta) delta;

            if (containerDelta.getValuesToDelete() != null) {
                // todo do 'bulk' delete like delete from ... where oid=? and id in (...)
                for (PrismContainerValue value : (Collection<PrismContainerValue>) containerDelta.getValuesToDelete()) {
                    Query query = session.getNamedQuery("delete.lookupTableDataRow");
                    query.setString("oid", tableOid);
                    query.setInteger("id", RUtil.toInteger(value.getId()));
                    query.executeUpdate();
                }
            }
            if (containerDelta.getValuesToAdd() != null) {
                int currentId = findLastIdInRepo(session, tableOid, "get.lookupTableLastId") + 1;
                addLookupTableRows(session, tableOid, containerDelta.getValuesToAdd(), currentId);
            }
            if (containerDelta.getValuesToReplace() != null) {
                deleteLookupTableRows(session, tableOid);
                addLookupTableRows(session, tableOid, containerDelta.getValuesToReplace(), 1);
            }
        }
    }

    private void updateCampaignCases(Session session, RObject object, Collection<? extends ItemDelta> modifications) {
        if (modifications.isEmpty()) {
            return;
        }

        if (!(object instanceof RAccessCertificationCampaign)) {
            throw new IllegalStateException("Object being modified is not a RAccessCertificationCampaign; it is " + object.getClass());
        }
        final RAccessCertificationCampaign rCampaign = (RAccessCertificationCampaign) object;
        final String campaignOid = object.getOid();

        for (ItemDelta delta : modifications) {
            if (!(delta instanceof ContainerDelta) || delta.getPath().size() != 1) {
                throw new IllegalStateException("Wrong campaign delta sneaked into updateCampaignCases: class=" + delta.getClass() + ", path=" + delta.getPath());
            }
            // one "case" container modification
            ContainerDelta containerDelta = (ContainerDelta) delta;

            if (containerDelta.getValuesToDelete() != null) {
                // todo do 'bulk' delete like delete from ... where oid=? and id in (...)
                for (PrismContainerValue value : (Collection<PrismContainerValue>) containerDelta.getValuesToDelete()) {
                    Query query = session.getNamedQuery("delete.campaignCase");
                    query.setString("oid", campaignOid);
                    query.setInteger("id", RUtil.toInteger(value.getId()));
                    query.executeUpdate();
                }
            }
            if (containerDelta.getValuesToAdd() != null) {
                int currentId = findLastIdInRepo(session, campaignOid, "get.campaignCaseLastId") + 1;
                addCertificationCampaignCases(session, campaignOid, containerDelta.getValuesToAdd(), currentId);
            }
            if (containerDelta.getValuesToReplace() != null) {
                deleteCertificationCampaignCases(session, campaignOid);
                addCertificationCampaignCases(session, campaignOid, containerDelta.getValuesToReplace(), 1);
            }
        }
    }

    private int findLastIdInRepo(Session session, String tableOid, String queryName) {
        Query query = session.getNamedQuery(queryName);
        query.setString("oid", tableOid);
        Integer lastId = (Integer) query.uniqueResult();
        if (lastId == null) {
            lastId = 0;
        }
        return lastId;
    }

    @Override
    public <T extends ObjectType> int countObjects(Class<T> type, ObjectQuery query, OperationResult result) {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Counting objects of type '{}', query (on trace level).", new Object[]{type.getSimpleName()});
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Full query\n{}", new Object[]{(query == null ? "undefined" : query.debugDump())});
        }

        OperationResult subResult = result.createMinorSubresult(COUNT_OBJECTS);
        subResult.addParam("type", type.getName());
        subResult.addParam("query", query);

        if (query != null) {
            ObjectFilter filter = query.getFilter();
            filter = ObjectQueryUtil.simplify(filter);
            if (filter instanceof NoneFilter) {
                subResult.recordSuccess();
                return 0;
            }
            query = query.cloneEmpty();
            query.setFilter(filter);
        }

        final String operation = "counting";
        int attempt = 1;

        while (true) {
            try {
                return countObjectsAttempt(type, query, subResult);
            } catch (RuntimeException ex) {
                attempt = logOperationAttempt(null, operation, attempt, ex, subResult);
            }
        }
    }

    private <T extends ObjectType> int countObjectsAttempt(Class<T> type, ObjectQuery query, OperationResult result) {
        LOGGER_PERFORMANCE.debug("> count objects {}", new Object[]{type.getSimpleName()});

        int count = 0;

        Session session = null;
        try {
            Class<? extends RObject> hqlType = ClassMapper.getHQLTypeClass(type);

            session = beginReadOnlyTransaction();
            Number longCount;
            if (query == null || query.getFilter() == null) {
                // this is 5x faster than count with 3 inner joins, it can probably improved also for queries which
                // filters uses only properties from concrete entities like RUser, RRole by improving interpreter [lazyman]
                SQLQuery sqlQuery = session.createSQLQuery("SELECT COUNT(*) FROM " + RUtil.getTableName(hqlType));
                longCount = (Number) sqlQuery.uniqueResult();
            } else {
                RQuery rQuery;
                if (isUseNewQueryInterpreter(query)) {
                    QueryEngine2 engine = new QueryEngine2(getConfiguration(), getPrismContext());
                    rQuery = engine.interpret(query, type, null, true, session);
                } else {
                    QueryEngine engine = new QueryEngine(getConfiguration(), getPrismContext());
                    rQuery = engine.interpret(query, type, null, true, session);
                }

                longCount = (Number) rQuery.uniqueResult();
            }
            LOGGER.trace("Found {} objects.", longCount);
            count = longCount != null ? longCount.intValue() : 0;
        } catch (QueryException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        return count;
    }

    @Override
    public <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjects(Class<T> type, ObjectQuery query,
                                                                                 Collection<SelectorOptions<GetOperationOptions>> options,
                                                                                 OperationResult result) throws SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        logSearchInputParameters(type, query, false, null);

        OperationResult subResult = result.createSubresult(SEARCH_OBJECTS);
        subResult.addParam("type", type.getName());
        subResult.addParam("query", query);
        // subResult.addParam("paging", paging);

        if (query != null) {
            ObjectFilter filter = query.getFilter();
            filter = ObjectQueryUtil.simplify(filter);
            if (filter instanceof NoneFilter) {
                subResult.recordSuccess();
                return new SearchResultList(new ArrayList<PrismObject<T>>(0));
            } else if (filter instanceof AllFilter){
            	query = query.cloneEmpty();
            	query.setFilter(null);
            } else {
	            query = query.cloneEmpty();
	            query.setFilter(filter);
            }
            
        }

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("searchObjects");

        final String operation = "searching";
        int attempt = 1;
        try {
            while (true) {
                try {
                    return searchObjectsAttempt(type, query, options, subResult);
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(null, operation, attempt, ex, subResult);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    @Override
    public <T extends Containerable> SearchResultList<T> searchContainers(Class<T> type, ObjectQuery query,
                                                                          Collection<SelectorOptions<GetOperationOptions>> options, OperationResult parentResult)
            throws SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        logSearchInputParameters(type, query, false, null);

        OperationResult result = parentResult.createSubresult(SEARCH_CONTAINERS);
        result.addParam("type", type.getName());
        result.addParam("query", query);

        if (query != null) {
            ObjectFilter filter = query.getFilter();
            filter = ObjectQueryUtil.simplify(filter);
            if (filter instanceof NoneFilter) {
                result.recordSuccess();
                return new SearchResultList(new ArrayList<T>(0));
            } else if (filter instanceof AllFilter) {
                query = query.cloneEmpty();
                query.setFilter(null);
            } else {
                query = query.cloneEmpty();
                query.setFilter(filter);
            }
        }

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("searchContainers");

        final String operation = "searching";
        int attempt = 1;
        try {
            while (true) {
                try {
                    return searchContainersAttempt(type, query, options, result);
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(null, operation, attempt, ex, result);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private <T> void logSearchInputParameters(Class<T> type, ObjectQuery query, boolean iterative, Boolean strictlySequential) {
        ObjectPaging paging = query != null ? query.getPaging() : null;
        LOGGER.debug("Searching objects of type '{}', query (on trace level), offset {}, count {}, iterative {}, strictlySequential {}.",
                new Object[]{type.getSimpleName(), (paging != null ? paging.getOffset() : "undefined"),
                        (paging != null ? paging.getMaxSize() : "undefined"), iterative, strictlySequential}
        );

        if (!LOGGER.isTraceEnabled()) {
            return;
        }

        LOGGER.trace("Full query\n{}\nFull paging\n{}", new Object[]{
                (query == null ? "undefined" : query.debugDump()),
                (paging != null ? paging.debugDump() : "undefined")});

        if (iterative) {
            LOGGER.trace("Iterative search by paging: {}, batch size {}",
                    getConfiguration().isIterativeSearchByPaging(),
                    getConfiguration().getIterativeSearchByPagingBatchSize());
        }
    }

    private <T extends ObjectType> SearchResultList<PrismObject<T>> searchObjectsAttempt(Class<T> type, ObjectQuery query,
                                                                                         Collection<SelectorOptions<GetOperationOptions>> options,
                                                                                         OperationResult result) throws SchemaException {
        LOGGER_PERFORMANCE.debug("> search objects {}", new Object[]{type.getSimpleName()});
        List<PrismObject<T>> list = new ArrayList<>();
        Session session = null;
        try {
            session = beginReadOnlyTransaction();
            RQuery rQuery;

            if (isUseNewQueryInterpreter(query)) {
                QueryEngine2 engine = new QueryEngine2(getConfiguration(), getPrismContext());
                rQuery = engine.interpret(query, type, options, false, session);
            } else {
                QueryEngine engine = new QueryEngine(getConfiguration(), getPrismContext());
                rQuery = engine.interpret(query, type, options, false, session);
            }

            List<GetObjectResult> objects = rQuery.list();
            LOGGER.trace("Found {} objects, translating to JAXB.", new Object[]{(objects != null ? objects.size() : 0)});

            for (GetObjectResult object : objects) {
                PrismObject<T> prismObject = updateLoadedObject(object, type, options, session);
                list.add(prismObject);
            }

            session.getTransaction().commit();
        } catch (QueryException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        return new SearchResultList<PrismObject<T>>(list);
    }

    private <C extends Containerable> SearchResultList<C> searchContainersAttempt(Class<C> type, ObjectQuery query,
                                                                                  Collection<SelectorOptions<GetOperationOptions>> options,
                                                                                  OperationResult result) throws SchemaException {

        if (!(AccessCertificationCaseType.class.equals(type))) {
            throw new UnsupportedOperationException("Only AccessCertificationCaseType is supported here now.");
        }

        LOGGER_PERFORMANCE.debug("> search containers {}", new Object[]{type.getSimpleName()});
        List<C> list = new ArrayList<>();
        Session session = null;
        try {
            session = beginReadOnlyTransaction();

            QueryEngine2 engine = new QueryEngine2(getConfiguration(), getPrismContext());
            RQuery rQuery = engine.interpret(query, type, options, false, session);

            List<GetObjectResult> items = rQuery.list();
            LOGGER.trace("Found {} items, translating to JAXB.", items.size());

            for (GetObjectResult item : items) {
                PrismContainerValue<C> cvalue = (PrismContainerValue<C>) updateLoadedCertificationCase(item, options, session);
                list.add(cvalue.asContainerable());
            }

            session.getTransaction().commit();
        } catch (QueryException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        return new SearchResultList<C>(list);
    }

    /**
     * This method provides object parsing from String and validation.
     */
    private <T extends ObjectType> PrismObject<T> updateLoadedObject(GetObjectResult result, Class<T> type,
                                                                     Collection<SelectorOptions<GetOperationOptions>> options,
                                                                     Session session) throws SchemaException {

        String xml = RUtil.getXmlFromByteArray(result.getFullObject(), getConfiguration().isUseZip());
        PrismObject<T> prismObject;
        try {
        	// "Postel mode": be tolerant what you read. We need this to tolerate (custom) schema changes
            prismObject = getPrismContext().parseObject(xml, XNodeProcessorEvaluationMode.COMPAT);
        } catch (SchemaException e) {
            LOGGER.debug("Couldn't parse object because of schema exception ({}):\nObject: {}", e, xml);
            throw e;
        } catch (RuntimeException e) {
            LOGGER.debug("Couldn't parse object because of unexpected exception ({}):\nObject: {}", e, xml);
            throw e;
        }

        if (FocusType.class.isAssignableFrom(prismObject.getCompileTimeClass())) {
            if (SelectorOptions.hasToLoadPath(FocusType.F_JPEG_PHOTO, options)) {
                //todo improve, use user.hasPhoto flag and take options into account [lazyman]
                //this is called only when options contains INCLUDE user/jpegPhoto
                Query query = session.getNamedQuery("get.focusPhoto");
                query.setString("oid", prismObject.getOid());
                byte[] photo = (byte[]) query.uniqueResult();
                if (photo != null) {
                    PrismProperty property = prismObject.findOrCreateProperty(FocusType.F_JPEG_PHOTO);
                    property.setRealValue(photo);
                }
            }
        } else if (ShadowType.class.equals(prismObject.getCompileTimeClass())) {
            //we store it because provisioning now sends it to repo, but it should be transient
            prismObject.removeContainer(ShadowType.F_ASSOCIATION);

            LOGGER.debug("Loading definitions for shadow attributes.");

            Short[] counts = result.getCountProjection();
            Class[] classes = GetObjectResult.EXT_COUNT_CLASSES;

            for (int i = 0; i < classes.length; i++) {
                if (counts[i] == null || counts[i] == 0) {
                    continue;
                }

                applyShadowAttributeDefinitions(classes[i], prismObject, session);
            }
            LOGGER.debug("Definitions for attributes loaded. Counts: {}", Arrays.toString(counts));
        } else if (LookupTableType.class.equals(prismObject.getCompileTimeClass())) {
            updateLoadedLookupTable(prismObject, options, session);
        } else if (AccessCertificationCampaignType.class.equals(prismObject.getCompileTimeClass())) {
            updateLoadedCampaign(prismObject, options, session);
        }

        GetOperationOptions rootOptions = SelectorOptions.findRootOptions(options);
        
        if (GetOperationOptions.isResolveNames(rootOptions)) {
            final List<String> oidsToResolve = new ArrayList<String>();
            Visitor oidExtractor = new Visitor() {
                @Override
                public void visit(Visitable visitable) {
                    if (visitable instanceof PrismReferenceValue) {
                        PrismReferenceValue value = (PrismReferenceValue) visitable;
                        if (value.getTargetName() != null) {    // just for sure
                            return;
                        }
                        if (value.getObject() != null) {        // improbable but possible
                            value.setTargetName(value.getObject().getName());
                            return;
                        }
                        if (value.getOid() == null) {           // shouldn't occur as well
                            return;
                        }
                        oidsToResolve.add(value.getOid());
                    }
                }
            };
            prismObject.accept(oidExtractor);

			if (!oidsToResolve.isEmpty()) {
				Query query = session.getNamedQuery("resolveReferences");
				query.setParameterList("oid", oidsToResolve);
				query.setResultTransformer(Transformers.ALIAS_TO_ENTITY_MAP);

				List<Map<String, Object>> results = query.list();
				final Map<String, PolyString> oidNameMap = consolidateResults(results);

                Visitor nameSetter = new Visitor() {
                    @Override
                    public void visit(Visitable visitable) {
                        if (visitable instanceof PrismReferenceValue) {
                            PrismReferenceValue value = (PrismReferenceValue) visitable;
                            if (value.getTargetName() == null && value.getOid() != null) {
                                value.setTargetName(oidNameMap.get(value.getOid()));
                            }
                        }
                    }
                };
                prismObject.accept(nameSetter);
			}
        }
        
        validateObjectType(prismObject, type);

        return prismObject;
    }


    private PrismContainerValue<AccessCertificationCaseType> updateLoadedCertificationCase(GetObjectResult result,
                                                                                           Collection<SelectorOptions<GetOperationOptions>> options,
                                                                                           Session session) throws SchemaException {

        AccessCertificationCaseType aCase = RAccessCertificationCase.createJaxb(result.getFullObject(), getPrismContext());
        PrismContainerValue<AccessCertificationCaseType> cvalue = aCase.asPrismContainerValue();
        validateContainerValue(cvalue, AccessCertificationCaseType.class);
        return cvalue;
    }


    private Map<String, PolyString> consolidateResults(List<Map<String, Object>> results){
    	Map<String, PolyString> oidNameMap = new HashMap<String, PolyString>();
    	for (Map<String, Object> map : results){
    		PolyStringType name = RPolyString.copyToJAXB((RPolyString) map.get("1"));
    		oidNameMap.put((String)map.get("0"), new PolyString(name.getOrig(), name.getNorm()));
    	}
    	return oidNameMap;
    }

    private GetOperationOptions findLookupTableGetOption(Collection<SelectorOptions<GetOperationOptions>> options) {
        final ItemPath tablePath = new ItemPath(LookupTableType.F_ROW);

        Collection<SelectorOptions<GetOperationOptions>> filtered = SelectorOptions.filterRetrieveOptions(options);
        for (SelectorOptions<GetOperationOptions> option : filtered) {
            ObjectSelector selector = option.getSelector();
            if (selector == null) {
            	// Ignore this. These are top-level options. There will not
            	// apply to lookup table
            	continue;
            }
            ItemPath selected = selector.getPath();

            if (tablePath.equivalent(selected)) {
                return option.getOptions();
            }
        }

        return null;
    }

    private <T extends ObjectType> void updateLoadedLookupTable(PrismObject<T> object,
                                                                Collection<SelectorOptions<GetOperationOptions>> options,
                                                                Session session) throws SchemaException {
        if (!SelectorOptions.hasToLoadPath(LookupTableType.F_ROW, options)) {
            return;
        }

        LOGGER.debug("Loading lookup table data.");

        GetOperationOptions getOption = findLookupTableGetOption(options);
        RelationalValueSearchQuery queryDef = getOption == null ? null : getOption.getRelationalValueSearchQuery();
        Criteria criteria = setupLookupTableRowsQuery(session, queryDef, object.getOid());
        if (queryDef != null && queryDef.getPaging() != null) {
            ObjectPaging paging = queryDef.getPaging();

            if (paging.getOffset() != null) {
                criteria.setFirstResult(paging.getOffset());
            }
            if (paging.getMaxSize() != null) {
                criteria.setMaxResults(paging.getMaxSize());
            }

            ItemPath orderByPath = paging.getOrderBy();
            if (paging.getDirection() != null && orderByPath != null && !orderByPath.isEmpty()) {
                if (orderByPath.size() > 1 || !(orderByPath.first() instanceof NameItemPathSegment)) {
                    throw new SchemaException("OrderBy has to consist of just one naming segment");
                }
                String orderBy = ((NameItemPathSegment) (orderByPath.first())).getName().getLocalPart();
                switch (paging.getDirection()) {
                    case ASCENDING:
                        criteria.addOrder(Order.asc(orderBy));
                        break;
                    case DESCENDING:
                        criteria.addOrder(Order.desc(orderBy));
                        break;
                }
            }
        }

        List<RLookupTableRow> rows = criteria.list();
        if (rows == null || rows.isEmpty()) {
            return;
        }

        LookupTableType lookup = (LookupTableType) object.asObjectable();
        List<LookupTableRowType> jaxbRows = lookup.getRow();
        for (RLookupTableRow row : rows) {
            LookupTableRowType jaxbRow = row.toJAXB();
            jaxbRows.add(jaxbRow);
        }
    }

    private <T extends ObjectType> void updateLoadedCampaign(PrismObject<T> object,
                                                             Collection<SelectorOptions<GetOperationOptions>> options,
                                                             Session session) throws SchemaException {
        if (!SelectorOptions.hasToLoadPath(AccessCertificationCampaignType.F_CASE, options)) {
            return;
        }

        LOGGER.debug("Loading certification campaign cases.");

        Criteria criteria = session.createCriteria(RAccessCertificationCase.class);
        criteria.add(Restrictions.eq("ownerOid", object.getOid()));

        List<RAccessCertificationCase> cases = criteria.list();
        if (cases == null || cases.isEmpty()) {
            return;
        }

        AccessCertificationCampaignType campaign = (AccessCertificationCampaignType) object.asObjectable();
        List<AccessCertificationCaseType> jaxbCases = campaign.getCase();
        for (RAccessCertificationCase rCase : cases) {
            AccessCertificationCaseType jaxbCase = rCase.toJAXB(getPrismContext());
            jaxbCases.add(jaxbCase);
        }
    }

    private Criteria setupLookupTableRowsQuery(Session session, RelationalValueSearchQuery queryDef, String oid) {
        Criteria criteria = session.createCriteria(RLookupTableRow.class);
        criteria.add(Restrictions.eq("ownerOid", oid));

        if (queryDef != null
                && queryDef.getColumn() != null
                && queryDef.getSearchType() != null
                && StringUtils.isNotEmpty(queryDef.getSearchValue())) {

            String param = queryDef.getColumn().getLocalPart();
            String value = queryDef.getSearchValue();
            if (LookupTableRowType.F_LABEL.equals(queryDef.getColumn())) {
                param = "label.norm";

                PolyString poly = new PolyString(value);
                poly.recompute(new PrismDefaultPolyStringNormalizer());
                value = poly.getNorm();
            }
            switch (queryDef.getSearchType()) {
                case EXACT:
                    criteria.add(Restrictions.eq(param, value));
                    break;
                case STARTS_WITH:
                    criteria.add(Restrictions.like(param, value + "%"));
                    break;
                case SUBSTRING:
                    criteria.add(Restrictions.like(param, "%" + value + "%"));
            }
        }

        return criteria;
    }

    private void applyShadowAttributeDefinitions(Class<? extends RAnyValue> anyValueType,
                                                 PrismObject object, Session session) throws SchemaException {

        PrismContainer attributes = object.findContainer(ShadowType.F_ATTRIBUTES);

        Query query = session.getNamedQuery("getDefinition." + anyValueType.getSimpleName());
        query.setParameter("oid", object.getOid());
        query.setParameter("ownerType", RObjectExtensionType.ATTRIBUTES);

        List<Object[]> values = query.list();
        if (values == null || values.isEmpty()) {
            return;
        }

        for (Object[] value : values) {
        	QName name = RUtil.stringToQName((String) value[0]);
            QName type = RUtil.stringToQName((String) value[1]);
            Item item = attributes.findItem(name);
            
            // A switch statement used to be here
            // but that caused strange trouble with OpenJDK. This if-then-else works.
            if (item.getDefinition() == null) {
            	RValueType rValType = (RValueType) value[2];
            	if (rValType == RValueType.PROPERTY) {
            		PrismPropertyDefinition<Object> def = new PrismPropertyDefinition<Object>(name, type, object.getPrismContext());
            		item.applyDefinition(def, true);
            	} else if (rValType == RValueType.REFERENCE) {
            		PrismReferenceDefinition def = new PrismReferenceDefinition(name, type, object.getPrismContext());
            		item.applyDefinition(def, true);
            	} else {
            		throw new UnsupportedOperationException("Unsupported value type " + rValType);
            	}
            }
        }
    }

    @Override
    public <T extends ObjectType> void modifyObject(Class<T> type, String oid,
                                                    Collection<? extends ItemDelta> modifications,
                                                    OperationResult result)
            throws ObjectNotFoundException, SchemaException, ObjectAlreadyExistsException {

        Validate.notNull(modifications, "Modifications must not be null.");
        Validate.notNull(type, "Object class in delta must not be null.");
        Validate.notEmpty(oid, "Oid must not null or empty.");
        Validate.notNull(result, "Operation result must not be null.");

        OperationResult subResult = result.createSubresult(MODIFY_OBJECT);
        subResult.addParam("type", type.getName());
        subResult.addParam("oid", oid);
        subResult.addCollectionOfSerializablesAsParam("modifications", modifications);

        if (modifications.isEmpty()) {
            LOGGER.debug("Modification list is empty, nothing was modified.");
            subResult.recordStatus(OperationResultStatus.SUCCESS, "Modification list is empty, nothing was modified.");
            return;
        }

        if (InternalsConfig.encryptionChecks) {
            CryptoUtil.checkEncrypted(modifications);
        }

        if (InternalsConfig.consistencyChecks) {
            ItemDelta.checkConsistence(modifications, ConsistencyCheckScope.THOROUGH);
        } else {
            ItemDelta.checkConsistence(modifications, ConsistencyCheckScope.MANDATORY_CHECKS_ONLY);
        }

        if (LOGGER.isTraceEnabled()) {
            for (ItemDelta modification : modifications) {
                if (modification instanceof PropertyDelta<?>) {
                    PropertyDelta<?> propDelta = (PropertyDelta<?>) modification;
                    if (propDelta.getPath().equivalent(new ItemPath(ObjectType.F_NAME))) {
                        Collection<PrismPropertyValue<PolyString>> values = propDelta.getValues(PolyString.class);
                        for (PrismPropertyValue<PolyString> pval : values) {
                            PolyString value = pval.getValue();
                            LOGGER.trace("NAME delta: {} - {}", value.getOrig(), value.getNorm());
                        }
                    }
                }
            }
        }

        final String operation = "modifying";
        int attempt = 1;

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("modifyObject");

        try {
            while (true) {
                try {
                    modifyObjectAttempt(type, oid, modifications, subResult);
                    return;
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(oid, operation, attempt, ex, subResult);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }

    }

    private <T extends ObjectType> void modifyObjectAttempt(Class<T> type, String oid,
                                                            Collection<? extends ItemDelta> modifications,
                                                            OperationResult result) throws ObjectNotFoundException,
            SchemaException, ObjectAlreadyExistsException, SerializationRelatedException {

        // shallow clone - because some methods, e.g. filterLookupTableModifications manipulate this collection
        modifications = new ArrayList<>(modifications);

        LOGGER.debug("Modifying object '{}' with oid '{}'.", new Object[]{type.getSimpleName(), oid});
        LOGGER_PERFORMANCE.debug("> modify object {}, oid={}, modifications={}",
                new Object[]{type.getSimpleName(), oid, modifications});
        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Modifications:\n{}", new Object[]{DebugUtil.debugDump(modifications)});
        }

        Session session = null;
        OrgClosureManager.Context closureContext = null;
        try {
            session = beginTransaction();

            closureContext = getClosureManager().onBeginTransactionModify(session, type, oid, modifications);

            Collection<? extends ItemDelta> lookupTableModifications = filterLookupTableModifications(type, modifications);
            Collection<? extends ItemDelta> campaignCaseModifications = filterCampaignCaseModifications(type, modifications);

            // JpegPhoto (RFocusPhoto) is a special kind of entity. First of all, it is lazily loaded, because photos are really big.
            // Each RFocusPhoto naturally belongs to one RFocus, so it would be appropriate to set orphanRemoval=true for focus-photo
            // association. However, this leads to a strange problem when merging in-memory RFocus object with the database state:
            // If in-memory RFocus object has no photo associated (because of lazy loading), then the associated RFocusPhoto is deleted.
            //
            // To prevent this behavior, we've set orphanRemoval to false. Fortunately, the remove operation on RFocus
            // seems to be still cascaded to RFocusPhoto. What we have to implement ourselves, however, is removal of RFocusPhoto
            // _without_ removing of RFocus. In order to know whether the photo has to be removed, we have to retrieve
            // its value, apply the delta (e.g. if the delta is a DELETE VALUE X, we have to know whether X matches current
            // value of the photo), and if the resulting value is empty, we have to manually delete the RFocusPhoto instance.
            //
            // So the first step is to retrieve the current value of photo - we obviously do this only if the modifications
            // deal with the jpegPhoto property.
            Collection<SelectorOptions<GetOperationOptions>> options;
            boolean containsFocusPhotoModification = FocusType.class.isAssignableFrom(type) && containsPhotoModification(modifications);
            if (containsFocusPhotoModification) {
                options = Arrays.asList(SelectorOptions.create(FocusType.F_JPEG_PHOTO, GetOperationOptions.createRetrieve(RetrieveOption.INCLUDE)));
            } else {
                options = null;
            }

            // get object
            PrismObject<T> prismObject = getObject(session, type, oid, options, true);
            // apply diff
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("OBJECT before:\n{}", new Object[]{prismObject.debugDump()});
            }
            PrismObject<T> originalObject = null;
            if (getClosureManager().isEnabled()) {
                originalObject = prismObject.clone();
            }
            ItemDelta.applyTo(modifications, prismObject);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("OBJECT after:\n{}", prismObject.debugDump());
            }

            // Continuing the photo treatment: should we remove the (now obsolete) focus photo?
            // We have to test prismObject at this place, because updateFullObject (below) removes photo property from the prismObject.
            boolean shouldPhotoBeRemoved = containsFocusPhotoModification && ((FocusType) prismObject.asObjectable()).getJpegPhoto() == null;

            // merge and update object
            LOGGER.trace("Translating JAXB to data type.");
            RObject rObject = createDataObjectFromJAXB(prismObject, PrismIdentifierGenerator.Operation.MODIFY);
            rObject.setVersion(rObject.getVersion() + 1);

            updateFullObject(rObject, prismObject);
            session.merge(rObject);

            updateLookupTableData(session, rObject, lookupTableModifications);
            updateCampaignCases(session, rObject, campaignCaseModifications);

            if (getClosureManager().isEnabled()) {
                getClosureManager().updateOrgClosure(originalObject, modifications, session, oid, type, OrgClosureManager.Operation.MODIFY, closureContext);
            }

            // JpegPhoto cleanup: As said before, if a focus has to have no photo (after modifications are applied),
            // we have to remove the photo manually.
            if (shouldPhotoBeRemoved) {
                Query query = session.createQuery("delete RFocusPhoto where ownerOid = :oid");
                query.setParameter("oid", prismObject.getOid());
                query.executeUpdate();
                LOGGER.trace("Focus photo for {} was deleted", prismObject.getOid());
            }

            LOGGER.trace("Before commit...");
            session.getTransaction().commit();
            LOGGER.trace("Committed!");
        } catch (ObjectNotFoundException ex) {
            rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (ConstraintViolationException ex) {
            handleConstraintViolationException(session, ex, result);

            rollbackTransaction(session, ex, result, true);

            LOGGER.debug("Constraint violation occurred (will be rethrown as ObjectAlreadyExistsException).", ex);
            // we don't know if it's only name uniqueness violation, or something else,
            // therefore we're throwing it always as ObjectAlreadyExistsException

            //todo improve (we support only 5 DB, so we should probably do some hacking in here)
            throw new ObjectAlreadyExistsException(ex);
        } catch (SchemaException ex) {
            rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (QueryException | DtoTranslationException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupClosureAndSessionAndResult(closureContext, session, result);
            LOGGER.trace("Session cleaned up.");
        }
    }

    private <T extends ObjectType> Collection<? extends ItemDelta> filterLookupTableModifications(Class<T> type,
                                                                                                  Collection<? extends ItemDelta> modifications) {
        Collection<ItemDelta> tableDelta = new ArrayList<>();
        if (!LookupTableType.class.equals(type)) {
            return tableDelta;
        }

        ItemPath tablePath = new ItemPath(LookupTableType.F_ROW);
        for (ItemDelta delta : modifications) {
            ItemPath path = delta.getPath();
            if (path.isEmpty()) {
                throw new UnsupportedOperationException("Lookup table cannot be modified via empty-path modification");
            } else if (path.equivalent(tablePath)) {
                tableDelta.add(delta);
            } else if (path.isSuperPath(tablePath)) {
                // todo - what about modifications with path like table[id] or table[id]/xxx where xxx=key|value|label?
                throw new UnsupportedOperationException("Lookup table row can be modified only by specifying path=table");
            }
        }

        modifications.removeAll(tableDelta);

        return tableDelta;
    }

    private <T extends ObjectType> Collection<? extends ItemDelta> filterCampaignCaseModifications(Class<T> type,
                                                                                                   Collection<? extends ItemDelta> modifications) {
        Collection<ItemDelta> caseDelta = new ArrayList<>();
        if (!AccessCertificationCampaignType.class.equals(type)) {
            return caseDelta;
        }

        ItemPath casePath = new ItemPath(AccessCertificationCampaignType.F_CASE);
        for (ItemDelta delta : modifications) {
            ItemPath path = delta.getPath();
            if (path.isEmpty()) {
                throw new UnsupportedOperationException("Certification campaign cannot be modified via empty-path modification");
            } else if (path.equivalent(casePath)) {
                caseDelta.add(delta);
            } else if (path.isSuperPath(casePath)) {
                // todo - what about modifications with path like case[id] or case[id]/xxx?
                throw new UnsupportedOperationException("Campaign case can be modified only by specifying path=case");
            }
        }

        modifications.removeAll(caseDelta);

        return caseDelta;
    }

    private <T extends ObjectType> boolean containsPhotoModification(Collection<? extends ItemDelta> modifications) {
        ItemPath photoPath = new ItemPath(FocusType.F_JPEG_PHOTO);
        for (ItemDelta delta : modifications) {
            ItemPath path = delta.getPath();
            if (path.isEmpty()) {
                throw new UnsupportedOperationException("Focus cannot be modified via empty-path modification");
            } else if (photoPath.isSubPathOrEquivalent(path)) { // actually, "subpath" variant should not occur
                return true;
            }
        }

        return false;
    }

    private void cleanupClosureAndSessionAndResult(final OrgClosureManager.Context closureContext, final Session session, final OperationResult result) {
        if (closureContext != null) {
            getClosureManager().cleanUpAfterOperation(closureContext, session);
        }
        cleanupSessionAndResult(session, result);
    }

    private void handleConstraintViolationException(Session session, ConstraintViolationException ex, OperationResult result) {

        // BRUTAL HACK - in PostgreSQL, concurrent changes in parentRefOrg sometimes cause the following exception
        // "duplicate key value violates unique constraint "XXXX". This is *not* an ObjectAlreadyExistsException,
        // more likely it is a serialization-related one.
        //
        // TODO: somewhat generalize this approach - perhaps by retrying all operations not dealing with OID/name uniqueness

        SQLException sqlException = findSqlException(ex);
        if (sqlException != null) {
            SQLException nextException = sqlException.getNextException();
            LOGGER.debug("ConstraintViolationException = {}; SQL exception = {}; embedded SQL exception = {}", new Object[]{ex, sqlException, nextException});
            String[] ok = new String[]{
                    "duplicate key value violates unique constraint \"m_org_closure_pkey\"",
                    "duplicate key value violates unique constraint \"m_reference_pkey\""
            };
            String msg1;
            if (sqlException.getMessage() != null) {
                msg1 = sqlException.getMessage();
            } else {
                msg1 = "";
            }
            String msg2;
            if (nextException != null && nextException.getMessage() != null) {
                msg2 = nextException.getMessage();
            } else {
                msg2 = "";
            }
            for (int i = 0; i < ok.length; i++) {
                if (msg1.contains(ok[i]) || msg2.contains(ok[i])) {
                    rollbackTransaction(session, ex, result, false);
                    throw new SerializationRelatedException(ex);
                }
            }
        }
    }

    @Override
    public <T extends ShadowType> List<PrismObject<T>> listResourceObjectShadows(String resourceOid,
                                                                                 Class<T> resourceObjectShadowType,
                                                                                 OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        Validate.notEmpty(resourceOid, "Resource oid must not be null or empty.");
        Validate.notNull(resourceObjectShadowType, "Resource object shadow type must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Listing resource object shadows '{}' for resource '{}'.",
                new Object[]{resourceObjectShadowType.getSimpleName(), resourceOid});
        OperationResult subResult = result.createSubresult(LIST_RESOURCE_OBJECT_SHADOWS);
        subResult.addParam("oid", resourceOid);
        subResult.addParam("resourceObjectShadowType", resourceObjectShadowType);

        final String operation = "listing resource object shadows";
        int attempt = 1;

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("listResourceObjectShadow");

        try {
            while (true) {
                try {
                    return listResourceObjectShadowsAttempt(resourceOid, resourceObjectShadowType, subResult);
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(resourceOid, operation, attempt, ex, subResult);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private <T extends ShadowType> List<PrismObject<T>> listResourceObjectShadowsAttempt(
            String resourceOid, Class<T> resourceObjectShadowType, OperationResult result)
            throws ObjectNotFoundException, SchemaException {

        LOGGER_PERFORMANCE.debug("> list resource object shadows {}, for resource oid={}",
                new Object[]{resourceObjectShadowType.getSimpleName(), resourceOid});
        List<PrismObject<T>> list = new ArrayList<>();
        Session session = null;
        try {
            session = beginReadOnlyTransaction();
            Query query = session.getNamedQuery("listResourceObjectShadows");
            query.setString("oid", resourceOid);
            query.setResultTransformer(GetObjectResult.RESULT_TRANSFORMER);

            List<GetObjectResult> shadows = query.list();
            LOGGER.debug("Query returned {} shadows, transforming to JAXB types.",
                    new Object[]{(shadows != null ? shadows.size() : 0)});

            if (shadows != null) {
                for (GetObjectResult shadow : shadows) {
                    PrismObject<T> prismObject = updateLoadedObject(shadow, resourceObjectShadowType, null, session);
                    list.add(prismObject);
                }
            }
            session.getTransaction().commit();
        } catch (SchemaException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        return list;
    }

    private <T extends ObjectType> void validateObjectType(PrismObject<T> prismObject, Class<T> type)
            throws SchemaException {
        if (prismObject == null || !type.isAssignableFrom(prismObject.getCompileTimeClass())) {
            throw new SchemaException("Expected to find '" + type.getSimpleName() + "' but found '"
                    + prismObject.getCompileTimeClass().getSimpleName() + "' (" + prismObject.toDebugName()
                    + "). Bad OID in a reference?");
        }
        if (InternalsConfig.consistencyChecks) {
            prismObject.checkConsistence();
        }
        if (InternalsConfig.readEncryptionChecks) {
            CryptoUtil.checkEncrypted(prismObject);
        }
    }

    private <C extends Containerable> void validateContainerValue(PrismContainerValue<C> cvalue, Class<C> type)
            throws SchemaException {
        if (cvalue == null) {
            throw new SchemaException("Null object as a result of repository get operation for " + type);
        }
        Class<? extends Containerable> realType = cvalue.asContainerable().getClass();
        if (!type.isAssignableFrom(realType)) {
            throw new SchemaException("Expected to find '" + type.getSimpleName() + "' but found '" + realType.getSimpleName());
        }
        // TODO call check consistence if possible
    }

    private <T extends ObjectType> RObject createDataObjectFromJAXB(PrismObject<T> prismObject,
                                                                    PrismIdentifierGenerator.Operation operation)
            throws SchemaException {

        PrismIdentifierGenerator generator = new PrismIdentifierGenerator();
        IdGeneratorResult generatorResult = generator.generate(prismObject, operation);

        T object = prismObject.asObjectable();

        RObject rObject;
        Class<? extends RObject> clazz = ClassMapper.getHQLTypeClass(object.getClass());
        try {
            rObject = clazz.newInstance();
            Method method = clazz.getMethod("copyFromJAXB", object.getClass(), clazz,
                    PrismContext.class, IdGeneratorResult.class);
            method.invoke(clazz, object, rObject, getPrismContext(), generatorResult);
        } catch (Exception ex) {
            String message = ex.getMessage();
            if (StringUtils.isEmpty(message) && ex.getCause() != null) {
                message = ex.getCause().getMessage();
            }
            throw new SchemaException(message, ex);
        }

        return rObject;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.repo.api.RepositoryService#getRepositoryDiag()
     */
    @Override
    public RepositoryDiag getRepositoryDiag() {
        LOGGER.debug("Getting repository diagnostics.");

        RepositoryDiag diag = new RepositoryDiag();
        diag.setImplementationShortName(IMPLEMENTATION_SHORT_NAME);
        diag.setImplementationDescription(IMPLEMENTATION_DESCRIPTION);

        SqlRepositoryConfiguration config = getConfiguration();

        //todo improve, find and use real values (which are used by sessionFactory) MID-1219
        diag.setDriverShortName(config.getDriverClassName());
        diag.setRepositoryUrl(config.getJdbcUrl());
        diag.setEmbedded(config.isEmbedded());

        Enumeration<Driver> drivers = DriverManager.getDrivers();
        while ((drivers != null && drivers.hasMoreElements())) {
            Driver driver = drivers.nextElement();
            if (!driver.getClass().getName().equals(config.getDriverClassName())) {
                continue;
            }

            diag.setDriverVersion(driver.getMajorVersion() + "." + driver.getMinorVersion());
        }

        List<LabeledString> details = new ArrayList<>();
        diag.setAdditionalDetails(details);
        details.add(new LabeledString(DETAILS_DATA_SOURCE, config.getDataSource()));
        details.add(new LabeledString(DETAILS_HIBERNATE_DIALECT, config.getHibernateDialect()));
        details.add(new LabeledString(DETAILS_HIBERNATE_HBM_2_DDL, config.getHibernateHbm2ddl()));

        readDetailsFromConnection(diag, config);

        Collections.sort(details, new Comparator<LabeledString>() {

            @Override
            public int compare(LabeledString o1, LabeledString o2) {
                return String.CASE_INSENSITIVE_ORDER.compare(o1.getLabel(), o2.getLabel());
            }
        });

        return diag;
    }

    private void readDetailsFromConnection(RepositoryDiag diag, final SqlRepositoryConfiguration config) {
        final List<LabeledString> details = diag.getAdditionalDetails();

        Session session = getSessionFactory().openSession();
        try {
            session.beginTransaction();
            session.doWork(new Work() {

                @Override
                public void execute(Connection connection) throws SQLException {
                    details.add(new LabeledString(DETAILS_TRANSACTION_ISOLATION,
                            getTransactionIsolation(connection, config)));


                    Properties info = connection.getClientInfo();
                    if (info == null) {
                        return;
                    }

                    for (String name : info.stringPropertyNames()) {
                        details.add(new LabeledString(DETAILS_CLIENT_INFO + name, info.getProperty(name)));
                    }
                }
            });
            session.getTransaction().commit();

            if (!(getSessionFactory() instanceof SessionFactoryImpl)) {
                return;
            }
            SessionFactoryImpl factory = (SessionFactoryImpl) getSessionFactory();
            // we try to override configuration which was read from sql repo configuration with
            // real configuration from session factory
            String dialect = factory.getDialect() != null ? factory.getDialect().getClass().getName() : null;
            details.add(new LabeledString(DETAILS_HIBERNATE_DIALECT, dialect));
        } catch (Throwable th) {
            //nowhere to report error (no operation result available)
            session.getTransaction().rollback();
        } finally {
            cleanupSessionAndResult(session, null);
        }
    }

    private String getTransactionIsolation(Connection connection, SqlRepositoryConfiguration config) {
        String value = config.getTransactionIsolation() != null ?
                config.getTransactionIsolation().name() + "(read from repo configuration)" : null;

        try {
            switch (connection.getTransactionIsolation()) {
                case Connection.TRANSACTION_NONE:
                    value = "TRANSACTION_NONE (read from connection)";
                    break;
                case Connection.TRANSACTION_READ_COMMITTED:
                    value = "TRANSACTION_READ_COMMITTED (read from connection)";
                    break;
                case Connection.TRANSACTION_READ_UNCOMMITTED:
                    value = "TRANSACTION_READ_UNCOMMITTED (read from connection)";
                    break;
                case Connection.TRANSACTION_REPEATABLE_READ:
                    value = "TRANSACTION_REPEATABLE_READ (read from connection)";
                    break;
                case Connection.TRANSACTION_SERIALIZABLE:
                    value = "TRANSACTION_SERIALIZABLE (read from connection)";
                    break;
                default:
                    value = "Unknown value in connection.";
            }
        } catch (Exception ex) {
            //nowhere to report error (no operation result available)
        }

        return value;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.repo.api.RepositoryService#repositorySelfTest(com.evolveum.midpoint.schema.result.OperationResult)
     */
    @Override
    public void repositorySelfTest(OperationResult parentResult) {
        // TODO add some SQL-specific self-test methods
        // No self-tests for now
    }

    @Override
    public void testOrgClosureConsistency(boolean repairIfNecessary, OperationResult testResult) {
        getClosureManager().checkAndOrRebuild(this, true, repairIfNecessary, false, false, testResult);
    }

    @PostConstruct
    public void initialize() {
        getClosureManager().initialize(this);
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.repo.api.RepositoryService#getVersion(java.lang.Class, java.lang.String,
     * com.evolveum.midpoint.schema.result.OperationResult)
     */
    @Override
    public <T extends ObjectType> String getVersion(Class<T> type, String oid, OperationResult parentResult)
            throws ObjectNotFoundException, SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(oid, "Object oid must not be null.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        LOGGER.debug("Getting version for {} with oid '{}'.", new Object[]{type.getSimpleName(), oid});

        OperationResult subResult = parentResult.createMinorSubresult(GET_VERSION);
        subResult.addParam("type", type.getName());
        subResult.addParam("oid", oid);

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart(GET_VERSION);

        final String operation = "getting version";
        int attempt = 1;
        try {
            while (true) {
                try {
                    return getVersionAttempt(type, oid, subResult);
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(null, operation, attempt, ex, subResult);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private <T extends ObjectType> String getVersionAttempt(Class<T> type, String oid, OperationResult result)
            throws ObjectNotFoundException, SchemaException {
        LOGGER_PERFORMANCE.debug("> get version {}, oid={}", new Object[]{type.getSimpleName(), oid});

        String version = null;
        Session session = null;
        try {
            session = beginReadOnlyTransaction();
            Query query = session.getNamedQuery("getVersion");
            query.setString("oid", oid);

            Number versionLong = (Number) query.uniqueResult();
            if (versionLong == null) {
                throw new ObjectNotFoundException("Object '" + type.getSimpleName()
                        + "' with oid '" + oid + "' was not found.");
            }
            version = versionLong.toString();
        } catch (RuntimeException ex) {
            handleGeneralRuntimeException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        return version;
    }

    /* (non-Javadoc)
     * @see com.evolveum.midpoint.repo.api.RepositoryService#searchObjectsIterative(java.lang.Class,
     * com.evolveum.midpoint.prism.query.ObjectQuery, com.evolveum.midpoint.schema.ResultHandler,
     * com.evolveum.midpoint.schema.result.OperationResult)
     */
    @Override
    public <T extends ObjectType> SearchResultMetadata searchObjectsIterative(Class<T> type, ObjectQuery query,
                                                                              ResultHandler<T> handler,
                                                                              Collection<SelectorOptions<GetOperationOptions>> options,
                                                                              boolean strictlySequential,
                                                                              OperationResult result) throws SchemaException {
        Validate.notNull(type, "Object type must not be null.");
        Validate.notNull(handler, "Result handler must not be null.");
        Validate.notNull(result, "Operation result must not be null.");

        logSearchInputParameters(type, query, true, strictlySequential);

        OperationResult subResult = result.createSubresult(SEARCH_OBJECTS_ITERATIVE);
        subResult.addParam("type", type.getName());
        subResult.addParam("query", query);

        if (query != null) {
            ObjectFilter filter = query.getFilter();
            filter = ObjectQueryUtil.simplify(filter);
            if (filter instanceof NoneFilter) {
                subResult.recordSuccess();
                return null;
                //shouldn't be this in ObjectQueryUtil.simplify?
            } else if (filter instanceof AllFilter){
            	query = query.cloneEmpty();
            	query.setFilter(null);
            } else {
            	query = query.cloneEmpty();
            	query.setFilter(filter);
            }
        }

        if (getConfiguration().isIterativeSearchByPaging()) {
            if (strictlySequential) {
                searchObjectsIterativeByPagingStrictlySequential(type, query, handler, options, subResult);
            } else {
                searchObjectsIterativeByPaging(type, query, handler, options, subResult);
            }
            return null;
        }

//        turned off until resolved 'unfinished operation' warning
//        SqlPerformanceMonitor pm = getPerformanceMonitor();
//        long opHandle = pm.registerOperationStart(SEARCH_OBJECTS_ITERATIVE);

        final String operation = "searching iterative";
        int attempt = 1;
        try {
            while (true) {
                try {
                    searchObjectsIterativeAttempt(type, query, handler, options, subResult);
                    return null;
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(null, operation, attempt, ex, subResult);
//                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
//            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private <T extends ObjectType> void searchObjectsIterativeAttempt(Class<T> type, ObjectQuery query,
                                                                      ResultHandler<T> handler,
                                                                      Collection<SelectorOptions<GetOperationOptions>> options,
                                                                      OperationResult result) throws SchemaException {
        Session session = null;
        try {
            session = beginReadOnlyTransaction();
            RQuery rQuery;
            if (isUseNewQueryInterpreter(query)) {
                QueryEngine engine = new QueryEngine(getConfiguration(), getPrismContext());
                rQuery = engine.interpret(query, type, options, false, session);
            } else {
                QueryEngine2 engine = new QueryEngine2(getConfiguration(), getPrismContext());
                rQuery = engine.interpret(query, type, options, false, session);
            }

            ScrollableResults results = rQuery.scroll(ScrollMode.FORWARD_ONLY);
            try {
                Iterator<GetObjectResult> iterator = new ScrollableResultsIterator(results);
                while (iterator.hasNext()) {
                    GetObjectResult object = iterator.next();

                    PrismObject<T> prismObject = updateLoadedObject(object, type, options, session);
                    if (!handler.handle(prismObject, result)) {
                        break;
                    }
                }
            } finally {
                if (results != null) {
                    results.close();
                }
            }

            session.getTransaction().commit();
        } catch (SchemaException | QueryException | RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }
    }

    private boolean isUseNewQueryInterpreter(ObjectQuery query) {
        return query == null || query.isUseNewQueryInterpreter();
    }

    private <T extends ObjectType> void searchObjectsIterativeByPaging(Class<T> type, ObjectQuery query,
                                                                       ResultHandler<T> handler,
                                                                       Collection<SelectorOptions<GetOperationOptions>> options,
                                                                       OperationResult result)
            throws SchemaException {

        try {
            ObjectQuery pagedQuery = query != null ? query.clone() : new ObjectQuery();

            int offset;
            int remaining;
            final int batchSize = getConfiguration().getIterativeSearchByPagingBatchSize();

            ObjectPaging paging = pagedQuery.getPaging();

            if (paging == null) {
                paging = ObjectPaging.createPaging(0, 0);        // counts will be filled-in later
                pagedQuery.setPaging(paging);
                offset = 0;
                remaining = countObjects(type, query, result);
            } else {
                offset = paging.getOffset() != null ? paging.getOffset() : 0;
                remaining = paging.getMaxSize() != null ? paging.getMaxSize() : countObjects(type, query, result) - offset;
            }

main:       while (remaining > 0) {
                paging.setOffset(offset);
                paging.setMaxSize(remaining < batchSize ? remaining : batchSize);

                List<PrismObject<T>> objects = searchObjects(type, pagedQuery, options, result);

                for (PrismObject<T> object : objects) {
                    if (!handler.handle(object, result)) {
                        break main;
                    }
                }

                if (objects.size() == 0) {
                    break;                      // should not occur, but let's check for this to avoid endless loops
                }
                offset += objects.size();
                remaining -= objects.size();
            }
        } finally {
            if (result != null && result.isUnknown()) {
                result.computeStatus();
            }
        }
    }

    // Temporary hack. Represents special paging object that means
    // "give me objects with OID greater than specified one, sorted by OID ascending".
    //
    // TODO: replace by using cookie that is part of the standard ObjectPaging
    // (but think out all consequences, e.g. conflicts with the other use of the cookie)
    public static class ObjectPagingAfterOid extends ObjectPaging {
        private String oidGreaterThan;

        public String getOidGreaterThan() {
            return oidGreaterThan;
        }

        public void setOidGreaterThan(String oidGreaterThan) {
            this.oidGreaterThan = oidGreaterThan;
        }

        @Override
        public String toString() {
            return super.toString() + ", after OID: " + oidGreaterThan;
        }

        @Override
        public ObjectPagingAfterOid clone() {
            ObjectPagingAfterOid clone = new ObjectPagingAfterOid();
            copyTo(clone);
            return clone;
        }

        protected void copyTo(ObjectPagingAfterOid clone) {
            super.copyTo(clone);
            clone.oidGreaterThan = this.oidGreaterThan;
        }
    }

    /**
     * Strictly-sequential version of paged search.
     *
     * Assumptions:
     *  - During processing of returned object(s), any objects can be added, deleted or modified.
     *
     * Guarantees:
     *  - We return each object that existed in the moment of search start:
     *     - exactly once if it was not deleted in the meanwhile,
     *     - at most once otherwise.
     *  - However, we may or may not return any objects that were added during the processing.
     *
     * Constraints:
     *  - There can be no ordering prescribed. We use our own ordering.
     *  - Moreover, for simplicity we disallow any explicit paging.
     *
     *  Implementation is very simple - we fetch objects ordered by OID, and remember last OID fetched.
     *  Obviously no object will be present in output more than once.
     *  Objects that are not deleted will be there exactly once, provided their oid is not changed.
     */
    private <T extends ObjectType> void searchObjectsIterativeByPagingStrictlySequential(
            Class<T> type, ObjectQuery query, ResultHandler<T> handler,
            Collection<SelectorOptions<GetOperationOptions>> options, OperationResult result)
            throws SchemaException {

        try {
            ObjectQuery pagedQuery = query != null ? query.clone() : new ObjectQuery();

            String lastOid = "";
            final int batchSize = getConfiguration().getIterativeSearchByPagingBatchSize();

            if (pagedQuery.getPaging() != null) {
                throw new IllegalArgumentException("Externally specified paging is not supported on strictly sequential iterative search.");
            }

            ObjectPagingAfterOid paging = new ObjectPagingAfterOid();
            pagedQuery.setPaging(paging);

main:       for (;;) {
                paging.setOidGreaterThan(lastOid);
                paging.setMaxSize(batchSize);

                List<PrismObject<T>> objects = searchObjects(type, pagedQuery, options, result);

                for (PrismObject<T> object : objects) {
                    lastOid = object.getOid();
                    if (!handler.handle(object, result)) {
                        break main;
                    }
                }

                if (objects.size() == 0) {
                    break;
                }
            }
        } finally {
            if (result != null && result.isUnknown()) {
                result.computeStatus();
            }
        }
    }

    @Override
    public boolean isAnySubordinate(String upperOrgOid, Collection<String> lowerObjectOids) throws SchemaException {
        Validate.notNull(upperOrgOid, "upperOrgOid must not be null.");
        Validate.notNull(lowerObjectOids, "lowerObjectOids must not be null.");

        if (LOGGER.isTraceEnabled())
            LOGGER.trace("Querying for subordination upper {}, lower {}", new Object[]{upperOrgOid, lowerObjectOids});

        if (lowerObjectOids.isEmpty()) {
            // trivial case
            return false;
        }

        int attempt = 1;

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("matchObject");
        try {
            while (true) {
                try {
                    return isAnySubordinateAttempt(upperOrgOid, lowerObjectOids);
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(upperOrgOid, "isAnySubordinate", attempt, ex, null);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private boolean isAnySubordinateAttempt(String upperOrgOid, Collection<String> lowerObjectOids) {
        Session session = null;
        try {
            session = beginTransaction();

            Query query;
            if (lowerObjectOids.size() == 1) {
                query = session.getNamedQuery("isAnySubordinateAttempt.oneLowerOid");
                query.setString("dOid", lowerObjectOids.iterator().next());
            } else {
                query = session.getNamedQuery("isAnySubordinateAttempt.moreLowerOids");
                query.setParameterList("dOids", lowerObjectOids);
            }
            query.setString("aOid", upperOrgOid);

            Number number = (Number) query.uniqueResult();
            return number != null && number.longValue() != 0L;
        } catch (RuntimeException ex) {
            handleGeneralException(ex, session, null);
        } finally {
            cleanupSessionAndResult(session, null);
        }

        throw new SystemException("isAnySubordinateAttempt failed somehow, this really should not happen.");
    }

	@Override
	public long advanceSequence(String oid, OperationResult parentResult) throws ObjectNotFoundException,
			SchemaException {

        Validate.notEmpty(oid, "Oid must not null or empty.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        OperationResult result = parentResult.createSubresult(ADVANCE_SEQUENCE);
        result.addParam("oid", oid);

        if (LOGGER.isTraceEnabled())
            LOGGER.trace("Advancing sequence {}", oid);

        int attempt = 1;

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("advanceSequence");
        try {
            while (true) {
                try {
                    return advanceSequenceAttempt(oid, result);
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(oid, "advanceSequence", attempt, ex, null);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
	}

    private long advanceSequenceAttempt(String oid, OperationResult result) throws ObjectNotFoundException,
            SchemaException, SerializationRelatedException {

        long returnValue;

        LOGGER.debug("Advancing sequence with oid '{}'.", oid);
        LOGGER_PERFORMANCE.debug("> advance sequence, oid={}", oid);

        Session session = null;
        try {
            session = beginTransaction();

            PrismObject<SequenceType> prismObject = getObject(session, SequenceType.class, oid, null, true);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("OBJECT before:\n{}", prismObject.debugDump());
            }
            SequenceType sequence = prismObject.asObjectable();

            if (!sequence.getUnusedValues().isEmpty()) {
                returnValue = sequence.getUnusedValues().remove(0);
            } else {
                long counter = sequence.getCounter() != null ? sequence.getCounter() : 0L;
                long maxCounter = sequence.getMaxCounter() != null ? sequence.getMaxCounter() : Long.MAX_VALUE;
                boolean allowRewind = Boolean.TRUE.equals(sequence.isAllowRewind());

                if (counter < maxCounter) {
                    returnValue = counter;
                    sequence.setCounter(counter + 1);
                } else if (counter == maxCounter) {
                    returnValue = counter;
                    if (allowRewind) {
                        sequence.setCounter(0L);
                    } else {
                        sequence.setCounter(counter + 1);       // will produce exception during next run
                    }
                } else {        // i.e. counter > maxCounter
                    if (allowRewind) {          // shouldn't occur but...
                        LOGGER.warn("Sequence {} overflown with allowRewind set to true. Rewinding.", oid);
                        returnValue = 0;
                        sequence.setCounter(1L);
                    } else {
                        // TODO some better exception...
                        throw new SystemException("No (next) value available from sequence " + oid + ". Current counter = " + sequence.getCounter() + ", max value = " + sequence.getMaxCounter());
                    }
                }
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("Return value = {}, OBJECT after:\n{}", returnValue, prismObject.debugDump());
            }

            // merge and update object
            LOGGER.trace("Translating JAXB to data type.");
            RObject rObject = createDataObjectFromJAXB(prismObject, PrismIdentifierGenerator.Operation.MODIFY);
            rObject.setVersion(rObject.getVersion() + 1);

            updateFullObject(rObject, prismObject);
            session.merge(rObject);

            LOGGER.trace("Before commit...");
            session.getTransaction().commit();
            LOGGER.trace("Committed!");

            return returnValue;
        } catch (ObjectNotFoundException ex) {
            rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (SchemaException ex) {
            rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (QueryException | DtoTranslationException | RuntimeException ex) {
            handleGeneralException(ex, session, result);                                            // should always throw an exception
            throw new SystemException("Exception " + ex + " was not handled correctly", ex);        // ...so this shouldn't occur at all
        } finally {
            cleanupSessionAndResult(session, result);
            LOGGER.trace("Session cleaned up.");
        }
    }


	@Override
	public void returnUnusedValuesToSequence(String oid, Collection<Long> unusedValues, OperationResult parentResult)
			throws ObjectNotFoundException, SchemaException {
        Validate.notEmpty(oid, "Oid must not null or empty.");
        Validate.notNull(parentResult, "Operation result must not be null.");

        OperationResult result = parentResult.createSubresult(RETURN_UNUSED_VALUES_TO_SEQUENCE);
        result.addParam("oid", oid);

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Returning unused values of {} to sequence {}", unusedValues, oid);
        }
        if (unusedValues == null || unusedValues.isEmpty()) {
            result.recordSuccess();
            return;
        }

        int attempt = 1;

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("returnUnusedValuesToSequence");
        try {
            while (true) {
                try {
                    returnUnusedValuesToSequenceAttempt(oid, unusedValues, result);
                    return;
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(oid, "returnUnusedValuesToSequence", attempt, ex, null);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
	}

    private void returnUnusedValuesToSequenceAttempt(String oid, Collection<Long> unusedValues, OperationResult result) throws ObjectNotFoundException,
            SchemaException, SerializationRelatedException {

        LOGGER.debug("Returning unused values of {} to a sequence with oid '{}'.", unusedValues, oid);
        LOGGER_PERFORMANCE.debug("> return unused values, oid={}, values={}", oid, unusedValues);

        Session session = null;
        try {
            session = beginTransaction();

            PrismObject<SequenceType> prismObject = getObject(session, SequenceType.class, oid, null, true);
            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("OBJECT before:\n{}", prismObject.debugDump());
            }
            SequenceType sequence = prismObject.asObjectable();
            int maxUnusedValues = sequence.getMaxUnusedValues() != null ? sequence.getMaxUnusedValues() : 0;
            Iterator<Long> valuesToReturnIterator = unusedValues.iterator();
            while (valuesToReturnIterator.hasNext() && sequence.getUnusedValues().size() < maxUnusedValues) {
                Long valueToReturn = valuesToReturnIterator.next();
                if (valueToReturn == null) {        // sanity check
                    continue;
                }
                if (!sequence.getUnusedValues().contains(valueToReturn)) {
                    sequence.getUnusedValues().add(valueToReturn);
                } else {
                    LOGGER.warn("UnusedValues in sequence {} already contains value of {} - ignoring the return request", oid, valueToReturn);
                }
            }

            if (LOGGER.isTraceEnabled()) {
                LOGGER.trace("OBJECT after:\n{}", prismObject.debugDump());
            }

            // merge and update object
            LOGGER.trace("Translating JAXB to data type.");
            RObject rObject = createDataObjectFromJAXB(prismObject, PrismIdentifierGenerator.Operation.MODIFY);
            rObject.setVersion(rObject.getVersion() + 1);

            updateFullObject(rObject, prismObject);
            session.merge(rObject);

            LOGGER.trace("Before commit...");
            session.getTransaction().commit();
            LOGGER.trace("Committed!");
        } catch (ObjectNotFoundException ex) {
            rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (SchemaException ex) {
            rollbackTransaction(session, ex, result, true);
            throw ex;
        } catch (QueryException | DtoTranslationException | RuntimeException ex) {
            handleGeneralException(ex, session, result);                                            // should always throw an exception
            throw new SystemException("Exception " + ex + " was not handled correctly", ex);        // ...so this shouldn't occur at all
        } finally {
            cleanupSessionAndResult(session, result);
            LOGGER.trace("Session cleaned up.");
        }
    }

    @Override
    public String executeArbitraryQuery(String query, OperationResult result) {
        Validate.notEmpty(query, "Query must not be empty.");
        Validate.notNull(result, "Operation result must not be null.");

        LOGGER.debug("Executing arbitrary query '{}'.", query);

        final String operation = "querying";
        int attempt = 1;

        OperationResult subResult = result.createMinorSubresult(EXECUTE_ARBITRARY_QUERY);
        subResult.addParam("query", query);

        SqlPerformanceMonitor pm = getPerformanceMonitor();
        long opHandle = pm.registerOperationStart("executeArbitraryQuery");

        try {
            while (true) {
                try {
                    return executeArbitraryQueryAttempt(query, subResult);
                } catch (RuntimeException ex) {
                    attempt = logOperationAttempt(null, operation, attempt, ex, subResult);
                    pm.registerOperationNewTrial(opHandle, attempt);
                }
            }
        } finally {
            pm.registerOperationFinish(opHandle, attempt);
        }
    }

    private String executeArbitraryQueryAttempt(String queryString, OperationResult result) {
        LOGGER_PERFORMANCE.debug("> execute query {}", queryString);

        Session session = null;
        StringBuffer answer = new StringBuffer();
        try {
            session = beginReadOnlyTransaction();       // beware, not all databases support read-only transactions!

            Query query = session.createQuery(queryString);
            List results = query.list();
            if (results != null) {
                answer.append("Result: ").append(results.size()).append(" item(s):\n\n");
                for (Object item : results) {
                    if (item instanceof Object[]) {
                        boolean first = true;
                        for (Object item1 : (Object[]) item) {
                            if (first) {
                                first = false;
                            } else {
                                answer.append(",");
                            }
                            answer.append(item1);
                        }
                    } else {
                        answer.append(item);
                    }
                    answer.append("\n");
                }
            }
            session.getTransaction().rollback();
        } catch (RuntimeException ex) {
            handleGeneralException(ex, session, result);
        } finally {
            cleanupSessionAndResult(session, result);
        }

        if (LOGGER.isTraceEnabled()) {
            LOGGER.trace("Executed query:\n{}\nwith result:\n{}", queryString, answer);
        }

        return answer.toString();
    }




}
