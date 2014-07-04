package com.sferadev.etic.taskModel;

import com.google.gson.annotations.SerializedName;

import org.odk.collect.android.database.TaskAssignment;

import java.util.List;

public class TaskResponse {

    public String message;
    public String status;
    @SerializedName("data")
    public List<TaskAssignment> taskAssignments;
}
