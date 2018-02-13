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

/**
 *
 * @author Grzegorz Skorupa <g.skorupa at gmail.com>
 */
public class ClientException extends Exception {
    
    private static final int SESSION_EXPIRED = 401;
    private static final int UNKNOWN = 1000;
    private int code;
    private String message;
    
    public ClientException(int code){
        this.code = code;
        this.message = "";
    }
    
    public ClientException(int code, String message){
        this.code = code;
        this.message = message;
    }
    
    public int getCode(){
        return code;
    }
    
    public String getMessage(){
        return "HTTP error code "+getCode()+ ": "+message;
    }
}
