import * as functions from "firebase-functions";
import * as admin from "firebase-admin";
import * as geolib from "geolib";


admin.initializeApp();

export const updateUserData = functions.firestore
    .document("Users/{userId}/DetectionData/Data")
    .onUpdate(async (change, context) => {
      const newDataUser = change.after.data();

      // ...or the previous value before this update
      const preDataUser = change.before.data();

      // access a particular field as you would any JS property
      const city = newDataUser.City;
      const street = newDataUser.Street;
      const latitudeUser = newDataUser.Latitude;
      const longitudeUser = newDataUser.Longitude;
      let detections = newDataUser.Detections;
      const time = newDataUser.Time;
      const bearing = newDataUser.Bearing;
      const biasOffsetDistance=8;

      const newLocation = geolib.computeDestinationPoint(
          {latitude: latitudeUser, longitude: longitudeUser},
          biasOffsetDistance,
          bearing);

      const newLatitude = newLocation.latitude;
      const newLongitude = newLocation.longitude;

      if (detections > 2) {
        detections = 2;
      }

      let totalAvailableParking = 0;
      let totalNumOfParking = 0;

      const pathStreet = admin.firestore()
          .doc("City/"+city+"/Street/"+street);
      const pathParking = admin.firestore()
          .collection("City/"+city+"/Street/"+street+"/Parking");


      const genericCityFiled = {
        "demy": ";)",
      };

      const genericParking = {
        "Latitude": latitudeUser,
        "Longitude": longitudeUser,
        "Available": 0,
        "PotentialParkingSpots": detections,
        "Time": time,
      };

      const genericParkingDoc = {
        "TimeStamp": time,
        "Detections": detections,
      };

      const genericStreetField = {
        "TotalParking": 0,
        "AvailableParking": 0,
        "Capacity": 100,
        "LastUpdate": time,
      };

      if ((newDataUser !== preDataUser) &&
       (latitudeUser !==0 ) && (longitudeUser !== 0) ) {
        functions.logger.log("Update Started");

        // search parking
        const sStreet = await pathStreet.get();
        if (sStreet.exists) {
          let flagWithBearing=0;
          let flagWithoutBearing=0;
          let pName;
          let pCheckAvailable;
          const pParking = await pathParking.get();
          pParking.forEach(async function(park) {
            const parkName = park.id;
            const pLatitude = park.get("Latitude");
            const pLongitude = park.get("Longitude");
            const pPotentialPark = park
                .get("PotentialParkingSpots");
            const pAvailable = park.get("Available");

            // calculate distance

            const distanceWithoutBearing = geolib.getDistance(
                {latitude: latitudeUser, longitude: longitudeUser},
                {latitude: pLatitude, longitude: pLongitude}
            );
            const distanceWithBearing = geolib.getDistance(
                {latitude: newLatitude, longitude: newLongitude},
                {latitude: pLatitude, longitude: pLongitude}
            );

            // calculate total Potential Parking
            totalNumOfParking =
                        totalNumOfParking+pPotentialPark;

            if (distanceWithoutBearing < 2.5 &&
                          flagWithoutBearing === 0) {
              flagWithoutBearing = 1;
            }

            if (pPotentialPark !== 0) {
              if (distanceWithBearing < 2.5 &&
                             flagWithBearing === 0) {
                let checkAvailable =
                            pPotentialPark-detections;
                if (checkAvailable<0) {
                  checkAvailable = 0;
                }

                pName=parkName;
                pCheckAvailable=checkAvailable;
                flagWithBearing = 1;
                totalAvailableParking =
                            totalAvailableParking+checkAvailable;
              } else {
                totalAvailableParking =
                            totalAvailableParking+pAvailable;
              }
            }
          });// end of parking loop

          if (flagWithBearing === 1) {
            functions.logger.
                log("Parking Exists, Updating Progress..");
            await admin.firestore()
                .doc("City/"+city+"/Street/"+street+
                "/Parking/"+pName)
                .update(
                    {"Available":
                          (pCheckAvailable), "Time": time});
            await admin.firestore().collection(
                "City/"+city+"/Street/"+street+
                  "/Parking/"+pName+"/Archive")
                .add(genericParkingDoc);
          }
          const parkingLength = pParking.size;
          if (flagWithBearing === 0 && detections!==
                         0 && flagWithoutBearing === 0) {
            functions.logger
                .log("Parking Dosen't Exist, Adding a new Parking");
            if (detections !== 0) {
              const parkNum = parkingLength+1;
              const parkingName:string = "parking_"+ parkNum;
              await admin.firestore().collection(
                  "City/"+city+"/Street/"+street+
                          "/Parking/"+parkingName+"/Archive")
                  .doc().set(genericParkingDoc);
              await admin.firestore().collection(
                  "City/"+city+"/Street/"+street+"/Parking")
                  .doc(parkingName).set(genericParking);
            }
          } else {
            if (flagWithoutBearing === 1) {
              functions.logger.
                  log("Parking Already Exists..");
            } else {
              functions.logger.
                  log("Ignoring Parking - Potential Parking Is 0");
            }
          }
        } else {
          if (detections !== 0) {
            functions.logger.
                log("Street Dosen't Exist, Adding a new Street");

            await admin.firestore()
                .collection("City/").doc(city).set(genericCityFiled);
            await admin.firestore()
                .collection("City/"+city+"/Street/")
                .doc(street).collection("Parking")
                .doc("parking_1").set(genericParking);
            await admin.firestore()
                .collection("City/"+city+"/Street/")
                .doc(street).set(genericStreetField);
            await admin.firestore().collection(
                "City/"+city+"/Street/"+street+
                "/Parking/parking_1/Archive")
                .doc().set(genericParkingDoc);
          } else {
            functions.logger.
                log("Ignoring Parking - Potential Parking Is 0");
          }
        } // end of street

        // calculate street capacity
        const capacity =
          100 - (totalAvailableParking/totalNumOfParking)*100;
        functions.logger.
            log("total av:"+totalAvailableParking+
        "  total parking:"+totalNumOfParking+"CAP:"+capacity);

        admin.firestore()
            .collection("City/"+city+"/Street/")
            .doc(street).set({
              "Capacity": capacity,
              "AvailableParking": totalAvailableParking,
              "TotalParking": totalNumOfParking,
              "LastUpdate": time,
            });
      }
    });

