/*
 * Copyright © 2015 Rashmi and others.  All rights reserved.
 *
 * This program and the accompanying materials are made available under the
 * terms of the Eclipse Public License v1.0 which accompanies this distribution,
 * and is available at http://www.eclipse.org/legal/epl-v10.html
 */
package org.opendaylight.messenger.impl;

import java.util.concurrent.Future;

import org.opendaylight.controller.md.sal.binding.api.DataBroker;
import org.opendaylight.controller.md.sal.binding.api.ReadOnlyTransaction;
import org.opendaylight.controller.md.sal.binding.api.WriteTransaction;
import org.opendaylight.controller.md.sal.common.api.data.LogicalDatastoreType;
import org.opendaylight.controller.md.sal.common.api.data.ReadFailedException;
import org.opendaylight.controller.md.sal.common.api.data.TransactionCommitFailedException;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.ProviderContext;
import org.opendaylight.controller.sal.binding.api.BindingAwareBroker.RpcRegistration;
import org.opendaylight.controller.sal.binding.api.BindingAwareProvider;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.messenger.rev170817.GreetingRegistry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.messenger.rev170817.GreetingRegistryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.messenger.rev170817.MessengerService;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.messenger.rev170817.MessengerWorldInput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.messenger.rev170817.MessengerWorldOutput;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.messenger.rev170817.MessengerWorldOutputBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.messenger.rev170817.greeting.registry.GreetingRegistryEntry;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.messenger.rev170817.greeting.registry.GreetingRegistryEntryBuilder;
import org.opendaylight.yang.gen.v1.urn.opendaylight.params.xml.ns.yang.messenger.rev170817.greeting.registry.GreetingRegistryEntryKey;
import org.opendaylight.yangtools.yang.binding.InstanceIdentifier;
import org.opendaylight.yangtools.yang.common.RpcResult;
import org.opendaylight.yangtools.yang.common.RpcResultBuilder;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import java.util.StringTokenizer;
import java.util.ArrayList;



import com.google.common.base.Optional;
import com.google.common.util.concurrent.CheckedFuture;
import com.google.common.util.concurrent.FutureCallback;
import com.google.common.util.concurrent.Futures;

import java.io.File;
import java.io.IOException;
import java.io.FileWriter;
import java.util.Scanner;
import org.json.JSONObject;
import org.json.JSONArray;

import java.net.URL;
import java.net.URI;
import java.net.URISyntaxException;


public class MessengerProvider implements BindingAwareProvider, AutoCloseable, MessengerService {

    private static final Logger LOG = LoggerFactory.getLogger(MessengerProvider.class);
    private RpcRegistration<MessengerService> MessengerService;
    private DataBroker db;

    @Override
    public void onSessionInitiated(ProviderContext session) {
        LOG.info("MessengerProvider Session Initiated");
        db = session.getSALService(DataBroker.class);
        MessengerService = session.addRpcImplementation(MessengerService.class, this);

        initializeDataTree(db);
    }

    @Override
    public void close() throws Exception {
        LOG.info("MessengerProvider Closed");
        if (MessengerService != null) {
        	MessengerService.close();
        }
    }

    @Override
    public Future<RpcResult<MessengerWorldOutput>> messengerWorld(MessengerWorldInput input) {
    	MessengerWorldOutput output = new MessengerWorldOutputBuilder()
                .setGreeting(readFromGreetingRegistry(input))
                .build();
        writeToGreetingRegistry(input, output);
        return RpcResultBuilder.success(output).buildFuture();
    }

    private void initializeDataTree(DataBroker db) {
        LOG.info("Preparing to initialize the greeting registry");
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        InstanceIdentifier<GreetingRegistry> iid = InstanceIdentifier.create(GreetingRegistry.class);
        GreetingRegistry greetingRegistry = new GreetingRegistryBuilder()
                .build();
        transaction.put(LogicalDatastoreType.OPERATIONAL, iid, greetingRegistry);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        Futures.addCallback(future, new LoggingFuturesCallBack<>("Failed to create greeting registry", LOG));
    }

