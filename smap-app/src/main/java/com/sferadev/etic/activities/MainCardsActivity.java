/*
 * Copyright (C) 2014 Alexis Rico - SferaDev
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

 /**
 * @author Alexis Rico - SferaDev
 * based on Neil Penman's work
 */

package com.sferadev.etic.activities;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.InputType;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import com.fima.cardsui.views.CardUI;
import com.sferadev.etic.R;
import com.sferadev.etic.listeners.TaskDownloaderListener;
import com.sferadev.etic.tasks.DownloadTasksTask;

import org.odk.collect.android.activities.FormDownloadList;
import org.odk.collect.android.application.Collect;
import org.odk.collect.android.preferences.AdminPreferencesActivity;
import org.odk.collect.android.preferences.PreferencesActivity;
import org.odk.collect.android.utilities.CompatibilityUtils;

import java.util.HashMap;
import java.util.Iterator;
import java.util.Set;

public class MainCardsActivity extends Activity implements TaskDownloaderListener{

    //CardUI
    private CardUI mCardMain;

    private AlertDialog mAlertDialog;
    private static final int PROGRESS_DIALOG = 1;
    private static final int ALERT_DIALOG = 2;
	private static final int PASSWORD_DIALOG = 3;
    
 // request codes for returning chosen form to main menu.
    private static final int FORM_CHOOSER = 0;
    private static final int INSTANCE_UPLOADER = 2;
    
    private static final int MENU_PREFERENCES = Menu.FIRST;
    private static final int MENU_GETFORMS = Menu.FIRST + 1;

    private String mProgressMsg;
    private String mAlertMsg;
    private ProgressDialog mProgressDialog;  
    public DownloadTasksTask mDownloadTasks;
	private Context mContext;
    SharedPreferences sharedPreferences;
	private SharedPreferences mAdminPreferences;
    
	public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // must be at the beginning of any activity that can be called from an external intent
        try {
            Collect.createODKDirs();
        } catch (RuntimeException e) {
            createErrorDialog(e.getMessage(), true);
            return;
        }

        setContentView(R.layout.main_cards);

