package com.your.company.HelloWorld;

import com.google.android.gms.cast.RemoteMediaPlayer;

public class ChromecastMediaController {
	private RemoteMediaPlayer remote = null;
	public ChromecastMediaController(RemoteMediaPlayer mRemoteMediaPlayer) {
		this.remote = mRemoteMediaPlayer;
	}
}
