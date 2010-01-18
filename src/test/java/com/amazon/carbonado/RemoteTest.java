/*
 * Copyright 2010 Amazon Technologies, Inc. or its affiliates.
 * Amazon, Amazon.com and Carbonado are trademarks or registered trademarks
 * of Amazon Technologies, Inc. or its affiliates.  All rights reserved.
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

package com.amazon.carbonado;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import static org.junit.Assert.*;

import org.junit.*;

import com.amazon.carbonado.repo.dirmi.*;

import org.cojen.dirmi.Environment;
import org.cojen.dirmi.Session;

import com.amazon.carbonado.stored.StorableTestBasic;
import com.amazon.carbonado.*;

/**
 * 
 *
 * @author Olga Kuznetsova
 */
public class RemoteTest {
    public static void main(String[] args) {
	org.junit.runner.JUnitCore.main(RemoteTest.class.getName());
    }

    @Test
    public void remoteBasicTest() throws Exception {
	Session[] sessionPair = new Environment().newSessionPair();
	sessionPair[0].send("hi");
	assertEquals("hi", sessionPair[1].receive());
    }

    @Test
    public void mapRepoInsertTest() {
	try{
	    Repository repo = com.amazon.carbonado.repo.map.MapRepositoryBuilder.newRepository();
	    
	    Session[] pair = new Environment().newSessionPair();
	    pair[0].send(RemoteRepositoryServer.from(repo));
	    RemoteRepository remoteRepo = (RemoteRepository) pair[1].receive();
	    
	    assertTrue(!(remoteRepo instanceof RemoteRepositoryServer));

	    Repository clientRepo = ClientRepository.from(remoteRepo);
	    assertTrue(!(clientRepo instanceof RemoteRepository));
	    assertTrue(clientRepo instanceof ClientRepository);
	    
	    Storage<StorableTestBasic> storage = repo.storageFor(StorableTestBasic.class);
	    StorableTestBasic stb = storage.prepare();
	    stb.setId(2);
	    if (!stb.tryLoad()) {
		stb.setStringProp("world");
		stb.setIntProp(321);
		stb.setLongProp(313244232323432L);
		stb.setDoubleProp(1.423423);
		stb.tryInsert();
	    }
	    
	    Storage <StorableTestBasic> clientstorage = clientRepo.storageFor(StorableTestBasic.class);
	    assertTrue(!(repo.equals(clientRepo)));
	    StorableTestBasic stb1 = clientstorage.prepare();
	    stb1.setId(2);
	    assertTrue(stb1.tryLoad());
	    assertEquals("world", stb1.getStringProp());
	    assertEquals(321, stb1.getIntProp());
	    assertEquals(313244232323432L, stb1.getLongProp());
	    assertEquals(1.423423, stb1.getDoubleProp());
	    
	    /////////////////////////
	    stb = storage.prepare();
	    stb.setId(3);
	    if (!stb.tryLoad()) {
		stb.setStringProp("hello");
		stb.setIntProp(1);
		stb.setLongProp(3L);
		stb.setDoubleProp(1.4);
		stb.tryInsert();
	    }	
	    
	    stb1 = clientstorage.prepare();
	    stb1.setId(3);
	    assertTrue(stb1.tryLoad());
	    assertEquals("hello", stb1.getStringProp());

	    ///////////////////////
	    stb1 = clientstorage.prepare();
	    stb1.setId(2);
	    assertTrue(stb1.tryLoad());
	    stb1.setStringProp("new world");
	    assertTrue(stb1.tryUpdate());
	    
	    stb = storage.prepare();
	    stb.setId(2);
	    assertTrue(stb.tryLoad());
	    assertEquals("new world", stb.getStringProp());
	} catch (Exception e) {
	    assertTrue(false);
	}
    }

    @Test
    public void anotherTest() throws Exception {
	Repository repo = com.amazon.carbonado.repo.map.MapRepositoryBuilder.newRepository();
	    
	Session[] pair = new Environment().newSessionPair();
	pair[0].send(RemoteRepositoryServer.from(repo));
        RemoteRepository remoteRepo = (RemoteRepository) pair[1].receive();
    }

