package com.sferadev.etic.taskModel;

import java.util.List;

import com.google.gson.annotations.SerializedName;
import org.odk.collect.android.database.TaskAssignment;

public class TaskResponse {

	public String message;
	public String status;
	@SerializedName("data")
	public List<TaskAssignment> taskAssignments;
}
