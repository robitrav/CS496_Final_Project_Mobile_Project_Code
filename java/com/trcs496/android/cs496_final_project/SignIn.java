/*
This class is the beggining of the CS496 Project
It sends POST requests to the API containing the users username and password, receiving the users key in exchange
Key is passed to other activities so that user can continue to access data kept on server
 */

package com.trcs496.android.cs496_final_project;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import org.json.JSONObject;

import okhttp3.FormBody;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;

public class SignIn extends AppCompatActivity {

    EditText userName;
    EditText password;
    TextView message;
    private final OkHttpClient client = new OkHttpClient();
    public final static String KEY = "KEY";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_sign_in);
        message = (TextView) findViewById(R.id.message);//initialize materials from layout
        userName = (EditText) findViewById(R.id.userName);
        password = (EditText) findViewById(R.id.password);
    }
    //listens for login button to be clicked, calls make submission with appropriate path sent as string
    public void loginListener(View view){
        if (allFieldsPresent()){
            makeSubmission("user_login");
        }
    }
    //listens for sign up button, makes call to makeSubmission method with appropriate path segment sent as string
    public void signupListener(View view){
        if (allFieldsPresent()){
            makeSubmission("user_create_account");
        }
    }
    //returns true if all req'd fields are present, else returns false
    //req'd fields are user name and password
    private boolean allFieldsPresent(){
        if (userName.getText().toString().equals("")){
            message.setText("Please inlcude User Name");
        }
        else if (password.getText().toString().equals("")){
            message.setText("Please include password");
        }
        else{
            return true;
        }
        return false;
    }
    //builds url using action sent to it
    private HttpUrl buildURL(String action){
        HttpUrl url = new HttpUrl.Builder()
                .scheme("https")
                .host("cs496finalproject-150703.appspot.com")
                .addPathSegment(action)
                .build();
        return url;
    }
    //builds header--app requires replies to be json objects
    private okhttp3.Headers getHeader(){
        okhttp3.Headers header = new okhttp3.Headers.Builder()
                .set("Accept", "application/json")
                .build();
        return header;
    }
    //builds the body by retrieving username and password
    private FormBody buildBody(){
        FormBody body = new FormBody.Builder()
                .add("userName", userName.getText().toString())
                .add("password", password.getText().toString())
                .build();
        return body;
    }
    //this function makes the actual submission, calling on url, body and header building functions
    //accepts a string that contains the appropriate pathway (called action here)
    //if key received and response is success then starts up main intent, else displays message
    private void makeSubmission(String action){
        final String act = action;
        new Thread(new Runnable() {//network calls must be made on nonmain/nonUI thread to avoid blocking UI
            @Override
            public void run() {
                HttpUrl url = buildURL(act);//build URL
                okhttp3.Headers header = getHeader();//get header
                FormBody body = buildBody();//get body
                okhttp3.Request request = new okhttp3.Request.Builder()//build request
                        .url(url)
                        .headers(header)
                        .post(body)
                        .build();
                try{//make request, in try in case something happens
                    final okhttp3.Response response;
                    response = client.newCall(request).execute();
                    if (!response.isSuccessful()){//something wrong happened
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {//changing textView must be done on UI thread
                                message.setText("There was an error in receiving response from server");
                            }
                        });
                    }
                    else{
                        runOnUiThread(new Runnable() {//successfully contacted server
                            @Override
                            public void run() {//changing textView must be done on UI thread
                                try{
//                                    message.setText(response.body().string());
                                    JSONObject object = new JSONObject(response.body().string());//response needs to be json object
                                    if (object.getString("result").equals("success")){//here's where we need to start up the next activity
                                        Intent intent = new Intent(getApplicationContext(),MainActivity.class);
                                        intent.putExtra(KEY,object.getString("key"));//send key to main activity
                                        startActivity(intent);//start main activity


                                        //message.setText(object.getString("key"));
                                    }
                                    else{
                                        message.setText(object.getString("cause").toString());//print error if we fail to receive success message
                                    }
                                }catch (Exception e){//error getting body
                                    message.setText(e.toString());
                                }

                            }
                        });
                    }
                }
                catch(final Exception e){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {//changing textView must be done on UI thread
                            message.setText("There was an error: " + e.toString());
                        }
                    });
                }
            }
        }).start();
    }
}
