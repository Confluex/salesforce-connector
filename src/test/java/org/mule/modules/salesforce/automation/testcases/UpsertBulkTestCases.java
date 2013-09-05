/**
 * Mule Salesforce Connector
 *
 * Copyright (c) MuleSoft, Inc.  All rights reserved.  http://www.mulesoft.com
 *
 * The software in this package is published under the terms of the CPAL v1.0
 * license, a copy of which has been included with this distribution in the
 * LICENSE.txt file.
 */

package org.mule.modules.salesforce.automation.testcases;

import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.experimental.categories.Category;
import org.mule.api.MuleEvent;
import org.mule.api.processor.MessageProcessor;

import com.sforce.async.BatchInfo;
import com.sforce.soap.partner.SaveResult;



public class UpsertBulkTestCases extends SalesforceTestParent {

	
	private MessageProcessor createSingleFlow;
	private MessageProcessor upsertBulkFlow;
	private MessageProcessor batchInfoFlow;
	private MessageProcessor batchResultFlow;
	private MessageProcessor deleteFlow;

    Map<String, Object> refAccountTestObjects;
    String refAccountId;

	@Before
	public void setUp() {
		
		createSingleFlow = lookupFlowConstruct("create-single-from-message");
    	upsertBulkFlow = lookupFlowConstruct("upsert-bulk");
    	batchInfoFlow = lookupFlowConstruct("batch-info");
    	batchResultFlow = lookupFlowConstruct("batch-result");
		deleteFlow = lookupFlowConstruct("delete-from-message");
    	
		try {

            refAccountId = createAccountToRef();
            MuleEvent response;

			testObjects = (HashMap<String,Object>) context.getBean("upsertBulkTestData");

	        response = createSingleFlow.process(getTestEvent(testObjects));
	        SaveResult saveResult = (SaveResult) response.getMessage().getPayload();
			((HashMap<String,Object>) context.getBean("upsertBulkSObjectToBeUpdated")).put("Id", saveResult.getId());
            ((HashMap<String,Object>) context.getBean("upsertBulkSObjectToBeUpdated")).put("WhatId", refAccountId);
            ((HashMap<String,Object>) context.getBean("upsertBulkSObjectToBeInserted")).put("WhatId", refAccountId);
		} catch (Exception e) {
			e.printStackTrace();
			fail();
		}
     
	}

    private String createAccountToRef() throws Exception {
        refAccountTestObjects = (HashMap<String, Object>) context.getBean("upsertAccountToRefSObjectMap");
        MuleEvent response = createSingleFlow.process(getTestEvent(refAccountTestObjects));
        final String refAccountId = response.getMessage().getPayload(SaveResult.class).getId();

        refAccountTestObjects.put("idsToDeleteFromMessage", new ArrayList<String>() {{
            add(refAccountId);
        }});

        return refAccountId;
    }

    @After
	public void tearDown() {
		
		try {
			
			if (testObjects.containsKey("idsToDeleteFromMessage")) {		
				deleteFlow.process(getTestEvent(testObjects));	
			}

            if (refAccountTestObjects.containsKey("idsToDeleteFromMessage")) {
                deleteFlow.process(getTestEvent(refAccountTestObjects));
            }

		} catch (Exception e) {
				e.printStackTrace();
				fail();
		}
		
	}
	
	@Category({RegressionTests.class})
	@Test
	public void testUpsertBulk() {
		
    	BatchInfo batchInfo;
		
		try {
			
			batchInfo = getBatchInfoByOperation(upsertBulkFlow);
			
			do {
				
				Thread.sleep(BATCH_PROCESSING_DELAY);
				testObjects.put("batchInfoRef", batchInfo);
				batchInfo = getBatchInfoByOperation(batchInfoFlow);

			} while (batchInfo.getState().equals(com.sforce.async.BatchStateEnum.InProgress) || batchInfo.getState().equals(com.sforce.async.BatchStateEnum.Queued));
	
			assertTrue(batchInfo.getState().equals(com.sforce.async.BatchStateEnum.Completed));
			
			assertBatchSuccessAndUpdatedSObjectId(getBatchResult(batchResultFlow));
		
		} catch (Exception e) {
			// TODO Auto-generated catch block
				e.printStackTrace();
				fail();
		}
		
	}

}