# The sample workshop application

For the hands-on workshop labs you will be provided a complete spring mvc web server application together
with a corresponding spring mvc thymeleaf web client app.    

__Table of Contents__

* [Application Component View](#application-components)
* [Server Architecture](#server-architecture)
  * [REST Api](#rest-api)
  * [Server Layers](#server-layers)
  * [Users and Roles](#server-users-and-roles)
  * [Provided application](#provided-server-application)
* [Client Architecture](#client-architecture)
  * [Client Layers](#client-layers)
  * [Users and Roles](#client-users-and-roles)
  * [Provided application](#provided-client-application)

## Application Components

The server application provides a RESTful service for administering books and users 
(a very _lightweight_ books library).

Use cases of this application are:

* Administer books (Creating/editing/deleting books)
* List available books
* Borrow a book
* Return a borrowed book
* Administer library users 

## Server Architecture

The RESTful service for books and users is build using the Spring MVC annotation model and Spring HATEOAS.

The application also contains a complete documentation for the RESTful API that is automatically 
generated with spring rest docs. You can find this in the directory _'build/asciidoc/html5'_ after performing a full 
gradle build or online here: [REST API documentation](https://andifalk.github.io/cloud-native-microservices-security/api-doc.html).

The initial server application is not secured at all. 

### Server Layers

The domain model of the server application is quite simple and just consists of _Book_ and _User_ models.   
The packages of the application are organized according to the different application layers:

* __web__: Contains the complete RESTful service
* __service__: The service classes (quite simple for workshop, usually these contain the business logic)
* __data__: All domain models and repositories

Each layer is organized the same way for the _book_ and _user_ domains.

In addition there more packages with supporting functions:

* __common__: Classes that are reused in multiple other packages
* __config__: All spring configuration classes

### REST API

To call the provided REST API you can use curl or httpie. 
For details on how to call the REST API please consult the [REST API documentation](https://andifalk.github.io/cloud-native-microservices-security/api-doc.html) 
which also provides sample requests for curl and httpie.

### Server Users and roles

There are three target user roles for this application:

* LIBRARY_USER: Standard library user who can list, borrow and return his currently borrowed books
* LIBRARY_CURATOR: A curator user who can add, edit or delete books
* LIBRARY_ADMIN: An administrator user who can list, add or remove users

__Important:__ We will use the following users in all subsequent labs from now on:

| Username | Email                    | Password | Role            |
| ---------| ------------------------ | -------- | --------------- |
| bwayne   | bruce.wayne@example.com  | wayne    | LIBRARY_USER    |
| bbanner  | bruce.banner@example.com | banner   | LIBRARY_USER    |
| pparker  | peter.parker@example.com | parker   | LIBRARY_CURATOR |
| ckent    | clark.kent@example.com   | kent     | LIBRARY_ADMIN   |

These users are automatically created and persisted upon application start.
We will these users later for implementing the different authentication mechanisms.

### Provided Server application

You can find the provided initial server application beneath the [lab 1 folder](../lab1) as 
[library-server](../lab1/library-server).

