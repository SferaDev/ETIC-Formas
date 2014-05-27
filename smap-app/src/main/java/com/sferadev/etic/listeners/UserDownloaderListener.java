/*
 * Copyright (C) TODO
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.sferadev.etic.listeners;

import java.util.HashMap;

import com.sferadev.etic.taskModel.UserDetail;

import android.os.Bundle;

/**
 * @author Nick Huynh (minqiang.huang@gmail.com)
 */
public interface UserDownloaderListener {
    void userDownloadingComplete(HashMap<String, UserDetail> result);
    void userDownloadingProgressUpdate(String progress);
}