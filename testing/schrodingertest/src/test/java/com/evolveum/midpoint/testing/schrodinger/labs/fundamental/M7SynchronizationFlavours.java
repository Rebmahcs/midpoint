/*
 * Copyright (c) 2010-2019 Evolveum and contributors
 *
 * This work is dual-licensed under the Apache License 2.0
 * and European Union Public License. See LICENSE file for details.
 */
package com.evolveum.midpoint.testing.schrodinger.labs.fundamental;

import com.codeborne.selenide.Selenide;

import com.evolveum.midpoint.schrodinger.MidPoint;
import com.evolveum.midpoint.schrodinger.component.ProjectionsTab;
import com.evolveum.midpoint.schrodinger.component.common.table.AbstractTableWithPrismView;
import com.evolveum.midpoint.schrodinger.component.common.table.Table;
import com.evolveum.midpoint.schrodinger.component.resource.ResourceAccountsTab;
import com.evolveum.midpoint.schrodinger.page.resource.ViewResourcePage;
import com.evolveum.midpoint.schrodinger.page.task.TaskPage;
import com.evolveum.midpoint.schrodinger.page.user.UserPage;
import com.evolveum.midpoint.testing.schrodinger.labs.AbstractLabTest;
import com.evolveum.midpoint.testing.schrodinger.scenarios.ScenariosCommons;

import com.evolveum.midpoint.xml.ns._public.common.common_3.TaskType;

import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.BeforeClass;
import org.testng.annotations.Test;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;

/**
 * @author skublik
 */

public class M7SynchronizationFlavours extends AbstractLabTest {

    protected static final String LAB_OBJECTS_DIRECTORY = LAB_FUNDAMENTAL_DIRECTORY + "M7/";
    private static final Logger LOG = LoggerFactory.getLogger(M7SynchronizationFlavours.class);
    private static final File ARCHETYPE_EMPLOYEE_FILE = new File(LAB_OBJECTS_DIRECTORY + "archetypes/archetype-employee.xml");
    private static final File SYSTEM_CONFIGURATION_FILE_7 = new File(LAB_OBJECTS_DIRECTORY + "systemconfiguration/system-configuration-7.xml");
    private static final File NUMERIC_PIN_FIRST_NONZERO_POLICY_FILE = new File(LAB_OBJECTS_DIRECTORY + "valuepolicies/numeric-pin-first-nonzero-policy.xml");
    private static final File HR_NO_EXTENSION_RESOURCE_FILE = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-hr-noextension.xml");
    private static final File CSV_1_RESOURCE_FILE_7 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-1-document-access-7.xml");
    private static final File CSV_2_RESOURCE_FILE_7 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-2-canteen-7.xml");
    private static final File CSV_3_RESOURCE_FILE_7 = new File(LAB_OBJECTS_DIRECTORY + "resources/localhost-csvfile-3-ldap-7.xml");

    @BeforeClass(alwaysRun = true, dependsOnMethods = { "springTestContextPrepareTestInstance" })
    @Override
    public void beforeClass() throws IOException {
        super.beforeClass();
    }

    @Override
    protected List<File> getObjectListToImport(){
        return Arrays.asList(ARCHETYPE_EMPLOYEE_FILE, SYSTEM_CONFIGURATION_FILE_7);
    }