// schedule function
exports.scheduledFunction = functions.pubsub.
    schedule("every 720 minutes").onRun(async (context) => {
      console.log("This will run every 12 Hours!");

      const TWO_WEEKS_IN_SECONDS = 14*24*60*60;
      // const oneHour = 60*60;
      const currentTime = admin.firestore.Timestamp.now();
      const agoDate = new admin.firestore.Timestamp(
          currentTime.seconds - TWO_WEEKS_IN_SECONDS,
          currentTime.nanoseconds);

      const street = await admin.firestore()
          .collection("City/Tel Aviv-Yafo/Street").get();
      street.forEach(async function(st) {
        const stName = st.id;

        const parking = await admin.firestore()
            .collection("City/Tel Aviv-Yafo/Street/" +
                    stName + "/Parking").get();
        parking.forEach(async function(park3) {
          const parkName = park3.id;
          const parkAray = [0, 0, 0];

          const archive = await admin.firestore().
              collection("City/Tel Aviv-Yafo/Street/" +
                              stName + "/Parking/" + parkName + "/Archive")
              .get();
          archive.
              forEach(async function(ptimeStamp) {
                const pTime = ptimeStamp
                    .updateTime.toDate();

                const pDetection = ptimeStamp
                    .get("Detections");

                if (pTime >= agoDate.toDate()) {
                  // functions.logger.
                  //     log("dedection: "+
                  //     pDetection+
                  // " timeStamp: "+pTime +
                  //  " agoDate: "+agoDate
                  //         .toDate()+
                  //  " now: "+ currentTime
                  //         .toDate());
                  parkAray[pDetection] += 1;
                }
              });

          // end Archive loop

          const potentialParkingSpots =
                              parkAray.indexOf(Math.max(...parkAray));

          // functions.logger.
          //     log("Street: "+stName +
          //      " Parking Number: " + parkName +
          //     " Array of parking: "+
          //     parkAray+" max elements = "+
          //     Math.max(...parkAray)+
          //     " index: "+
          //     potentialParkingSpots);
          // update the Potential Park
          await admin.firestore()
              .doc("City/Tel Aviv-Yafo/Street/" + stName +
                              "/Parking/" + parkName)
              .update(
                  {"PotentialParkingSpots":
                                       potentialParkingSpots});
          const availableParking = park3.get("Available");

          if (potentialParkingSpots < availableParking) {
            await admin.firestore()
                .doc("City/Tel Aviv-Yafo/Street/" + stName +
                                "/Parking/" + parkName)
                .update(
                    {"Available": potentialParkingSpots});
          }
        }); // end parking loop
      }
      );
    }); // end street loop

