/*
 * Copyright (C) 2012 Jamie Nicol <jamie@thenicols.net>
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package org.jamienicol.episodes;

import android.app.Activity;
import android.content.AsyncQueryHandler;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.ListView;
import android.widget.TextView;
import com.actionbarsherlock.app.SherlockListFragment;
import org.jamienicol.episodes.db.EpisodesTable;
import org.jamienicol.episodes.db.ShowsProvider;

public class EpisodesListFragment extends SherlockListFragment
	implements LoaderManager.LoaderCallbacks<Cursor>
{
	private int showId;
	private int seasonNumber;
	private EpisodesCursorAdapter listAdapter;

	public interface OnEpisodeSelectedListener {
		public void onEpisodeSelected(int episodeId);
	}
	private OnEpisodeSelectedListener onEpisodeSelectedListener;

	public static EpisodesListFragment newInstance(int showId,
	                                               int seasonNumber) {
		EpisodesListFragment instance = new EpisodesListFragment();

		Bundle args = new Bundle();
		args.putInt("showId", showId);
		args.putInt("seasonNumber", seasonNumber);

		instance.setArguments(args);
		return instance;
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		try {
			onEpisodeSelectedListener = (OnEpisodeSelectedListener)activity;
		} catch (ClassCastException e) {
			String message =
				String.format("%s must implement OnEpisodeSelectedListener",
				              activity.toString());
			throw new ClassCastException(message);
		}
	}

	public View onCreateView(LayoutInflater inflater,
	                         ViewGroup container,
	                         Bundle savedInstanceState) {
		return inflater.inflate(R.layout.episodes_list_fragment,
		                        container,
		                        false);
	}

	@Override
	public void onActivityCreated(Bundle savedInstanceState) {
		super.onActivityCreated(savedInstanceState);

		listAdapter = new EpisodesCursorAdapter(getActivity(), null, 0);
		setListAdapter(listAdapter);

		showId = getArguments().getInt("showId");
		seasonNumber = getArguments().getInt("seasonNumber");

		Bundle loaderArgs = new Bundle();
		loaderArgs.putInt("showId", showId);
		loaderArgs.putInt("seasonNumber", seasonNumber);
		getLoaderManager().initLoader(0, loaderArgs, this);
	}

	@Override
	public Loader<Cursor> onCreateLoader(int id, Bundle args) {
		int showId = args.getInt("showId");
		int seasonNumber = args.getInt("seasonNumber");

		String[] projection = {
			EpisodesTable.COLUMN_ID,
			EpisodesTable.COLUMN_NAME,
			EpisodesTable.COLUMN_WATCHED
		};
		String selection = String.format("%s=? AND %s=?",
		                                 EpisodesTable.COLUMN_SHOW_ID,
		                                 EpisodesTable.COLUMN_SEASON_NUMBER);
		String[] selectionArgs = {
			new Integer(showId).toString(),
			new Integer(seasonNumber).toString()
		};

		return new CursorLoader(getActivity(),
		                        ShowsProvider.CONTENT_URI_EPISODES,
		                        projection,
		                        selection,
		                        selectionArgs,
		                        EpisodesTable.COLUMN_EPISODE_NUMBER + " ASC");
	}

	@Override
	public void onLoadFinished(Loader<Cursor> loader, Cursor data) {
		listAdapter.swapCursor(data);
	}

	@Override
	public void onLoaderReset(Loader<Cursor> loader) {
		listAdapter.swapCursor(null);
	}

	@Override
	public void onListItemClick(ListView l, View v, int position, long id) {
		// pass the episode id to the listener.
		onEpisodeSelectedListener.onEpisodeSelected((int)id);
	}

	private static class EpisodesCursorAdapter
		extends CursorAdapter
	{
		public EpisodesCursorAdapter(Context context, Cursor c, int flags) {
			super(context, c, flags);
		}

		@Override
		public void bindView(View view, Context context, Cursor cursor) {
			int idColumnIndex =
				cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_ID);
			final int id = cursor.getInt(idColumnIndex);

			final ContentResolver contentResolver =
				context.getContentResolver();

			int nameColumnIndex =
				cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_NAME);
			String name = cursor.getString(nameColumnIndex);
			TextView nameView =
				(TextView)view.findViewById(R.id.episode_name_view);
			nameView.setText(name);

			int watchedColumnIndex =
				cursor.getColumnIndexOrThrow(EpisodesTable.COLUMN_WATCHED);
			int watched = cursor.getInt(watchedColumnIndex);
			CheckBox watchedCheckBox =
				(CheckBox)view.findViewById(R.id.episode_watched_check_box);

			watchedCheckBox.setOnCheckedChangeListener(null);

			watchedCheckBox.setChecked(watched != 0);

			watchedCheckBox.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
				public void onCheckedChanged(CompoundButton buttonView,
				                             boolean isChecked) {
					AsyncQueryHandler handler =
						new AsyncQueryHandler(contentResolver) {};
					ContentValues epValues = new ContentValues();
					epValues.put(EpisodesTable.COLUMN_WATCHED, isChecked);

					Uri epUri =
						Uri.withAppendedPath(ShowsProvider.CONTENT_URI_EPISODES,
						                     new Integer(id).toString());
					handler.startUpdate(0,
					                    null,
					                    epUri,
					                    epValues,
					                    null,
					                    null);
				}
			});
		}

		@Override
		public View newView(Context context, Cursor cursor, ViewGroup parent) {
			LayoutInflater inflater = LayoutInflater.from(context);
			return inflater.inflate(R.layout.episodes_list_item, parent, false);
		}
	}
}
