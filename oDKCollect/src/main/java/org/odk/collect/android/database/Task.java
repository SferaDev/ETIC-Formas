package org.odk.collect.android.database;

import java.util.Date;

public class Task {
	public int id;
	public int location_id;
	public String type;
	public String title;
	public String url;
	public String form_id;
	public String initial_data;
	public String assignment_mode;
	public boolean gps;
	public boolean camera;
	public boolean barcode;
	public boolean rfid;
	public int repeats;
	public int priority;
	public Date scheduled_at;
	public String address;			// Key value pairs representing an unstructured address
	public Date from_date;
	public Date due_date;
	public Date created_date;
	public String created_by;
	public String status;
}
