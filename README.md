# taskdj
A full Android app with Firebase

My first project. It is a fully functional app and tested on Google Play for a couple of years.

The premise was to create an app that allows people to become friends and give each other assignments. 
The assignments may have a monetary reward (symbolic). 

- In short: how to get your kids to clean their room :-)

I addition I experimented with various Google features such as purchase, advertising, crash analytics.
I am not a designer and the GUI is somewhat old fashioned.


This application was built with Android Studio. It uses AndroidX.
To build the app you will need to create a firebase account.
It uses the Realtime database (not Firestore).
It also uses Crashlytics and other features.
Authentication is email and Google.

In addition I use one Google function as it is not possible to send notification from 
one phone to another directly. This function will trigger on changes to a table and send notifications
to the chosen devices, sendNotifications:

<pre>
const functions = require('firebase-functions');
const admin = require('firebase-admin'); 

admin.initializeApp(functions.config().firebase); 
exports.sendNotifications = 
	functions.database.ref('/taskdj-pro/messages').onWrite(event => {

		if (event.before.exists()) {
	        	return null;
	      	}

		if (!event.after.exists()) {
	        	return null;
		}

		const val = event.after.val();

		const textVal = val.msg; 
		const destVal = val.dest;
		console.log(textVal); 
		console.log(destVal); 
		const payload = {
			notification: { title: "TaskDJ", body: textVal },
			android: { 
				notification: { "sound" : "default" ,
					       "icon" : "R.mipmap.ic_launcher" } 
			},
			token: destVal
		}; 
		return admin.messaging().send(payload)
			.then((response) => { 
				console.log("Successfully sent message", response); 
				return;
			})
			.catch((error) => {
				console.log("Error sending message", error); 
			}); 
	});
</pre>
