package com.trcs496.android.cs496_final_project;

import android.app.ActionBar;
import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.squareup.picasso.Picasso;

import org.json.JSONArray;
import org.json.JSONObject;

import okhttp3.FormBody;
import okhttp3.Headers;
import okhttp3.HttpUrl;
import okhttp3.OkHttpClient;
import okhttp3.Request;

public class ImageEdit extends AppCompatActivity {

    private String scheme;//scheme and host that are used by API
    private String host;
    private final String action_single_image = "photo_get_single";//actions/paths of API
    private final String action_single_person = "person_get_single";
    private final String action_change_tagged = "tag_person";
    private String userKey;//user and photo keys sent by main activity
    private String photoKey;

    private EditText photoDescription;//layout fields that we need to fill out or pull data from
    private TextView uploadDate;
    private ImageView image;
    private TextView message;
    private LinearLayout personList;
    private LinearLayout personsNotInPhoto;

    final OkHttpClient client = new OkHttpClient();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        scheme = getString(R.string.scheme);//pulled from Strings xml file
        host = getString(R.string.host);

        Intent intent = getIntent();//get user and image keys
        userKey = intent.getStringExtra(MainActivity.USER_KEY);
        photoKey = intent.getStringExtra(MainActivity.IMAGE_KEY);

        setContentView(R.layout.activity_image_edit);
        userKey = getIntent().getStringExtra(MainActivity.USER_KEY);
        photoKey = getIntent().getStringExtra(MainActivity.IMAGE_KEY);
        //set values of things from layout fields (EditText, Textviews, etc)
        photoDescription = (EditText) findViewById(R.id.photoDescription);
        uploadDate = (TextView) findViewById(R.id.uploadDate);
        image = (ImageView) findViewById(R.id.image);
        message = (TextView) findViewById(R.id.message);
        personList = (LinearLayout) findViewById(R.id.listInPhoto);
        personsNotInPhoto = (LinearLayout) findViewById(R.id.listNotInPhoto);