    private void writeToGreetingRegistry(MessengerWorldInput input, MessengerWorldOutput output) {
        WriteTransaction transaction = db.newWriteOnlyTransaction();
        InstanceIdentifier<GreetingRegistryEntry> iid = toInstanceIdentifier(input);
        GreetingRegistryEntry greeting = new GreetingRegistryEntryBuilder()
                .setGreeting(output.getGreeting())
                .setName(input.getName())
                .build();
        transaction.put(LogicalDatastoreType.OPERATIONAL, iid, greeting);
        CheckedFuture<Void, TransactionCommitFailedException> future = transaction.submit();
        Futures.addCallback(future, new LoggingFuturesCallBack<Void>("Failed to write greeting to greeting registry", LOG));
    }

    private InstanceIdentifier<GreetingRegistryEntry> toInstanceIdentifier(MessengerWorldInput input) {
        InstanceIdentifier<GreetingRegistryEntry> iid = InstanceIdentifier.create(GreetingRegistry.class)
            .child(GreetingRegistryEntry.class, new GreetingRegistryEntryKey(input.getName()));
        return iid;
    }

    private String readFromGreetingRegistry(MessengerWorldInput input) {
        String result = "Hello " + input.getName();
        ReadOnlyTransaction transaction = db.newReadOnlyTransaction();
        InstanceIdentifier<GreetingRegistryEntry> iid = toInstanceIdentifier(input);
        CheckedFuture<Optional<GreetingRegistryEntry>, ReadFailedException> future =
                transaction.read(LogicalDatastoreType.CONFIGURATION, iid);
        Optional<GreetingRegistryEntry> optional = Optional.absent();
        try {
            optional = future.checkedGet();
        } catch (ReadFailedException e) {
            LOG.warn("Reading greeting failed:",e);
        }
        if(optional.isPresent()) {
            result = optional.get().getGreeting();
        }
        return result;
    }
    
    // i. Parse the CSV file in Java

