# authentication

Run sample app

    workgroup-2:autok starosud$ sbt "run --mock"
    [info] Loading project definition from /Users/starosud/Work/xfiles/autok/project
    ...
    Token Server mock started at port: 51067
    DateTime Service mock started at port: 51068
    Enter empty line to exit
    URL> http://localhost:51068/datetime
    Success(HttpResponse({"datetime":"2015-05-15T06:03:19"},200,Map(Server -> Jetty(6.1.26), Status -> HTTP/1.1 200 OK, Transfer-Encoding -> chunked)))
