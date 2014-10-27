package com.jamol.contacts;

import android.accounts.Account;
import android.accounts.AccountManager;
import android.accounts.AuthenticatorDescription;
import android.app.Activity;
import android.app.ListActivity;
import android.content.ContentProviderOperation;
import android.content.ContentProviderResult;
import android.content.ContentResolver;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetFileDescriptor;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.provider.Contacts;
import android.provider.ContactsContract;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.QuickContactBadge;
import android.widget.TextView;
import android.widget.Toast;

import java.io.FileDescriptor;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


public class MainActivity extends ListActivity {

    private MyListViewAdapter mListAdapter;
    private TextView mFooterTextView;
    private static final int MSG_CONTACT_LOADING = 0;
    private static final int MSG_CONTACT_DONE = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        View footer = getLayoutInflater().inflate(R.layout.lv_footer, null);
        mFooterTextView = (TextView)footer.findViewById(R.id.textFooter);
        getListView().addFooterView(footer);

        getListView().setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                TextView tv = (TextView) view.findViewById(R.id.userName);
                Toast.makeText(MainActivity.this, "你点击了第" + position + "项的" + tv.getText().toString(), Toast.LENGTH_LONG).show();
            }
        });

        getListView().setOnCreateContextMenuListener(new View.OnCreateContextMenuListener() {
            @Override
            public void onCreateContextMenu(ContextMenu menu, View v, ContextMenu.ContextMenuInfo menuInfo) {
                AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)menuInfo;
                View itemView = info.targetView;
                TextView userName = (TextView)itemView.findViewById(R.id.userName);
                menu.setHeaderIcon(R.drawable.ic_launcher);
                menu.setHeaderTitle(userName.getText().toString());
                menu.add(1, 0, 0, "修改");
                menu.add(0, 1, 0, "删除");
            }
        });

        List<Map<String, Object>> userList = new ArrayList<Map<String, Object>>();
        mListAdapter = new MyListViewAdapter(this, userList);
        setListAdapter(mListAdapter);
        mFooterTextView.setText("Loading ...");

        //addContact("测试1", "13958023408", ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE,
        //        "vnd.sec.contact.phone", "vnd.sec.contact.phone");

        //getAllContacts();

        final Handler handler=new Handler() {
            @Override
            public void handleMessage(Message msg) {
                super.handleMessage(msg);
                switch (msg.what){
                    case MSG_CONTACT_LOADING:
                        mFooterTextView.setText("Loading ... " + (msg.arg2*100/msg.arg1) + "%");
                        if(msg.obj != null) {
                            mListAdapter.setItems((List<Map<String, Object>>) msg.obj);
                        }
                        break;
                    case MSG_CONTACT_DONE:
                        if(msg.obj != null) {
                            mListAdapter.setItems((List<Map<String, Object>>) msg.obj);
                        }
                        mFooterTextView.setText(mListAdapter.getCount() + " 个联系人");
                        break;
                }
            }
        };
        Thread thread = new Thread()
        {
            @Override
            public void run() {
                try {
                    loadPhoneContacts(handler);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        };
        thread.start();
    }

    @Override
    public boolean onContextItemSelected(MenuItem item) {
        AdapterView.AdapterContextMenuInfo info = (AdapterView.AdapterContextMenuInfo)item.getMenuInfo();
        View itemView = info.targetView;
        TextView userName = (TextView)itemView.findViewById(R.id.userName);
        String op = "";
        switch (item.getItemId()) {
            case 0:
                op = "修改";
                break;

            case 1:
                op = "删除";
                break;

            default:
                break;
        }
        Toast.makeText(this, op+userName.getText().toString(), Toast.LENGTH_LONG).show();
        return super.onContextItemSelected(item);
    }

    void getAccounts(){
        Log.i("Contacts", "---------------------------------------------------------------");
        AccountManager am = AccountManager.get(this);
        //Account[] accounts = am.getAccountsByType("com.smids.accountType");
        AuthenticatorDescription[] accountTypes = am.getAuthenticatorTypes();
        for (int i = 0; i < accountTypes.length; i++) {
            String accountType = accountTypes[i].type;
            Account[] accounts = am.getAccountsByType(accountType);
            Log.i("Contacts", "account type: " + accountType + ", account count:" + accounts.length);
            //AuthenticatorDescription ad = getAuthenticatorDescription(systemAccountType, accountTypes);
            //AccountData data = new AccountData(a[i].name, ad);
            //mAccounts.add(data);
        }

        Log.i("Contacts", "---------------------------------------------------------------");
        Account[] accounts = am.getAccounts();
        for (int i = 0; i < accounts.length; ++i) {
            Log.i("Contacts", "accountType: " + accounts[i].type + ", accountName: " + accounts[i].name);
        }
    }

    void getAllContacts() {
        ContentResolver cr = getContentResolver();
        Uri uri = ContactsContract.Contacts.CONTENT_URI;
        Cursor contactCursor = cr.query(uri, null, null, null, null);
        if (contactCursor.getCount() > 0) {
            while (contactCursor.moveToNext()) {
                String id = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.Contacts._ID));
                String name = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.Contacts.DISPLAY_NAME));
                String lookupKey = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                Log.i("Contacts", "id=" + id + ", name=" + name + ", lookup=" + lookupKey);
                if (Integer.parseInt(contactCursor.getString(
                        contactCursor.getColumnIndex(ContactsContract.Contacts.HAS_PHONE_NUMBER))) > 0) {
                    Cursor phoneCursor = cr.query(
                        ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                        null,
                        ContactsContract.CommonDataKinds.Phone.CONTACT_ID +" = ?",
                        new String[]{id}, null);
                    while (phoneCursor.moveToNext()) {
                        String phoneNo = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                        String phoneType = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                        //Log.i("Contacts", "name: " + name + ", phone No: " + phoneNo + ", phone type: " + phoneType);
                    }
                    phoneCursor.close();
                }
            }
            contactCursor.close();
        }
    }

    void getSIMContacts() {
        //读取SIM卡手机号,有两种可能:content://icc/adn与content://sim/adn
        try {
            Uri uri = Uri.parse("content://icc/adn");
            Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                while (cursor.moveToNext()) {
                    // 取得联系人名字
                    int nameFieldColumnIndex = cursor.getColumnIndex("name");
                    String name = cursor.getString(nameFieldColumnIndex);
                    // 取得电话号码
                    int numberFieldColumnIndex = cursor.getColumnIndex("number");
                    String phoneNo = cursor.getString(numberFieldColumnIndex);
                    Log.i("Contacts", "name: " + name + ", phone No: " + phoneNo);
                }
                cursor.close();
            }
        } catch (Exception e) {
            Log.i("Contacts", e.toString());
        }
    }

    void loadPhoneContacts(Handler handler) {
        ContentResolver cr = getContentResolver();
        final String[] RAW_PROJECTION = new String[] {
                ContactsContract.RawContacts._ID,
                ContactsContract.RawContacts.CONTACT_ID,
                ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY,
                ContactsContract.RawContacts.ACCOUNT_TYPE,
                ContactsContract.RawContacts.ACCOUNT_NAME,
        };
        final String[] CONTACT_PROJECTION = new String[] {
                ContactsContract.Contacts.LOOKUP_KEY,
                ContactsContract.Contacts.PHOTO_THUMBNAIL_URI
        };
        final String[] PHONE_PROJECTION = new String[] {
                ContactsContract.CommonDataKinds.Phone.NUMBER,
                ContactsContract.CommonDataKinds.Phone.TYPE
        };
        String[] rawSelectionArgs = {
                "com.anddroid.contacts.sim",
                "com.cisco.im.account"
        };
        List<Map<String, Object>> userList = new ArrayList<Map<String, Object>>();

        Cursor rawCursor = cr.query(ContactsContract.RawContacts.CONTENT_URI,
                null,
                ContactsContract.RawContacts.ACCOUNT_TYPE + " <> ?" + " AND "
                    + ContactsContract.RawContacts.ACCOUNT_TYPE + " <> ?",
                //ContactsContract.RawContacts.ACCOUNT_TYPE + "='com.anddroid.contacts.phone'",
                rawSelectionArgs,
                ContactsContract.RawContacts.SORT_KEY_PRIMARY + " COLLATE LOCALIZED ASC");
                //ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY + " COLLATE LOCALIZED ASC");
        if(rawCursor == null){
            Message msg = new Message();
            msg.what = MSG_CONTACT_DONE;
            msg.obj = userList;
            handler.sendMessage(msg);
            return ;
        }
        Log.i("Contacts", "Contact count: " + rawCursor.getCount());
        while (rawCursor.moveToNext()) {
            String rawId = rawCursor.getString(rawCursor.getColumnIndex(ContactsContract.RawContacts._ID));
            String contactId = rawCursor.getString(rawCursor.getColumnIndex(ContactsContract.RawContacts.CONTACT_ID));
            String name = rawCursor.getString(rawCursor.getColumnIndex(ContactsContract.RawContacts.DISPLAY_NAME_PRIMARY));
            String accountType = rawCursor.getString(rawCursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_TYPE));
            String accountName = rawCursor.getString(rawCursor.getColumnIndex(ContactsContract.RawContacts.ACCOUNT_NAME));

            // get LOOKUP_KEY from table Contacts
            Uri contactUri = null;
            String thumbnailUri = "";
            String[] contactSelectionArgs = { contactId };
            Cursor contactCursor = cr.query(ContactsContract.Contacts.CONTENT_URI,
                    CONTACT_PROJECTION,
                    ContactsContract.Contacts._ID + " = ?",
                    contactSelectionArgs,
                    null);
            if(contactCursor != null){
                if(contactCursor.moveToFirst()) {
                    String lookupKey = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.Contacts.LOOKUP_KEY));
                    contactUri = ContactsContract.Contacts.getLookupUri(Long.parseLong(contactId), lookupKey);
                    thumbnailUri = contactCursor.getString(contactCursor.getColumnIndex(ContactsContract.Contacts.PHOTO_THUMBNAIL_URI));
                }
                contactCursor.close();
            }

            // get phone number from table Data
            Cursor phoneCursor = cr.query(
                    ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                    PHONE_PROJECTION,
                    ContactsContract.CommonDataKinds.Phone.CONTACT_ID + " = ?",
                    new String[]{contactId}, null);
            String phoneNum = "";
            if (phoneCursor != null) {
                while (phoneCursor.moveToNext()) {
                    String phoneNo = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                    String phoneType = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.TYPE));
                    phoneNum = phoneNo;
                    // 对手机号码进行预处理（去掉号码前的+86、首尾空格、“-”号等）
                    phoneNum = phoneNum.replaceAll("^(\\+86)", "");
                    phoneNum = phoneNum.replaceAll("^(86)", "");
                    phoneNum = phoneNum.replaceAll("-", "");
                    phoneNum = phoneNum.trim();
                    break;
                }
                phoneCursor.close();
            }
            Log.i("Contacts", "rawId: " + rawId + ", contactId=" + contactId + ", name: " + name
                    + ", phoneNo: " + phoneNum + ", accountType: " + accountType
                    + ", accountName: " + accountName);
            HashMap<String, Object> hMap = new HashMap<String, Object>();
            hMap.put("userName", name);
            hMap.put("phoneNum", phoneNum);
            hMap.put("contactUri", contactUri);
            hMap.put("thumbnailUri", thumbnailUri);
            userList.add(hMap);

            Message msg = new Message();
            msg.what = MSG_CONTACT_LOADING;
            msg.arg1 = rawCursor.getCount();
            msg.arg2 = userList.size();
            if(msg.arg2%10 == 0) {
                msg.obj = new ArrayList<Map<String, Object>>(userList);
            }
            handler.sendMessage(msg);
        }
        rawCursor.close();

        Message msg = new Message();
        msg.what = MSG_CONTACT_DONE;
        msg.obj = userList;
        handler.sendMessage(msg);
        return ;
    }

    void getPhoneContacts2() {
        ContentResolver resolver = getContentResolver();
        // 获取手机联系人
        Cursor phoneCursor = resolver.query(ContactsContract.CommonDataKinds.Phone.CONTENT_URI, null, null, null, null);
        if (phoneCursor != null) {
            while (phoneCursor.moveToNext()) {
                //得到手机号码
                String phoneNo = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER));
                //当手机号码为空的或者为空字段 跳过当前循环
                if (TextUtils.isEmpty(phoneNo))
                    continue;

                //得到联系人名称
                String name = phoneCursor.getString(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME));

                //得到联系人ID
                Long contactid = phoneCursor.getLong(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.CONTACT_ID));

                //得到联系人头像ID
                Long photoid = phoneCursor.getLong(phoneCursor.getColumnIndex(ContactsContract.CommonDataKinds.Phone.PHOTO_ID));
                Log.i("Contacts", "id: " + contactid + ", name: " + name + ", phone No: " + phoneNo);
            }

            phoneCursor.close();
        }
    }

    void addContact(String name, String phoneNum, int phoneType, String accountType, String accountName){
        ArrayList<ContentProviderOperation> ops = new ArrayList<ContentProviderOperation>();
        ops.add(ContentProviderOperation.newInsert(ContactsContract.RawContacts.CONTENT_URI)
                .withValue(ContactsContract.RawContacts.ACCOUNT_TYPE, accountType)
                .withValue(ContactsContract.RawContacts.ACCOUNT_NAME, accountName)
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.StructuredName.DISPLAY_NAME, name)
                .build());
        ops.add(ContentProviderOperation.newInsert(ContactsContract.Data.CONTENT_URI)
                .withValueBackReference(ContactsContract.Data.RAW_CONTACT_ID, 0)
                .withValue(ContactsContract.Data.MIMETYPE,
                        ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE)
                .withValue(ContactsContract.CommonDataKinds.Phone.NUMBER, phoneNum)
                        //.withValue(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE)
                .withValue(ContactsContract.CommonDataKinds.Phone.TYPE, phoneType)
                .build());
        try {
            ContentProviderResult[] results = getContentResolver().applyBatch(ContactsContract.AUTHORITY, ops);
            for(ContentProviderResult result : results){
                Log.i("Contacts", result.uri.toString());
            }
        } catch (Exception e) {
            Log.e("Contacts", "Exception encountered while inserting contact: " + e);
        }
    }

    void addContact(){
        ContentValues values = new ContentValues();
//首先向RawContacts.CONTENT_URI执行一个空值插入，目的是获取系统返回的rawContactId
        Uri rawContactUri = getContentResolver().insert(ContactsContract.RawContacts.CONTENT_URI, values);
        long rawContactId = ContentUris.parseId(rawContactUri);
//往data表入姓名数据
        values.clear();
        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.StructuredName.CONTENT_ITEM_TYPE);//内容类型
        values.put(ContactsContract.CommonDataKinds.StructuredName.GIVEN_NAME, "李天山");
        getContentResolver().insert(android.provider.ContactsContract.Data.CONTENT_URI, values);
