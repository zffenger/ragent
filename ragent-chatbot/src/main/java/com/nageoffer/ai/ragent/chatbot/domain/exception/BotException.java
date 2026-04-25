/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements.  See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.nageoffer.ai.ragent.chatbot.domain.exception;

import com.nageoffer.ai.ragent.framework.errorcode.IErrorCode;

/**
 * 机器人模块异常
 * <p>
 * 用于机器人模块内部的业务异常，继承自 RuntimeException
 */
public class BotException extends RuntimeException {

    /**
     * 错误码
     */
    private final String errorCode;

    /**
     * 错误信息
     */
    private final String errorMessage;

    public BotException(String errorMessage) {
        super(errorMessage);
        this.errorCode = "BOT_ERROR";
        this.errorMessage = errorMessage;
    }

    public BotException(String errorCode, String errorMessage) {
        super(errorMessage);
        this.errorCode = errorCode;
        this.errorMessage = errorMessage;
    }

    public BotException(String errorMessage, Throwable cause) {
        super(errorMessage, cause);
        this.errorCode = "BOT_ERROR";
        this.errorMessage = errorMessage;
    }

    public BotException(IErrorCode errorCode) {
        super(errorCode.message());
        this.errorCode = errorCode.code();
        this.errorMessage = errorCode.message();
    }

    public BotException(IErrorCode errorCode, Throwable cause) {
        super(errorCode.message(), cause);
        this.errorCode = errorCode.code();
        this.errorMessage = errorCode.message();
    }

    public String getErrorCode() {
        return errorCode;
    }

    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String toString() {
        return "BotException{" +
                "errorCode='" + errorCode + '\'' +
                ", errorMessage='" + errorMessage + '\'' +
                '}';
    }
}
