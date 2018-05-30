/*
 * Copyright 2017 Grzegorz Skorupa <g.skorupa at gmail.com>.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *       http://www.apache.org/licenses/LICENSE-2.0
 * 
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.signocom.signomix.client;

import org.cricketmsf.Kernel;
import org.cricketmsf.Runner;
import org.junit.*;
import org.junit.Test;

/**
 *
 * @author Grzegorz Skorupa <g.skorupa at gmail.com>
 */
public class NewEmptyJUnitTest {
    
    static Kernel service;
    
    public NewEmptyJUnitTest() {
    }
    
    @Test
    public void testMe(){
        System.out.println("test");
        Assert.assertNull(service);
    }
    @Test
    public void testMe2(){
        System.out.println("test2");
    }
    
    @BeforeClass
    public static void setup(){
        System.out.println("@setup");
        
    }
    
    @AfterClass
    public static void shutdown(){
        service.shutdown();
        System.out.println("@shutdown");
    }
    
    @Before
    public void before(){
        System.out.println("@before");
    }
    
}
