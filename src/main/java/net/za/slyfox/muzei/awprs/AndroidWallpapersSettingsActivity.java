/* ********************************************************************************************** */
/*
 * Copyright (C) 2014, Philip Cronje
 * All rights reserved.
 *
 * Distributed under the terms of the BSD 2-Clause License. See LICENSE in the source distribution
 * for the full terms of the license.
 */
/* ********************************************************************************************** */
package net.za.slyfox.muzei.awprs;

/* ********************************************************************************************** */
import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.util.SparseIntArray;
import android.view.MenuItem;
import android.view.View;
import android.widget.CompoundButton;
import android.widget.RadioGroup;
import android.widget.RadioGroup.OnCheckedChangeListener;

/* ********************************************************************************************** */
public class AndroidWallpapersSettingsActivity extends Activity implements OnCheckedChangeListener
{
	@Override protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);
		setContentView(R.layout.settings);

		int rotateIntervalMinutes = AndroidWallpapersArtSource.getSharedPreferences(this)
				.getInt(AndroidWallpapersArtSource.PREFKEY_ROTATE_INTERVAL_MINUTES,
						AndroidWallpapersArtSource.DEFAULT_ROTATE_INTERVAL_MINUTES);
		int radioButtonId = rotateIdsByValue.get(rotateIntervalMinutes);
		if(radioButtonId != 0)
		{
			View view = findViewById(radioButtonId);
			if(view != null)
			{
				CompoundButton compoundButton = (CompoundButton)view;
				compoundButton.setChecked(true);
			}
			else
			{
				Log.w(TAG, "Could not find radio button with mapped ID " + radioButtonId);
			}
		}
		else
		{
			Log.w(TAG, "Could not map rotation interval (" + rotateIntervalMinutes
					+ ") to radio button");
		}

		RadioGroup radioGroup = (RadioGroup)findViewById(R.id.radio_group);
		radioGroup.setOnCheckedChangeListener(this);

		getActionBar().setDisplayHomeAsUpEnabled(true);

	} // onCreate

	@Override public boolean onOptionsItemSelected(MenuItem item)
	{
		switch(item.getItemId())
		{
			case android.R.id.home:
				/* The only valid entry point into this activity is from Muzei, so we assume we can
				 * simply finish() to return to it.
				 */
				finish();
				break;

			default:
				return super.onOptionsItemSelected(item);
		}

		return true;
	}

	@Override public void onCheckedChanged(RadioGroup group, int checkedId)
	{
		int rotateIntervalMinutes = rotateValuesById.get(checkedId, -1);
		if(rotateIntervalMinutes == -1)
		{
			Log.w(TAG, "Could not map checked radio button ID " + checkedId + " to interval value");
			return;
		}

		if(BuildConfig.DEBUG)
		{
			Log.d(TAG, "Updating interval to " + rotateIntervalMinutes + " minutes");
		}

		AndroidWallpapersArtSource.getSharedPreferences(this)
				.edit()
				.putInt(AndroidWallpapersArtSource.PREFKEY_ROTATE_INTERVAL_MINUTES,
						rotateIntervalMinutes)
				.apply();

	} // onCheckedChanged

	private static final String TAG = "a:AndroidWallpapersSettings";

	private static final SparseIntArray rotateIdsByValue = new SparseIntArray();
	private static final SparseIntArray rotateValuesById = new SparseIntArray();
	static
	{
		rotateIdsByValue.put(0, R.id.rotate_interval_none);
		rotateIdsByValue.put(60, R.id.rotate_interval_1h);
		rotateIdsByValue.put(60 * 3, R.id.rotate_interval_3h);
		rotateIdsByValue.put(60 * 6, R.id.rotate_interval_6h);
		rotateIdsByValue.put(60 * 24, R.id.rotate_interval_24h);
		rotateIdsByValue.put(60 * 24 * 3, R.id.rotate_interval_3d);
		for(int i = 0; i < rotateIdsByValue.size(); ++i)
		{
			rotateValuesById.put(rotateIdsByValue.valueAt(i), rotateIdsByValue.keyAt(i));
		}

	} // static

} // AndroidWallpapersSettingsActivity

/* ********************************************************************************************** */
