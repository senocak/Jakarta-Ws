# request.http
```
###
GET http://localhost:8080/

###
WEBSOCKET ws://localhost:8080/chat/username1

###
WEBSOCKET ws://localhost:8080/chat/username1
Content-Type: application/json

===
{
"msg": "msg1"
}
=== wait-for-server
=== wait-for-server
{
"msg": "msg2"
}
```