package com.trcs496.android.cs496_final_project;

/*
This class allows the user to make edits to persons
After hitting the button to enter this activity, EditText field prefilled with data from server
User can fill out EditText fields, hit edit button to send EditText fields to server to change data fields.
User can also hit delete, will remove person from DB
 */

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;

import org.json.JSONObject;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class PersonEdit extends AppCompatActivity {

    private TextView message;//fields from layout
    private EditText firstName;
    private EditText lastName;
    private EditText dob;

    private String userKey;//keys sent by main activity
    private String personKey;

    private String scheme;//Strings pulled from Strings xml
    private String host;

    private String action_edit = "edit_person";//actions/API URL paths
    private String action_delete = "delete_person";
    private String action_get_person = "person_get_single";

    final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_person_edit);

        Intent intent = getIntent();//get keys
        userKey = intent.getStringExtra(MainActivity.USER_KEY);
        personKey = intent.getStringExtra(MainActivity.PERSON_KEY);

        scheme = getString(R.string.scheme);//fill scheme and host values
        host = getString(R.string.host);

        message = (TextView) findViewById(R.id.message);//set fields from layout to proper views
        firstName = (EditText) findViewById(R.id.firstName);
        lastName = (EditText) findViewById(R.id.lastName);
        dob = (EditText) findViewById(R.id.dob);

        getPerson();
    }
    //this function receives an action, pulls user and person keys and fills out url
    private HttpUrl buildURL(String action){
        HttpUrl url = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegments(action)
                .addPathSegments(userKey)
                .addPathSegment(personKey)
                .build();
        return url;
    }
    //creates header
    private okhttp3.Headers getHeader(){
        okhttp3.Headers header = new okhttp3.Headers.Builder()
                .set("Accept", "application/json")
                .build();
        return header;
    }
    //builds body from what is in EditText fields
    private FormBody buildBody(){
        FormBody body = new FormBody.Builder()
                .add("firstName", firstName.getText().toString())
                .add("lastName", lastName.getText().toString())
                .add("dob", dob.getText().toString())
                .build();
        return body;
    }
    //this function gets the selected person from the server
    public void getPerson(){
        new Thread(new Runnable() {//network calls can't be made in threads
            @Override
            public void run() {
                HttpUrl url = buildURL(action_get_person);//build urls
                okhttp3.Headers headers = getHeader();//build header
                Request request = new Request.Builder()//build request
                        .url(url)
                        .headers(headers)
                        .get()
                        .build();

                try {//send request
                    final okhttp3.Response response;
                    response = client.newCall(request).execute();//get response
                    if (!response.isSuccessful()) {//if response bad, (400+, 500+ etc, print response)
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    message.setText(response.body().string());
                                } catch (Exception e) {//or print error message if trouble converting response to String
                                    message.setText(e.toString());
                                }
                            }
                        });
                    } else {//response == good, fill EditText fields
                        final JSONObject object = new JSONObject(response.body().string());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    firstName.setText(object.get("firstName").toString());
                                    lastName.setText(object.get("lastName").toString());
                                    dob.setText(object.get("dob").toString());
                                }catch (Exception e){
                                    message.setText(e.toString());
                                }
                            }
                        });

                    }
                }catch (final Exception e){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            message.setText(e.toString());
                        }
                    });
                }


            }
        }).start();
    }
    //listens for edit button, will send data in EditText fields to API server
    public void listenEdit(View view){
        new Thread(new Runnable() {//network calls can't be made on UI thread
            @Override
            public void run() {
                HttpUrl url = buildURL(action_edit);//build url w/ edit
                okhttp3.Headers headers = getHeader();//get header
                FormBody formBody = buildBody();//get body
                Request request = new Request.Builder()//build request
                        .url(url)
                        .headers(headers)
                        .put(formBody)//making a put to do edits as per API
                        .build();

                try {//send request, get response
                    final okhttp3.Response response;
                    response = client.newCall(request).execute();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response.isSuccessful()){//if response is good (not 400 or 500), print results
                                try{
                                    JSONObject object = new JSONObject(response.body().string());//convert resposne to json
                                    message.setText("Message From Server: " + object.get("result").toString());//print fail or success
                                    if (!object.get("result").toString().equals("success")){//if not success get cause
                                        message.setText(message.getText() + "\n" + object.get("cause").toString());
                                    }
                                }catch (Exception e){
                                    message.setText(e.toString());
                                }

                            }else{
                                message.setText("ERROR: \n" + response.toString());
                            }

                        }
                    });


                }catch (final Exception e){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            message.setText(e.toString());
                        }
                    });
                }

            }
        }).start();
    }

    //this function listens for the delete button, makes the DELETE request
    public void listenDelete(View view){
        new Thread(new Runnable() {//don't run on UI thread
            @Override
            public void run() {
                HttpUrl url = buildURL(action_delete);//build url with delete path
                okhttp3.Headers headers = getHeader();
                Request request = new Request.Builder()
                        .url(url)
                        .headers(headers)
                        .delete()//make the delete call
                        .build();

                try {//try for response
                    final okhttp3.Response response;
                    response = client.newCall(request).execute();

                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            if (response.isSuccessful()){
                                try{
                                    JSONObject object = new JSONObject(response.body().string());//get response into json
                                    message.setText("Message From Server: " + object.get("result").toString());//print result
                                    if (!object.get("result").toString().equals("success")){//if result is fail, print why it failed (the 'cause' key)
                                        message.setText(message.getText() + "\n" + object.get("cause").toString());
                                    }else{//otherwise end activity--should add in an else if statement to catch in case response isn't fail or success
                                        finish();
                                    }
                                }catch (Exception e){
                                    message.setText(e.toString());
                                }

                            }else{
                                message.setText("ERROR: \n" + response.toString());//if response 400+, print error message
                            }

                        }
                    });


                }catch (final Exception e){//if error caught, print it
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            message.setText(e.toString());
                        }
                    });
                }

            }
        }).start();
    }

}
