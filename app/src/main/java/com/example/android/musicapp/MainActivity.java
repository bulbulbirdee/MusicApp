package com.example.android.musicapp;

import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.database.Cursor;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Binder;
import android.os.IBinder;
import android.os.PowerManager;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.app.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;

public class MainActivity extends AppCompatActivity {

    private ArrayList<song> songList;
    private ListView songView;
    private MusicService musicSrv;
    private Intent playIntent;
    private boolean musicBound=false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        songView = (ListView) findViewById(R.id.song_list);
        songList = new ArrayList<song>();
        getSongList();

        Collections.sort(songList, new Comparator<song>() {
            public int compare(song a, song b) {
                return a.getTitle().compareTo(b.getTitle());
            }
        });
    }

    public void songPicked(View view){
        musicSrv.setSong(Integer.parseInt(view.getTag().toString()));
        musicSrv.playSong();
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_shuffle:
                //shuffle
                break;
            case R.id.action_end:
                stopService(playIntent);
                musicSrv=null;
                System.exit(0);
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onDestroy() {
        stopService(playIntent);
        musicSrv=null;
        super.onDestroy();
    }

    private ServiceConnection musicConnection = new ServiceConnection(){

        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            MusicService.MusicBinder binder = (MusicService.MusicBinder)service;
            //get service
            musicSrv = binder.getService();
            //pass list
            musicSrv.setList(songList);
            musicBound = true;
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            musicBound = false;
        }
    };
    @Override
    protected void onStart() {
        super.onStart();
        if(playIntent==null){
            playIntent = new Intent(this, MusicService.class);
            bindService(playIntent, musicConnection, Context.BIND_AUTO_CREATE);
            startService(playIntent);
        }
    }

    public void getSongList() {
        //retrieve song info
        ContentResolver musicResolver = getContentResolver();
        Uri musicUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
        Cursor musicCursor = musicResolver.query(musicUri, null, null, null, null);

        if (musicCursor != null && musicCursor.moveToFirst()) {
            //get columns
            int titleColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.TITLE);
            int idColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media._ID);
            int artistColumn = musicCursor.getColumnIndex
                    (android.provider.MediaStore.Audio.Media.ARTIST);
            //add songs to list
            do {
                long thisId = musicCursor.getLong(idColumn);
                String thisTitle = musicCursor.getString(titleColumn);
                String thisArtist = musicCursor.getString(artistColumn);
                songList.add(new song(thisId, thisTitle, thisArtist));
            }
            while (musicCursor.moveToNext());
        }

        SongAdapter songAdt = new SongAdapter(this, songList);
        songView.setAdapter(songAdt);

    }


    public class song {
        private long id;
        private String title;
        private String artist;

        public song(long songID, String songTitle, String songArtist) {
            id = songID;
            title = songTitle;
            artist = songArtist;

        }

        public long getId() {
            return id;
        }

        public String getTitle() {
            return title;
        }

        public String getArtist() {
            return artist;
        }
    }

    public class SongAdapter extends BaseAdapter {

        private ArrayList<song> songs;
        private LayoutInflater songInf;

        @Override
        public int getCount() {
            return songs.size();
        }

        @Override
        public Object getItem(int arg0) {
            // TODO Auto-generated method stub
            return null;
        }

        @Override
        public long getItemId(int arg0) {
            // TODO Auto-generated method stub
            return 0;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            //map to song layout
            LinearLayout songLay = (LinearLayout) songInf.inflate
                    (R.layout.song, parent, false);
            //get title and artist views
            TextView songView = (TextView) songLay.findViewById(R.id.song_title);
            TextView artistView = (TextView) songLay.findViewById(R.id.song_artist);
            //get song using position
            song currSong = songs.get(position);
            //get title and artist strings
            songView.setText(currSong.getTitle());
            artistView.setText(currSong.getArtist());
            //set position as tag
            songLay.setTag(position);
            return songLay;
        }

        public SongAdapter(Context c, ArrayList<song> theSongs) {
            songs = theSongs;
            songInf = LayoutInflater.from(c);
        }

    }

    public class MusicService extends Service implements
            MediaPlayer.OnPreparedListener, MediaPlayer.OnErrorListener,
            MediaPlayer.OnCompletionListener{

        //media player
        private MediaPlayer player;
        //song list
        private ArrayList<song> songs;
        //current position
        private int songPosn;
        private final IBinder musicBind = new MusicBinder();

        public void setList(ArrayList<song> theSongs){
            songs=theSongs;
        }

        public class MusicBinder extends Binder {
            MusicService getService() {
                return MusicService.this;
            }
        }

        @Override
        public void onCompletion(MediaPlayer mediaPlayer) {

        }

        @Override
        public boolean onError(MediaPlayer mediaPlayer, int i, int i1) {
            return false;
        }

        @Override
        public void onPrepared(MediaPlayer mp) {
            //start playback
            mp.start();
        }

        public void setSong(int songIndex){
            songPosn=songIndex;
        }

        public MusicService() {
            super();
        }

            @Override
            public IBinder onBind(Intent intent) {
                return musicBind;
            }

        @Override
        public boolean onUnbind(Intent intent){
            player.stop();
            player.release();
            return false;
        }

        public void playSong(){
            player.reset();
            //get song
            song playSong = songs.get(songPosn);
//get id
            long currSong = playSong.getId();
//set uri
            Uri trackUri = ContentUris.withAppendedId(
                    android.provider.MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
                    currSong);

            try{
                player.setDataSource(getApplicationContext(), trackUri);
            }
            catch(Exception e){
                Log.e("MUSIC SERVICE", "Error setting data source", e);
            }
            player.prepareAsync();
        }


        public void onCreate() {
            //create the service
            super.onCreate();
           //initialize position
            songPosn = 0;
           //create player
            player = new MediaPlayer();
            initMusicPlayer();
        }

        public void initMusicPlayer(){
            //set player properties
            player.setWakeMode(getApplicationContext(),
                    PowerManager.PARTIAL_WAKE_LOCK);
            player.setAudioStreamType(AudioManager.STREAM_MUSIC);
            player.setOnPreparedListener(this);
            player.setOnCompletionListener(this);
            player.setOnErrorListener(this);
        }

    }
}







