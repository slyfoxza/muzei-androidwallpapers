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
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.google.android.apps.muzei.api.Artwork;
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
import java.util.Arrays;
import java.util.Random;

/* ********************************************************************************************** */
public class AndroidWallpapersArtSource extends RemoteMuzeiArtSource
{
	private static final String TAG = "AndroidWallpapersArtSource";

	private Random random;
	private Elements wallpapers;

	public AndroidWallpapersArtSource()
	{
		super("AndroidWallpapersArtSource");
		random = new Random();

	} // AndroidWallpapersArtSource()

	@Override public void onCreate()
	{
		super.onCreate();
		setUserCommands(BUILTIN_COMMAND_ID_NEXT_ARTWORK);

	} // onCreate

	@Override protected void onTryUpdate(int reason) throws RetryException
	{
		Connection connection = Jsoup.connect("http://androidwallpape.rs");

		try
		{
			Connection.Response response = connection.execute();
			Document document = response.parse();
			wallpapers = document.select("#wallpapers ul li[data-id]");
			Element wallpaper = selectWallpaper();

			String id = wallpaper.attr("data-id");

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
						+ byline + ", imageUri=" + fullURI.toString() + ", token=" + id
						+ ", viewURI=" + viewURI.toString());
			}

			Artwork.Builder builder = new Artwork.Builder()
					.title(elmTitleHeading.text())
					.imageUri(fullURI)
					.token(id)
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

	private Element selectWallpaper() throws RetryException
	{
		int wallpaperCount = wallpapers.size();
		if(wallpaperCount <= 0)
		{
			Log.w(TAG, "No wallpapers found, requesting retry");
			throw new RetryException();
		}

		for(int attempt = 0; attempt < 10; ++attempt)
		{
			Element wallpaper = wallpapers.get(random.nextInt(wallpaperCount));

			String idString = wallpaper.attr("data-id");
			int id;
			try
			{
				id = Integer.valueOf(idString);
			}
			catch(NumberFormatException e)
			{
				/*
				 * Not a fatal error, just means it's incompatible with the integer ID assumption of
				 * the wallpaper blacklist, so assume that the wallpaper is good to go.
				 */
				return wallpaper;
			}

			int blacklistIndex = Arrays.binarySearch(
					getResources().getIntArray(R.array.blacklist_ids), id);
			if(blacklistIndex >= 0)
			{
				if(BuildConfig.DEBUG)
				{
					Log.d(TAG, "Selected blacklisted wallpaper " + idString + ", selecting another");
				}

				continue;
			}

			return wallpaper;
		}

		Log.w(TAG, "Exhausted attempts to select non-blacklisted wallpaper, requesting retry");
		throw new RetryException();

	} // selectWallpaper

	private void reschedule()
	{
		scheduleUpdate(System.currentTimeMillis() + 24L * 60L * 60L * 1000L);

	} // reschedule

} // AndroidWallpapersArtSource

/* ********************************************************************************************** */
