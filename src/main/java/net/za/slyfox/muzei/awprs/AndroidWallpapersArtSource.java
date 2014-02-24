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
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
import com.google.android.apps.muzei.api.MuzeiArtSource;
import com.google.android.apps.muzei.api.RemoteMuzeiArtSource;

import org.jsoup.Connection;
import org.jsoup.HttpStatusException;
import org.jsoup.Jsoup;
import org.jsoup.helper.StringUtil;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.net.SocketTimeoutException;
import java.util.Random;

/* ********************************************************************************************** */
public class AndroidWallpapersArtSource extends RemoteMuzeiArtSource
{
	public static final String PREFKEY_ROTATE_INTERVAL_MINUTES = "rotateIntervalMinutes";
	public static final int DEFAULT_ROTATE_INTERVAL_MINUTES = 24 * 60; // 24 hours

	public AndroidWallpapersArtSource()
	{
		super(SOURCE_NAME);

	} // AndroidWallpapersArtSource()

	@Override public void onCreate()
	{
		super.onCreate();
		setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);

	} // onCreate

	public static SharedPreferences getSharedPreferences(Context context)
	{
		return MuzeiArtSource.getSharedPreferences(context, SOURCE_NAME);

	} // getSharedPreferences

	@Override protected void onTryUpdate(int reason) throws RetryException
	{
		Connection connection = Jsoup.connect("http://androidwallpape.rs");

		try
		{
			Connection.Response response = connection.execute();
			Document document = response.parse();
			Elements wallpapers = document.select("#wallpapers ul li[data-id]");

			int wallpaperCount = wallpapers.size();
			if(wallpaperCount <= 0)
			{
				Log.w(TAG, "No wallpapers found, requesting retry");
				throw new RetryException();
			}
			Random random = new Random();
			Element wallpaper = wallpapers.get(random.nextInt(wallpaperCount));

			Element elmTitleHeading = wallpaper.select("h2 a").first();
			if(elmTitleHeading == null)
			{
				Log.w(TAG, "Selected wallpaper without title, requesting retry");
				throw new RetryException();
			}

			Element elmAuthor = wallpaper.select(".author a").first();

			String fullURIString = elmTitleHeading.attr("href");
			if(StringUtil.isBlank(fullURIString))
			{
				Log.w(TAG, "Selected wallpaper with blank href, requesting retry");
				throw new RetryException();
			}
			fullURIString = Uri.encode(fullURIString, ":/");
			Uri fullURI = Uri.parse(fullURIString);
			Uri viewURI = fullURI;

			String byline = null;
			if(elmAuthor != null)
			{
				byline = elmAuthor.text();
				if(!StringUtil.isBlank(byline))
				{
					String viewURIString = elmAuthor.attr("href");
					if(!StringUtil.isBlank(viewURIString))
					{
						viewURI = Uri.parse(viewURIString);
					}
				}
			}

			if(BuildConfig.DEBUG)
			{
				Log.d(TAG, "Publishing artwork: title=" + elmTitleHeading.text() + ", byline="
						+ byline + ", imageUri=" + fullURI.toString() + ", token="
						+ wallpaper.attr("data-id") + ", viewURI=" + viewURI.toString());
			}

			Artwork.Builder builder = new Artwork.Builder()
					.title(elmTitleHeading.text())
					.imageUri(fullURI)
					.token(wallpaper.attr("data-id"))
					.viewIntent(new Intent(Intent.ACTION_VIEW, viewURI));
			if(byline != null)
			{
				builder.byline(byline);
			}

			publishArtwork(builder.build());
		}
		catch(HttpStatusException e)
		{
			int statusCode = e.getStatusCode();
			if((statusCode >= 500) || (statusCode < 600))
			{
				if(BuildConfig.DEBUG)
				{
					Log.d(TAG, "Requesting retry due to HTTP status error", e);
				}

				throw new RetryException(e);
			}
		}
		catch(SocketTimeoutException e)
		{
			if(BuildConfig.DEBUG)
			{
				Log.d(TAG, "Requesting retry due to socket timeout", e);
			}

			throw new RetryException(e);
		}
		catch(IOException e)
		{
			Log.w(TAG, "I/O exception occurred during update", e);
		}

		reschedule();

	} // onTryUpdate

	private void reschedule()
	{
		long rotateIntervalMinutes = (long)getSharedPreferences().getInt(
				PREFKEY_ROTATE_INTERVAL_MINUTES, DEFAULT_ROTATE_INTERVAL_MINUTES);
		scheduleUpdate(System.currentTimeMillis() + rotateIntervalMinutes * 60L * 1000L);

	} // reschedule

	private static final String SOURCE_NAME = "AndroidWallpapersArtSource";
	private static final String TAG = SOURCE_NAME;

} // AndroidWallpapersArtSource

/* ********************************************************************************************** */