    public String getFile(String fileName) {

    	StringBuilder result = new StringBuilder("");
    	
    	LOG.info("getFile starting **********************************************");

    	//Get file from resources folder
    	ClassLoader classLoader = getClass().getClassLoader();
    	File file = new File(classLoader.getResource(fileName).getFile());

    	try (Scanner scanner = new Scanner(file)) {

    		while (scanner.hasNextLine()) {
    			String line = scanner.nextLine();
    			LOG.warn("line ---> " + line);
    			System.out.println("fettah ---> " + line);
    			result.append(line).append("\n");
    		}

    		scanner.close();

    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	
    	LOG.info("getFile ending **********************************************");

    	return result.toString();

      }
    
    // i. Parse the CSV file in JSON
    public void parseFiletoJSON(String fileName) {
    	
    	LOG.info("parseFiletoJSON starting **********************************************");
    	//Get file from resources folder
    	ClassLoader classLoader = getClass().getClassLoader();
    	File file = new File(classLoader.getResource(fileName).getFile());
    	
    	boolean buildHeader = true;
    	StringTokenizer st = null;
    	ArrayList<String> headerElements = new ArrayList<String>();
    	ArrayList<String> valueElements = new ArrayList<String>();
    	
    	JSONObject objPeriodic = new JSONObject();
    	JSONArray elementList = new JSONArray();
    	objPeriodic.put("ChemicalElements",elementList);
    	
    	try (Scanner scanner = new Scanner(file)) {

    		while (scanner.hasNextLine()) {
    			String line = scanner.nextLine();
    			st = new StringTokenizer(line, ",");
    			if ( buildHeader )
    			{
    				 
                     while(st.hasMoreTokens())
                     {
                             
                    	     headerElements.add(st.nextToken());
     
                     }
                     buildHeader = false;
                     
                 	// System.out.println("fettah   Header size ---> " + headerElements.size());
                	// System.out.println("fettah   Header elements ---> " + headerElements);
                	
    			}
    			else
    			{
   				 
	    			 valueElements.clear();
	                 while(st.hasMoreTokens())
	                 {
	                        
	                	     valueElements.add(st.nextToken());
	 
	                 }
	                 
	                 
	                 for (int i = 0; i < headerElements.size(); i++) {
	         			
	         			String elementName = headerElements.get(i);
	         			String elementValue = "";
	         			if ( i < valueElements.size() )
	         				elementValue = valueElements.get(i);
	         			JSONObject obj = new JSONObject();
	         	        obj.put(elementName, elementValue);
	         	        elementList.put(obj);
	          		 }
	          		 
	                 
	             	// System.out.println("fettah   valueElements size ---> " + valueElements.size());
	            	// System.out.println("fettah   valueElements elements ---> " + valueElements);
	            	
    				
    			}
    			
    			
    		}

    		scanner.close();

    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	
    
    		
    		
    		String dir2 = classLoader.getResource(fileName).getFile();
    		int index = dir2.indexOf("target");
    		if ( index != -1 )
    		{
    			String dir22 = dir2.substring(0, index);
    			
	    		
	    		File fileOutput = new File(dir22 + "src/main/resources/Periodic_JSON.txt");
		    	FileWriter fr = null;
		        try {
		        	fr = new FileWriter(fileOutput);
		            fr.write(objPeriodic.toString());
		        } catch (IOException e) {
		            e.printStackTrace();
		        }finally{
		            //close resources
		            try {
		                fr.close();
		            } catch (IOException e) {
		                e.printStackTrace();
		            }
		        }
    		}
	        
    		LOG.info("parseFiletoJSON ending **********************************************");
    	
      }    
    
    
    // i. Parse the CSV file in XML
    public void parseFiletoXML(String fileName) {
    	
    	LOG.info("parseFiletoXML starting **********************************************");
    	
    	StringBuilder result = new StringBuilder("");
    	result.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>\n");
    	result.append("<PeriodicElements>\n");
    	

    	//Get file from resources folder
    	ClassLoader classLoader = getClass().getClassLoader();
    	File file = new File(classLoader.getResource(fileName).getFile());
    	
    	boolean buildHeader = true;
    	StringTokenizer st = null;
    	ArrayList<String> headerElements = new ArrayList<String>();
    	ArrayList<String> valueElements = new ArrayList<String>();
    	
    	
    	try (Scanner scanner = new Scanner(file)) {

    		while (scanner.hasNextLine()) {
    			String line = scanner.nextLine();
    			st = new StringTokenizer(line, ",");
    			if ( buildHeader )
    			{
    				 
                     while(st.hasMoreTokens())
                     {
                             
                    	     headerElements.add(st.nextToken());
     
                     }
                     buildHeader = false;
                     
       
    			}
    			else
    			{
   				 
	    			 valueElements.clear();
	                 while(st.hasMoreTokens())
	                 {
	                        
	                	     valueElements.add(st.nextToken());
	 
	                 }
	                 
	                 result.append("<PeriodicElement>");
	                 for (int i = 0; i < headerElements.size(); i++) {
	         			
	         			String elementName = headerElements.get(i);
	         			String elementValue = "";
	         			if ( i < valueElements.size() )
	         				elementValue = valueElements.get(i);
	         			
	         			result.append("<"+ elementName+">" + elementValue + "</"+ elementName+">");
	         		
			
	          		 }
	                 result.append("</PeriodicElement>\n");
	          		 
	          
    			}
    			
    			
    		}

    		scanner.close();

    	} catch (IOException e) {
    		e.printStackTrace();
    	}
    	
    	result.append("</PeriodicElements>");
    		
    	
    		String dir2 = classLoader.getResource(fileName).getFile();
    		int index = dir2.indexOf("target");
    		if ( index != -1 )
    		{
    			String dir22 = dir2.substring(0, index);
    			System.out.println("fettah   dir22 ---> " + dir22);
    		
    		
    			File fileOutput = new File(dir22 + "src/main/resources/Periodic_XML.xml");
		    	FileWriter fr = null;
		        try {
		        	fr = new FileWriter(fileOutput);
		            fr.write(result.toString());
		        } catch (IOException e) {
		            e.printStackTrace();
		        }finally{
		            //close resources
		            try {
		                fr.close();
		            } catch (IOException e) {
		                e.printStackTrace();
		            }
		        }		
    		
	    	
    		}
	        
    		LOG.info("parseFiletoXML starting **********************************************");
    	
      }    
}
