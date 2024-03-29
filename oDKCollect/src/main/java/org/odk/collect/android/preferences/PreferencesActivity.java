/*
 * Copyright (C) 2011 University of Washington
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

package org.odk.collect.android.preferences;

import org.odk.collect.android.R;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.SharedPreferences.Editor;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.preference.CheckBoxPreference;
import android.preference.EditTextPreference;
import android.preference.ListPreference;
import android.preference.Preference;
import android.preference.Preference.OnPreferenceChangeListener;
import android.preference.PreferenceActivity;
import android.preference.PreferenceCategory;
import android.preference.PreferenceManager;
import android.preference.PreferenceScreen;
import android.provider.MediaStore.Images;
import android.text.InputFilter;
import android.text.Spanned;

/**
 * Handles general preferences.
 * 
 * @author Thomas Smyth, Sassafras Tech Collective (tom@sassafrastech.com; constraint behavior option)
 */
public class PreferencesActivity extends PreferenceActivity implements
		OnPreferenceChangeListener {

	protected static final int IMAGE_CHOOSER = 0;

	public static final String KEY_INFO = "info";
	public static final String KEY_LAST_VERSION = "lastVersion";
	public static final String KEY_FIRST_RUN = "firstRun";
	public static final String KEY_SHOW_SPLASH = "showSplash";
	public static final String KEY_SPLASH_PATH = "splashPath";
	public static final String KEY_FONT_SIZE = "font_size";
	public static final String KEY_SELECTED_GOOGLE_ACCOUNT = "selected_google_account";
	public static final String KEY_GOOGLE_SUBMISSION = "google_submission_id";

	public static final String KEY_USERNAME = "username";
	public static final String KEY_PASSWORD = "password";

	public static final String KEY_PROTOCOL = "protocol";

	// must match /res/arrays.xml
	public static final String PROTOCOL_ODK_DEFAULT = "odk_default";
	public static final String PROTOCOL_GOOGLE = "google";
	public static final String PROTOCOL_OTHER = "";

	public static final String NAVIGATION_SWIPE = "swipe";
	public static final String NAVIGATION_BUTTONS = "buttons";
	public static final String NAVIGATION_SWIPE_BUTTONS = "swipe_buttons";

	public static final String CONSTRAINT_BEHAVIOR_ON_SWIPE = "on_swipe";
	public static final String CONSTRAINT_BEHAVIOR_ON_FINALIZE = "on_finalize";
	public static final String CONSTRAINT_BEHAVIOR_DEFAULT = "on_swipe";

	public static final String KEY_FORMLIST_URL = "formlist_url";
	public static final String KEY_SUBMISSION_URL = "submission_url";

	public static final String KEY_COMPLETED_DEFAULT = "default_completed";

	public static final String KEY_AUTH = "auth";

	public static final String KEY_AUTOSEND_WIFI = "autosend_wifi";
	public static final String KEY_AUTOSEND_NETWORK = "autosend_network";

	public static final String KEY_NAVIGATION = "navigation";
	public static final String KEY_CONSTRAINT_BEHAVIOR = "constraint_behavior";

	private PreferenceScreen mSplashPathPreference;
	private EditTextPreference mSubmissionUrlPreference;
	private EditTextPreference mFormListUrlPreference;
    private EditTextPreference mUsernamePreference;
	private EditTextPreference mPasswordPreference;
	private ListPreference mFontSizePreference;
	private ListPreference mNavigationPreference;
	private ListPreference mConstraintBehaviorPreference;

	private CheckBoxPreference mAutosendWifiPreference;
	private CheckBoxPreference mAutosendNetworkPreference;
	private ListPreference mProtocolPreference;

	@Override
	protected void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);
		addPreferencesFromResource(R.xml.preferences);

		setTitle(getString(R.string.app_name) + " > "
				+ getString(R.string.general_preferences));

		// not super safe, but we're just putting in this mode to help
		// administrate
		// would require code to access it
		boolean adminMode = getIntent().getBooleanExtra("adminMode", false);

		SharedPreferences adminPreferences = getSharedPreferences(
				AdminPreferencesActivity.ADMIN_PREFERENCES, 0);

		boolean serverAvailable = adminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_CHANGE_SERVER, true);
		boolean urlAvailable = adminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_CHANGE_URL, true);

		PreferenceCategory autosendCategory = (PreferenceCategory) findPreference(getString(R.string.autosend));
		mAutosendWifiPreference = (CheckBoxPreference) findPreference(KEY_AUTOSEND_WIFI);
		boolean autosendWifiAvailable = adminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_AUTOSEND_WIFI, true);
		if (!(autosendWifiAvailable || adminMode)) {
			autosendCategory.removePreference(mAutosendWifiPreference);
		}

		mAutosendNetworkPreference = (CheckBoxPreference) findPreference(KEY_AUTOSEND_NETWORK);
		boolean autosendNetworkAvailable = adminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_AUTOSEND_NETWORK, true);
		if (!(autosendNetworkAvailable || adminMode)) {
			autosendCategory.removePreference(mAutosendNetworkPreference);
		}

		if (!(autosendNetworkAvailable || autosendWifiAvailable || adminMode)) {
			getPreferenceScreen().removePreference(autosendCategory);
		}

		PreferenceCategory serverCategory = (PreferenceCategory) findPreference(getString(R.string.server_preferences));

		// declared early to prevent NPE in toggleServerPaths
		mFormListUrlPreference = (EditTextPreference) findPreference(KEY_FORMLIST_URL);
		mSubmissionUrlPreference = (EditTextPreference) findPreference(KEY_SUBMISSION_URL);

		if (!(serverAvailable || adminMode)) {
			Preference protocol = findPreference(KEY_PROTOCOL);
			serverCategory.removePreference(protocol);
		} else {
		}

		mUsernamePreference = (EditTextPreference) findPreference(KEY_USERNAME);
		mUsernamePreference.setOnPreferenceChangeListener(this);
		mUsernamePreference.setSummary(mUsernamePreference.getText());
		mUsernamePreference.getEditText().setFilters(
				new InputFilter[] { getReturnFilter() });

		boolean usernameAvailable = adminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_CHANGE_USERNAME, true);
		if (!(usernameAvailable || adminMode)) {
			serverCategory.removePreference(mUsernamePreference);
		}

		mPasswordPreference = (EditTextPreference) findPreference(KEY_PASSWORD);
		mPasswordPreference
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {
					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						String pw = newValue.toString();

						if (pw.length() > 0) {
							mPasswordPreference.setSummary("********");
						} else {
							mPasswordPreference.setSummary("");
						}
						return true;
					}
				});
		if (mPasswordPreference.getText() != null
				&& mPasswordPreference.getText().length() > 0) {
			mPasswordPreference.setSummary("********");
		}
		mUsernamePreference.getEditText().setFilters(
				new InputFilter[] { getReturnFilter() });

		boolean passwordAvailable = adminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_CHANGE_PASSWORD, true);
		if (!(passwordAvailable || adminMode)) {
			serverCategory.removePreference(mPasswordPreference);
		}

		// mFormListUrlPreference.setOnPreferenceChangeListener(this);				smap
		// mFormListUrlPreference.setSummary(mFormListUrlPreference.getText());		smap
		if (!(serverAvailable || adminMode)) {
			serverCategory.removePreference(mFormListUrlPreference);
		}

		// mSubmissionUrlPreference.setOnPreferenceChangeListener(this);			smap
		// mSubmissionUrlPreference.setSummary(mSubmissionUrlPreference.getText());	smap
		if (!(serverAvailable || adminMode)) {
			serverCategory.removePreference(mSubmissionUrlPreference);
		}

		if (!(serverAvailable || urlAvailable || usernameAvailable || passwordAvailable
				|| /* googleAccountAvailable || */ adminMode)) {				// smap - remove google account
			getPreferenceScreen().removePreference(serverCategory);
		}

		PreferenceCategory clientCategory = (PreferenceCategory) findPreference(getString(R.string.client));

		boolean navigationAvailable = adminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_NAVIGATION, true);
		mNavigationPreference = (ListPreference) findPreference(KEY_NAVIGATION);
		mNavigationPreference.setSummary(mNavigationPreference.getEntry());
		mNavigationPreference
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						int index = ((ListPreference) preference)
								.findIndexOfValue(newValue.toString());
						String entry = (String) ((ListPreference) preference)
								.getEntries()[index];
						((ListPreference) preference).setSummary(entry);
						return true;
					}
				});
		if (!(navigationAvailable || adminMode)) {
			clientCategory.removePreference(mNavigationPreference);
		}
		
		boolean constraintBehaviorAvailable = adminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_CONSTRAINT_BEHAVIOR, true);
		mConstraintBehaviorPreference = (ListPreference) findPreference(KEY_CONSTRAINT_BEHAVIOR);
		mConstraintBehaviorPreference.setSummary(mConstraintBehaviorPreference.getEntry());
		mConstraintBehaviorPreference
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						int index = ((ListPreference) preference)
								.findIndexOfValue(newValue.toString());
						String entry = (String) ((ListPreference) preference)
								.getEntries()[index];
						((ListPreference) preference).setSummary(entry);
						return true;
					}
				});
		if (!(constraintBehaviorAvailable || adminMode)) {
			clientCategory.removePreference(mConstraintBehaviorPreference);
		}
		
		boolean fontAvailable = adminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_CHANGE_FONT_SIZE, true);
		mFontSizePreference = (ListPreference) findPreference(KEY_FONT_SIZE);
		mFontSizePreference.setSummary(mFontSizePreference.getEntry());
		mFontSizePreference
				.setOnPreferenceChangeListener(new OnPreferenceChangeListener() {

					@Override
					public boolean onPreferenceChange(Preference preference,
							Object newValue) {
						int index = ((ListPreference) preference)
								.findIndexOfValue(newValue.toString());
						String entry = (String) ((ListPreference) preference)
								.getEntries()[index];
						((ListPreference) preference).setSummary(entry);
						return true;
					}
				});
		if (!(fontAvailable || adminMode)) {
			clientCategory.removePreference(mFontSizePreference);
		}

		boolean defaultAvailable = adminPreferences.getBoolean(
				AdminPreferencesActivity.KEY_DEFAULT_TO_FINALIZED, true);

		Preference defaultFinalized = findPreference(KEY_COMPLETED_DEFAULT);
		if (!(defaultAvailable || adminMode)) {
			clientCategory.removePreference(defaultFinalized);
		}

		if (!(fontAvailable || defaultAvailable
				|| navigationAvailable || adminMode)) {
			getPreferenceScreen().removePreference(clientCategory);
		}

	}

	private void disableFeaturesInDevelopment() {
		// remove Google Collections from protocol choices in preferences
		ListPreference protocols = (ListPreference) findPreference(KEY_PROTOCOL);
		int i = protocols.findIndexOfValue(PROTOCOL_GOOGLE);
		if (i != -1) {
			CharSequence[] entries = protocols.getEntries();
			CharSequence[] entryValues = protocols.getEntryValues();

			CharSequence[] newEntries = new CharSequence[entryValues.length - 1];
			CharSequence[] newEntryValues = new CharSequence[entryValues.length - 1];
			for (int k = 0, j = 0; j < entryValues.length; ++j) {
				if (j == i)
					continue;
				newEntries[k] = entries[j];
				newEntryValues[k] = entryValues[j];
				++k;
			}

			protocols.setEntries(newEntries);
			protocols.setEntryValues(newEntryValues);
		}
	}

	private void setSplashPath(String path) {
		SharedPreferences sharedPreferences = PreferenceManager
				.getDefaultSharedPreferences(this);
		Editor editor = sharedPreferences.edit();
		editor.putString(KEY_SPLASH_PATH, path);
		editor.commit();

		mSplashPathPreference = (PreferenceScreen) findPreference(KEY_SPLASH_PATH);
		mSplashPathPreference.setSummary(mSplashPathPreference
				.getSharedPreferences().getString(KEY_SPLASH_PATH,
						getString(R.string.default_splash_path)));
	}

	@Override
	protected void onActivityResult(int requestCode, int resultCode,
			Intent intent) {
		super.onActivityResult(requestCode, resultCode, intent);
		if (resultCode == RESULT_CANCELED) {
			// request was canceled, so do nothing
			return;
		}

		switch (requestCode) {
		case IMAGE_CHOOSER:
			String sourceImagePath = null;

			// get gp of chosen file
			Uri uri = intent.getData();
			if (uri.toString().startsWith("file")) {
				sourceImagePath = uri.toString().substring(6);
			} else {
				String[] projection = { Images.Media.DATA };
				Cursor c = null;
				try {
					c = getContentResolver().query(uri, projection, null, null,
							null);
					int i = c.getColumnIndexOrThrow(Images.Media.DATA);
					c.moveToFirst();
					sourceImagePath = c.getString(i);
				} finally {
					if (c != null) {
						c.close();
					}
				}
			}

			// setting image path
			setSplashPath(sourceImagePath);
			break;
		}
	}

	/**
	 * Disallows whitespace from user entry
	 * 
	 * @return
	 */
	private InputFilter getWhitespaceFilter() {
		InputFilter whitespaceFilter = new InputFilter() {
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				for (int i = start; i < end; i++) {
					if (Character.isWhitespace(source.charAt(i))) {
						return "";
					}
				}
				return null;
			}
		};
		return whitespaceFilter;
	}

	/**
	 * Disallows carriage returns from user entry
	 * 
	 * @return
	 */
	private InputFilter getReturnFilter() {
		InputFilter returnFilter = new InputFilter() {
			public CharSequence filter(CharSequence source, int start, int end,
					Spanned dest, int dstart, int dend) {
				for (int i = start; i < end; i++) {
					if (Character.getType((source.charAt(i))) == Character.CONTROL) {
						return "";
					}
				}
				return null;
			}
		};
		return returnFilter;
	}

	/**
	 * Generic listener that sets the summary to the newly selected/entered
	 * value
	 */
	@Override
	public boolean onPreferenceChange(Preference preference, Object newValue) {
		preference.setSummary((CharSequence) newValue);
		return true;
	}

}