    @Test
    public void transactionsTest() throws Exception {
	Repository repo = com.amazon.carbonado.repo.map.MapRepositoryBuilder.newRepository();
	Session[] pair = new Environment().newSessionPair();
	pair[0].send(RemoteRepositoryServer.from(repo));
	RemoteRepository remoteRepo = (RemoteRepository) pair[1].receive();
	final Repository clientRepo = ClientRepository.from(remoteRepo);

	final Storage<StorableTestBasic> clientStorage = clientRepo.storageFor(StorableTestBasic.class);
	StorableTestBasic stb = clientStorage.prepare();
	stb.setId(2);
	stb.setStringProp("world");
	stb.setIntProp(321);
	stb.setLongProp(313244232323432L);
	stb.setDoubleProp(1.423423);
	stb.insert();
	Transaction txn = clientRepo.enterTransaction();
	try {
	    stb.setStringProp("world1");
	    stb.update();
	    Thread otherThread = new Thread() {
		    public void run() {
			Transaction txn1 = 
			    clientRepo.enterTransaction(IsolationLevel.READ_COMMITTED);
			try {
			    StorableTestBasic b = clientStorage.prepare();
			    b.setId(2);
			    try {
				isSet = b.tryLoad();
				newTransactionValue = b.getStringProp();
			    } finally {
				txn1.exit();
			    }
			} catch (Exception e) {
			    isSet = false;
			}
		    }
		};
	    otherThread.start();
	    otherThread.join();
	    // assertEquals(true, isSet);
	    // assertEquals("world1", newTransactionValue);

	    txn.commit();
	} finally {
	    txn.exit();
	}
    }

    private volatile boolean isSet = true;
    private volatile String newTransactionValue;

    @Test
    public void transactionsAttachDetachTest() throws Exception {
	Repository repo = com.amazon.carbonado.repo.map.MapRepositoryBuilder.newRepository();
	Session[] pair = new Environment().newSessionPair();
	pair[0].send(RemoteRepositoryServer.from(repo));
	RemoteRepository remoteRepo = (RemoteRepository) pair[1].receive();
	final Repository clientRepo = ClientRepository.from(remoteRepo);

	final Storage<StorableTestBasic> clientStorage = clientRepo.storageFor(StorableTestBasic.class);
	StorableTestBasic stb = clientStorage.prepare();
	stb.setId(2);
	stb.setStringProp("world");
	stb.setIntProp(321);
	stb.setLongProp(313244232323432L);
	stb.setDoubleProp(1.423423);
	stb.insert();
	final Transaction txn = clientRepo.enterTransaction();
	try {
	    Query<StorableTestBasic> query = clientStorage.query();
	    Cursor<StorableTestBasic> cursor = query.fetch();
	    try {
		final Transaction txn1 = clientRepo.enterTransaction();
		Thread otherThread = new Thread() {
		    public void run() {
			//	txn.attach();
		    }
		};
	
		otherThread.start();
		otherThread.join();
	    } catch (Exception e) {
		assertTrue(false);
	    }
	} finally {
	    txn.exit();
	}

    }

    @Test
    public void querriesTest() throws Exception { 
	Repository repo = com.amazon.carbonado.repo.map.MapRepositoryBuilder.newRepository();
	Session[] pair = new Environment().newSessionPair();
	pair[0].send(RemoteRepositoryServer.from(repo));
	RemoteRepository remoteRepo = (RemoteRepository) pair[1].receive();
	Repository clientRepo = ClientRepository.from(remoteRepo);

	Storage<StorableTestBasic> clientStorage = clientRepo.storageFor(StorableTestBasic.class);
	StorableTestBasic stb = clientStorage.prepare();
	stb.setId(2);
	if (!stb.tryLoad()) {
	    stb.setStringProp("world");
	    stb.setIntProp(321);
	    stb.setLongProp(313244232323432L);
	    stb.setDoubleProp(1.423423);
	    stb.tryInsert();
	}


    }

    @Test
    public void sequenceGeneratorTest() { 

    }

    @Test
    public void exceptionTest() {

    }

    @Test
    public void threadTest() {

    }

    @Test
    public void closingTest() throws Exception {
	Repository repo = com.amazon.carbonado.repo.map.MapRepositoryBuilder.newRepository();
	Session[] pair = new Environment().newSessionPair();
	pair[0].send(RemoteRepositoryServer.from(repo));
	RemoteRepository remoteRepo = (RemoteRepository) pair[1].receive();
	Repository clientRepo = ClientRepository.from(remoteRepo);

	Storage<StorableTestBasic> clientStorage = clientRepo.storageFor(StorableTestBasic.class);
	StorableTestBasic stb = clientStorage.prepare();
	stb.setId(2);
	if (!stb.tryLoad()) {
	    stb.setStringProp("world");
	    stb.setIntProp(321);
	    stb.setLongProp(313244232323432L);
	    stb.setDoubleProp(1.423423);
	    stb.tryInsert();
	}

	clientRepo.close();

	Storage<StorableTestBasic> actualStorage = repo.storageFor(StorableTestBasic.class);
	StorableTestBasic stb1 = actualStorage.prepare();
	stb1.setId(3);
	boolean caught = false;
	try {
	    stb.tryLoad();
	} catch (IllegalStateException e) {
	    caught = true;
	}
	assertTrue(caught);
    }
}