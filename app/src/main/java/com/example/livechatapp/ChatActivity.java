package com.example.livechatapp;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.*;
import android.support.v7.app.ActionBarActivity;
import android.support.v7.widget.PopupMenu;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.util.TypedValue;
import android.view.Display;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;


public class ChatActivity extends ActionBarActivity {

    private FirebaseDatabase dataBase;
    private ValueEventListener chatRoomEventListener;
    private BaseAdapter chatMessageAdapter;
    private String chatRoomName;
    private String usernameExtra;
    private boolean incognitoMode;
    private ArrayList<Message> messageList;
    private DatabaseReference chatRoomRef;
    private boolean chatRoomEmpty;
    private ListView listView;
    //private int orientationInt;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_chat);

        listView = (ListView) findViewById(R.id.chatViewArea);

        Log.d("Testing", "Activity Started");

        //a Message() takes five values:
        //Message(String username, long sendTime, String chatMessage, String messageType, boolean isComplete)
        messageList = new ArrayList<Message>();

        final SimpleDateFormat enterDateFormat = new SimpleDateFormat("E MMM d, yyyy 'at' hh:mm:ss a zzz");
        final SimpleDateFormat sendDateFormat = new SimpleDateFormat("hh:mm a");

        chatRoomName = getIntent().getExtras().getString("chatRoomName");
        getSupportActionBar().setTitle(chatRoomName);
        usernameExtra = getIntent().getExtras().getString("username");
        incognitoMode = getIntent().getExtras().getBoolean("incognitoValue");
        Log.d("Testing", "incognitoMode = " + incognitoMode);