    @Test(groups={"M7"})
    public void mod07test01RunningImportFromResource() throws IOException {
        importObject(NUMERIC_PIN_FIRST_NONZERO_POLICY_FILE, true);

        hrTargetFile = new File(getTestTargetDir(), HR_FILE_SOURCE_NAME);
        FileUtils.copyFile(HR_SOURCE_FILE, hrTargetFile);

        importObject(HR_NO_EXTENSION_RESOURCE_FILE, true);
        changeResourceAttribute(HR_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, hrTargetFile.getAbsolutePath(), true);

        ResourceAccountsTab<ViewResourcePage> accountTab = basicPage.listResources()
                .table()
                    .clickByName(HR_RESOURCE_NAME)
                        .clickAccountsTab()
                            .clickSearchInResource();
        accountTab.table()
                .selectCheckboxByName("001212")
                    .clickImport()
                    .and()
                .and()
            .feedback()
                .isSuccess();

        UserPage owner = accountTab.table()
                .clickOnOwnerByName("X001212");

        owner.assertName("X001212");

        basicPage.listResources()
                .table()
                    .clickByName(HR_RESOURCE_NAME)
                        .clickAccountsTab()
                            .importTask()
                                .clickCreateNew()
                                    .selectTabBasic()
                                        .form()
                                            .addAttributeValue("name","Initial import from HR")
                                            .and()
                                        .and()
                                    .clickSaveAndRun()
                                        .feedback()
                                            .isInfo();

        showTask("Initial import from HR")
                .selectTabOperationStatistics()
                    .assertSuccessfullyProcessedCountMatch(14);
        basicPage.listUsers(ARCHETYPE_EMPLOYEE_PLURAL_LABEL).assertObjectsCountEquals(14);
    }

