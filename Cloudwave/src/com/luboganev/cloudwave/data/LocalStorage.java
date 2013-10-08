package com.luboganev.cloudwave.data;

import java.util.ArrayList;

/**
 * Object used as a model for the local storage
 */
public class LocalStorage {
	/** The name of the artist whose tracks we show in the live wallpaper */
	public String artistUsername;
	/** The list of tracks objects */
	public ArrayList<Track> artistTracks;
	/** The currently shown track index from the list of tracks */
	public int currentTrackIndex;
	/** The random index of the next shown track from the list of tracks */
	public int nextRandomIndex;
}
