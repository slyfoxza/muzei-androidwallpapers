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
import java.util.Random;

/* ********************************************************************************************** */
public class AndroidWallpapersArtSource extends RemoteMuzeiArtSource
{
	private static final String TAG = "AndroidWallpapersArtSource";

	public AndroidWallpapersArtSource()
	{
		super("AndroidWallpapersArtSource");

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
			Elements wallpapers = document.select("#wallpapers ul li[data-id]");
			Random random = new Random();
			Element wallpaper = wallpapers.get(random.nextInt(wallpapers.size()));

			Element elmTitleHeading = wallpaper.select("h2 a").first();
			Element elmAuthor = wallpaper.select(".author a").first();

			String fullURIString = elmTitleHeading.attr("href");
			fullURIString = Uri.encode(fullURIString, ":/");

			Uri fullURI = Uri.parse(fullURIString);
			Uri viewURI = fullURI;
			String viewURIString = elmAuthor.attr("href");
			String byline = elmAuthor.text();
			if(!StringUtil.isBlank(viewURIString))
			{
				viewURI = Uri.parse(viewURIString);
			}

			if(BuildConfig.DEBUG)
			{
				Log.d(TAG, "Publishing artwork: title=" + elmTitleHeading.text() + ", byline="
						+ byline + ", imageUri=" + fullURI.toString() + ", token="
						+ wallpaper.attr("data-id") + ", viewURI=" + viewURI.toString());
			}

			publishArtwork(new Artwork.Builder()
					.title(elmTitleHeading.text())
					.byline(byline)
					.imageUri(fullURI)
					.token(wallpaper.attr("data-id"))
					.viewIntent(new Intent(Intent.ACTION_VIEW, viewURI))
					.build());
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
		scheduleUpdate(System.currentTimeMillis() + 24L * 60L * 60L * 1000L);

	} // reschedule

} // AndroidWallpapersArtSource

/* ********************************************************************************************** */