        getPhoto();//get the photo
        getPersonsNotInPhoto();//get the people who aren't in the photo, so that we can make buttons to add them
    }
    //this function gets a list of people who aren't in the photo, makes buttons for them to add them to the photo
    public void getPersonsNotInPhoto(){

        new Thread(new Runnable() {
            @Override
            public void run() {//can't make network calls on main/ui thread
                HttpUrl url = buildURL("person");//path to get all people
                Headers headers = getHeader();
                Request request = new Request.Builder()//build request
                        .url(url)
                        .headers(headers)
                        .get()
                        .build();

                try{//get response
                    final okhttp3.Response response;
                    response = client.newCall(request).execute();
                    if (!response.isSuccessful()){//if resposne is a failing value, print resposnse
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    message.setText(response.body().string());
                                }catch (Exception e){
                                    message.setText(e.toString());
                                }

                            }
                        });
                    }
                    else{//otherwise, make an object of the resposne
                        final JSONObject object = new JSONObject(response.body().string());
                        //final JSONObject object = new JSONObject("{'result':'fail'}");
                        if (object.get("result").toString().equals("fail")){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        message.setText("Message from Server: " + object.get("cause").toString());
                                    }catch (Exception e){
                                        message.setText(e.toString());
                                    }
                                }
                            });
                        }
                        else if (object.get("result").equals("success")){//if we're successful, there will be an array of JSON objects in the response containing people
                            JSONArray array = object.getJSONArray("people");//get the array
                            weedOutThoseInList(array);//weed out those who need to be
                        }
                        else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {//print if we don't succeed or fail
                                    message.setText(object.toString());
                                }
                            });
                        }
                    }
                }
                catch (final Exception e){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            message.setText("ERROR: " + e.toString());
                        }
                    });
                }

            }
        }).start();
    }
    //this function weeds out those who were in the full list of people but are in the photo
    public void weedOutThoseInList(final JSONArray arrayOfAllPeople){

        for (int i = 0; i < arrayOfAllPeople.length(); i++){
            try{
                final JSONObject object = arrayOfAllPeople.getJSONObject(i);
                final JSONArray arrayOfPhotosPersonIn = object.getJSONArray("taggedPhotos");

                if (arrayOfPhotosPersonIn.length() != 0){
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            boolean personInPhoto = false;
                            try{
                                for (int x = 0; x < arrayOfPhotosPersonIn.length(); x++){//cycle through all the photos the person is in and see if it's the photo of this activity
                                    if (arrayOfPhotosPersonIn.get(x).toString().equals(photoKey)){
                                        personInPhoto = true;
                                    }
                                }

                                if (personInPhoto == false){//person isn't in photo, add a button to put them in it
                                    PicassoButton button = new PicassoButton(getApplicationContext());//used as temp place holder--studio can't find Button
                                    String name = object.get("firstName").toString() + " " + object.get("lastName");//put persons name in button
                                    button.setText("Add " + name);//along with the add command so we know what we're doing to them
                                    button.setTag(object.get("key"));//set tag ot be the persons key
                                    button.setOnClickListener(listenAdd(button));//set listener
                                    button.setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                    personsNotInPhoto.addView(button);//add button to proper view/lsit
                                }
                            }catch (Exception e){
                                message.setText(e.toString());
                            }

                        }
                    });
                }
                else{//the person is in no photos--if in no photos they're not in this one and need to be added to the list of people we can add
                    runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            try{
                                PicassoButton button = new PicassoButton(getApplicationContext());//used as temp place holder--studio can't find Button
                                String name = object.get("firstName").toString() + " " + object.get("lastName");//add their name and key to button
                                button.setText("Add " + name);
                                button.setTag(object.get("key"));
                                button.setOnClickListener(listenAdd(button));//set listener
                                button.setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                personsNotInPhoto.addView(button);//add person button to the list
                            }catch (Exception e){
                                message.setText(e.toString());
                            }


                        }
                    });
                }


            }catch (final Exception e){//not running on UI-thread when called-should prolly check this but short on time :(
                runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        message.setText(e.toString());
                    }
                });
            }

        }
    }
    //this function listens to change the description of the photo
    public void listenChangePhotoName(View view){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpUrl url = buildURL("edit_photo",userKey,photoKey);//send proper path and keys
                Headers headers = getHeader();
                FormBody formBody = buildBody();//build the body with the new description
                Request request = new Request.Builder()//build the request
                        .url(url)
                        .headers(headers)
                        .put(formBody)
                        .build();

                try{//send the request
                    final okhttp3.Response response;
                    response = client.newCall(request).execute();
                    if (!response.isSuccessful()){//something wrong happened
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {//changing textView must be done on UI thread
                                message.setText("There was an error in receiving response from server\n" + response.body().toString());
                            }
                        });
                    }
                    else{
                        final JSONObject object = new JSONObject(response.body().string());//response is JSON object
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {//print whatever response message is
                                try{
                                    if (object.get("result").toString().equals("fail")){
                                        message.setText("Message from server: " + object.get("cause").toString());
                                    }
                                    else if (object.get("result").toString().equals("success")){
                                        message.setText("Message From Server: " + object.get("result") + "\nOld Description: " + object.get("from").toString() + "\nNew Description: " + object.get("to").toString());
                                    }
                                    else{
                                        message.setText(response.body().string());
                                    }
                                }
                                catch (Exception e){
                                    message.setText(e.toString());
                                }

                            }
                        });
                    }
                }
                catch (final Exception e){
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
    //ge the photo we're in
    private void getPhoto(){

        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpUrl url = buildURL(action_single_image, photoKey);//add action and photokey
                Headers headers = getHeader();
                Request request = new Request.Builder()
                        .url(url)
                        .headers(headers)
                        .get()
                        .build();
                try{
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
                        final JSONObject object = new JSONObject(response.body().string());//retrieve JSON response
                        JSONArray taggedPeople = object.getJSONArray("taggedPeople");//pull array of JSON objects from JSON resposne
                        getTaggedPeople(taggedPeople);//send off to method that will add their buttons so they can be removed
                        runOnUiThread(new Runnable() {//successfully contacted server
                            @Override
                            public void run() {//changing textView must be done on UI thread
                                try{
                                    if (object.get("result").toString().equals("success")){//get messages and display to user
                                        photoDescription.setText(object.get("description").toString());
                                        uploadDate.setText("Upload Date: " + object.get("uploadDate").toString());
                                        Picasso.with(getApplicationContext()).load(object.get("image").toString()).into(image);
                                    }
                                    else{
                                        message.setText(message.getText() + "\nCause: " + object.get("cause"));
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
    //the following functions build URLs accepting assorted keys and actions to make submissions to API
    private HttpUrl buildURL(String action){
        HttpUrl url = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegments(action)
                .addPathSegment(userKey)
                .build();
        return url;
    }


    private HttpUrl buildURL(String action, String firstKey){
        HttpUrl url = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegments(action)
                .addPathSegments(userKey)
                .addPathSegment(firstKey)
                .build();
        return url;
    }

    private HttpUrl buildURL(String action, String userKey, String firstKey){
        HttpUrl url = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegments(action)
                .addPathSegments(userKey)
                .addPathSegment(firstKey)
                .build();
        return url;
    }
    //used to build url for puting and deleting tagged people in photos
    private HttpUrl buildURL(String action, String userKey, String firstKey, String secondKey){
        HttpUrl url = new HttpUrl.Builder()
                .scheme(scheme)
                .host(host)
                .addPathSegments(action)
                .addPathSegments(userKey)
                .addPathSegments("photo")
                .addPathSegments(firstKey)
                .addPathSegments("person")
                .addPathSegment(secondKey)
                .build();
        return url;
    }
    //builds header, our app needs to receive JSON objects
    private okhttp3.Headers getHeader(){
        okhttp3.Headers header = new okhttp3.Headers.Builder()
                .set("Accept", "application/json")
                .build();
        return header;
    }
    //build body that contains photo description
    private FormBody buildBody(){
        FormBody body = new FormBody.Builder()
                .add("photoDescription", photoDescription.getText().toString())
                .build();
        return body;
    }
    //this function goes through the array of people who have been tagged in the photo and adds buttons to be able to remove them from the photo
    public void getTaggedPeople(final JSONArray taggedPeople){
        for (int i = 0; i < taggedPeople.length(); i++){//cycle through each key contained in array, get that particular person from API server database
            final int person = i;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    HttpUrl url;
                    try{
                        url = buildURL(action_single_person,userKey,taggedPeople.get(person).toString());//build url with proper url function
                        Headers headers = getHeader();
                        Request request = new Request.Builder()//build the request
                                .url(url)
                                .headers(headers)
                                .get()
                                .build();

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
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {//changing textView must be done on UI thread
                                    try{
                                        JSONObject object = new JSONObject(response.body().string());//get object of body

                                        PicassoButton button = new PicassoButton(getApplicationContext());//used as temp place holder--studio can't find Button
                                        String name = object.get("firstName").toString() + " " + object.get("lastName");//add persons name to button, along with remove note so we know what action we're taking
                                        button.setText("Remove " + name);
                                        button.setTag(object.get("key"));//add key as tag for button
                                        button.setOnClickListener(listenRemove(button));//set listener
                                        button.setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                        personList.addView(button);//add button to proper list

                                    }
                                    catch(Exception e){
                                        message.setText(e.toString());
                                    }
                                }
                            });
                        }

                    }
                    catch(final Exception e){
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
    //listener to be able to add person to photo
    View.OnClickListener listenAdd(final PicassoButton button){
        return new View.OnClickListener(){
            public void onClick(View v){
                addPerson(v.getTag().toString());
            }
        };
    }
    //function to add person to photo, needs to also set remove button in appropriate list
    private void addPerson(final String personKey){
        new Thread(new Runnable() {
            @Override
            public void run() {

                HttpUrl url = buildURL(action_change_tagged,userKey,photoKey,personKey);//get proper URL
                Headers headers = getHeader();
                Request request = new Request.Builder()
                        .url(url)
                        .headers(headers)
                        .method("put",null)//need to use custom method put, okhttp doesn't support it naturally
                        .build();

                try{
                    final okhttp3.Response response;//send request, get response
                    response = client.newCall(request).execute();
                    if (!response.isSuccessful()){//something wrong happened
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {//changing textView must be done on UI thread
                                try{
                                    message.setText("There was an error in receiving response from server\n" + response.toString());
                                }
                                catch(Exception e){
                                    message.setText(e.toString());
                                }

                            }
                        });
                    }
                    else{
                        final JSONObject object = new JSONObject(response.body().string());//get object from response
                        if (object.get("result").toString().equals("fail")){
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        message.setText("Message from Server: " + object.get("cause").toString());
                                    }
                                    catch (Exception e){
                                        message.setText(e.toString());
                                    }

                                }
                            });
                        }
                        else if (object.get("result").toString().equals("success")){//successfully add person to photo
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        personsNotInPhoto.removeView(personsNotInPhoto.findViewWithTag(personKey));
                                        PicassoButton button = new PicassoButton(getApplicationContext());//used as temp place holder--studio can't find Button
                                        String name = object.get("personName").toString();//put name in button
                                        button.setText("Remove " + name);//along with remove
                                        button.setTag(personKey);//set tag to be key
                                        button.setOnClickListener(listenRemove(button));
                                        button.setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                        personList.addView(button);//add button
                                        message.setText("Message from server: " + object.get("result").toString());//set message for user
                                    }
                                    catch(Exception e){
                                        message.setText(e.toString());
                                    }

                                }
                            });
                        }
                        else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    message.setText(object.toString());
                                }
                            });
                        }
                    }
                }
                catch(final Exception e){
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
    //listener for remove buttons
    View.OnClickListener listenRemove(final PicassoButton button){
        return new View.OnClickListener(){
            public void onClick(View v){
                removePerson(v.getTag().toString());
            }
        };
    }
    //function to remove person from photo, needs to add button to add person list for the removed person
    private void removePerson(final String personKey){
        new Thread(new Runnable() {
            @Override
            public void run() {
                //generate request
                HttpUrl url = buildURL(action_change_tagged,userKey,photoKey,personKey);
                Headers headers = getHeader();
                Request request = new Request.Builder()
                        .url(url)
                        .headers(headers)
                        .delete()
                        .build();

                try{
                    final okhttp3.Response response;
                    response = client.newCall(request).execute();
                    if (!response.isSuccessful()){//something wrong happened
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {//changing textView must be done on UI thread
                                try{
                                    message.setText("There was an error in receiving response from server\n" + response.toString());
                                }
                                catch(Exception e){
                                    message.setText(e.toString());
                                }

                            }
                        });
                    }
                    else{
                        final JSONObject object = new JSONObject(response.body().string());//get response, will be JSON object
                        if (object.get("result").toString().equals("fail")){//if we fail, print cause
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        message.setText("Message from Server: " + object.get("cause").toString());
                                    }
                                    catch (Exception e){
                                        message.setText(e.toString());
                                    }

                                }
                            });
                        }
                        else if (object.get("result").toString().equals("success")){//if we get success message
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    try{
                                        personList.removeView(personList.findViewWithTag(personKey));//remove person from the person view
                                        PicassoButton button = new PicassoButton(getApplicationContext());//used as temp place holder--studio can't find Button
                                        String name = object.get("personName").toString();
                                        button.setText("Add " + name);//add their name
                                        button.setTag(object.get("personKey"));//set the tag to be the person key
                                        button.setOnClickListener(listenAdd(button));
                                        button.setLayoutParams(new ActionBar.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT));
                                        personsNotInPhoto.addView(button);//add person button to the list of those not in photo once they've been removed
                                        message.setText("Message from server: " + object.get("result").toString());
                                    }
                                    catch(Exception e){
                                        message.setText(e.toString());
                                    }

                                }
                            });
                        }
                        else{
                            runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    message.setText(object.toString());
                                }
                            });
                        }
                    }
                }
                catch(final Exception e){
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
    //listener for call to delete the image
    public void listenDelete(View view){
        new Thread(new Runnable() {
            @Override
            public void run() {
                HttpUrl url = buildURL("delete_person",userKey,photoKey);//url call to delete person
                Headers headers = getHeader();
                Request request = new Request.Builder()
                        .url(url)
                        .headers(headers)
                        .delete()//need delete method
                        .build();

                try {
                    final okhttp3.Response response;
                    response = client.newCall(request).execute();

                    if (!response.isSuccessful()){
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                message.setText("Error in response from server" + response.toString());
                            }
                        });
                    }
                    else{
                        final JSONObject object = new JSONObject(response.body().string());
                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                try{
                                    message.setText("Message from Server: " + object.get("result").toString());
                                    if (object.get("result").toString().equals("success")){
                                        Toast.makeText(getApplicationContext(), "Deleted: " + object.get("deleted").toString(), Toast.LENGTH_SHORT).show();//set TOAST because the instance will be closed so that user won't see display message
                                        finish();//finsish instance one the person no longer exists
                                    }
                                    else if (object.get("result").toString().equals("fail")){//print the error message if we fail to delete the person
                                        message.setText(message.getText() + "\n" + object.get("cause").toString());
                                    }
                                    else{//print the whole response if we neither fail nor succeed to delete the person
                                        message.setText(object.toString());
                                    }
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

}