        final Spannable enterSpannable = new SpannableString(" has entered");
        enterSpannable.setSpan(new ForegroundColorSpan(Color.parseColor("#3d3d3d")), 0, enterSpannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        final Spannable exitSpannable = new SpannableString(" has left");
        exitSpannable.setSpan(new ForegroundColorSpan(Color.parseColor("#3d3d3d")), 0, exitSpannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        final Spannable errorSpannable = new SpannableString("Error: Message Incomplete");
        errorSpannable.setSpan(new ForegroundColorSpan(Color.parseColor("#ff0000")), 0, errorSpannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        dataBase = FirebaseDatabase.getInstance();
        chatRoomRef = dataBase.getReference().child("ChatRooms").child(chatRoomName);

        chatMessageAdapter = new BaseAdapter() {
            @Override
            public int getCount() {
                int count = messageList.size();
                return count;
            }

            @Override
            public Object getItem(int i) {
                return null;
            }

            @Override
            public long getItemId(int i) {
                return 0;
            }

            @Override //Partially modelled after http://stackoverflow.com/questions/35761897/how-do-i-make-a-relative-layout-an-item-of-my-listview-and-detect-gestures-over
            public View getView(int position, View convertView, ViewGroup parent) {
                LayoutInflater layoutInflater;
                ViewHolder listViewHolder;
                String messageType = messageList.get(position).getMessageType();
                if( convertView == null ){
                    layoutInflater = (LayoutInflater) getApplicationContext().getSystemService(Context.LAYOUT_INFLATER_SERVICE);
                    convertView = layoutInflater.inflate(R.layout.relativelayout_chat_box, parent, false);

                    listViewHolder = new ViewHolder();
                    listViewHolder.sendTime = (TextView) convertView.findViewById(R.id.chatSendTimeTextView);
                    listViewHolder.username = (TextView) convertView.findViewById(R.id.chatUsernameTextView);
                    listViewHolder.message = (TextView) convertView.findViewById(R.id.chatMessageTextView);
                    convertView.setTag(listViewHolder);
                } else {
                    listViewHolder = (ViewHolder) convertView.getTag();
                }

                //Checks if the message is complete
                if(messageList.get(position).getIsComplete()) {

                    //Converts the long value to proper time and date format
                    Date sendDate = new Date(messageList.get(position).getSendTime());
                    String time = enterDateFormat.format(sendDate);
                    if (messageType.equals("chat")) {
                        time = sendDateFormat.format(sendDate);
                    }

                    Spannable usernameSpannable = new SpannableString(messageList.get(position).getUsername());
                    String usernameColor = "#3d3d3d";
                    if (messageList.get(position).getUsername().equals("~Anonymous~")) {
                        usernameColor = "#ff3f7f";
                    } else if (messageList.get(position).getUsername().equals("1678")) {
                        usernameColor = "#5BE300";
                    } else if (messageList.get(position).getUsername().contains("/#")) { //If username includes hexadecimal, removes hexadecimal and makes color that hexadecimal
                        final int hexIndex = messageList.get(position).getUsername().indexOf("/#");
                        String possibleHexColor = "#";
                        boolean isHex = true;
                        if(hexIndex + 6 < messageList.get(position).getUsername().length()) {
                            for (int currentIndex = hexIndex + 2; currentIndex <= hexIndex + 7; currentIndex++) {
                                char currentChar = messageList.get(position).getUsername().charAt(currentIndex);
                                if (Character.isDigit(currentChar) || Character.isLetter(currentChar)) {
                                    possibleHexColor += messageList.get(position).getUsername().charAt(currentIndex);
                                } else {
                                    currentIndex = hexIndex + 8;
                                    isHex = false;
                                }
                            }
                        } else isHex = false;
                        if(isHex) {
                            usernameColor = possibleHexColor;

                            //Ensures that the hex value is only removed if the username contains characters in addition to the hex value
                            if(messageList.get(position).getUsername().replaceFirst("/" + possibleHexColor, "").replace(" ", "").length() != 0) {
                                usernameSpannable = new SpannableString(messageList.get(position).getUsername().replaceFirst("/" + possibleHexColor, ""));
                            }
                        }
                    }

                    usernameSpannable.setSpan(new ForegroundColorSpan(Color.parseColor(usernameColor)), 0, usernameSpannable.length(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                    listViewHolder.username.setText(usernameSpannable);

                    listViewHolder.sendTime.setText(time);

                    if (messageType.equals("chat")) {
                        listViewHolder.message.setText(messageList.get(position).getChatMessage());
                        listViewHolder.message.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 18);
                    } else if (messageType.equals("enter")) {
                        listViewHolder.username.append(enterSpannable);
                        listViewHolder.message.setText(null);
                        listViewHolder.message.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 0);
                    } else if (messageType.equals("exit")) {
                        listViewHolder.username.append(exitSpannable);
                        listViewHolder.message.setText(null);
                        listViewHolder.message.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 0);
                    }

                } else {
                    listViewHolder.username.setText(errorSpannable);
                    listViewHolder.message.setText(null);
                    listViewHolder.message.setTextSize(TypedValue.COMPLEX_UNIT_DIP, 0);
                    listViewHolder.sendTime.setText(null);
                }

                return convertView;
            }
        };

        chatRoomEventListener = new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                if(!dataSnapshot.child("Messages").child("" + (dataSnapshot.child("Messages").getChildrenCount() - 1)).child("isComplete").getValue().equals(null)) {
                    if(dataSnapshot.child("Messages").child("" + (dataSnapshot.child("Messages").getChildrenCount() - 1)).child("isComplete").getValue(Boolean.class)) {
                        chatRoomEmpty = dataSnapshot.child("Empty").getValue(Boolean.class);
                        messageList.clear();
                        if (!chatRoomEmpty) {
                            for (int i = 0; i < dataSnapshot.child("Messages").getChildrenCount(); i++) {
                                //Checks that the current message is complete.
                                boolean tempIsComplete = dataSnapshot.child("Messages").child("" + i).child("isComplete").getValue(Boolean.class);
                                long tempSendTime = -1;
                                String tempMessage = null;
                                String tempUsername = null;
                                String tempMessageType = null;
                                if(tempIsComplete) {
                                    tempSendTime = dataSnapshot.child("Messages").child("" + i).child("sendTime").getValue(Long.class);
                                    tempMessage = dataSnapshot.child("Messages").child("" + i).child("message").getValue(String.class);
                                    tempUsername = dataSnapshot.child("Messages").child("" + i).child("username").getValue(String.class);
                                    tempMessageType = dataSnapshot.child("Messages").child("" + i).child("messageType").getValue(String.class);
                                }

                                messageList.add(new Message(tempUsername, tempSendTime, tempMessage, tempMessageType, tempIsComplete));
                            }
                            chatMessageAdapter.notifyDataSetChanged();

                            //Scrolls now when sending and receiving.
                            listView.clearFocus();
                            listView.post(new Runnable() {
                                @Override
                                public void run() {
                                    listView.setSelection(chatMessageAdapter.getCount() - 1);
                                }
                            });
                        }
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("Error", "ChatRoomEventListener Cancelled");
                Toast connectionErrorToast = Toast.makeText(getApplicationContext(), "Connection Error", Toast.LENGTH_SHORT);
                connectionErrorToast.setGravity(Gravity.CENTER, 0, 0);
                connectionErrorToast.show();
            }
        };

        //Moved to onResume()
        /*chatRoomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final long chatRoomSize = dataSnapshot.child("Messages").getChildrenCount();

                //Prevents user has entered messages from being displayed on activity restart (mainly caused by screen rotation)
                if(getIntent().getExtras().getBoolean("firstRunTime")) {

                    //Creates the 'user has entered' message.
                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("isComplete").setValue(false);

                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("sendTime").setValue(System.currentTimeMillis());

                    if (incognitoMode) {
                        chatRoomRef.child("Messages").child("" + chatRoomSize).child("username").setValue("~Anonymous~");
                        //chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("incognito").setValue(true);
                    } else {
                        chatRoomRef.child("Messages").child("" + chatRoomSize).child("username").setValue(usernameExtra);
                    }

                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("messageType").setValue("enter");

                    //Prevents other devices in the rooms from crashing due to lack of information.
                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("isComplete").setValue(true);

                    chatRoomRef.child("Empty").setValue(false);

                    getIntent().putExtra("firstRunTime", false);
                }

                chatRoomRef.addValueEventListener(chatRoomEventListener);
                listView.setAdapter(chatMessageAdapter);

                //Sets onClickListener used for deleting a user's messages (NOTE: ~maybe~ add a way to make sure a user can't change their name to delete another persons messages (HARD))
                //Remove this section of code if I don't want people to be able to delete messages.
                listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                    @Override
                    public boolean onItemLongClick(AdapterView<?> arg0, View view, final int position, long id) {
                        if(usernameExtra.equals(messageList.get(position).getUsername()) && messageList.get(position).getMessageType().equals("chat") && messageList.get(position).getIsComplete()) {
                            PopupMenu deleteMessagePopup = new PopupMenu(ChatActivity.this, view);
                            deleteMessagePopup.getMenuInflater().inflate(R.menu.deletepopup_menu, deleteMessagePopup.getMenu());
                            deleteMessagePopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                @Override
                                public boolean onMenuItemClick(MenuItem item) {
                                    chatRoomRef.child("Messages").child("" + position).child("message").setValue("[Deleted]");

                                    return true;
                                }
                            });

                            deleteMessagePopup.show();

                        }
                        return true;
                    }
                });
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {}
        });*/

    }


    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_chat, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    public void sendMessage(View view) {
        EditText chatBoxEditText = (EditText) findViewById(R.id.chatBox);
        String unsentMessage = chatBoxEditText.getText().toString();

        //Still uses "" instead of the trim method so that people can say stuff like "     ".
        if(!unsentMessage.equals("")) {
            chatBoxEditText.setText("");
            final int newMessageArrayNum = messageList.size();
            chatRoomRef.removeEventListener(chatRoomEventListener);

            chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("isComplete").setValue(false);
            chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("message").setValue(unsentMessage);
            chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("sendTime").setValue(System.currentTimeMillis());
            chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("messageType").setValue("chat");

            if(incognitoMode) {
                chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("username").setValue("~Anonymous~");
                //chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("incognito").setValue(true);
            } else {
                chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("username").setValue(usernameExtra);
            }

            if(chatRoomEmpty) {
                chatRoomRef.child("Empty").setValue(false);
            }


            chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("isComplete").setValue(true);
            chatRoomRef.addValueEventListener(chatRoomEventListener);
        }
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();

        //Important note for future coding: finish() does not kill the activity until all processes are done (like active eventListeners)
        chatRoomRef.removeEventListener(chatRoomEventListener);
        this.finish();
    }

    @Override
    public void onPause() {
        super.onPause();

        //Checks if the restart is due to orientation change.
        if(this.getResources().getConfiguration().orientation != getIntent().getExtras().getInt("orientation")) {
            getIntent().putExtra("orientRestart", true);
        } else {
            getIntent().putExtra("orientRestart", false);
        }

        if(!getIntent().getExtras().getBoolean("orientRestart")) {
            //Creates user has left message
            chatRoomRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    final long chatRoomSize = dataSnapshot.child("Messages").getChildrenCount();

                    chatRoomRef.removeEventListener(chatRoomEventListener);
                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("isComplete").setValue(false);

                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("sendTime").setValue(System.currentTimeMillis());

                    if (incognitoMode) {
                        chatRoomRef.child("Messages").child("" + chatRoomSize).child("username").setValue("~Anonymous~");
                        //chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("incognito").setValue(true);
                    } else {
                        chatRoomRef.child("Messages").child("" + chatRoomSize).child("username").setValue(usernameExtra);
                    }

                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("messageType").setValue("exit");

                    //Prevents other devices in the rooms from crashing due to lack of information.
                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("isComplete").setValue(true);

                }

                @Override
                public void onCancelled(DatabaseError databaseError) {

                }
            });
        }
    }

    /*@Override
    public void onRestart() {
        super.onRestart();

        //Adds listener back and creates an entrance message.
        chatRoomRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                final long chatRoomSize = dataSnapshot.child("Messages").getChildrenCount();

                //Creates the 'user has entered' message.
                chatRoomRef.child("Messages").child("" + chatRoomSize).child("isComplete").setValue(false);

                chatRoomRef.child("Messages").child("" + chatRoomSize).child("sendTime").setValue(System.currentTimeMillis());

                if (incognitoMode) {
                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("username").setValue("~Anonymous~");
                    //chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("incognito").setValue(true);
                } else {
                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("username").setValue(usernameExtra);
                }

                chatRoomRef.child("Messages").child("" + chatRoomSize).child("messageType").setValue("enter");

                //Prevents other devices in the rooms from crashing due to lack of information.
                chatRoomRef.child("Messages").child("" + chatRoomSize).child("isComplete").setValue(true);

                chatRoomRef.child("Empty").setValue(false);

                chatRoomRef.addValueEventListener(chatRoomEventListener);
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {

            }
        });
    }*/

    @Override
    public void onResume() {
        super.onResume();

        //Records orientation
        int orientationInt = this.getResources().getConfiguration().orientation;
        getIntent().putExtra("orientation", orientationInt);

        if(!getIntent().getExtras().getBoolean("orientRestart")) {
            chatRoomRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(DataSnapshot dataSnapshot) {
                    final long chatRoomSize = dataSnapshot.child("Messages").getChildrenCount();

                    //Creates the 'user has entered' message.
                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("isComplete").setValue(false);

                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("sendTime").setValue(System.currentTimeMillis());

                    if (incognitoMode) {
                        chatRoomRef.child("Messages").child("" + chatRoomSize).child("username").setValue("~Anonymous~");
                        //chatRoomRef.child("Messages").child("" + newMessageArrayNum).child("incognito").setValue(true);
                    } else {
                        chatRoomRef.child("Messages").child("" + chatRoomSize).child("username").setValue(usernameExtra);
                    }

                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("messageType").setValue("enter");

                    //Prevents other devices in the rooms from crashing due to lack of information.
                    chatRoomRef.child("Messages").child("" + chatRoomSize).child("isComplete").setValue(true);

                    chatRoomRef.child("Empty").setValue(false);

                    chatRoomRef.addValueEventListener(chatRoomEventListener);
                    listView.setAdapter(chatMessageAdapter);

                    //Sets onClickListener used for deleting a user's messages (NOTE: ~maybe~ add a way to make sure a user can't change their name to delete another persons messages (HARD))
                    //Remove this section of code if I don't want people to be able to delete messages.
                    listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                        @Override
                        public boolean onItemLongClick(AdapterView<?> arg0, View view, final int position, long id) {
                            if (usernameExtra.equals(messageList.get(position).getUsername()) && messageList.get(position).getMessageType().equals("chat") && messageList.get(position).getIsComplete()) {
                                PopupMenu deleteMessagePopup = new PopupMenu(ChatActivity.this, view);
                                deleteMessagePopup.getMenuInflater().inflate(R.menu.deletepopup_menu, deleteMessagePopup.getMenu());
                                deleteMessagePopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                                    @Override
                                    public boolean onMenuItemClick(MenuItem item) {
                                        chatRoomRef.child("Messages").child("" + position).child("message").setValue("[Deleted]");

                                        return true;
                                    }
                                });

                                deleteMessagePopup.show();

                            }
                            return true;
                        }
                    });
                }

                @Override
                public void onCancelled(DatabaseError databaseError) {
                }
            });
        } else {
            chatRoomRef.addValueEventListener(chatRoomEventListener);
            listView.setAdapter(chatMessageAdapter);

            //Sets onClickListener used for deleting a user's messages (NOTE: ~maybe~ add a way to make sure a user can't change their name to delete another persons messages (HARD))
            //Remove this section of code if I don't want people to be able to delete messages.
            listView.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
                @Override
                public boolean onItemLongClick(AdapterView<?> arg0, View view, final int position, long id) {
                    if (usernameExtra.equals(messageList.get(position).getUsername()) && messageList.get(position).getMessageType().equals("chat") && messageList.get(position).getIsComplete()) {
                        PopupMenu deleteMessagePopup = new PopupMenu(ChatActivity.this, view);
                        deleteMessagePopup.getMenuInflater().inflate(R.menu.deletepopup_menu, deleteMessagePopup.getMenu());
                        deleteMessagePopup.setOnMenuItemClickListener(new PopupMenu.OnMenuItemClickListener() {
                            @Override
                            public boolean onMenuItemClick(MenuItem item) {
                                chatRoomRef.child("Messages").child("" + position).child("message").setValue("[Deleted]");

                                return true;
                            }
                        });

                        deleteMessagePopup.show();

                    }
                    return true;
                }
            });
        }
    }
}

//For temporarily holding values of each chat box.
class ViewHolder {
    TextView username;
    TextView sendTime;
    TextView message;
}


