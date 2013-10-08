package com.luboganev.cloudwave.data;

/**
 * This object serves as a model for the the tracks that are shown in the live wallpaper
 */
public class Track {
	/** The id of the track */
	public long id;
	/** The title of the track. It is shown under the sound wave */
	public String title;
	/** The permalink of the track. It is opened if the user double taps on the wallpaper */
	public String permalinkUrl;
	/** The url of the sound wave file on SoundCloud */
	public String waveformUrl;
}