    @Test(dependsOnMethods = {"mod07test01RunningImportFromResource"}, groups={"M7"})
    public void mod07test02RunningAccountReconciliation() throws IOException {
        csv1TargetFile = new File(getTestTargetDir(), CSV_1_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_1_SOURCE_FILE, csv1TargetFile);
        csv2TargetFile = new File(getTestTargetDir(), CSV_2_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_2_SOURCE_FILE, csv2TargetFile);
        csv3TargetFile = new File(getTestTargetDir(), CSV_3_FILE_SOURCE_NAME);
        FileUtils.copyFile(CSV_3_SOURCE_FILE, csv3TargetFile);

        importObject(CSV_1_RESOURCE_FILE_7, true);
        changeResourceAttribute(CSV_1_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv1TargetFile.getAbsolutePath(), true);
        importObject(CSV_2_RESOURCE_FILE_7, true);
        changeResourceAttribute(CSV_2_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv2TargetFile.getAbsolutePath(), true);
        importObject(CSV_3_RESOURCE_FILE_7, true);
        changeResourceAttribute(CSV_3_RESOURCE_NAME, ScenariosCommons.CSV_RESOURCE_ATTR_FILE_PATH, csv3TargetFile.getAbsolutePath(), true);

        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);
        createReconTask("CSV-1 Reconciliation", CSV_1_RESOURCE_NAME);
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);
        deselectDryRun("CSV-1 Reconciliation");
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);
        assertContainsProjection("X001212", CSV_1_RESOURCE_OID, "jsmith");

        createReconTask("CSV-2 Reconciliation", CSV_2_RESOURCE_NAME);
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);
        deselectDryRun("CSV-2 Reconciliation");
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);
        assertContainsProjection("X001212", CSV_2_RESOURCE_OID, "jsmith");

        createReconTask("CSV-3 Reconciliation", CSV_3_RESOURCE_NAME);
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);
        deselectDryRun("CSV-3 Reconciliation");
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);
        assertContainsProjection("X001212", CSV_3_RESOURCE_OID, "cn=John Smith,ou=ExAmPLE,dc=example,dc=com");
    }

    @Test(dependsOnMethods = {"mod07test02RunningAccountReconciliation"}, groups={"M7"})
    public void mod07test03RunningAttributeReconciliation() throws IOException {
        FileUtils.copyFile(CSV_1_SOURCE_FILE_7_3, csv1TargetFile);

        showTask("CSV-1 Reconciliation", "Reconciliation tasks").clickRunNow();

        showShadow(CSV_1_RESOURCE_NAME, "Login", "jkirk")
                        .form()
                        .assertPropertyInputValues("groups", "Internal Employees", "Essential Documents");

    }

    @Test(dependsOnMethods = {"mod07test03RunningAttributeReconciliation"}, groups={"M7"})
    public void mod07test04RunningLiveSync() throws IOException {
        Selenide.sleep(MidPoint.TIMEOUT_MEDIUM_6_S);
        TaskPage task = basicPage.newTask();
        task.setHandlerUriForNewTask("Live synchronization task");
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);
        task.selectTabBasic()
                .form()
                    .addAttributeValue("objectclass", "AccountObjectClass")
                    .addAttributeValue(TaskType.F_NAME, "HR Synchronization")
                    .selectOption("recurrence","Recurring")
                    .selectOption("binding","Tight")
                    .editRefValue("objectRef")
                        .selectType("Resource")
                        .table()
                            .search()
                                .byName()
                                    .inputValue(HR_RESOURCE_NAME)
                                    .updateSearch()
                                .and()
                            .clickByName(HR_RESOURCE_NAME)
                    .and()
                .and()
            .selectScheduleTab()
                .form()
                    .addAttributeValue("interval", "5")
                    .and()
                .and()
            .clickSaveAndRun()
                .feedback()
                    .isInfo();

        FileUtils.copyFile(HR_SOURCE_FILE_7_4_PART_1, hrTargetFile);
        Selenide.sleep(20000);
        showUser("X000999")
                .assertGivenName("Arnold")
                .assertFamilyName("Rimmer")
                .selectTabBasic()
                    .form()
                        .assertPropertySelectValue("administrativeStatus", "Enabled");

        FileUtils.copyFile(HR_SOURCE_FILE_7_4_PART_2, hrTargetFile);
        Selenide.sleep(20000);
        showUser("X000999")
                .assertGivenName("Arnold J.");

        FileUtils.copyFile(HR_SOURCE_FILE_7_4_PART_3, hrTargetFile);
        Selenide.sleep(20000);
        showUser("X000999")
                .selectTabBasic()
                    .form()
                        .assertPropertySelectValue("administrativeStatus", "Disabled");

        FileUtils.copyFile(HR_SOURCE_FILE_7_4_PART_4, hrTargetFile);
        Selenide.sleep(20000);
        showUser("X000999")
                .selectTabBasic()
                    .form()
                        .assertPropertySelectValue("administrativeStatus", "Enabled");

    }

    private Table<ProjectionsTab<UserPage>> assertContainsProjection(String user, String resourceOid, String accountName) {
       AbstractTableWithPrismView<ProjectionsTab<UserPage>> table = showUser(user).selectTabProjections().table();
       Selenide.screenshot(user + "_" + resourceOid + "_" + accountName);
       return table
                    .search()
                        .referencePanelByItemName("Resource")
                            .inputRefOid(resourceOid)
                            .updateSearch()
                        .and()
                    .assertTableContainsText(accountName);
    }

    private void createReconTask(String reconTaskName, String resource){
        TaskPage task = basicPage.newTask();
        task.setHandlerUriForNewTask("Reconciliation task");
        Selenide.sleep(MidPoint.TIMEOUT_SHORT_4_S);
        task.selectTabBasic()
                .form()
                    .addAttributeValue("objectclass", "AccountObjectClass")
                    .selectOption("dryRun", "True")
                    .addAttributeValue(TaskType.F_NAME, reconTaskName)
                    .editRefValue("objectRef")
                        .selectType("Resource")
                            .table()
                                .search()
                                    .byName()
                                        .inputValue(resource)
                                        .updateSearch()
                                    .and()
                                .clickByName(resource)
                .and()
            .and()
                .clickSaveAndRun()
                    .feedback()
                        .isInfo();
    }

    private void deselectDryRun(String taskName) {
        showTask(taskName).selectTabBasic()
                .form()
                    .selectOption("dryRun", "Undefined")
                .and()
            .and()
        .clickSaveAndRun()
            .feedback()
                .isInfo();
    }
}
