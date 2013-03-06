package com.automattic.simplenote;

import java.util.Date;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v4.widget.SimpleCursorAdapter;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;

import com.actionbarsherlock.app.SherlockListFragment;
import com.automattic.simplenote.models.Note;

/**
 * A list fragment representing a list of Notes. This fragment also supports
 * tablet devices by allowing list items to be given an 'activated' state upon
 * selection. This helps indicate which item is currently being viewed in a
 * {@link NoteEditorFragment}.
 * <p>
 * Activities containing this fragment MUST implement the {@link Callbacks}
 * interface.
 */
public class NoteListFragment extends SherlockListFragment {

	private NotesCursorAdapter notesAdapter;
	private int mNumPreviewLines;

	/**
	 * The serialization (saved instance state) Bundle key representing the
	 * activated item position. Only used on tablets.
	 */
	private static final String STATE_ACTIVATED_POSITION = "activated_position";

	/**
	 * The fragment's current callback object, which is notified of list item
	 * clicks.
	 */
	private Callbacks mCallbacks = sCallbacks;

	/**
	 * The current activated item position. Only used on tablets.
	 */
	private int mActivatedPosition = ListView.INVALID_POSITION;

	/**
	 * A callback interface that all activities containing this fragment must
	 * implement. This mechanism allows activities to be notified of item
	 * selections.
	 */
	public interface Callbacks {
		/**
		 * Callback for when a note has been selected.
		 */
		public void onNoteSelected(Note note);
	}

	/**
	 * A dummy implementation of the {@link Callbacks} interface that does
	 * nothing. Used only when this fragment is not attached to an activity.
	 */
	private static Callbacks sCallbacks = new Callbacks() {
		@Override
		public void onNoteSelected(Note note) {
		}
	};

	/**
	 * Mandatory empty constructor for the fragment manager to instantiate the
	 * fragment (e.g. upon screen orientation changes).
	 */
	public NoteListFragment() {
	}

	@Override
	public void onCreate(Bundle savedInstanceState) {
		super.onCreate(savedInstanceState);

		NoteDB db = new NoteDB(getActivity().getApplicationContext());
		Cursor cursor = db.fetchAllNotes();

		String[] columns = new String[] { "content", "content", "creationDate" };
		int[] views = new int[] { R.id.note_title, R.id.note_content, R.id.note_date };
		
		SharedPreferences sharedPref = PreferenceManager.getDefaultSharedPreferences(getActivity());
		mNumPreviewLines = Integer.valueOf(sharedPref.getString("pref_key_preview_lines", "2"));

		notesAdapter = new NotesCursorAdapter(getActivity().getApplicationContext(), R.layout.note_list_row, cursor, columns, views, 0);

		setListAdapter(notesAdapter);
	}

	@Override
	public void onViewCreated(View view, Bundle savedInstanceState) {
		super.onViewCreated(view, savedInstanceState);

		// Restore the previously serialized activated item position.
		if (savedInstanceState != null && savedInstanceState.containsKey(STATE_ACTIVATED_POSITION)) {
			setActivatedPosition(savedInstanceState.getInt(STATE_ACTIVATED_POSITION));
		}
	}

	@Override
	public void onAttach(Activity activity) {
		super.onAttach(activity);

		// Activities containing this fragment must implement its callbacks.
		if (!(activity instanceof Callbacks)) {
			throw new IllegalStateException("Activity must implement fragment's callbacks.");
		}

		mCallbacks = (Callbacks) activity;
	}

	@Override
	public void onDetach() {
		super.onDetach();

		// Reset the active callbacks interface to the dummy implementation.
		mCallbacks = sCallbacks;
	}

	@Override
	public void onListItemClick(ListView listView, View view, int position, long id) {
		super.onListItemClick(listView, view, position, id);

		// Notify the active callbacks interface (the activity, if the
		// fragment is attached to one) that an item has been selected.
		Note note = (Note) notesAdapter.getItem(position);
		mCallbacks.onNoteSelected(note);
	}

	@Override
	public void onSaveInstanceState(Bundle outState) {
		super.onSaveInstanceState(outState);
		if (mActivatedPosition != ListView.INVALID_POSITION) {
			// Serialize and persist the activated item position.
			outState.putInt(STATE_ACTIVATED_POSITION, mActivatedPosition);
		}
	}

	/**
	 * Turns on activate-on-click mode. When this mode is on, list items will be
	 * given the 'activated' state when touched.
	 */
	public void setActivateOnItemClick(boolean activateOnItemClick) {
		// When setting CHOICE_MODE_SINGLE, ListView will automatically
		// give items the 'activated' state when touched.
		getListView().setChoiceMode(activateOnItemClick ? ListView.CHOICE_MODE_SINGLE : ListView.CHOICE_MODE_NONE);
	}

	private void setActivatedPosition(int position) {
		if (position == ListView.INVALID_POSITION) {
			getListView().setItemChecked(mActivatedPosition, false);
		} else {
			getListView().setItemChecked(position, true);
		}

		mActivatedPosition = position;
	}
	
	@SuppressWarnings("deprecation")
	public void refreshList() {
		notesAdapter.c.requery();
		notesAdapter.notifyDataSetChanged();
	}

	public class NotesCursorAdapter extends SimpleCursorAdapter {
		
		Cursor c;
	    Context context;

		public NotesCursorAdapter(Context context, int layout, Cursor c, String[] from, int[] to, int flags) {
			super(context, layout, c, from, to, flags);
			this.c = c;
			this.context = context;
		}

		@Override
		public View getView(int position, View view, ViewGroup parent) {
			if (view == null)
				view = View.inflate(getActivity().getBaseContext(), R.layout.note_list_row, null);
			
			c.moveToPosition(position);
			
			TextView titleTextView = (TextView) view.findViewById(R.id.note_title);
	        TextView contentTextView = (TextView) view.findViewById(R.id.note_content);
	        TextView dateTextView = (TextView) view.findViewById(R.id.note_date);
	        
	        contentTextView.setMaxLines(mNumPreviewLines);
	        
	        String title = c.getString(3);
	        if (title != null) {
	        	titleTextView.setText(c.getString(3));
	        	if (c.getString(5) != null)
	        		contentTextView.setText(c.getString(5));
	        	else
	        		contentTextView.setText(c.getString(4));
	        } else {
	        	titleTextView.setText(c.getString(4));
	        	contentTextView.setText(c.getString(4));
	        }
	        
	        String formattedDate = android.text.format.DateFormat.getTimeFormat(context).format(new Date(c.getLong(4)));
	        
	        dateTextView.setText(formattedDate);
			
			return view;
		}

	}
}