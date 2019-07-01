# Imaging API for Interstellar project
This project was created specifically for the Interstellar team. The main objective of the project
is to search for Satellite images that represent different sensor bands data and combine it into a single, 
human-readible image. 


## Prerequisites
- Java 8 
- Maven 3.6.1 

## Configuration
- Application properties are listed in Application.properties. You need to define the folder where the sensor bands
information is stored. The property is: 
    granules.path
    
##Executing 
Follow these steps 

- Navigate to the root of the project 

run
```sh
$ mvn install
$ mvn spring-boot:run
```

Application will start in port 8080

##Testing

You can test the application by making a POST request to the endpoint http://localhost:8080/generate-image

Ej. of body: 

{
"utmZone": 33,
"latitudeBand": "U",
"gridSquare": "UP",
"date": "2018-08-04",
"channelMap": "visible"
}
    