        // init CardView
        mCardMain = (CardUI) findViewById(R.id.cardsview);
        loadCards();
        mCardMain.refresh();

        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        if(sharedPreferences.getString("username", null) == null){
            signIn();
        }
    }

    public void loadCards(){
        MyBaseCard cEnterData = new MyBaseCard(getResources().getString(R.string.etic_enter_data), getResources().getString(R.string.etic_enter_data_caption), "#FF4444", "#CC0000", false, false);
        cEnterData.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                processEnterData();
            }
        });
        mCardMain.addCard(cEnterData);

        MyBaseCard cSendData = new MyBaseCard(getResources().getString(R.string.etic_send_data), getResources().getString(R.string.etic_send_data_caption), "#FFBB33", "#FF8800", false, false);
        cSendData.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                processSendData();
            }
        });
        mCardMain.addCard(cSendData);

        MyBaseCard cManageFiles = new MyBaseCard(getResources().getString(R.string.etic_delete_data), getResources().getString(R.string.etic_delete_data_caption), "#AA66CC", "#9933CC", false, false);
        cManageFiles.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                processManageFiles();
            }
        });
        mCardMain.addCard(cManageFiles);
    }

    private void signIn() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        AlertDialog dialog;
        LayoutInflater inflater = this.getLayoutInflater();

        builder.setView(inflater.inflate(R.layout.dialog_signin, null))
                .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        Dialog f = (Dialog) dialog;
                        EditText username = (EditText) f.findViewById(R.id.dialog_username);
                        EditText password = (EditText) f.findViewById(R.id.dialog_password);
                        SharedPreferences.Editor editor = sharedPreferences.edit();
                        editor.putString("username", username.getText().toString());
                        editor.putString("password", password.getText().toString());
                        editor.commit();
                    }
                })
                .setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int id) {
                        dialog.cancel();
                    }
                });
        dialog = builder.create();
        dialog.show();
    };

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);

		CompatibilityUtils.setShowAsAction(
				menu.add(0, MENU_GETFORMS, 1, R.string.etic_get_forms).setIcon(
						android.R.drawable.ic_menu_add),
						MenuItem.SHOW_AS_ACTION_IF_ROOM);
		
		CompatibilityUtils.setShowAsAction(
				menu.add(0, MENU_PREFERENCES, 2, R.string.etic_settings).setIcon(
						android.R.drawable.ic_menu_preferences),
						MenuItem.SHOW_AS_ACTION_IF_ROOM);
        return true;
    }
    
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case MENU_PREFERENCES:
            	createPreferencesMenu();
                return true;
            case MENU_GETFORMS:
                processGetForms();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
    
    
    private void createErrorDialog(String errorMsg, final boolean shouldExit) {
        mAlertDialog = new AlertDialog.Builder(this).create();
        mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
        mAlertDialog.setMessage(errorMsg);
        DialogInterface.OnClickListener errorListener = new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int i) {
                switch (i) {
                    case DialogInterface.BUTTON1:
                        if (shouldExit) {
                            finish();
                        }
                        break;
                }
            }
        };
        mAlertDialog.setCancelable(false);
        mAlertDialog.setButton(getString(R.string.ok), errorListener);
        mAlertDialog.show();
    }
    
    /*
     * Process menu options
     */
    public void createPreferencesMenu() {
        Intent i = new Intent(this, PreferencesActivity.class);
        startActivity(i);
    }
    
    private void processEnterData() {
    	Intent i = new Intent(getApplicationContext(), org.odk.collect.android.activities.FormChooserList.class);
        startActivityForResult(i, FORM_CHOOSER);
    }
    
    // Get new forms
    private void processGetForms() {   
    	
		Collect.getInstance().getActivityLogger().logAction(this, "downloadBlankForms", "click");
		Intent i = new Intent(getApplicationContext(), FormDownloadList.class);
		startActivity(i);
    }
    
    // Send data
    private void processSendData() {
    	Intent i = new Intent(getApplicationContext(), org.odk.collect.android.activities.InstanceUploaderList.class);
        startActivityForResult(i, INSTANCE_UPLOADER);
    }
    
    // Get tasks from the task management server
    private void processGetTask() {   
    	
    	mProgressMsg = getString(R.string.smap_synchronising);	
    	showDialog(PROGRESS_DIALOG);
        mDownloadTasks = new DownloadTasksTask();
        mDownloadTasks.setDownloaderListener(this, mContext);
        mDownloadTasks.execute();
    }
    
	/*
	 * Download task methods
	 */
	public void progressUpdate(String progress) {
		mProgressMsg = progress;
		mProgressDialog.setMessage(mProgressMsg);		
	}
	
    private void processManageFiles() {
    	Intent i = new Intent(getApplicationContext(), org.odk.collect.android.activities.FileManagerTabs.class);
        startActivity(i);
    }
    
    /*
	 */
	public void taskDownloadingComplete(HashMap<String, String> result) {
		try {
            dismissDialog(PROGRESS_DIALOG);
            removeDialog(PROGRESS_DIALOG);
        } catch (Exception e) {
            // tried to close a dialog not open. don't care.
        }
		try {
			dismissDialog(ALERT_DIALOG);
            removeDialog(ALERT_DIALOG);
        } catch (Exception e) {
            // tried to close a dialog not open. don't care.
        }

		if(result != null) {
	        StringBuilder message = new StringBuilder();
	        Set<String> keys = result.keySet();
	        Iterator<String> it = keys.iterator();
	
	        //String[] selectionArgs = new String[keys.size()];
	        while (it.hasNext()) {
	            String key = it.next();
	            if(key.equals("err_not_enabled")) {
	            	message.append(this.getString(R.string.smap_tasks_not_enabled));
	            } else if(key.equals("err_no_tasks")) {
	            	message.append(this.getString(R.string.smap_no_tasks));
	            } else {	
	            	message.append(key + " - " + result.get(key) + "\n\n");
	            }
	        }
	
	        mAlertMsg = message.toString().trim();
	    	showDialog(ALERT_DIALOG);
		}
	}
	
    /**
	 * Dismiss any showing dialogs that we manage.
	 */
	private void dismissDialogs() {
		if (mAlertDialog != null && mAlertDialog.isShowing()) {
			mAlertDialog.dismiss();
		}
	}

    /*
     * (non-Javadoc)
     * @see android.app.Activity#onActivityResult(int, int, android.content.Intent)
     * Debug code used in development of new Intents
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent intent) {
    	if(resultCode == RESULT_OK) {
	        switch (requestCode) {
	            // returns with a form path, start entry
	            case 10:
	            	Log.i("MainActivityList", "onActivityResult");
	            	if (intent.hasExtra("status")) {
	            		String status = intent.getExtras().getString("status");
	            		if(status.equals("success")) {
	            			if (intent.hasExtra("instanceUri")) {
	    	            		String instanceUri = intent.getExtras().getString("instanceUri");
	    	            		Log.i("MainListActivity uri", instanceUri);
	    	                	Intent i = new Intent(this, org.odk.collect.android.activities.FormEntryActivity.class);
	    	                	Uri inst = Uri.parse(instanceUri);
	    	                	i.setData(inst);
	    	                	startActivityForResult(i, 10);
	    	            	}

	            		} else {
	            			if (intent.hasExtra("message")) {
	    	            		String message = intent.getExtras().getString("message");
	    	            		Log.e("MainListActivity", message);
	            			}

	            		}
	            	}

	                break;
	            default:
	                break;
	        }
	        super.onActivityResult(requestCode, resultCode, intent);
    	}
    }
    
    @Override
    protected Dialog onCreateDialog(int id) {
        switch (id) {
            case PROGRESS_DIALOG:
                mProgressDialog = new ProgressDialog(this);
                DialogInterface.OnClickListener loadingButtonListener =
                    new DialogInterface.OnClickListener() {
						public void onClick(DialogInterface dialog, int which) {
                            dialog.dismiss();
                            mDownloadTasks.setDownloaderListener(null, mContext);
                            mDownloadTasks.cancel(true);
                        }
                    };
                mProgressDialog.setTitle(getString(R.string.downloading_data));
                mProgressDialog.setMessage(mProgressMsg);
                mProgressDialog.setIcon(android.R.drawable.ic_dialog_info);
                mProgressDialog.setIndeterminate(true);
                mProgressDialog.setCancelable(false);
                mProgressDialog.setButton(getString(R.string.cancel), loadingButtonListener);
                return mProgressDialog;
            case ALERT_DIALOG:
                mAlertDialog = new AlertDialog.Builder(this).create();
                mAlertDialog.setMessage(mAlertMsg);
                mAlertDialog.setTitle(getString(R.string.smap_get_tasks));
                DialogInterface.OnClickListener quitListener = new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int i) {
                    	dialog.dismiss();
                    }
                };
                mAlertDialog.setCancelable(false);
                mAlertDialog.setButton(getString(R.string.ok), quitListener);
                mAlertDialog.setIcon(android.R.drawable.ic_dialog_info);
                return mAlertDialog;
    		case PASSWORD_DIALOG:
    			AlertDialog.Builder builder = new AlertDialog.Builder(this);
    			final AlertDialog passwordDialog = builder.create();

    			passwordDialog.setTitle(getString(R.string.enter_admin_password));
    			final EditText input = new EditText(this);
    			input.setInputType(InputType.TYPE_TEXT_VARIATION_PASSWORD);
    			input.setTransformationMethod(PasswordTransformationMethod
    					.getInstance());
    			passwordDialog.setView(input, 20, 10, 20, 10);

    			passwordDialog.setButton(AlertDialog.BUTTON_POSITIVE,
    					getString(R.string.ok),
    					new DialogInterface.OnClickListener() {
    						public void onClick(DialogInterface dialog,
    								int whichButton) {
    							String value = input.getText().toString();
    							String pw = mAdminPreferences.getString(
    									AdminPreferencesActivity.KEY_ADMIN_PW, "");
    							if (pw.compareTo(value) == 0) {
    								Intent i = new Intent(getApplicationContext(),
    										AdminPreferencesActivity.class);
    								startActivity(i);
    								input.setText("");
    								passwordDialog.dismiss();
    							} else {
    								Toast.makeText(
    										MainCardsActivity.this,
    										getString(R.string.admin_password_incorrect),
    										Toast.LENGTH_SHORT).show();
    								Collect.getInstance()
    										.getActivityLogger()
    										.logAction(this, "adminPasswordDialog",
    												"PASSWORD_INCORRECT");
    							}
    						}
    					});

    			passwordDialog.setButton(AlertDialog.BUTTON_NEGATIVE,
    					getString(R.string.cancel),
    					new DialogInterface.OnClickListener() {

    						public void onClick(DialogInterface dialog, int which) {
    							Collect.getInstance()
    									.getActivityLogger()
    									.logAction(this, "adminPasswordDialog",
    											"cancel");
    							input.setText("");
    							return;
    						}
    					});

    			passwordDialog.getWindow().setSoftInputMode(
    					WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
    			return passwordDialog;
        }
        return null;
    }

}