// return null;

// leaving parking

exports.leavingParking = functions.https.onCall((data, context) => {
  const userId = context.auth?.uid;
  functions.logger.log("UserId: " + userId);
  const city = data.City;
  const street = data.Street;
  const latitudeUser = data.Latitude;
  const longitudeUser = data.Longitude;
  functions.logger.log("City: " + city);


  const currentTime = admin.firestore.Timestamp.now();

  // definition of paths
  const pathStreet = admin.firestore()
      .doc("City/"+city+"/Street/"+street);
  const pathParking = admin.firestore()
      .collection("City/"+city+"/Street/"+street+"/Parking");

  let flag=0;
  functions.logger.log("City: " + city+
    " Street: "+street+" lat: "+
    latitudeUser+" long: "+longitudeUser);

  // get parking by user latitude and longitude

  pathStreet.get().then(
      (streetPromise) => {
        if (streetPromise.exists) {
          pathParking.get().then(
              (parkingPromise) => {
                parkingPromise.forEach(async function(park) {
                  const parkName = park.id;
                  const pLatitude = park.get("Latitude");
                  const pLongitude = park.get("Longitude");
                  const pAvailable = park.get("Available");
                  const pPotentialPark = park
                      .get("PotentialParkingSpots");
                  const distance = geolib.getDistance(
                      {latitude: latitudeUser, longitude: longitudeUser},
                      {latitude: pLatitude, longitude: pLongitude}
                  );

                  if (pPotentialPark !== 0) {
                    if (distance < 2.5 && flag===0) {
                      if (pAvailable < 2 && pAvailable !== pPotentialPark) {
                        const leaveMeasage =
                               "Updating Parking: "+parkName+ ", pPot: "+
                               pPotentialPark+", pAv: "+pAvailable+
                                ", User Left his parking..";
                        functions.logger.
                            log("Parking Exists, "+leaveMeasage);
                        admin.firestore()
                            .doc("City/"+city+"/Street/"+street+
                    "/Parking/"+parkName)
                            .update(
                                {"Available":
                              (pAvailable+1), "Time": currentTime});
                        flag=1;
                      } else {
                        admin.firestore()
                            .doc("City/"+city+"/Street/"+street+
                    "/Parking/"+parkName)
                            .update(
                                {"Time": currentTime});
                        functions.logger.
                            log("Parking: "+parkName+" Is Already Available, " +
                            " pPot: "+
                            pPotentialPark+", pAv: "+pAvailable);
                      } // end if pAvailable < 2
                    }
                  } else {
                    functions.logger
                        .log("Parking Is Not Valid.. :(");
                  }
                });// end of parking loop
              });// end find path park to update
        } else {
          functions.logger.
              log("Street Dosen't Exist...");
        }
      }); // end of street
}); // function leavingParking


