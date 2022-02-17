/*
 * Copyright (C) 2009, 2021 BonitaSoft S.A.
 * BonitaSoft is a trademark of BonitaSoft SA.
 * This software file is BONITASOFT CONFIDENTIAL. Not For Distribution.
 * For commercial licensing information, contact:
 * BonitaSoft, 32 rue Gustave Eiffel - 38000 Grenoble
 * or BonitaSoft US, 51 Federal Street, Suite 305, San Francisco, CA 94107
 */
package com.company.bonita.test;

import static com.bonitasoft.test.toolkit.contract.ComplexInputBuilder.complexInput;
import static com.bonitasoft.test.toolkit.contract.ContractBuilder.newContract;
import static com.bonitasoft.test.toolkit.predicate.ProcessInstancePredicates.containsPendingUserTasks;
import static com.bonitasoft.test.toolkit.predicate.ProcessInstancePredicates.processInstanceArchived;
import static com.bonitasoft.test.toolkit.predicate.ProcessInstancePredicates.processInstanceStarted;

import static org.assertj.core.api.Assertions.*;

import static org.awaitility.Awaitility.await;


import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.parallel.Execution;
import org.junit.jupiter.api.parallel.ExecutionMode;

import com.bonitasoft.test.toolkit.BonitaTestToolkit;
import com.bonitasoft.test.toolkit.junit.extension.BonitaTests;
import com.bonitasoft.test.toolkit.model.BusinessData;
import static com.bonitasoft.test.toolkit.predicate.TaskPredicates.taskArchived;
import com.bonitasoft.test.toolkit.model.Task;

@BonitaTests
@Execution(ExecutionMode.CONCURRENT) // Optional, execute each test method in parallel
class InventoryRequestIT{
    @Test
    void testStockOut(BonitaTestToolkit toolkit) {

        Assertions.setPrintAssertionsDescription(true);

        // Get the user that will start the process
        var walterBates = toolkit.getUser("walter.bates");
        // Get my process (pool name)
        var myRequestProcess = toolkit.getProcessDefinition("InventoryRequest");

        //Build the process inputs for the contract
        var complexInputBuilder = complexInput()
                .textInput("itemID", "C012504")
                .integerInput("qtyrequested", 5);

        // Param1 : name of the contract
        var myinstanciationcontract = newContract().complexInput("arequestInput", complexInputBuilder).build();
        
                // ----Start process----
        var processInstance = myRequestProcess.startProcessFor(walterBates, myinstanciationcontract);
        await("MyRequestProcess instance has not been started and Validate Request task is not ready").until(processInstance, processInstanceStarted()
                .and(containsPendingUserTasks("Check the Stock"))); 
        
        // Check the correct  data initiation
        // Data assertions
        BusinessData request= processInstance.getBusinessData("arequest");

        assertThat(request.getStringField("itemID")).as("===> Check the first attribute  %s", request.getStringField("itemID")).isEqualTo("C012504");
        assertThat(request.getIntegerField("qtyrequested")).isEqualTo(5);

        // ----Execute the first human task----
        //Get the user that should be able to execute the task
        var helenKelly = toolkit.getUser("helen.kelly");

        // Get the task
        var invcheckTask = processInstance.getFirstPendingUserTask("Check the Stock");

        //Check that Helen kelly can execute the task
        assertThat(invcheckTask.getCandidates()).contains(helenKelly);

        //Execute the inventory check
        //Build the process inputs for the task contract
        var complexInvCheck = complexInput().booleanInput("instock", false);

        // checktask contract
        var invcheckcontract = newContract().complexInput("invcheckInput", complexInvCheck).build();

        //Run inv check with a stock out
        invcheckTask.execute(helenKelly, invcheckcontract);

        //Wait for the rest of the process to be executed
        await().until(invcheckTask, taskArchived());
        await().until(processInstance, processInstanceArchived());

        //Verify that the expected process branch is used - check order and status
        assertThat(processInstance.searchTasks()).map(Task::getName).as("Correct path used").containsExactlyInAnyOrder("Check the Stock", "Send Refusal Notification");
        assertThat(processInstance.getFirstTask("Send Refusal Notification").isArchived()).isTrue();

        // Check the business variables
        assertThat(request.getBooleanField("instock")).isFalse();

    }
    @Test
    void requestValid(BonitaTestToolkit toolkit) {

        Assertions.setPrintAssertionsDescription(true);

        // Get the user that will start the process
        var walterBates = toolkit.getUser("walter.bates");
        // Get my process (pool name)
        var myRequestProcess = toolkit.getProcessDefinition("InventoryRequest");

        //Build the process inputs for the contract
        var complexInputBuilder = complexInput()
                .textInput("itemID", "C012507")
                .integerInput("qtyrequested", 100);

        // Param1 : name of the contract
        var myinstanciationcontract = newContract().complexInput("arequestInput", complexInputBuilder).build();
        
        // ----Start process----
        var processInstance = myRequestProcess.startProcessFor(walterBates, myinstanciationcontract);
        await("MyRequestProcess instance has not been started and Validate Request task is not ready").until(processInstance, processInstanceStarted()
                .and(containsPendingUserTasks("Check the Stock"))); 
        
        // Check the correct  data initiation
        // Data assertions
        Request request= processInstance.getBusinessData("arequest", Request.class);

        //Make sure that we only have one
        assertThat(request.getItemID()).as("===> Check the first attribute  %s", request.getItemID()).isEqualTo("C012507");
        assertThat(request.getQtyrequested()).isEqualTo(100);

        // ----Execute the first human task----
        //Get the user that should be able to execute the task
        var helenKelly = toolkit.getUser("helen.kelly");

        // Get the task
        var invcheckTask = processInstance.getFirstPendingUserTask("Check the Stock");

        //Check that Helen kelly can execute the task
        assertThat(invcheckTask.getCandidates()).contains(helenKelly);

        //Execute the inventory check
        //Build the process inputs for the task contract
        var complexInvCheck = complexInput().booleanInput("instock", true);

        // checktask contract
        var invcheckcontract = newContract().complexInput("invcheckInput", complexInvCheck).build();

        //Run inv check with a stock out
        invcheckTask.execute(helenKelly, invcheckcontract);

        //Wait for the rest of the process to be executed
        await().until(invcheckTask, taskArchived());
        await().until(processInstance, processInstanceArchived());

        //Verify that the expected process branch is used - check order and status
        assertThat(processInstance.searchTasks()).map(Task::getName).as("Correct path used").containsExactlyInAnyOrder("Check the Stock", "Send Item");
        assertThat(processInstance.getFirstTask("Send Item").isArchived()).isTrue();

        // Check the business variables
        request= processInstance.getBusinessData("arequest", Request.class);
        assertThat(request.getInstock()).isTrue();

    }

    interface Request{
            long getPersistenceId();
            String getItemID();
            Boolean getInstock();
            Integer getQtyrequested();
    }
}