//往data表入电话数据
        values.clear();
        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Phone.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.Phone.NUMBER, "13921009789");
        values.put(ContactsContract.CommonDataKinds.Phone.TYPE, ContactsContract.CommonDataKinds.Phone.TYPE_MOBILE);
        getContentResolver().insert(android.provider.ContactsContract.Data.CONTENT_URI, values);
//往data表入Email数据
        values.clear();
        values.put(ContactsContract.Data.RAW_CONTACT_ID, rawContactId);
        values.put(ContactsContract.Data.MIMETYPE, ContactsContract.CommonDataKinds.Email.CONTENT_ITEM_TYPE);
        values.put(ContactsContract.CommonDataKinds.Email.DATA, "liming@itcast.cn");
        values.put(ContactsContract.CommonDataKinds.Email.TYPE, ContactsContract.CommonDataKinds.Email.TYPE_WORK);
        getContentResolver().insert(android.provider.ContactsContract.Data.CONTENT_URI, values);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private Bitmap loadContactPhotoThumbnail(String photoData) {
        // Creates an asset file descriptor for the thumbnail file.
        AssetFileDescriptor afd = null;
        // try-catch block for file not found
        try {
            // Creates a holder for the URI.
            Uri thumbUri = Uri.parse(photoData);

            /*
             * Retrieves an AssetFileDescriptor object for the thumbnail
             * URI
             * using ContentResolver.openAssetFileDescriptor
             */
            afd = getContentResolver().openAssetFileDescriptor(thumbUri, "r");
            /*
             * Gets a file descriptor from the asset file descriptor.
             * This object can be used across processes.
             */
            FileDescriptor fileDescriptor = afd.getFileDescriptor();
            // Decode the photo file and return the result as a Bitmap
            // If the file descriptor is valid
            if (fileDescriptor != null) {
                // Decodes the bitmap
                return BitmapFactory.decodeFileDescriptor(
                        fileDescriptor, null, null);
            }
            // If the file isn't found
        } catch (FileNotFoundException e) {
            /*
             * Handle file not found errors
             */
            // In all cases, close the asset file descriptor
        } finally {
            if (afd != null) {
                try {
                    afd.close();
                } catch (IOException e) {}
            }
        }
        return null;
    }

    public class MyListViewAdapter extends BaseAdapter {
        private List<Map<String, Object>> mItems;
        private LayoutInflater mInflater;

        public MyListViewAdapter(Context context, List<Map<String, Object>> items) {
            this.mItems = items;
            mInflater = (LayoutInflater)context.getSystemService(Context.LAYOUT_INFLATER_SERVICE);
        }

        public void setItems(List<Map<String, Object>> items){
            this.mItems = items;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mItems.size();
        }

        @Override
        public Object getItem(int position) {
            return mItems.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        public final class ViewHolder{
            //public ImageView userImage;
            public TextView userName;
            public TextView phoneNum;
            public ImageView callImage;
            public QuickContactBadge quickBadge;
        }

        @Override
        public View getView(int position, View view, ViewGroup parent) {
            ViewHolder holder = null;
            if (view == null) {
                view = mInflater.inflate(R.layout.lv_item, null);
                holder = new ViewHolder();
                //holder.userImage = (ImageView) view.findViewById(R.id.userImage);
                holder.userName = (TextView) view.findViewById(R.id.userName);
                holder.phoneNum = (TextView) view.findViewById(R.id.phoneNum);
                holder.callImage = (ImageView) view.findViewById(R.id.callImage);
                holder.quickBadge = (QuickContactBadge) view.findViewById(R.id.quickBadge);
                view.setTag(holder);
                holder.quickBadge.setMode(ContactsContract.QuickContact.MODE_SMALL);
                holder.callImage.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        String phoneNum = (String)v.getTag();
                        if(phoneNum != null){
                            Intent dialIntent = new Intent(Intent.ACTION_CALL, Uri.parse("tel:" + phoneNum));
                            startActivity(dialIntent);
                        }
                    }
                });
            } else {
                holder = (ViewHolder)view.getTag();
            }

            //holder.userImage.setBackgroundResource((Integer)mItems.get(position).get("userImage"));
            String phoneNum = mItems.get(position).get("phoneNum").toString();
            holder.userName.setText(mItems.get(position).get("userName").toString());
            holder.phoneNum.setText(phoneNum);
            holder.callImage.setTag(holder.phoneNum.getText().toString());
            holder.callImage.setImageResource(R.drawable.ic_call);
            holder.callImage.setBackgroundResource(R.drawable.img_bg);
            //holder.callImage.setBackgroundResource((Integer)mItems.get(position).get("callImage"));
            Uri contactUri = (Uri)mItems.get(position).get("contactUri");
            if(contactUri != null) {
                holder.quickBadge.assignContactUri(contactUri);
            } else {
                holder.quickBadge.assignContactFromPhone(phoneNum, true);
            }
            Object objThunbnailUri = mItems.get(position).get("thumbnailUri");
            Bitmap thumbnail = null;
            if(objThunbnailUri != null) {
                thumbnail = loadContactPhotoThumbnail(objThunbnailUri.toString());
            } else {
                thumbnail = BitmapFactory.decodeResource(getResources(), R.drawable.ic_user);
            }
            holder.quickBadge.setImageBitmap(thumbnail);
            return view;
        }
    }
}
